# score + narration + foley → out/mix.wav (48 kHz stereo). Run from anywhere: .venv/bin/python mix.py
import json, os, sys, numpy as np, soundfile as sf
HERE = os.path.dirname(os.path.abspath(__file__)); LIB = os.environ.get('LIB', '/root/lemo-opuscar')
sys.path.insert(0, os.path.join(LIB, 'core', 'audio'))
import sfx as X, pluck as P
from scipy.signal import butter, sosfilt, fftconvolve, resample_poly
SR = X.SR; DUR = 25.0; N = int(DUR * SR); rng = np.random.default_rng(5)
BEAT = 60 / 96; BAR = 4 * BEAT
ev = json.load(open(os.path.join(HERE, 'events.json')))['ev']

def st(n=N): return np.zeros((n, 2), np.float32)
def lp(x, f): return sosfilt(butter(2, f, 'low', fs=SR, output='sos'), x)
def hp(x, f): return sosfilt(butter(2, f, 'high', fs=SR, output='sos'), x)
def bp(x, a, b): return sosfilt(butter(2, [a, b], 'band', fs=SR, output='sos'), x)
IRn = int(1.6 * SR); IR = rng.standard_normal((IRn, 2)) * np.exp(-np.arange(IRn) / SR * 3.6)[:, None]
IR = np.stack([lp(IR[:, 0], 5500), lp(IR[:, 1], 5500)], 1); IR /= np.sqrt((IR ** 2).sum(0))
def verb(x, wet=.25):
    y = np.stack([fftconvolve(x[:, 0], IR[:, 0])[:len(x)], fftconvolve(x[:, 1], IR[:, 1])[:len(x)]], 1)
    return x * (1 - wet) + y * wet
def rms(x): return np.sqrt(np.mean(x ** 2) + 1e-12)

# ---------------- music (96 bpm, one bar = 2.5 s) ----------------
mus = st()
CH = {'C': ['C', 'E', 'G'], 'Am': ['A', 'C', 'E'], 'F': ['F', 'A', 'C'], 'G': ['G', 'B', 'D'], 'Dm': ['D', 'F', 'A'], 'Em': ['E', 'G', 'B']}
PROG = ['C', 'Am', 'F', 'G', 'C', 'Am', 'F', 'Dm', 'F', 'C']
def tones(ch, octv): return [f'{n}{octv}' for n in CH[ch]]
SILENT = [(9.55, 10.2), (20.5, 21.25)]
def ok(t): return not any(a <= t < b for a, b in SILENT)
def put(x, t, g=1., pan=0.):
    if ok(t): X.add(mus, x, t, g, pan)

for b, ch in enumerate(PROG):
    t0 = b * BAR; T = tones(ch, 5); L = tones(ch, 4)
    # kalimba: the thread through the whole film
    pat = [(0, T[0]), (1.5, T[2]), (2, T[1]), (3, T[2])] if b < 2 else [(0, T[0]), (.5, T[1]), (1.5, T[2]), (2, T[1]), (2.5, T[0]), (3.5, T[2])]
    if b == 7: pat = [(0, T[0]), (2, T[2])]
    for k, (bt, n) in enumerate(pat):
        put(P.pluck('kalimba', n, None, .55 + .1 * (k == 0)), t0 + bt * BEAT, .55, rng.uniform(-.3, .3))
    # guitar: one pluck per beat while the mountains paint in, then gentle arpeggios
    if 2 <= b <= 6:
        for bt in range(4):
            n = [f'{CH[ch][0]}3', L[1], L[2], L[1]][bt]
            put(P.pluck('nylon_guitar', n, 1.2, .62), t0 + bt * BEAT, .5, -.25)
    # bass on 1 and 3 from the reveal
    if 4 <= b <= 7:
        put(P.pluck('upright_bass_pizz', f'{CH[ch][0]}2', 1.0, .7), t0, .6)
        put(P.pluck('upright_bass_pizz', f'{CH[ch][2]}2', 1.0, .6), t0 + 2 * BEAT, .5)
