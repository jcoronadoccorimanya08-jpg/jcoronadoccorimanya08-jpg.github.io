# score + foley + narration → out/mix.wav (48 kHz stereo). Everything is synthesised here, from events.json.
import json, os, sys, numpy as np, soundfile as sf
HERE = os.path.dirname(os.path.abspath(__file__)); LIB = os.environ.get('LIB', '/root/lemo-opuscar')
sys.path.insert(0, os.path.join(LIB, 'core', 'audio'))
import sfx as X
from scipy.signal import butter, sosfilt, fftconvolve, resample_poly
SR = X.SR; DUR = 25.0; N = int(DUR * SR); rng = np.random.default_rng(21)
ev = json.load(open(os.path.join(HERE, 'events.json')))['ev']
FREEZE, PRESS = 5.6, 6.4

def lp(x, f): return sosfilt(butter(2, f, 'low', fs=SR, output='sos'), x)
def hp(x, f): return sosfilt(butter(2, f, 'high', fs=SR, output='sos'), x)
def bp(x, a, b): return sosfilt(butter(2, [a, b], 'band', fs=SR, output='sos'), x)
def hz(n):
    if isinstance(n, (int, float)): return 440 * 2 ** ((n - 69) / 12)
    names = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}; s = 1 if '#' in n else 0
    return 440 * 2 ** ((names[n[0]] + s + 12 * (int(n[-1]) + 1) - 69) / 12)
def tt(d): return np.arange(int(d * SR)) / SR
def mallet(note, kind='vib', v=.8, cents=0.0, d=None):
    f = hz(note) * 2 ** (cents / 1200)
    P = {'mar': ([1, 3.93, 9.2], [1, .3, .08], [.45, .1, .04]), 'vib': ([1, 4.0, 10.1], [1, .18, .05], [2.4, .5, .15]),
         'glk': ([1, 2.76, 5.4], [1, .4, .15], [1.3, .45, .18])}[kind]
    d = d or max(P[2]) * 2.6; t = tt(d); y = np.zeros_like(t)
    for r, a, tau in zip(*P):
        if f * r < SR / 2.2: y += a * np.sin(2 * np.pi * f * r * t) * np.exp(-t / tau)
    if kind == 'vib': y *= 1 + .22 * np.sin(2 * np.pi * 5.4 * t)
    att = np.minimum(1, t / .002); click = hp(rng.standard_normal(len(t)), 2500) * np.exp(-t / .004) * .08
    return ((y * att + click) * v * .3).astype(np.float32)
def pad(notes, d, g=.04):
    t = tt(d); y = np.zeros_like(t)
    for n in notes:
        f = hz(n)
        for det in (-.08, .07): y += np.sin(2 * np.pi * f * 2 ** (det / 12) * t)
    return (lp(y * np.minimum(1, t / .5) * np.minimum(1, (d - t) / .6), 1800) * g).astype(np.float32)
def sine_drop(f0, f1, d, v):
    t = tt(d); f = f1 + (f0 - f1) * np.exp(-t / (d * .25)); return (np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / (d * .4)) * v).astype(np.float32)
def st(): return np.zeros((N, 2), np.float32)
IRn = int(1.4 * SR); IR = rng.standard_normal((IRn, 2)) * np.exp(-np.arange(IRn) / SR * 4.2)[:, None]
IR = np.stack([lp(IR[:, 0], 6000), lp(IR[:, 1], 6000)], 1); IR /= np.sqrt((IR ** 2).sum(0))
def verb(x, wet=.2): return x * (1 - wet) + np.stack([fftconvolve(x[:, i], IR[:, i])[:len(x)] for i in (0, 1)], 1) * wet
def rms(x): return np.sqrt(np.mean(x ** 2) + 1e-12)

mus, fx = st(), st()
# ── A: the mess. Scattered marimba, detuned, random pans, denser and denser
AM = ['A3', 'C4', 'E4', 'G4', 'A4', 'D4', 'B3']
t = 1.3; k = 0
while t < FREEZE:
    X.add(mus, mallet(AM[k % 7], 'mar', rng.uniform(.4, .7), rng.uniform(-35, 35)), t, .9, rng.uniform(-.8, .8))
    t += max(.07, .5 * (1 - (t - 1.3) / 4.6) ** 1.4); k += 1
