# score + foley + narration → out/mix.wav (48 kHz stereo).
# Narration: voice/out/<id>.wav (ElevenLabs when voice/eleven.py has run, else the Kokoro draft).
# Music: music/track.wav when present (a licensed track), else the score synthesised here.
import json, os, sys, numpy as np, soundfile as sf
HERE = os.path.dirname(os.path.abspath(__file__)); LIB = os.environ.get('LIB', '/root/lemo-opuscar')
sys.path.insert(0, os.path.join(LIB, 'core', 'audio'))
import sfx as X
from scipy.signal import butter, sosfilt, fftconvolve, resample_poly
SR = X.SR; DUR = 25.4; N = int(DUR * SR); rng = np.random.default_rng(33)
ev = json.load(open(os.path.join(HERE, 'events.json')))['ev']
FREEZE, PRESS, OFF, LOGO = 5.4, 5.9, 21.2, 21.45

def lp(x, f): return sosfilt(butter(2, f, 'low', fs=SR, output='sos'), x)
def hp(x, f): return sosfilt(butter(2, f, 'high', fs=SR, output='sos'), x)
def bp(x, a, b): return sosfilt(butter(2, [a, b], 'band', fs=SR, output='sos'), x)
def hz(n):
    names = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}; s = 1 if '#' in n else 0
    return 440 * 2 ** ((names[n[0]] + s + 12 * (int(n[-1]) + 1) - 69) / 12)
def tt(d): return np.arange(int(d * SR)) / SR
def mallet(note, kind='vib', v=.8, cents=0.0, d=None):
    f = hz(note) * 2 ** (cents / 1200)
    P = {'mar': ([1, 3.93, 9.2], [1, .3, .08], [.45, .1, .04]), 'vib': ([1, 4.0, 10.1], [1, .18, .05], [2.4, .5, .15]), 'glk': ([1, 2.76, 5.4], [1, .4, .15], [1.3, .45, .18])}[kind]
    d = d or max(P[2]) * 2.6; t = tt(d); y = np.zeros_like(t)
    for r, a, tau in zip(*P):
        if f * r < SR / 2.2: y += a * np.sin(2 * np.pi * f * r * t) * np.exp(-t / tau)
    if kind == 'vib': y *= 1 + .22 * np.sin(2 * np.pi * 5.4 * t)
    click = hp(rng.standard_normal(len(t)), 2500) * np.exp(-t / .004) * .08
    return ((y * np.minimum(1, t / .002) + click) * v * .3).astype(np.float32)
def pad(notes, d, g=.04):
    t = tt(d); y = np.zeros_like(t)
    for n in notes:
        for det in (-.08, .07): y += np.sin(2 * np.pi * hz(n) * 2 ** (det / 12) * t)
    return (lp(y * np.minimum(1, t / .4) * np.minimum(1, (d - t) / .5), 1800) * g).astype(np.float32)
def drop(f0, f1, d, v):
    t = tt(d); f = f1 + (f0 - f1) * np.exp(-t / (d * .25)); return (np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / (d * .4)) * v).astype(np.float32)
def hat(v=1.): n = int(.05 * SR); return (hp(rng.standard_normal(n), 7000) * np.exp(-np.arange(n) / SR * 90) * v * .12).astype(np.float32)
def st(): return np.zeros((N, 2), np.float32)
IRn = int(1.4 * SR); IR = rng.standard_normal((IRn, 2)) * np.exp(-np.arange(IRn) / SR * 4.2)[:, None]
IR = np.stack([lp(IR[:, 0], 6000), lp(IR[:, 1], 6000)], 1); IR /= np.sqrt((IR ** 2).sum(0))
def verb(x, wet=.2): return x * (1 - wet) + np.stack([fftconvolve(x[:, i], IR[:, i])[:len(x)] for i in (0, 1)], 1) * wet
def rms(x): return np.sqrt(np.mean(x ** 2) + 1e-12)

mus, fx = st(), st()
track = os.path.join(HERE, 'music', 'track.wav')
if os.path.exists(track):
    x, sr = sf.read(track, dtype='float32'); x = x if x.ndim == 2 else np.stack([x, x], 1)
    x = np.stack([resample_poly(x[:, i], SR, sr) for i in (0, 1)], 1).astype(np.float32)[:N]; mus[:len(x)] = x * .5
