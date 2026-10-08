"""The ten figures.  Each builder returns a dict with the solid SDF (without
the cavity), mesh bounds, plinth size and the texts engraved in Blender."""
import math

import numpy as np

from sdf import *            # noqa: F401,F403
from sdf import F, _len
from common import plinth, plinth_groove, PLINTH_H

PT = PLINTH_H          # plinth top


def _fig(key, title, sub, solid, w, d, hi_z, color, back=None, extra=None):
    m = 4.0
    bx = max(w / 2, extra or 0) + m
    return dict(key=key, title=title, sub=sub, solid=solid,
                plinth=(w, d), color=color, back=back or [],
                bounds=((-bx, -d / 2 - m - (extra or 0), -1.0),
                        (bx, d / 2 + m + (extra or 0), hi_z + m)))


def base_plinth(w, d):
    return plinth(w, d) - plinth_groove(w, d, PT - 3.2)


def surf_y(r_of_z, z):
    return -r_of_z(z)


# =========================================================================== 1
def trojan():
    W, D = 72, 60
    # ---- wooden cart with planks and four wheels
    cart = box((60, 44, 7.5), c=(0, 0, PT + 3.25), r=1.4)
    planks = u2(*[seg2((-31, y), (31, y), 0.45) for y in (-11, 0, 11)])
    cart = engrave(cart, planks, 0.7, plane='xy', front='+z')
    nails = u2(*[c2(0.9, (x, y)) for x in (-26, 26) for y in (-16.5, -5.5, 5.5, 16.5)])
    cart = engrave(cart, nails, 0.5, plane='xy', front='+z')

    def wheel(x, y):
        s = 1 if x > 0 else -1
        disc = cylinder(9.0, -1.8, 1.8, rr=0.8)
        rim = torus(8.0, 1.5)
        hub = cylinder(3.2, -1.8, 4.2, rr=1.0)
        spokes = union(*[box((15, 2.4, 3.2), r=0.6).rot('z', a) for a in (0, 60, 120)])
        w = union(disc, rim, hub, spokes, k=0.6)
        w = difference(w, cylinder(1.3, 3.0, 6.0))
        w = w.rot('y', 90 * s)
        return w.move(x, y, PT + 9.0)
    wheels = union(*[wheel(x, y) for x in (-31.0, 31.0) for y in (-13.5, 13.5)])

    # ---- the horse (chess-knight style, carved from planks)
    neck = round_cone((0, 3, 21), (0, -1, 74), 20.5, 16.5)
    collar = cylinder(23.5, PT + 5, PT + 12, rr=2.5)
    groove = lambda p: 0.55 * pulse(p[:, 2] - 1.5, 7.0, 0.75) * smoothstep(26, 30, p[:, 2]) \
        * (1 - smoothstep(72, 76, p[:, 2]))
    neck = neck.displace(groove)
    # hidden hatch on the right flank (the payload door)
    hatch = sub2(rect2(15, 19, c=(1.5, 47), r=1.5), rect2(13.4, 17.4, c=(1.5, 47), r=0.8))
    neck = engrave(neck, hatch, 0.9, plane='yz', front='+x')
    hinges = union(box((2.4, 3.0, 3.0), c=(18.9, 8.8, 41), r=0.6),
                   box((2.4, 3.0, 3.0), c=(18.9, 8.8, 53), r=0.6),
                   sphere(1.6, (19.0, -4.3, 47)))

    head = ellipsoid((14.5, 31, 15.0)).rot('x', 42).move(0, -13, 90)
    cheek = ellipsoid((15.5, 13, 13.5), (0, -1, 87))
    muzzle = ellipsoid((12.0, 10.5, 10.5), (0, -34, 70))
    jaw = ellipsoid((11.0, 13, 7.0), (0, -22, 69))
    forelock = ellipsoid((4.0, 5.5, 8.0)).rot('x', 35).move(0, -1, 103)
    ears = union(*[round_cone((s * 6, 5, 98), (s * 7.5, 10, 113), 4.6, 1.3) for s in (-1, 1)])
    mane = []
    for i, z in enumerate(np.arange(30, 101, 7.0)):
        t = (z - 21) / 53
        cy = 3 - 4 * t
        r = 20.5 - 4 * t
        yb = cy + r - 1.5 if z < 80 else 12.5 - (z - 80) * 0.25
        mane.append(ellipsoid((4.8, 6.5, 7.5)).rot('x', -35).move(0, yb + 1.0, z))
    mane = union(*mane, k=1.2)
    horse = union(neck, collar, k=5)
    horse = union(horse, head, cheek, muzzle, jaw, k=5)
    horse = union(horse, ears, k=2)
    horse = union(horse, forelock, k=1.5)
    horse = union(horse, mane, k=1.5)
    horse = union(horse, hinges, k=0.5)
    # bridle around the muzzle + cheek straps
    sdir = (0, -math.cos(math.radians(42)), -math.sin(math.radians(42)))
    bridle = torus(11.4, 1.3).matrix(look_matrix(sdir)).move(0, -30, 74)
    straps = union(*[capsule((s * 11.6, -27, 77), (s * 14.0, -5, 95), 1.1) for s in (-1, 1)])
    horse = union(horse, bridle, straps, k=0.5)
    # eyes, nostrils, mouth
    eyes = union(*[sphere(4.8, (s * 12.6, -14, 95)) for s in (-1, 1)])
    horse = union(horse, eyes, k=1.0)
    horse = difference(horse, *[sphere(2.1, (s * 17.0, -15.6, 95.3)) for s in (-1, 1)], k=0.3)
    nostrils = union(*[ellipsoid((1.4, 2.3, 2.0)).rot('z', s * 25).move(s * 4.8, -44.3, 70.5) for s in (-1, 1)])
    horse = difference(horse, nostrils, k=0.5)
    mouth = seg2((-44, 63.5), (-30, 66.5), 0.55)
    horse = engrave(horse, mouth, 1.0, plane='yz')

    solid = union(base_plinth(W, D), cart, k=1.0)
    solid = union(solid, wheels, k=0.8)
    solid = union(solid, horse, k=2.0)
    return _fig('01_trojan', 'TROJAN HORSE', 'MALWARE  ·  TROJAN', solid, W, D, 116,
                (0.72, 0.50, 0.30),
                back=[('A GIFT FOR YOU', 3.2)])