X.add(mus, mallet('A3', 'mar', .7), .05, .9, 0)
# fan + coil whine, held down under the voice, then rising into the freeze
t1 = tt(FREEZE); fan = lp(rng.standard_normal(len(t1)), 900) * .02
whine = np.sin(2 * np.pi * 6100 * t1) * .006 * (np.clip((t1 - 1.5) / 3.5, 0, 1) * .4 + np.clip((t1 - 5.05) / .55, 0, 1) * 1.6)
amb = (fan * np.clip((t1 - .5) / 1.5, 0, 1) * (1 + np.clip((t1 - 5.05) / .55, 0, 1)) + whine).astype(np.float32)
X.add(fx, amb, 0, 1, 0)
# ── foley + score from the picture's events
LYD = ['C6', 'E6', 'G6', 'B6', 'D6', 'F#6', 'A6']
ARP = ['C5', 'E5', 'G5', 'B5', 'D6', 'F#6', 'G6']
for e in ev:
    t, ty = e['t'], e['type']
    if ty == 'card':     # scattered notification glass: each a different pitch, detuned, panned anywhere
        f = rng.uniform(1700, 3400); d = .18; tc = tt(d)
        x = (np.sin(2 * np.pi * f * tc) + .4 * np.sin(2 * np.pi * f * 2.7 * tc)) * np.exp(-tc / .05)
        X.add(fx, x.astype(np.float32) * .05, t, 1, e.get('pan', 0))
        X.add(fx, X.norm(lp(rng.standard_normal(int(.04 * SR)) * np.exp(-np.arange(int(.04 * SR)) / SR * 90), 1500), .05), t, 1, e.get('pan', 0))
    elif ty == 'press':
        X.add(mus, mallet('C3', 'mar', .9, d=1.2), t, 1.2, 0); X.add(fx, sine_drop(130, 48, .8, .5), t, 1, 0)
    elif ty == 'land': X.add(mus, mallet(LYD[e['k'] % 7], 'glk', .35), t, .6, rng.uniform(-.15, .15))
    elif ty == 'snap':
        for n in ['C4', 'G4', 'B4', 'E5', 'F#5']: X.add(mus, mallet(n, 'vib', .6), t, .7, 0)
        X.add(fx, X.norm(lp(X.thump(.7, 70), 600), .4), t, 1, 0)
    elif ty == 'lit':
        i = e['i']; n = ARP[i % 7]; n = n[:-1] + str(int(n[-1]) + (1 if i >= 14 else 0))
        X.add(mus, mallet(n, 'glk' if i % 2 else 'vib', .55 + .015 * i), t, .75, (i % 7 - 3) * .08)
        X.add(fx, X.norm(hp(rng.standard_normal(int(.02 * SR)), 3000) * np.exp(-np.arange(int(.02 * SR)) / SR * 300), .06), t, 1, 0)
    elif ty == 'dive': X.add(fx, lp(X.whoosh(.42, .25), 4000), t, 1, 0)
    elif ty == 'digits':
        for j in range(int(e.get('d', .5) / .035)): X.add(fx, X.norm(bp(rng.standard_normal(int(.012 * SR)), 2000, 6000), .05), t + j * .035, 1, rng.uniform(-.2, .2))
    elif ty == 'number':
        for n in ['C3', 'G3', 'E4', 'B4', 'D5', 'F#5']: X.add(mus, mallet(n, 'vib', .75), t, .85, 0)
        X.add(mus, mallet('C7', 'glk', .5), t, .6, 0); X.add(fx, sine_drop(90, 42, 1.2, .55), t, 1, 0)
    elif ty == 'sweep':
        n = int(1.2 * SR); x = bp(rng.standard_normal(n), 4000, 11000) * np.sin(np.pi * np.arange(n) / n) ** 2
        X.add(fx, X.norm(x, .05), t, 1, -.3)
    elif ty == 'notif':
        X.add(fx, mallet('E6', 'glk', .55), t, .9, .1); X.add(fx, mallet('B6', 'glk', .5), t + .12, .9, .1)
    elif ty == 'tap': X.add(fx, X.norm(lp(X.click(.8, .8), 3000), .18), t, 1, 0)
    elif ty == 'ring':
        for j, n in enumerate(['G5', 'B5', 'D6']): X.add(mus, mallet(n, 'glk', .5), t + j * .09, .7, 0)
    elif ty == 'lock': X.add(mus, mallet('F#6', 'glk', .45), t, .6, 0)
    elif ty == 'crt':
        X.add(fx, sine_drop(1400, 70, .3, .18), t, 1, 0); X.add(fx, X.norm(hp(rng.standard_normal(int(.06 * SR)), 1500) * np.exp(-np.arange(int(.06 * SR)) / SR * 60), .06), t + .25, 1, 0)
    elif ty == 'logo':
        for n in ['C3', 'G3', 'C4', 'E4', 'B4', 'D5', 'F#5']: X.add(mus, mallet(n, 'vib', .7, d=4.2), t, .85, 0)
        X.add(mus, mallet('G6', 'glk', .45), t + .5, .5, .1)