else:
    # A: the hook. Scattered marimba, detuned and panned anywhere, with a tension pulse
    AM = ['A3', 'C4', 'E4', 'G4', 'A4', 'D4', 'B3']; t = .05; k = 0
    while t < FREEZE:
        X.add(mus, mallet(AM[k % 7], 'mar', rng.uniform(.4, .7), rng.uniform(-35, 35)), t, .9, rng.uniform(-.8, .8)); t += max(.09, .3 * (1 - t / 5.4) ** .8); k += 1
    for b in np.arange(.08, FREEZE, .5): X.add(mus, drop(120, 44, .3, .5), b, 1, 0)
    # B → end: one tuned family (C lydian), a soft kick on the beat and a shaker
    for t0, d, ch in [(5.9, 1.6, ['C3', 'G3', 'B3', 'E4']), (7.45, 4.0, ['C3', 'G3', 'B3', 'E4']), (11.0, 4.0, ['A2', 'E3', 'G3', 'C4']), (15.3, 2.3, ['F2', 'C3', 'E3', 'A3']), (17.6, 3.6, ['C3', 'G3', 'B3', 'E4'])]:
        X.add(mus, pad(ch, d + .3, .03), t0, 1, 0)
    for b in np.arange(7.45, OFF, .5):
        X.add(mus, drop(150, 46, .3, .55), b, 1, 0)
        X.add(mus, hat(1.0), b + .25, 1, .25)
    ARP = ['C5', 'G5', 'B5', 'E6', 'D6', 'B5', 'G5', 'F#5']
    for i, b in enumerate(np.arange(7.45, 15.0, .25)):
        if i % 2 == 0 or b > 11: X.add(mus, mallet(ARP[i % 8], 'glk' if i % 4 else 'vib', .35), b, .55, (i % 8 - 4) * .06)
    for i, b in enumerate(np.arange(17.6, OFF, .5)): X.add(mus, mallet(['E5', 'G5', 'B5', 'D6'][i % 4], 'vib', .35), b, .5, 0)
LYD = ['C6', 'E6', 'G6', 'B6', 'D6', 'F#6', 'A6']
for e in ev:
    t, ty = e['t'], e['type']
    if ty == 'card':
        f = rng.uniform(1700, 3400); tc = tt(.16); x = (np.sin(2 * np.pi * f * tc) + .4 * np.sin(2 * np.pi * f * 2.7 * tc)) * np.exp(-tc / .045)
        X.add(fx, x.astype(np.float32) * .035, t, 1, e.get('pan', 0))
    elif ty == 'stamp':
        X.add(fx, drop(110, 38, .5, .8), t, 1, 0); X.add(fx, X.norm(lp(rng.standard_normal(int(.08 * SR)) * np.exp(-np.arange(int(.08 * SR)) / SR * 40), 2500), .35), t, 1, 0)
        if e.get('red'): X.add(fx, mallet('A#3', 'mar', .8, 40), t, 1, 0)
    elif ty == 'press': X.add(mus, mallet('C3', 'mar', .9, d=1.2), t, 1.2, 0); X.add(fx, drop(140, 45, .9, .7), t, 1, 0)
    elif ty == 'name': X.add(mus, mallet('G6', 'glk', .5), t, .7, 0)
    elif ty == 'land': X.add(mus, mallet(LYD[e['k'] % 7], 'glk', .3), t, .5, rng.uniform(-.15, .15))
    elif ty == 'expand': X.add(fx, lp(X.whoosh(.45, .3), 5000), t - .1, 1, 0)
    elif ty == 'hi': X.add(mus, mallet(['E6', 'B6'][e['n']], 'glk', .55), t, .8, (-.2, .2)[e['n']])
    elif ty == 'tap': X.add(fx, X.norm(lp(X.click(.8, .8), 3000), .2), t, 1, 0)
    elif ty == 'open': X.add(fx, lp(X.whoosh(.32, .25), 6000), t, 1, 0)
    elif ty == 'tick': X.add(fx, X.norm(hp(rng.standard_normal(int(.02 * SR)), 3000) * np.exp(-np.arange(int(.02 * SR)) / SR * 300), .07), t, 1, 0)
    elif ty == 'swipe': X.add(fx, lp(X.whoosh(.35, .22), 5000), t, 1, .4)
    elif ty == 'dive': X.add(fx, lp(X.whoosh(.32, .3), 4000), t, 1, 0)
    elif ty == 'digits':
        for j in range(int(e.get('d', .4) / .035)): X.add(fx, X.norm(bp(rng.standard_normal(int(.012 * SR)), 2000, 6000), .05), t + j * .035, 1, rng.uniform(-.2, .2))
    elif ty == 'number':
        for n in ['C3', 'G3', 'E4', 'B4', 'D5', 'F#5']: X.add(mus, mallet(n, 'vib', .75), t, .9, 0)
        X.add(mus, mallet('C7', 'glk', .5), t, .6, 0); X.add(fx, drop(90, 40, 1.2, .7), t, 1, 0)
    elif ty == 'sweep': n = int(1.0 * SR); x = bp(rng.standard_normal(n), 4000, 11000) * np.sin(np.pi * np.arange(n) / n) ** 2; X.add(fx, X.norm(x, .05), t, 1, -.3)
    elif ty == 'notif': X.add(fx, mallet('E6', 'glk', .55), t, .9, .1); X.add(fx, mallet('B6', 'glk', .5), t + .12, .9, .1)
    elif ty == 'ring':
        for j, n in enumerate(['G5', 'B5', 'D6']): X.add(mus, mallet(n, 'glk', .5), t + j * .09, .7, 0)
    elif ty == 'lock': X.add(mus, mallet('F#6', 'glk', .45), t, .6, 0)
    elif ty == 'crt': X.add(fx, drop(1400, 70, .3, .2), t, 1, 0)
    elif ty == 'logo':
        for n in ['C3', 'G3', 'C4', 'E4', 'B4', 'D5', 'F#5']: X.add(mus, mallet(n, 'vib', .7, d=4.0), t, .9, 0)
        X.add(fx, drop(120, 40, 1.0, .6), t, 1, 0)
    elif ty == 'cta': X.add(mus, mallet('G6', 'glk', .5), t, .7, .1); X.add(mus, mallet('D7', 'glk', .4), t + .1, .5, .1)