# =========================================================================== 2
def wannacry():
    W, D = 72, 58
    body = box((60, 40, 70), c=(0, 0, PT + 35), r=7)
    fy = -20.0
    # panel line
    body = engrave(body, i2(rect2(51, 61, c=(0, PT + 35), r=5), lambda u, v: -rect2(49.6, 59.6, c=(0, PT + 35), r=4.3)(u, v)),
                   0.8, front='-y')
    # escutcheon around the keyhole
    body = emboss(body, rect2(16, 25, c=(0, PT + 23), r=7.5), 1.2, front='-y', k=0.4)
    # worried eyebrows, closed crying eyes
    brows = u2(seg2((-5, PT + 59.5), (-20, PT + 56.5), 1.15), seg2((5, PT + 59.5), (20, PT + 56.5), 1.15))
    eyes = u2(arc2(6, 1.25, 25, 155, c=(-13, PT + 45)), arc2(6, 1.25, 25, 155, c=(13, PT + 45)))
    body = engrave(body, u2(brows, eyes), 1.6, front='-y')
    # tears (embossed)
    def drop(cx, cz, r):
        return u2(c2(r, (cx, cz)), polygon2([(cx - r * 0.86, cz + r * 0.5), (cx + r * 0.86, cz + r * 0.5), (cx, cz + r * 2.6)]), k=r * 0.5)
    tears = u2(drop(-13, PT + 33, 3.0), drop(13, PT + 33, 3.0), drop(-17.5, PT + 21, 2.3), drop(17.5, PT + 21, 2.3),
               seg2((-13, PT + 38.5), (-13, PT + 35), 1.0), seg2((13, PT + 38.5), (13, PT + 35), 1.0))
    body = emboss(body, tears, 1.6, front='-y', k=0.4)
    # keyhole = wailing mouth
    keyhole = u2(c2(4.6, (0, PT + 27.5)), polygon2([(-2.0, PT + 27), (2.0, PT + 27), (3.6, PT + 15.5), (-3.6, PT + 15.5)]), k=0.8)
    body = engrave(body, keyhole, 4.2, front='-y')
    # shackle
    leg = lambda s: cylinder(6.2, PT + 66, PT + 88, c=(s * 19, 0))
    arc = intersect(torus(19, 6.2).rot('x', 90).move(0, 0, PT + 88), halfspace((0, 0, -1), (0, 0, PT + 88)))
    shackle = union(leg(-1), leg(1), arc)
    notch = cylinder(7.0, PT + 74, PT + 77, c=(19, 0)) - cylinder(5.0, PT + 73, PT + 78, c=(19, 0))
    shackle = difference(shackle, notch)
    collars = union(*[cylinder(8.6, PT + 68, PT + 72.5, c=(s * 19, 0), rr=1.4) for s in (-1, 1)])
    lock = union(body, collars, k=1.0)
    lock = union(lock, shackle, k=0.8)
    # rivets on the front plate
    rivets = union(*[sphere(1.5, (x, fy - 0.2, z)) for x in (-21.5, 21.5) for z in (PT + 8.5, PT + 61.5)])
    lock = union(lock, rivets, k=0.3)
    solid = union(base_plinth(W, D), lock, k=2.0)
    return _fig('02_wannacry', 'WANNACRY', 'RANSOMWARE  ·  2017', solid, W, D, PT + 101,
                (0.85, 0.16, 0.16),
                back=[('OOPS, YOUR FILES ARE ENCRYPTED!', 3.2), ('SEND $300 WORTH OF BITCOIN', 3.2)])


