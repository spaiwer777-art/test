"""Diver-mask post pass: mask frame, droplets above water, splash foam at the plunge.

  python3 diver_post.py <rendered frames dir> <output dir> <submerge frame>
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

src, dst, submerge = sys.argv[1], sys.argv[2], int(sys.argv[3])
os.makedirs(dst, exist_ok=True)
W, H = 1080, 1920
random.seed(5)

# --- static mask: dark glass edges with a nose pocket at the bottom
mask = Image.new("L", (W // 4, H // 4), 0)
px = mask.load()
cx, cy, a, b = W / 8, H * 0.47 / 4, W * 0.6 / 4, H * 0.57 / 4
for y in range(H // 4):
    for x in range(W // 4):
        d = (abs(x - cx) / a) ** 4 + (abs(y - cy) / b) ** 4
        nose = ((x - cx) / (W * 0.15 / 4)) ** 2 + ((y - H * 1.07 / 4) / (H * 0.11 / 4)) ** 2
        v = min(1.0, max(0.0, (d - 0.75) / 0.6))
        if nose < 1.0:
            v = max(v, min(1.0, (1.0 - nose) / 0.5) * 0.92)
        px[x, y] = int(255 * v ** 1.3)
mask = mask.resize((W, H), Image.BICUBIC).filter(ImageFilter.GaussianBlur(28))
frame_layer = Image.new("RGBA", (W, H), (3, 8, 12, 255))
frame_layer.putalpha(mask)
# thin highlight where the glass meets the skirt
rim = mask.point(lambda v: 255 if 70 < v < 110 else 0).filter(ImageFilter.GaussianBlur(6))
rim_layer = Image.new("RGBA", (W, H), (150, 200, 210, 0))
rim_layer.putalpha(rim.point(lambda v: v // 6))

# --- droplets on the glass while at the surface
drops = []
for i in range(46):
    edge = random.random() < 0.6
    x = random.choice([random.uniform(40, 260), random.uniform(820, 1040)]) if edge else random.uniform(120, 960)
    drops.append({
        "x": x, "y": random.uniform(80, 1800), "r": random.uniform(6, 30) * (1.3 if edge else 1.0),
        "v": random.uniform(0, 1.6) if random.random() < 0.35 else 0.0,
    })


def apply_drops(im, f):
    """Each droplet is a tiny lens: it shows the scene behind it shrunk and upside down."""
    for dr in drops:
        r = int(dr["r"])
        x, y = int(dr["x"]), int(dr["y"] + dr["v"] * f)
        if not (r < x < W - r and r < y < H - r):
            continue
        R = int(r * 3.2)
        box = (max(0, x - R), max(0, y - R), min(W, x + R), min(H, y + R))
        patch = im.crop(box).transpose(Image.ROTATE_180).resize((2 * r, int(2.2 * r)), Image.BILINEAR)
        alpha = Image.new("L", patch.size, 0)
        ImageDraw.Draw(alpha).ellipse((1, 1, patch.size[0] - 2, patch.size[1] - 2), fill=235)
        alpha = alpha.filter(ImageFilter.GaussianBlur(max(1, r / 7)))
        shade = Image.new("RGBA", patch.size, (0, 10, 20, 0))
        ImageDraw.Draw(shade).ellipse((0, 0, patch.size[0] - 1, patch.size[1] - 1), outline=(0, 10, 20, 90), width=max(1, r // 5))
        patch = Image.alpha_composite(patch.convert("RGBA"), shade.filter(ImageFilter.GaussianBlur(max(1, r / 6))))
        patch.putalpha(alpha)
        im.alpha_composite(patch, (x - r, int(y - 1.1 * r)))
    spec = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(spec)
    for dr in drops:
        r = dr["r"]
        x, y = dr["x"], dr["y"] + dr["v"] * f
        d.ellipse((x - r * 0.5, y - r * 0.8, x - r * 0.18, y - r * 0.48), fill=(255, 255, 255, 150))
        if dr["v"] > 0:
            d.line((x, y - r, x, y - r - dr["v"] * f * 0.8), fill=(255, 255, 255, 18), width=int(r * 0.5))
    return Image.alpha_composite(im, spec.filter(ImageFilter.GaussianBlur(1.5)))


WASH = (17, 23)   # a swell breaks over the mask while still at the surface


def water_smear(im, k, tint=(170, 225, 230)):
    """Water rushing over the glass: heavy blur that clears as k goes 0 → 1."""
    blur = im.filter(ImageFilter.GaussianBlur(4 + 40 * (1 - k)))
    veil = Image.new("RGBA", (W, H), tint + (int(120 * (1 - k)),))
    out = Image.alpha_composite(blur, veil)
    return Image.blend(out, im, k ** 1.5)


files = sorted(f for f in os.listdir(src) if f.startswith("f") and f.endswith(".png"))
for name in files:
    f = int(name[1:5])
    im = Image.open(os.path.join(src, name)).convert("RGBA")
    if im.size != (W, H):
        im = im.resize((W, H), Image.LANCZOS)
    if WASH[0] <= f <= WASH[1]:
        # replace the frames where the swell covered the lens with a crossfade of the clean neighbours
        a = Image.open(os.path.join(src, f"f{WASH[0] - 1:04d}.png")).convert("RGBA")
        b = Image.open(os.path.join(src, f"f{WASH[1] + 1:04d}.png")).convert("RGBA")
        u = (f - WASH[0] + 1) / (WASH[1] - WASH[0] + 2)
        im = Image.blend(a, b, u)
        peak = 1 - abs(u - 0.4) / 0.6
        im = water_smear(im, max(0.0, 1 - peak))
    elif WASH[1] < f <= WASH[1] + 8:
        im = water_smear(im, 0.55 + 0.45 * (f - WASH[1]) / 8)
    if f < submerge:
        im = apply_drops(im, f)
    k = (f - (submerge - 1)) / 16
    if 0 <= k < 1:
        im = water_smear(im, k, tint=(200, 240, 245))
    im = Image.alpha_composite(im, rim_layer)
    im = Image.alpha_composite(im, frame_layer)
    im.convert("RGB").save(os.path.join(dst, name), compress_level=1)
print("post done", len(files))