mus, fx = verb(mus, .2), verb(fx, .1)
for buf in (mus, fx): buf[int(FREEZE * SR):int(PRESS * SR)] = 0          # the freeze: digital zero
t = np.arange(N) / SR; mus *= np.where((t >= OFF + .05) & (t < LOGO), .1, 1)[:, None]
tail = int(1.0 * SR); mus[-tail:] *= np.linspace(1, 0, tail)[:, None] ** 1.5; fx[-tail:] *= np.linspace(1, 0, tail)[:, None]

VO = {'l1': .1, 'l2': 2.95, 'l3': 6.0, 'l4': 7.75, 'l5': 11.3, 'l6': 17.7, 'l7': 21.5, 'l8': 23.6}
vo = np.zeros(N, np.float32); vm = np.zeros(N, np.float32)
for k, t0 in VO.items():
    x, sr = sf.read(os.path.join(HERE, 'voice', 'out', f'{k}.wav'), dtype='float32')
    x = resample_poly(x if x.ndim == 1 else x.mean(1), SR, sr).astype(np.float32); i = int(t0 * SR); n = min(len(x), N - i)
    vo[i:i + n] += x[:n]; vm[i:i + n] = 1
vo = hp(vo, 75); vo = vo + .18 * bp(vo, 120, 300) + .1 * bp(vo, 2500, 5000)
vo = X.compress(vo / (np.abs(vo).max() + 1e-9) * .9, thr=.2, ratio=3.5); vo = X.limit(vo, .9)
vo2 = verb(np.stack([vo, vo], 1), .06)
k = int(.2 * SR); env = np.clip(np.convolve(vm, np.ones(k) / k, 'same'), 0, 1)
mus *= (1 - (1 - 10 ** (-8 / 20)) * env)[:, None]; fx *= (1 - (1 - 10 ** (-4 / 20)) * env)[:, None]
m = vm > 0; gm = rms(vo2[m]) / (rms(mus[m]) * 10 ** (9 / 20)); mus *= gm; fx *= gm * 1.1
mix = vo2 + mus + fx; mix[int(FREEZE * SR):int(PRESS * SR)] = 0
mix = np.stack([X.limit(mix[:, 0], .95), X.limit(mix[:, 1], .95)], 1)
os.makedirs(os.path.join(HERE, 'out'), exist_ok=True); sf.write(os.path.join(HERE, 'out', 'mix.wav'), mix.astype(np.float32), SR)
print('mix written', round(len(mix) / SR, 2), 's', '(licensed track)' if os.path.exists(track) else '(synth score)')