# ── pads and a low pulse under the grid and the phone
for t0, d, ch in [(8.0, 5.0, ['C3', 'G3', 'B3', 'E4']), (13.8, 1.8, ['C3', 'G3', 'B3', 'E4']), (15.6, 2.1, ['C3', 'G3', 'B3', 'E4']), (17.7, 2.1, ['A2', 'E3', 'G3', 'C4']), (19.8, 2.1, ['F2', 'C3', 'E3', 'A3', 'B3'])]:
    X.add(mus, pad(ch, d + .3, .025), t0, 1, 0)
for b in np.arange(15.6, 21.9, .5): X.add(mus, sine_drop(62, 52, .35, .12), b, 1, 0)
for b in np.arange(8.5, 12.95, 1.0): X.add(mus, sine_drop(66, 55, .3, .08), b, 1, 0)

mus, fx = verb(mus, .22), verb(fx, .12)
# silences: digital zero in the freeze, near silence before the logo
for buf in (mus, fx): buf[int(FREEZE * SR):int(PRESS * SR)] = 0
g = np.ones(N, np.float32); t = np.arange(N) / SR
g *= np.where((t >= 21.92) & (t < 22.2), .08, 1); mus *= g[:, None]
tail = int(1.0 * SR); mus[-tail:] *= np.linspace(1, 0, tail)[:, None] ** 1.5; fx[-tail:] *= np.linspace(1, 0, tail)[:, None]

# ── narration (male voice), warmed and compressed
VO = {'l1': .5, 'l2': 3.0, 'l3': 6.6, 'l4': 9.8, 'l5': 15.75, 'l6': 18.5, 'l7': 22.35}
vo = np.zeros(N, np.float32); vm = np.zeros(N, np.float32)
for k, t0 in VO.items():
    x, sr = sf.read(os.path.join(HERE, 'voice', 'out', f'{k}.wav'), dtype='float32')
    x = resample_poly(x if x.ndim == 1 else x.mean(1), SR, sr).astype(np.float32); i = int(t0 * SR)
    vo[i:i + len(x)] += x[:N - i]; vm[i:i + len(x)] = 1
vo = hp(vo, 75); vo = vo + .2 * bp(vo, 120, 300) + .12 * bp(vo, 2500, 5000)
vo = X.compress(vo / (np.abs(vo).max() + 1e-9) * .9, thr=.2, ratio=3.5); vo = X.limit(vo, .9)
vo2 = verb(np.stack([vo, vo], 1), .07)
k = int(.2 * SR); env = np.clip(np.convolve(vm, np.ones(k) / k, 'same'), 0, 1)
mus *= (1 - (1 - 10 ** (-8 / 20)) * env)[:, None]; fx *= (1 - (1 - 10 ** (-4 / 20)) * env)[:, None]
m = vm > 0; gm = rms(vo2[m]) / (rms(mus[m]) * 10 ** (10 / 20)); mus *= gm; fx *= gm * 1.1
mix = vo2 + mus + fx
mix[int(FREEZE * SR):int(PRESS * SR)] = 0
mix = np.stack([X.limit(mix[:, 0], .95), X.limit(mix[:, 1], .95)], 1)
os.makedirs(os.path.join(HERE, 'out'), exist_ok=True); sf.write(os.path.join(HERE, 'out', 'mix.wav'), mix.astype(np.float32), SR)
print('mix written', round(len(mix) / SR, 2), 's')