# =========================================================================== 3
def morris_worm():
    W, D = 72, 60
    segs = []
    zs = np.arange(PT + 9, PT + 70, 9.0)
    for i, z in enumerate(zs):
        R = 21.5 - i * 0.45
        x = 2.2 * math.sin(z / 14.0)
        y = 1.2 * math.cos(z / 11.0)
        segs.append(ellipsoid((R, R * 0.97, 6.9), (x, y, z)))
    body = union(*segs, k=3.0)
    core = cylinder(17.0, PT, PT + 72, rr=4)
    body = union(body, core, k=2.0)
    head = sphere(20.0, (0, -1, PT + 80))
    worm = union(body, head, k=6)
    # tail coiled on the plinth
    tail = []
    for t in np.linspace(0, 1, 14):
        a = math.radians(-40 + 245 * t)
        r = 9.0 - 5.0 * t
        tail.append(sphere(r, (24 * math.cos(a), 19.5 * math.sin(a), PT + r * 0.62)))
    tail = union(*tail, k=2.5).scale((1, 1, 0.75), center=(0, 0, PT))
    worm = union(worm, tail, k=3.0)
    # face
    hz = PT + 80
    eyes = union(*[sphere(6.0, (s * 7.5, -16.8, hz + 4)) for s in (-1, 1)])
    worm = union(worm, eyes, k=1.2)
    worm = difference(worm, *[sphere(2.5, (s * 7.2, -23.4, hz + 4.4)) for s in (-1, 1)], k=0.3)
    glasses = union(*[torus(7.4, 1.05).rot('x', 90).move(s * 7.6, -20.8, hz + 4) for s in (-1, 1)])
    bridge = capsule((-1.0, -23.4, hz + 5.2), (1.0, -23.4, hz + 5.2), 1.0)
    temples = union(*[capsule((s * 14.6, -19.2, hz + 4.5), (s * 19.4, -5.0, hz + 5.5), 1.0) for s in (-1, 1)])
    worm = union(worm, glasses, bridge, temples, k=0.4)
    worm = engrave(worm, arc2(7.0, 1.0, 205, 335, c=(0, hz - 3)), 1.3, front='-y')
    teeth = u2(rect2(2.6, 3.0, c=(-1.5, hz - 10.8), r=0.6), rect2(2.6, 3.0, c=(1.5, hz - 10.8), r=0.6))
    worm = emboss(worm, teeth, 0.9, front='-y', k=0.2)
    antennae = []
    for s in (-1, 1):
        antennae.append(tube([(s * 5, -2, hz + 16), (s * 9, 0, hz + 25), (s * 15, -3, hz + 30)], [1.8, 1.6, 1.5]))
        antennae.append(sphere(3.0, (s * 15.5, -3.2, hz + 31)))
    worm = union(worm, *antennae, k=1.0)
    # segment pores (little dots along the body sides)
    solid = union(base_plinth(W, D), worm, k=2.0)
    return _fig('03_morris_worm', 'MORRIS WORM', 'THE FIRST WORM  ·  1988', solid, W, D, PT + 116,
                (0.95, 0.55, 0.62),
                back=[('6000 HOSTS IN 24 HOURS', 3.0)])


# =========================================================================== 4
def biohazard2d(s):
    parts = []
    holes = []
    for k in range(3):
        a = math.radians(90 + 120 * k)
        ca, sa = math.cos(a), math.sin(a)
        parts.append(c2(0.50 * s, (0.52 * s * ca, 0.52 * s * sa)))
        holes.append(c2(0.36 * s, (0.70 * s * ca, 0.70 * s * sa)))
        holes.append(seg2((0, 0), (1.2 * s * ca, 1.2 * s * sa), 0.05 * s))
    crescents = sub2(u2(*parts), *holes)
    ring = lambda u, v: np.abs(np.sqrt(u * u + v * v) - 0.44 * s) - 0.075 * s
    ring = sub2(ring, *[seg2((0, 0), (1.2 * s * math.cos(math.radians(90 + 120 * k + 60)),
                                    1.2 * s * math.sin(math.radians(90 + 120 * k + 60))), 0.09 * s) for k in range(3)])
    sym = u2(crescents, ring)
    return sub2(sym, c2(0.13 * s))


def phage():
    W, D = 72, 66
    tail = cylinder(16.6, PT - 1, PT + 42)
    rings = union(*[torus(16.7, 1.45, (0, 0, z)) for z in np.arange(PT + 12.5, PT + 40, 4.5)])
    tail = union(tail, rings, k=0.6)
    hexagon = lambda R: polygon2([(R * math.cos(math.radians(30 + 60 * i)), R * math.sin(math.radians(30 + 60 * i))) for i in range(6)])
    hx = hexagon(23.0)
    plate = extrude(lambda u, v: hx(u, v) + 1.2, PT + 5.2, PT + 9.8).offset(1.2)
    spikes = union(*[round_cone((22 * math.cos(math.radians(30 + 60 * i)), 22 * math.sin(math.radians(30 + 60 * i)), PT + 5),
                                (25 * math.cos(math.radians(30 + 60 * i)), 25 * math.sin(math.radians(30 + 60 * i)), PT + 1.2), 1.8, 0.8)
                     for i in range(6)])
    legs = []
    for i in range(6):
        a = math.radians(60 * i)
        ca, sa = math.cos(a), math.sin(a)
        p0 = (20.5 * ca, 20.5 * sa, PT + 8.5)
        p1 = (29 * ca, 29 * sa, PT + 25)
        p2 = (31.5 * ca, 31.5 * sa, PT + 1.8)
        legs.append(tube([p0, p1, p2], [2.3, 2.0, 1.9]))
        legs.append(sphere(2.8, p1))
        legs.append(ellipsoid((3.6, 3.6, 2.4), (31.5 * ca, 31.5 * sa, PT + 1.0)))
    legs = union(*legs, k=0.8)
    collar = cylinder(20.0, PT + 41, PT + 45.5, rr=1.6)
    neck = cylinder(15.9, PT + 40, PT + 62)

    # prolate icosahedral capsid, five-fold axis vertical
    Rc, E = 27.0, 14.0
    zc = PT + 76.0
    pts = [(0, 0, Rc + E / 2), (0, 0, -Rc - E / 2)]
    for k in range(5):
        a = math.radians(72 * k)
        pts.append((Rc * 2 / math.sqrt(5) * math.cos(a), Rc * 2 / math.sqrt(5) * math.sin(a), Rc / math.sqrt(5) + E / 2))
        a2 = a + math.radians(36)
        pts.append((Rc * 2 / math.sqrt(5) * math.cos(a2), Rc * 2 / math.sqrt(5) * math.sin(a2), -Rc / math.sqrt(5) - E / 2))
    pts = np.array(pts)
    # rotate so that a mid-band face looks to -Y
    _, hull = convex_polyhedron(pts)
    eq = hull.equations
    mid = [i for i in range(len(eq)) if abs(eq[i, 2]) < 0.5 and eq[i, 2] > 0.05]
    i0 = mid[0]
    az = math.atan2(eq[i0, 1], eq[i0, 0])
    rot = -math.pi / 2 - az
    cr, sr = math.cos(rot), math.sin(rot)
    pts = pts @ np.array([[cr, sr, 0], [-sr, cr, 0], [0, 0, 1]])
    pts[:, 2] += zc
    capsid, hull = convex_polyhedron(pts)
    eq = hull.equations
    fi = int(np.argmin([e[1] if abs(e[2]) < 0.5 else 9 for e in eq]))
    n = eq[fi, :3]
    dd = eq[fi, 3]
    tri = pts[hull.simplices[fi]]
    cen = tri.mean(axis=0)
    # face frame for the embossed biohazard sign
    uax = np.cross((0, 0, 1), n)
    uax /= np.linalg.norm(uax)
    vax = np.cross(n, uax)
    bio = biohazard2d(7.4)
    nF, uF, vF, cF = n.astype(F), uax.astype(F), vax.astype(F), cen.astype(F)
    # push the sign a little down towards the wide part of the triangle
    sgn = 1.0 if tri[:, 2].max() - cen[2] < cen[2] - tri[:, 2].min() else -1.0

    def relief(p):
        q = p - cF
        u = q @ uF
        v = q @ vF + F(sgn * 1.0)
        pd = p @ nF + F(dd)
        return np.maximum(bio(u, v), np.maximum(pd - 1.1, -pd - 2.0))
    head = union(capsid, SDF(relief))
    # faint edge grooves where the capsomer triangles meet would be lost in
    # printing; instead give every face a shallow inset panel
    virus = union(tail, plate, k=1.0)
    virus = union(virus, spikes, k=0.5)
    virus = union(virus, legs, k=1.0)
    virus = union(virus, collar, neck, k=1.2)
    virus = union(virus, head, k=1.5)
    solid = union(base_plinth(W, D), virus, k=1.0)
    return _fig('04_virus', 'VIRUS', 'SELF-REPLICATING CODE', solid, W, D, PT + 112,
                (0.35, 0.80, 0.35),
                back=[('CREEPER  ·  1971', 3.2)])