# music box: the burst, the reveal, the title
for i, n in enumerate(['C6', 'E6', 'G6', 'C7']): put(P.pluck('music_box', n, None, .5), 8.45 + i * .11, .45, .3)
for i, n in enumerate(['G5', 'C6', 'E6', 'G6', 'A6']): put(P.pluck('music_box', n, None, .45), 10.25 + i * .15, .4, -.3)
X.add(mus, P.pluck('music_box', 'C7', None, .55), 21.3, .5, .1)
X.add(mus, P.strum('nylon_guitar', ['F2', 'C3', 'F3', 'A3', 'C4'], 2.0, .6, spread=.035), 21.3, .7, -.15)
X.add(mus, P.strum('nylon_guitar', ['C3', 'G3', 'C4', 'E4', 'G4'], 3.5, .55, spread=.045), 22.5, .7, -.15)
X.add(mus, P.strum('kalimba', ['C5', 'E5', 'G5', 'C6'], None, .5, spread=.09), 22.5, .6, .2)
# a soft pad from the reveal to the night (sine + detuned partials, low-passed)
def pad(notes, d, g=.05):
    tt = np.arange(int(d * SR)) / SR; y = np.zeros_like(tt)
    for n in notes:
        f = 440 * 2 ** ((P.midi(n) - 69) / 12)
        for det in (-.12, .1): y += np.sin(2 * np.pi * f * 2 ** (det / 12) * tt) + .25 * np.sin(4 * np.pi * f * tt)
    e = np.minimum(1, tt / .6) * np.minimum(1, (d - tt) / .8)
    return lp(y * e, 2200) * g
for b in range(4, 8):
    ch = PROG[b]; X.add(mus, pad(tones(ch, 3) + [f'{CH[ch][0]}4'], BAR + .3, .03 if b < 7 else .022), b * BAR + .1, 1, 0)
g = np.ones(N, np.float32); tt = np.arange(N) / SR
g *= np.where((tt >= 9.55) & (tt < 10.2), np.clip(1 - (tt - 9.55) / .5, 0, 1) ** 2, 1)    # brush lifted: let the tails go
g *= np.where((tt >= 20.5) & (tt < 21.25), np.clip(1 - (tt - 20.5) / .5, .03, 1), 1)
mus *= g[:, None]
tail = int(1.2 * SR); mus[-tail:] *= np.linspace(1, 0, tail)[:, None] ** 1.5
mus = verb(mus, .28)

# ---------------- foley from the picture's events ----------------
fx = st()
def brushs(d, g, p0, p1):
    n = int(d * SR); p = np.arange(n) / n
    x = bp(rng.standard_normal(n), 1600, 7000) * (.6 + .4 * np.abs(np.sin(np.arange(n) / SR * 2 * np.pi * 8)))
    x += bp(rng.standard_normal(n), 300, 1400) * .4
    y = X.norm(x * np.sin(np.pi * p) ** .8, .3) * g; pan = p0 + (p1 - p0) * p
    return np.stack([y * np.cos((pan + 1) * np.pi / 4) * 1.414, y * np.sin((pan + 1) * np.pi / 4) * 1.414], 1)
def pencil(d):
    n = int(d * SR); x = hp(rng.standard_normal(n), 3000) * (rng.random(n) < .5) * .5
    x = bp(x, 3000, 9000) * (.5 + .5 * np.abs(np.sin(np.arange(n) / SR * 2 * np.pi * 14))) * np.sin(np.pi * np.arange(n) / n)
    return X.norm(x, .12)
def addst(buf, x2, t):
    i = int(t * SR); j = min(len(buf), i + len(x2)); buf[i:j] += x2[:j - i]
