"""Shared dimensions: flash-drive cavity, screw cap, display plinth.

All units are millimetres.  Z is up, the figure's face looks towards -Y,
the bottom of the plinth is z = 0 and the cavity axis is the Z axis.
"""
import math

import numpy as np

from sdf import (F, SDF, cylinder, box, union, difference, intersect, smin)

# ---------------------------------------------------------------- flash drive
DRIVE_MAX = (60.0, 21.0, 10.5)       # largest USB stick that fits (L x W x T)

# ---------------------------------------------------------------- cavity
CAV_R = 12.7          # Ø25.4 bore (21 x 10.5 stick has a 23.5 mm diagonal)
CAV_TOP = 67.0        # top of the cylindrical part; 45° cone above it

# ---------------------------------------------------------------- screw cap
CB_R = 20.6           # counterbore in the plinth bottom for the cap flange
CB_H = 3.6            # counterbore depth (= flange top)
FLANGE_R = 20.2       # cap flange radius (0.4 mm radial play)
FLANGE_Z0 = 0.2       # flange sits 0.2 mm recessed so the figure never rocks
CUP_R = 12.3          # cap inner bore (stick stands inside it)
PITCH = 3.0
TH_DEPTH = 1.5
TH_MINOR = 14.3       # male thread root radius (Ø28.6)
TH_MAJOR = TH_MINOR + TH_DEPTH   # Ø31.6
TH_LEN = 9.0          # male thread length (3 turns)
CLR_R = 0.25          # radial clearance per side
CLR_AX = 0.25         # axial (flank) clearance
FEM_TOP = CB_H + TH_LEN + 0.6    # top of the female thread

# ---------------------------------------------------------------- walls
MIN_WALL = 2.0

PLINTH_H = 15.0


def plinth(w=72.0, d=60.0, h=PLINTH_H, rc=8.0, chamfer=1.6, foot=0.5):
    """Rounded-rectangle display plinth with a flat front face at y = -d/2."""
    hw, hd = F(w / 2 - rc), F(d / 2 - rc)
    rc, h, chamfer, foot = F(rc), F(h), F(chamfer), F(foot)
    s2 = F(math.sqrt(0.5))

    def g(p):
        qx = np.abs(p[:, 0]) - hw
        qy = np.abs(p[:, 1]) - hd
        d2 = (np.sqrt(np.maximum(qx, 0) ** 2 + np.maximum(qy, 0) ** 2)
              + np.minimum(np.maximum(qx, qy), 0) - rc)
        z = p[:, 2]
        d = np.maximum(d2, np.abs(z - h / 2) - h / 2)
        d = np.maximum(d, (d2 + z - h + chamfer) * s2)     # top chamfer
        d = np.maximum(d, (d2 - z + foot) * s2)            # anti elephant-foot
        return d
    return SDF(g)


def plinth_groove(w, d, z, depth=0.6, width=0.8, rc=8.0):
    """Decorative ring groove around the plinth at height z."""
    hw, hd = F(w / 2 - rc), F(d / 2 - rc)
    rc, z, depth, width = F(rc), F(z), F(depth), F(width)

    def g(p):
        qx = np.abs(p[:, 0]) - hw
        qy = np.abs(p[:, 1]) - hd
        d2 = (np.sqrt(np.maximum(qx, 0) ** 2 + np.maximum(qy, 0) ** 2)
              + np.minimum(np.maximum(qx, qy), 0) - rc)
        return np.maximum(-(d2 + depth), np.abs(p[:, 2] - z) - width / 2)
    return SDF(g)


def cavity_sdf():
    """Everything the SDF stage removes: bore with 45° roof, flange counterbore
    and two finger notches.  The female thread is cut later in Blender."""
    bore = cylinder(CAV_R, -1.0, CAV_TOP)
    apex = CAV_TOP + CAV_R
    s2 = F(math.sqrt(0.5))

    def roof(p):
        r = np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)
        return np.maximum((r + p[:, 2] - apex) * s2, CAV_TOP - 1.0 - p[:, 2])
    cone = SDF(roof)
    cb = cylinder(CB_R, -1.0, CB_H)
    notches = union(box((14, 9.0, 2 * CB_H - 1.2), c=(0, CB_R, 0), r=1.0),
                    box((14, 9.0, 2 * CB_H - 1.2), c=(0, -CB_R, 0), r=1.0))
    return union(bore, cone, cb, notches)


def envelope_points(n_theta=96, dz=0.75):
    """Points on the surface that must lie inside the solid figure so that
    every wall around the cavity and thread is at least MIN_WALL thick."""
    pts = []
    th = np.linspace(0, 2 * math.pi, n_theta, endpoint=False)
    ct, st = np.cos(th), np.sin(th)

    def ring(r, z):
        for zz in np.atleast_1d(z):
            pts.append(np.stack([r * ct, r * st, np.full_like(ct, zz)], 1))

    w = MIN_WALL
    ring(TH_MAJOR + CLR_R + w, np.arange(CB_H, FEM_TOP + 0.01, dz))
    ring(CAV_R + w, np.arange(FEM_TOP, CAV_TOP + 0.01, dz))
    apex = CAV_TOP + CAV_R + w * math.sqrt(2)
    for zz in np.arange(CAV_TOP, apex, dz):
        r = apex - zz
        ring(r, zz)
    pts.append(np.array([[0, 0, apex]]))
    return np.concatenate(pts).astype(F)


def check_envelope(f):
    pts = envelope_points()
    d = f(pts)
    bad = d > -0.02
    return pts[bad], d[bad]