# =========================================================================== 5
def spyware():
    W, D = 72, 60
    rc = lambda z: 24.5 - 7.0 * (z - (PT + 9)) / 42.0
    coat = round_cone((0, 0, PT + 9), (0, 0, PT + 51), 24.5, 17.5)
    hem = cylinder(26.5, PT - 1, PT + 9, rr=3)
    shoulders = ellipsoid((23.5, 16.8, 9.5), (0, 0, PT + 51))
    coat = union(coat, hem, k=5)
    coat = union(coat, shoulders, k=4)
    # lapels, opening, pockets
    lapel = u2(polygon2([(0.5, PT + 38), (11.5, PT + 56), (5, PT + 57.5), (0.5, PT + 48)]),
               polygon2([(-0.5, PT + 38), (-11.5, PT + 56), (-5, PT + 57.5), (-0.5, PT + 48)]))
    coat = emboss(coat, lapel, 1.0, front='-y', k=0.3)
    coat = engrave(coat, u2(seg2((2.5, PT + 1), (2.5, PT + 38), 0.45),
                            rect2(11, 1.0, c=(-12.5, PT + 20), angle=8), rect2(11, 1.0, c=(12.5, PT + 20), angle=-8)),
                   0.8, front='-y')
    buttons = []
    for z in (PT + 15, PT + 24, PT + 33):
        for x in (-5.5, 9.5):
            buttons.append(sphere(1.8, (x, -math.sqrt(max(rc(z) ** 2 - x * x, 1)) - 0.2, z)))
    belt_z = PT + 28.5
    belt = torus(rc(belt_z) + 0.2, 1.9, (0, 0, belt_z))
    buckle = box((8.5, 3.0, 6.5), c=(0, -rc(belt_z) - 1.2, belt_z), r=1.0) - box((5.0, 6, 3.0), c=(0, -rc(belt_z) - 2.5, belt_z), r=0.5)
    arms = union(*[round_cone((s * 20.5, 2, PT + 51), (s * 19.5, -9.5, PT + 22), 6.3, 5.0) for s in (-1, 1)])
    coat = union(coat, belt, k=0.6)
    coat = union(coat, buckle, *buttons, k=0.3)
    coat = union(coat, arms, k=2.5)
    # popped collar
    collar = intersect(round_cone((0, 0, PT + 51), (0, 0, PT + 64), 15.2, 16.8).shell(1.2),
                       cylinder(30, PT + 51, PT + 64))
    collar = difference(collar, box((13, 30, 40), c=(0, -15, PT + 70)).rot('x', -12, center=(0, 0, PT + 52)))
    neck = cylinder(14.0, PT + 48, PT + 70)
    head = sphere(19.5, (0, -1, PT + 74))
    ears = union(*[ellipsoid((2.8, 4.2, 5.2), (s * 19.2, 0, PT + 73)) for s in (-1, 1)])
    hz = PT + 74
    glasses = union(*[box((11.5, 3.2, 6.8), r=1.6).rot('z', s * 20).move(s * 6.9, -18.9, hz + 2) for s in (-1, 1)])
    bridge = capsule((-1.6, -20.6, hz + 3.0), (1.6, -20.6, hz + 3.0), 1.0)
    temples = union(*[capsule((s * 12.3, -16.0, hz + 3), (s * 18.0, -3.5, hz + 3.6), 0.95) for s in (-1, 1)])
    nose = sphere(2.6, (0, -20.4, hz - 3))
    face = union(head, ears, k=1.0)
    face = union(face, glasses, bridge, temples, nose, k=0.5)
    face = engrave(face, arc2(5.0, 0.75, 215, 300, c=(1.5, hz - 4.5)), 1.0, front='-y')
    # fedora
    brim = ellipsoid((29.5, 27.5, 2.3), (0, -1, PT + 87))
    crown = cylinder(16.2, PT + 86, PT + 103, c=(0, -1), rr=4.5)
    crown = difference(crown, capsule((0, -12, PT + 106.2), (0, 9, PT + 106.2), 4.0), k=2.0)
    crown = difference(crown, sphere(4.5, (-9.0, -20.2, PT + 100)), sphere(4.5, (9.0, -20.2, PT + 100)), k=2.0)
    band = cylinder(16.8, PT + 87.5, PT + 92, c=(0, -1), rr=0.6)
    hat = union(brim, crown, k=1.5)
    hat = union(hat, band, k=0.3)
    spy = union(coat, collar, k=1.0)
    spy = union(spy, neck, face, k=1.5)
    spy = union(spy, hat, k=0.8)
    solid = union(base_plinth(W, D), spy, k=2.0)
    return _fig('05_spyware', 'SPYWARE', 'PEGASUS  ·  2016', solid, W, D, PT + 110,
                (0.25, 0.27, 0.32),
                back=[('I SEE EVERYTHING', 3.2)])


