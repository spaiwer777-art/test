"""Synthesised soundtrack for ocean_whale: surf, splash, underwater rumble, bubbles, whale song, pad."""
import sys
import wave

import numpy as np

SR = 44100
FPS = 30
DUR = 421 / FPS
DIVE = 121 / FPS
WHALE = 230 / FPS
rng = np.random.default_rng(3)
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


def env(x0, x1, fade=0.15):
    e = np.clip((t - x0) / fade, 0, 1) * np.clip((x1 - t) / fade, 0, 1)
    return e


above = (t < DIVE).astype(float)
below = 1 - above
xf = np.clip((t - DIVE) / 0.08, 0, 1)

# Surf: two noise layers with slow swells
noise = rng.standard_normal(n)
swell = 0.55 + 0.45 * np.sin(2 * np.pi * 0.23 * t) ** 2
surf = (lowpass(noise, 1800) - lowpass(noise, 200)) * swell * 1.4
wind = lowpass(rng.standard_normal(n), 600) * 0.5
air = (surf + wind) * (1 - xf)

# Splash at the dive: bright noise burst
sp_env = np.exp(-np.clip(t - DIVE, 0, None) * 7) * (t >= DIVE - 0.02)
splash = lowpass(rng.standard_normal(n), 6000) * sp_env * 2.2

# Underwater: deep muffled rumble, slow movement
rumble = lowpass(lowpass(rng.standard_normal(n), 160), 160) * 8.0
rumble *= 0.7 + 0.3 * np.sin(2 * np.pi * 0.11 * t)
water = rumble * xf

# Bubbles: short upward chirps, dense right after the dive then sparse
bub = np.zeros(n)
times = list(DIVE + rng.exponential(0.06, 60).cumsum()) + list(DIVE + 2 + rng.uniform(0, DUR - DIVE - 2, 14))
for bt in times:
    if bt >= DUR - 0.1:
        continue
    i0 = int(bt * SR)
    d = 0.06
    tt = np.arange(int(d * SR)) / SR
    f0 = rng.uniform(500, 1400)
    chirp = np.sin(2 * np.pi * (f0 * tt + 3000 * tt * tt)) * np.exp(-tt * 55)
    bub[i0:i0 + len(chirp)] += chirp[: n - i0] * rng.uniform(0.15, 0.4)

# Whale song: gliding moans with harmonics and vibrato
song = np.zeros(n)
calls = [(WHALE - 0.4, 2.8, 210, 140), (WHALE + 2.8, 1.9, 320, 420), (WHALE + 5.0, 2.6, 180, 95), (WHALE + 7.4, 1.8, 260, 330)]
for start, d, fa, fb in calls:
    i0 = int(start * SR)
    if i0 >= n:
        continue
    tt = np.arange(int(d * SR)) / SR
    k = tt / d
    f = fa + (fb - fa) * (3 * k * k - 2 * k ** 3) + 6 * np.sin(2 * np.pi * 5.5 * tt)
    ph = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sin(ph) + 0.45 * np.sin(2 * ph) + 0.2 * np.sin(3 * ph + 0.5)
    e = np.sin(np.pi * k) ** 1.5
    seg = tone * e * 0.5
    song[i0:i0 + len(seg)] += seg[: n - i0]
# cheap reverb: multi-tap feedback delays
rev = song.copy()
for delay, g in ((0.11, 0.5), (0.23, 0.4), (0.37, 0.3), (0.53, 0.22), (0.79, 0.15)):
    dsm = int(delay * SR)
    rev[dsm:] += song[:-dsm] * g
song = lowpass(rev, 1400)

# Pad: A minor, swells in as the whale arrives
pad = np.zeros(n)
for f in (110, 164.8, 220, 261.6, 329.6):
    pad += np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 1.004 * t)
pad = lowpass(pad, 700) * np.clip((t - (WHALE - 3)) / 4, 0, 1) * 0.07
pad *= np.clip((DUR - t) / 1.5, 0, 1)

mix = 0.5 * air + splash * 0.6 + water * 0.55 + bub * 0.6 + song * 0.9 + pad
mix *= np.clip(t / 0.6, 0, 1) * np.clip((DUR - t) / 0.8, 0, 1)
mix = np.tanh(mix / (np.abs(mix).max() + 1e-9) * 1.6) * 0.9
stereo = np.stack([mix, np.roll(mix, 9)], axis=1)
with wave.open(sys.argv[1], "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((stereo * 32767).astype(np.int16).tobytes())
print("ok", DUR)
