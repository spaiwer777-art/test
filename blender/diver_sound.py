"""Soundtrack for diver_pov: surface wash, plunge, muffled ocean, regulator breathing, whale song.

  python3 diver_sound.py <breaths.txt> <out.wav>
"""
import sys
import wave

import numpy as np

SR = 44100
FPS = 30
DUR = 480 / FPS
with open(sys.argv[1]) as fh:
    exhales = [int(x) / FPS for x in fh.readline().split()]
    SUB = int(fh.readline()) / FPS
WHALE = 230 / FPS
rng = np.random.default_rng(9)
n = int(SR * DUR)
t = np.arange(n) / SR


def lowpass(x, cutoff):
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    prev = 0.0
    for i in range(len(x)):
        prev = (1 - a[i]) * x[i] + a[i] * prev
        y[i] = prev
    return y


def bandpass(x, lo, hi):
    return lowpass(x, hi) - lowpass(x, lo)


def place(buf, sig, start, gain=1.0):
    i0 = int(start * SR)
    if i0 >= n or i0 + len(sig) <= 0:
        return
    s0 = max(0, -i0)
    i0 = max(0, i0)
    seg = sig[s0: s0 + n - i0]
    buf[i0: i0 + len(seg)] += seg * gain


under = np.clip((t - SUB) / 0.06, 0, 1)
above = 1 - under

# Surface: close water lapping on the mask + open-sea wash + wind
noise = rng.standard_normal(n)
lap = bandpass(noise, 250, 2500) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.7 * t + 1) ** 4) * 1.6
wash = bandpass(rng.standard_normal(n), 120, 900) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.21 * t)) * 1.2
wind = lowpass(rng.standard_normal(n), 500) * 0.6
surface = (lap + wash + wind) * above

# Plunge: bright splash then a roar of bubbles
plunge = np.zeros(n)
st = np.arange(int(1.6 * SR)) / SR
place(plunge, bandpass(rng.standard_normal(len(st)), 300, 7000) * np.exp(-st * 5) * 2.5, SUB - 0.03)
place(plunge, lowpass(rng.standard_normal(len(st)), 700) * np.exp(-st * 2.2) * 3.0, SUB)

# Underwater ambience: deep pressure rumble + distant crackle
rumble = lowpass(lowpass(rng.standard_normal(n), 120), 120) * 9.0 * (0.75 + 0.25 * np.sin(2 * np.pi * 0.09 * t))
crackle = np.zeros(n)
for ct in rng.uniform(SUB, DUR, 220):
    place(crackle, np.exp(-np.arange(200) / 25.0) * rng.uniform(-1, 1), ct, 0.08)
ambience = (rumble + bandpass(crackle, 1500, 6000)) * under

# Regulator: inhale hiss with a valve whistle, exhale bubble burst
breath = np.zeros(n)
for ex in exhales:
    it = np.arange(int(1.25 * SR)) / SR
    env = np.sin(np.pi * it / it[-1]) ** 0.8
    hiss = bandpass(rng.standard_normal(len(it)), 1800, 6500) * env * 1.4
    whistle = np.sin(2 * np.pi * (2900 + 200 * it) * it) * env * 0.05
    place(breath, hiss + whistle, ex - 1.6)
    click = np.exp(-np.arange(400) / 60.0) * np.sin(np.arange(400) * 0.9)
    place(breath, click * 0.5, ex - 0.05)
    et = np.arange(int(2.0 * SR)) / SR
    eenv = np.clip(et / 0.05, 0, 1) * np.exp(-et * 1.3)
    gurgle = lowpass(rng.standard_normal(len(et)), 400) * (0.6 + 0.4 * np.sign(np.sin(2 * np.pi * 9 * et + 3 * np.sin(2 * np.pi * 3 * et))))
    place(breath, gurgle * eenv * 4.0, ex)
    for bt in ex + rng.exponential(0.035, 70).cumsum():
        d = np.arange(int(0.05 * SR)) / SR
        f0 = rng.uniform(350, 1100)
        place(breath, np.sin(2 * np.pi * (f0 * d + 2500 * d * d)) * np.exp(-d * 60) * rng.uniform(0.2, 0.5), bt)

# Whale song: low gliding moans with long reverb tails
song = np.zeros(n)
for start, d, fa, fb in ((WHALE - 1.0, 2.6, 190, 120), (WHALE + 2.2, 1.8, 300, 420), (WHALE + 4.6, 2.8, 160, 90), (WHALE + 7.3, 2.0, 250, 330)):
    tt = np.arange(int(d * SR)) / SR
    k = tt / d
    f = fa + (fb - fa) * (3 * k * k - 2 * k ** 3) + 5 * np.sin(2 * np.pi * 5 * tt)
    ph = 2 * np.pi * np.cumsum(f) / SR
    tone = (np.sin(ph) + 0.5 * np.sin(2 * ph) + 0.22 * np.sin(3 * ph + 0.4)) * np.sin(np.pi * k) ** 1.5
    place(song, tone * 0.45, start)
rev = song.copy()
for delay, g in ((0.09, 0.5), (0.21, 0.42), (0.34, 0.33), (0.52, 0.25), (0.81, 0.17), (1.2, 0.1)):
    ds = int(delay * SR)
    rev[ds:] += song[:-ds] * g
song = lowpass(rev, 1300) * under

mix = 0.45 * surface + 0.5 * plunge + 0.5 * ambience + 0.7 * breath + 0.85 * song
mix *= np.clip(t / 0.4, 0, 1) * np.clip((DUR - t) / 0.8, 0, 1)
mix = np.tanh(mix / (np.abs(mix).max() + 1e-9) * 1.5) * 0.9
stereo = np.stack([mix, np.roll(mix, 11)], axis=1)
with wave.open(sys.argv[2], "wb") as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes((stereo * 32767).astype(np.int16).tobytes())
print("ok", DUR)