# =========================================================================== 6
def pillow_heart(size, z_tip, R, ky):
    """Puffy heart: iq's exact 2D heart, inset by R and swept with an
    ellipse (R in X/Z, R*ky in Y)."""
    s, R, ky, zt = F(size), F(R), F(ky), F(z_tip)
    c = F(math.sqrt(2) / 4)

    def d2(x, z):
        x = np.abs(x)
        a = np.sqrt((x - 0.25) ** 2 + (z - 0.75) ** 2) - c
        b1 = x ** 2 + (z - 1.0) ** 2
        m = 0.5 * np.maximum(x + z, 0)
        b2 = (x - m) ** 2 + (z - m) ** 2
        b = np.sqrt(np.minimum(b1, b2)) * np.sign(x - z)
        return np.where(x + z > 1.0, a, b)

    def g(p):
        q = d2(p[:, 0] / s, (p[:, 2] - zt) / s) * s + R
        y = p[:, 1] / ky
        return np.sqrt(np.maximum(q, 0) ** 2 + y * y) - R + np.minimum(q, 0) * 0
    return SDF(g)


def heartbleed():
    W, D = 76, 60
    puddle = union(ellipsoid((33, 26, 5.5), (0, 0, PT + 0.5)),
                   ellipsoid((10, 8, 4.0), (27, -11, PT + 0.2)),
                   ellipsoid((9, 7.5, 4.0), (-27, 10, PT + 0.2)),
                   ellipsoid((8, 9, 4.0), (15, 20, PT + 0.2)),
                   ellipsoid((7, 6, 3.5), (-16, -21, PT + 0.2)),
                   ellipsoid((6, 5, 3.2), (-32, -7, PT + 0.2)), k=3)
    mz0, mz1, mr0, mr1 = PT + 1, PT + 36, 30.0, 17.5
    mound = round_cone((0, 0, mz0), (0, 0, mz1), mr0, mr1)
    mr = lambda z: mr0 + (mr1 - mr0) * (z - mz0) / (mz1 - mz0)
    heart = pillow_heart(70.0, PT + 17, 13.0, 1.6)
    # sticking plaster: the bug got patched (sort of)
    pc, ang = (16.5, PT + 71), -35
    plaster = rect2(26, 8.5, c=pc, r=4.0, angle=ang)
    heart = emboss(heart, plaster, 1.3, front='-y', k=0.4)
    ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
    pad = lambda u, v: np.abs(rect2(8.5, 6.6, c=pc, r=1.2, angle=ang)(u, v)) - 0.35
    dots = u2(*[c2(0.55, (pc[0] + ca * t + -sa * o, pc[1] + sa * t + ca * o))
                for t in (-9.0, -7.0, 7.0, 9.0) for o in (-1.4, 1.4)])
    heart = engrave(heart, u2(pad, dots), 0.6, front='-y')
    body = union(heart, mound, k=9)
    drips = []
    for a, z1, z2, r in ((-112, PT + 46, PT + 1, 3.6), (-70, PT + 42, PT + 1, 3.2), (-30, PT + 47, PT + 22, 3.0),
                         (10, PT + 44, PT + 1, 3.4), (60, PT + 42, PT + 26, 2.8), (115, PT + 46, PT + 1, 3.3),
                         (165, PT + 44, PT + 20, 3.0), (215, PT + 46, PT + 1, 3.5), (-150, PT + 42, PT + 28, 2.7)):
        ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
        r1 = mr(min(z1, mz1)) + 0.6
        r2 = mr(max(z2, PT + 4)) + (1.2 if z2 < PT + 5 else 0.5)
        drips.append(round_cone((r1 * ca, r1 * sa, z1), (r2 * ca, r2 * sa, z2 + r * 0.9), r * 0.7, r))
        drips.append(sphere(r * 1.18, (r2 * ca * 1.02, r2 * sa * 1.02, z2 + r * 0.9)))
    body = union(body, *drips, k=2.5)
    solid = union(base_plinth(W, D), puddle, k=1.5)
    solid = union(solid, body, k=4)
    return _fig('06_heartbleed', 'HEARTBLEED', 'CVE-2014-0160', solid, W, D, PT + 100,
                (0.80, 0.08, 0.14),
                back=[('OPENSSL  ·  2014', 3.4)])