for e in ev:
    t, ty, d = e['t'], e['type'], e.get('d', .5)
    if ty == 'brush': addst(fx, brushs(d, e.get('g', .5), e.get('pan', 0) - .3, e.get('pan', 0) + .3), t)
    elif ty == 'pencil': X.add(fx, pencil(d), t, .9, .3)
    elif ty == 'swell': addst(fx, brushs(d, .5, -.9, .9), t)
    elif ty == 'flame': X.add(fx, X.norm(lp(X.thump(.6, 85), 900), .35), t, 1, 0); X.add(fx, X.norm(hp(rng.standard_normal(int(.25 * SR)), 2500) * (rng.random(int(.25 * SR)) < .02), .12), t + .03, 1, .2)
    elif ty == 'bloom': X.add(fx, lp(X.whoosh(d, .35), 2500), t, 1, 0)
    elif ty == 'dabs':
        for k in range(12): X.add(fx, X.norm(lp(rng.standard_normal(int(.05 * SR)) * np.exp(-np.arange(int(.05 * SR)) / SR * 80), 2200), .1), t + k * .06, 1, rng.uniform(-.5, .5))
    elif ty == 'crickets':
        n = int(d * SR); tt = np.arange(n) / SR; y = np.zeros(n)
        for f, r, ph in ((4400, 3.1, 0), (4900, 2.6, 1.3)):
            y += np.sin(2 * np.pi * f * tt) * (np.sin(2 * np.pi * 30 * tt) > .2) * (np.sin(2 * np.pi * r * tt + ph) > .55)
        y *= np.minimum(1, tt / 1.2) * np.minimum(1, (d - tt) / 1.0)
        X.add(fx, y.astype(np.float32) * .018, t, 1, -.4); X.add(fx, np.roll(y, 9000).astype(np.float32) * .014, t, 1, .5)
    elif ty == 'ding': X.add(fx, X.ding(.25), t, 1, .1)
    elif ty == 'mist': X.add(fx, lp(X.whoosh(d, .3), 1800), t, 1, 0)
room = lp(rng.standard_normal(N), 380) * .006; fx[:, 0] += room; fx[:, 1] += np.roll(room, 4000)
fx = verb(fx, .18)

# ---------------- narration ----------------
VO = {'l1': .7, 'l2': 5.0, 'l3': 7.75, 'l4': 10.6, 'l5': 17.6, 'l6': 21.6}
vo = np.zeros(N, np.float32); vmask = np.zeros(N, np.float32)
for k, t in VO.items():
    x, sr = sf.read(os.path.join(HERE, 'voice', 'out', f'{k}.wav'), dtype='float32')
    if x.ndim > 1: x = x.mean(1)
    x = resample_poly(x, SR, sr).astype(np.float32)
    i = int(t * SR); vo[i:i + len(x)] += x; vmask[i:i + len(x)] = 1
# warm it: a little low-mid body, soften the hiss, compress, limit
vo = hp(vo, 85); vo = vo + .25 * bp(vo, 180, 420) - .15 * hp(vo, 7500)
vo = X.compress(vo / (np.abs(vo).max() + 1e-9) * .9, thr=.22, ratio=3.2); vo = X.limit(vo, .9)
vo2 = verb(np.stack([vo, vo], 1), .1)

# ducking: music 5 dB down under the voice, smoothed
k = int(.25 * SR); env = np.convolve(vmask, np.ones(k) / k, 'same')
mus *= (1 - (1 - 10 ** (-5 / 20)) * np.clip(env, 0, 1))[:, None]
# balance: voice ≈ 10 dB over the music (RMS where the voice speaks)
m = vmask > 0
gm = rms(vo2[m]) / (rms(mus[m]) * 10 ** (10 / 20)); mus *= gm; fx *= gm * 1.3
mix = vo2 + mus + fx
mix = np.stack([X.limit(mix[:, 0], .95), X.limit(mix[:, 1], .95)], 1)
os.makedirs(os.path.join(HERE, 'out'), exist_ok=True)
sf.write(os.path.join(HERE, 'out', 'mix.wav'), mix.astype(np.float32), SR)
bands = lambda x, a, b: 20 * np.log10(rms(bp(x, a, b)) + 1e-12)
print('mix written', round(len(mix) / SR, 2), 's; low 20–120 Hz vs 120–4k:', round(bands(mix[:, 0], 25, 120) - bands(mix[:, 0], 120, 4000), 1), 'dB')