# =========================================================================== 7
def phishing():
    W, D = 72, 60
    rock = union(ellipsoid((31, 26, 12), (0, 2, PT + 4)),
                 sphere(20, (0, 3, PT + 18)),
                 ellipsoid((14, 12, 9), (17, -12, PT + 5)),
                 ellipsoid((13, 11, 8), (-19, -8, PT + 6)),
                 ellipsoid((12, 10, 10), (-12, 16, PT + 10)), k=4)
    rock = rock.displace(lambda p: 0.45 * np.sin(p[:, 0] * 0.55) * np.sin(p[:, 1] * 0.6 + p[:, 2] * 0.3) * np.sin(p[:, 2] * 0.5))
    weed = []
    for x0, y0, sx in ((-22, 17, -1), (24, 13, 1)):
        pts = [(x0 + sx * 2.5 * math.sin(t * 1.3), y0 + 2 * math.cos(t), PT + 6 + t * 7.5) for t in np.linspace(0, 5, 9)]
        weed.append(tube(pts, list(np.linspace(2.8, 1.2, 9))))
    rock = union(rock, *weed, k=1.5)
    bz = PT + 51
    fish = ellipsoid((31, 29, 29), (0, -1, bz))
    jaw = ellipsoid((24, 12, 7), (0, -21, bz - 17))
    fish = union(fish, jaw, k=4)
    mouth = ellipsoid((23, 13, 7.5), (0, -33, bz - 9))
    fish = difference(fish, mouth, k=1.6)
    teeth = []
    for i, x in enumerate(np.arange(-15, 15.1, 6.0)):
        yf = -1 - 29 * math.sqrt(max(1 - (x / 31) ** 2 - (4 / 29) ** 2, 0.05)) + 2.6
        teeth.append(round_cone((x, yf, bz + 6.0), (x, yf - 0.6, bz - 1.5), 2.0, 0.35))
    for x in np.arange(-12, 12.1, 6.0):
        yf = -1 - 29 * math.sqrt(max(1 - (x / 31) ** 2 - (12 / 29) ** 2, 0.05)) + 3.6
        teeth.append(round_cone((x, yf, bz - 16.0), (x, yf - 0.6, bz - 9.5), 1.8, 0.35))
    fish = union(fish, *teeth, k=0.4)
    eyes = union(*[sphere(7.0, (s * 12.5, -21, bz + 14)) for s in (-1, 1)])
    pupils = union(*[sphere(3.0, (s * 12.8, -27.4, bz + 14.2)) for s in (-1, 1)])
    brows = union(*[capsule((s * 5.5, -26.2, bz + 21), (s * 19.5, -21, bz + 25), 1.6) for s in (-1, 1)])
    fish = union(fish, eyes, k=1.5)
    fish = union(fish, pupils, brows, k=0.5)
    dorsal = union(*[round_cone((0, 4 + 7 * k, bz + 28.5 - 3 * k), (0, 11 + 8 * k, bz + 38 - 4 * k), 3.0, 0.7) for k in range(3)])
    pect = union(*[ellipsoid((2.4, 11, 8)).rot('z', s * -25).rot('y', s * 15).move(s * 29.5, 5, bz - 4) for s in (-1, 1)])
    tail = union(ellipsoid((2.6, 11, 6.5)).rot('x', 35).move(0, 32, bz + 5),
                 ellipsoid((2.6, 11, 6.5)).rot('x', -35).move(0, 32, bz - 5),
                 ellipsoid((5, 8, 6.5), (0, 26, bz)), k=2)
    fish = union(fish, dorsal, pect, tail, k=2.0)
    # the lure: a fishing rod growing from the forehead with an e-mail on it
    rod = tube([(0, -12, bz + 25), (0, -18, bz + 38), (0, -29, bz + 46), (0, -41, bz + 45), (0, -46.5, bz + 40)],
               [2.4, 2.0, 1.8, 1.7, 1.6], k=0.5)
    env = box((17, 3.6, 12), c=(0, -46.5, bz + 33), r=1.2)
    env = engrave(env, poly2_line([(-7.4, bz + 38.2), (0, bz + 32.5), (7.4, bz + 38.2)], 0.45), 0.7, front='-y')
    env = engrave(env, u2(seg2((-7.4, bz + 27.8), (-2.6, bz + 32.0), 0.4), seg2((7.4, bz + 27.8), (2.6, bz + 32.0), 0.4)), 0.5, front='-y')
    fish = union(fish, rod, k=1.0)
    fish = union(fish, env, k=0.8)
    solid = union(base_plinth(W, D), rock, k=2.0)
    solid = union(solid, fish, k=6)
    return _fig('07_phishing', 'PHISHING', 'SOCIAL ENGINEERING', solid, W, D, bz + 50,
                (0.20, 0.45, 0.85), extra=8,
                back=[('CLICK HERE TO WIN', 3.2)])


# =========================================================================== 8
def log4shell():
    W, D = 72, 60
    R = 23.5
    top = PT + 73
    log = cylinder(R, PT - 1, top, rr=1.5)
    roots = union(*[round_cone((19 * math.cos(math.radians(a)), 19 * math.sin(math.radians(a)), PT + 12),
                               (31 * math.cos(math.radians(a)), 31 * math.sin(math.radians(a)), PT + 1.5), 7.5, 4.0)
                    for a in (30, 150, 215, 325)])
    branch = round_cone((17, 4, PT + 41), (31, 6, PT + 51), 6.5, 5.2)
    log = union(log, roots, k=4)
    log = union(log, branch, k=3)

    def bark(p):
        th = np.arctan2(p[:, 1], p[:, 0])
        z = p[:, 2]
        g = 0.5 + 0.5 * np.sin(22 * th + 1.7 * np.sin(z / 7.0) + 0.8 * np.sin(5 * th + z / 13.0))
        g = g ** 1.5
        side = 1 - smoothstep(top - 2.5, top - 0.5, z)
        return F(0.85) * g * side
    log = log.displace(bark)
    # end grain with growth rings and a check crack
    rings = u2(*[lambda u, v, rr=rr: np.abs(np.sqrt(u * u + v * v) - rr) - 0.42 for rr in (4.5, 9.0, 13.0, 17.0, 20.6)])
    rings = u2(rings, c2(1.2), seg2((2.5, 1.5), (18.5, 9.5), 0.55), seg2((-3, -2), (-15, -12.5), 0.5))
    log = engrave(log, rings, 0.7, plane='xy', front='+z')
    # a little sign nailed to the trunk with a shell prompt on it
    sign = box((25, 5, 18), c=(0, -22.6, PT + 40), r=1.6).rot('z', 0)
    prompt = u2(poly2_line([(-8.0, PT + 45), (-2.0, PT + 40), (-8.0, PT + 35)], 1.1), seg2((1.0, PT + 35), (8.5, PT + 35), 1.1))
    sign = engrave(sign, prompt, 1.2, front='-y')
    nails = union(*[sphere(1.3, (x, -25.0, z)) for x in (-10, 10) for z in (PT + 33.5, PT + 46.5)])
    sign = union(sign, nails, k=0.3)
    log = union(log, sign, k=0.8)
    # a snail crawling over the end grain: the "shell" of Log4Shell
    sp = []
    th_max = 3.4 * math.pi
    sc = (0.0, 6.0, top + 19.5)
    for th in np.arange(0.0, th_max + 1e-6, 0.06):
        rho = 9.8 * math.exp(0.2 * (th - th_max))
        rt = 0.80 * rho + 0.35
        ang = th + (-2.05 - th_max)
        sp.append(sphere(rt, (0, sc[1] + rho * math.cos(ang), sc[2] + rho * math.sin(ang))))
    shell = union(*sp, k=1.4)
    shell = bounded(shell, sc, 20)
    foot = union(ellipsoid((7.5, 20, 4.6), (0, -3, top + 3.0)),
                 ellipsoid((6.6, 7.5, 6.8), (0, -19, top + 7.5)).rot('x', -20, center=(0, -19, top + 7.5)), k=3)
    stalks = union(*[tube([(s * 2.4, -21, top + 12), (s * 3.6, -23, top + 17), (s * 4.6, -23.5, top + 21)], [1.25, 1.1, 1.0])
                     for s in (-1, 1)], *[sphere(2.2, (s * 4.7, -23.6, top + 22)) for s in (-1, 1)], k=0.6)
    snail = union(foot, stalks, k=1.0)
    snail = engrave(snail, arc2(3.0, 0.55, 210, 330, c=(0, top + 8.0)), 0.7, front='-y')
    snail = union(snail, shell, k=1.2)
    solid = union(base_plinth(W, D), log, k=2.0)
    solid = union(solid, snail, k=1.5)
    return _fig('08_log4shell', 'LOG4SHELL', 'CVE-2021-44228', solid, W, D, top + 40,
                (0.55, 0.38, 0.22),
                back=[('${JNDI:LDAP://}', 3.4)])


# =========================================================================== 9
def spectre():
    W, D = 72, 64
    chip = box((48, 48, 6.0), c=(0, 0, PT + 2.5), r=1.0)
    chip = engrave(chip, u2(sub2(rect2(26, 26, r=2), rect2(24.8, 24.8, r=1.4)), c2(1.6, (-19, -19))), 0.6, plane='xy', front='+z')

    def pins(p):
        x, y, z = p[:, 0], p[:, 1], p[:, 2]
        out = None
        for a, b in ((x, y), (y, x)):
            q = a - 4.6 * np.clip(np.round(a / 4.6), -4, 4)
            dx = np.abs(q) - 1.1
            dy = np.abs(np.abs(b) - 26.0) - 3.2
            dz = np.abs(z - (PT + 0.6)) - 0.6
            d = np.maximum(np.maximum(dx, dy), dz)
            out = d if out is None else np.minimum(out, d)
        return out
    chip = union(chip, SDF(pins))
    wisp = union(round_cone((0, 0, PT + 4), (0, 0, PT + 10), 19.5, 16.6),
                 round_cone((0, 0, PT + 10), (0, 0, PT + 22), 16.6, 17.5), k=3)

    def swirl(p):
        th = np.arctan2(p[:, 1], p[:, 0])
        ph = th / (2 * math.pi) * 4 + p[:, 2] / 9.0
        m = smoothstep(PT + 6, PT + 9, p[:, 2]) * (1 - smoothstep(PT + 18, PT + 22, p[:, 2]))
        return F(0.8) * pulse(ph, 1.0, 0.08) * m
    wisp = wisp.displace(swirl)
    gz = PT + 50
    hem = PT + 21
    body = union(sphere(26.5, (0, 0, gz)), cylinder(26.5, hem + 2, gz, rr=5), k=0)
    lobes = union(*[sphere(6.4, (22.0 * math.cos(math.radians(22.5 + 45 * k)), 22.0 * math.sin(math.radians(22.5 + 45 * k)), hem + 1.5))
                    for k in range(8)])
    ghost = union(body, lobes, k=3.0)
    ghost = engrave(ghost, u2(ell2(4.4, 6.8, c=(-9, gz + 5)), ell2(4.4, 6.8, c=(9, gz + 5))), 3.0, front='-y')
    ghost = engrave(ghost, ell2(3.2, 4.2, c=(0, gz - 8)), 2.4, front='-y')
    arms = union(*[round_cone((s * 23, -2, gz - 8), (s * 33, -8, gz - 13), 6.2, 4.6) for s in (-1, 1)])
    ghost = union(ghost, arms, k=2.5)
    ghost = union(ghost, wisp, k=5)
    hx, hy, hz = -33.5, -8.5, gz - 13
    stick = tube([(hx, hy, hz - 4), (hx - 2.5, hy - 2.5, hz + 12), (hx - 4.5, hy - 3.5, hz + 27), (hx - 3.5, hy - 4.5, hz + 39)],
                 [2.1, 1.9, 1.6, 1.3])
    twig1 = tube([(hx - 3.0, hy - 2.8, hz + 16), (hx - 10.0, hy - 5.0, hz + 25)], [1.3, 0.9])
    twig2 = tube([(hx - 4.0, hy - 3.8, hz + 31), (hx + 1.5, hy - 5.5, hz + 38)], [1.1, 0.8])
    leaves = union(ellipsoid((1.0, 2.4, 4.0)).rot('y', -40).move(hx - 11.5, hy - 5.4, hz + 27.5),
                   ellipsoid((1.0, 2.4, 4.0)).rot('y', 35).move(hx + 3.0, hy - 5.8, hz + 40.5),
                   ellipsoid((1.0, 2.4, 4.0)).rot('y', 10).move(hx - 3.0, hy - 4.8, hz + 42.5))
    branch = union(stick, twig1, twig2, k=0.6)
    branch = union(branch, leaves, k=0.5)
    ghost = union(ghost, branch, k=1.2)
    solid = union(base_plinth(W, D), chip, k=0.6)
    solid = union(solid, ghost, k=2.5)
    return _fig('09_spectre', 'SPECTRE', 'CVE-2017-5753', solid, W, D, gz + 40,
                (0.78, 0.84, 0.92), extra=6,
                back=[('BRANCH PREDICTION', 3.0), ('IS A FEATURE', 3.0)])


# =========================================================================== 10
def zeroday():
    W, D = 72, 60
    cz = PT + 42
    R = 33.0
    ball = sphere(R, (0, 0, cz))
    skull = u2(c2(8.6, (0, cz + 3.5)), rect2(10.5, 8.5, c=(0, cz - 4.5), r=2.4), k=1.5)
    skull = sub2(skull, c2(2.7, (-3.6, cz + 2.8)), c2(2.7, (3.6, cz + 2.8)),
                 polygon2([(-1.5, cz - 0.6), (1.5, cz - 0.6), (0, cz - 3.0)]),
                 *[seg2((x, cz - 9.5), (x, cz - 5.6), 0.45) for x in (-2.6, 0, 2.6)])
    bones = []
    for (x0, z0), (x1, z1) in (((-13, cz - 12.5), (13, cz + 12.5)), ((-13, cz + 12.5), (13, cz - 12.5))):
        bones.append(seg2((x0, z0), (x1, z1), 1.9))
        dx, dz = x1 - x0, z1 - z0
        L = math.hypot(dx, dz)
        nx, nz = -dz / L * 1.9, dx / L * 1.9
        for (ex, ez) in ((x0, z0), (x1, z1)):
            bones.append(c2(2.45, (ex + nx, ez + nz)))
            bones.append(c2(2.45, (ex - nx, ez - nz)))
    bones = sub2(u2(*bones), u2(c2(10.4, (0, cz + 3.5)), rect2(12.5, 10.5, c=(0, cz - 4.5), r=3)))
    ball = emboss(ball, u2(skull, bones), 1.5, front='-y', k=0.3)
    zero = u2(lambda u, v: np.abs(ell2(8.5, 12, c=(0, cz))(u, v)) - 1.7, seg2((-6.5, cz - 11), (6.5, cz + 11), 1.5))
    ball = emboss(ball, zero, 1.5, front='+y', k=0.3)
    stand = cylinder(20.0, PT - 1, PT + 13, rr=2)
    ring = torus(20.5, 2.0, (0, 0, PT + 3.5))
    collar = cylinder(10.5, cz + 26, cz + 38, rr=1.5)
    lip = torus(10.6, 1.7, (0, 0, cz + 36.6))
    fuse_pts = [(0, 0, cz + 37), (0, -2, cz + 46), (-5, -5, cz + 54), (-11, -3, cz + 59), (-14.5, 2, cz + 61)]
    fuse = tube(fuse_pts, [2.5, 2.4, 2.3, 2.2, 2.1], k=0.6)
    fuse = fuse.displace(lambda p: 0.35 * pulse((p[:, 0] + p[:, 1]) * 0.55 + p[:, 2] * 0.55, 1.6, 0.4))
    sc = np.array((-15.5, 3.2, cz + 62.0))
    spark = [sphere(3.3, sc)]
    golden = math.pi * (3 - math.sqrt(5))
    for i in range(12):
        zz = 1 - 2 * (i + 0.5) / 12
        rr = math.sqrt(1 - zz * zz)
        a = golden * i
        dvec = np.array((rr * math.cos(a), rr * math.sin(a), zz))
        spark.append(round_cone(sc, sc + dvec * (7.5 if i % 2 else 5.5), 1.4, 0.35))
    spark = union(*spark, k=0.5)
    bomb = union(ball, stand, k=6)
    bomb = union(bomb, ring, k=1)
    bomb = union(bomb, collar, k=3)
    bomb = union(bomb, lip, k=0.5)
    bomb = union(bomb, fuse, k=1.0)
    bomb = union(bomb, spark, k=0.8)
    solid = union(base_plinth(W, D), bomb, k=2.0)
    return _fig('10_zeroday', 'ZERO-DAY', 'NO PATCH AVAILABLE', solid, W, D, cz + 72,
                (0.12, 0.12, 0.14),
                back=[('DAYS SINCE DISCLOSURE: 0', 3.0)])


ALL = [trojan, wannacry, morris_worm, phage, spyware, heartbleed, phishing, log4shell, spectre, zeroday]
BY_KEY = {f.__name__: f for f in ALL}
