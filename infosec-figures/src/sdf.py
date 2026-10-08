"""Minimal signed-distance-field modelling kit (numpy).

Every shape is an `SDF` wrapping f(p) -> d, where p is an (N, 3) float32 array
of points in millimetres and d is negative inside the solid.  Shapes are
combined with (smooth) boolean operators and finally meshed with
`mesh_sdf`, which evaluates the field only in a narrow band around the
surface and runs marching cubes on it.
"""
import math
import os
from concurrent.futures import ProcessPoolExecutor

import numpy as np

F = np.float32


# --------------------------------------------------------------------------- core

class SDF:
    def __init__(self, f):
        self.f = f

    def __call__(self, p):
        return self.f(p)

    def __or__(self, o):
        return union(self, o)

    def __and__(self, o):
        return intersect(self, o)

    def __sub__(self, o):
        return difference(self, o)

    # transforms (applied to the shape, i.e. inverse applied to the points)
    def move(self, x=0.0, y=0.0, z=0.0):
        t = np.array([x, y, z], F)
        f = self.f
        return SDF(lambda p: f(p - t))

    def rot(self, axis, deg, center=(0, 0, 0)):
        m = rot_matrix(axis, deg)
        c = np.array(center, F)
        f = self.f
        return SDF(lambda p: f((p - c) @ m + c))

    def matrix(self, m, center=(0, 0, 0)):
        """Apply rotation matrix m (object space -> world space)."""
        m = np.asarray(m, F)
        c = np.array(center, F)
        f = self.f
        return SDF(lambda p: f((p - c) @ m + c))

    def scale(self, s, center=(0, 0, 0)):
        """Uniform or per-axis scale.  Per-axis scaling is not an exact
        distance anymore but keeps the zero set exact."""
        c = np.array(center, F)
        f = self.f
        if np.isscalar(s):
            s = F(s)
            return SDF(lambda p: f((p - c) / s + c) * s)
        s = np.array(s, F)
        k = F(s.min())
        return SDF(lambda p: f((p - c) / s + c) * k)

    def mirror_x(self):
        f = self.f

        def g(p):
            q = p.copy()
            q[:, 0] = np.abs(q[:, 0])
            return f(q)
        return SDF(g)

    def offset(self, r):
        """Grow (r > 0) or shrink (r < 0) the solid."""
        f = self.f
        r = F(r)
        return SDF(lambda p: f(p) - r)

    def shell(self, t):
        f = self.f
        t = F(t)
        return SDF(lambda p: np.abs(f(p)) - t)

    def displace(self, g):
        """Add a displacement field g(p) to the distance (g > 0 carves in)."""
        f = self.f
        return SDF(lambda p: f(p) + g(p))


def rot_matrix(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == 'x':
        m = [[1, 0, 0], [0, c, -s], [0, s, c]]
    elif axis == 'y':
        m = [[c, 0, s], [0, 1, 0], [-s, 0, c]]
    else:
        m = [[c, -s, 0], [s, c, 0], [0, 0, 1]]
    return np.array(m, F)


def look_matrix(direction, up=(0, 0, 1)):
    """Matrix whose local +Z axis points along `direction`."""
    z = np.array(direction, float)
    z /= np.linalg.norm(z)
    upv = np.array(up, float)
    if abs(np.dot(upv, z)) > 0.99:
        upv = np.array([0, 1, 0], float)
    x = np.cross(upv, z)
    x /= np.linalg.norm(x)
    y = np.cross(z, x)
    return np.stack([x, y, z], axis=1).astype(F)


def _len(v):
    return np.sqrt(np.einsum('ij,ij->i', v, v))


def smin(a, b, k):
    if k <= 0:
        return np.minimum(a, b)
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0.0, 1.0)
    return b * (1 - h) + a * h - k * h * (1 - h)


def smax(a, b, k):
    return -smin(-a, -b, k)


# --------------------------------------------------------------------------- booleans

def union(*shapes, k=0.0):
    shapes = [s for s in shapes if s is not None]
    fs = [s.f for s in shapes]
    k = F(k)

    def g(p):
        d = fs[0](p)
        for f in fs[1:]:
            d = smin(d, f(p), k)
        return d
    return SDF(g)


def intersect(*shapes, k=0.0):
    fs = [s.f for s in shapes]
    k = F(k)

    def g(p):
        d = fs[0](p)
        for f in fs[1:]:
            d = smax(d, f(p), k)
        return d
    return SDF(g)


def difference(a, *bs, k=0.0):
    fa = a.f
    fbs = [b.f for b in bs]
    k = F(k)

    def g(p):
        d = fa(p)
        for fb in fbs:
            d = smax(d, -fb(p), k)
        return d
    return SDF(g)


def engrave(base, mark2d, depth, plane='xz', front=None, width=0.0):
    """Carve a 2D mark into the surface of `base`, following its curvature.

    mark2d(u, v) is a 2D SDF in the projection plane; `front` limits the
    carving to one side ('-y', '+y', '+x', '-x', '+z').  Removes material
    that is inside the mark and within `depth` of the surface.
    """
    fb = base.f
    depth = F(depth)
    width = F(width)
    ax = _front_axis(front)

    def g(p):
        d = fb(p)
        u, v = _proj(p, plane)
        m = mark2d(u, v) - width
        region = np.maximum(m, -(d + depth))
        if ax is not None:
            region = np.maximum(region, -ax(p))
        return np.maximum(d, -region)
    return SDF(g)


def emboss(base, mark2d, height, plane='xz', front=None, k=0.0):
    """Raise a 2D mark `height` above the surface of `base` (projected)."""
    fb = base.f
    height = F(height)
    ax = _front_axis(front)

    def g(p):
        d = fb(p)
        u, v = _proj(p, plane)
        relief = np.maximum(mark2d(u, v), d - height)
        if ax is not None:
            relief = np.maximum(relief, -ax(p))
        return smin(d, relief, F(k))
    return SDF(g)


def _proj(p, plane):
    if plane == 'xz':
        return p[:, 0], p[:, 2]
    if plane == 'yz':
        return p[:, 1], p[:, 2]
    return p[:, 0], p[:, 1]


def _front_axis(front):
    """Returns a function > 0 on the requested side."""
    if front is None:
        return None
    sign = -1.0 if front[0] == '-' else 1.0
    i = 'xyz'.index(front[1])
    return lambda p: sign * p[:, i]


# --------------------------------------------------------------------------- 3D primitives

def sphere(r, c=(0, 0, 0)):
    c = np.array(c, F)
    r = F(r)
    return SDF(lambda p: _len(p - c) - r)


def ellipsoid(radii, c=(0, 0, 0)):
    c = np.array(c, F)
    rr = np.array(radii, F)

    def g(p):
        q = p - c
        k0 = _len(q / rr)
        k1 = _len(q / (rr * rr))
        return k0 * (k0 - 1.0) / np.maximum(k1, 1e-6)
    return SDF(g)


def box(size, c=(0, 0, 0), r=0.0):
    """Box with full edge lengths `size`, optionally rounded by r."""
    b = np.array(size, F) / 2 - F(r)
    c = np.array(c, F)
    r = F(r)

    def g(p):
        q = np.abs(p - c) - b
        out = _len(np.maximum(q, 0))
        ins = np.minimum(q.max(axis=1), 0)
        return out + ins - r
    return SDF(g)


def cylinder(r, z0, z1, c=(0, 0), rr=0.0):
    """Vertical cylinder radius r between z0 and z1, edge-rounded by rr."""
    cx, cy = F(c[0]), F(c[1])
    h = F((z1 - z0) / 2)
    zc = F((z1 + z0) / 2)
    r = F(r)
    rr = F(rr)

    def g(p):
        dx = np.sqrt((p[:, 0] - cx) ** 2 + (p[:, 1] - cy) ** 2) - r + rr
        dz = np.abs(p[:, 2] - zc) - h + rr
        out = np.sqrt(np.maximum(dx, 0) ** 2 + np.maximum(dz, 0) ** 2)
        return out + np.minimum(np.maximum(dx, dz), 0) - rr
    return SDF(g)


def capsule(a, b, r):
    a = np.array(a, F)
    b = np.array(b, F)
    ba = b - a
    bb = F(np.dot(ba, ba))
    r = F(r)

    def g(p):
        pa = p - a
        h = np.clip((pa @ ba) / bb, 0, 1)
        return _len(pa - h[:, None] * ba) - r
    return SDF(g)


def round_cone(a, b, ra, rb):
    """Cone between spheres (a, ra) and (b, rb) (iq's exact sdRoundCone)."""
    a = np.array(a, F)
    b = np.array(b, F)
    ra, rb = F(ra), F(rb)
    ba = b - a
    l2 = F(np.dot(ba, ba))
    rr = ra - rb
    a2 = l2 - rr * rr
    il2 = F(1.0 / l2)

    def g(p):
        pa = p - a
        y = pa @ ba
        z = y - l2
        xv = pa * l2 - y[:, None] * ba
        x2 = np.einsum('ij,ij->i', xv, xv)
        y2 = y * y * l2
        z2 = z * z * l2
        k = np.sign(rr) * rr * rr * x2
        d = np.empty(len(p), F)
        m1 = np.sign(z) * a2 * z2 > k
        m2 = np.sign(y) * a2 * y2 < k
        m3 = ~(m1 | m2)
        d[m1] = np.sqrt(x2[m1] + z2[m1]) * il2 - rb
        d[m2] = np.sqrt(x2[m2] + y2[m2]) * il2 - ra
        d[m3] = (np.sqrt(x2[m3] * a2 * il2) + y[m3] * rr) * il2 - ra
        return d
    return SDF(g)


def tube(points, radii, k=0.0):
    """Chain of round cones through `points` with per-point radii."""
    if np.isscalar(radii):
        radii = [radii] * len(points)
    parts = [round_cone(points[i], points[i + 1], radii[i], radii[i + 1])
             for i in range(len(points) - 1)]
    return union(*parts, k=k)


def torus(R, r, c=(0, 0, 0)):
    """Torus in the XY plane (axis Z)."""
    c = np.array(c, F)
    R, r = F(R), F(r)

    def g(p):
        q = p - c
        a = np.sqrt(q[:, 0] ** 2 + q[:, 1] ** 2) - R
        return np.sqrt(a * a + q[:, 2] ** 2) - r
    return SDF(g)


def halfspace(normal, point=(0, 0, 0)):
    """Solid on the side opposite to `normal`."""
    n = np.array(normal, F)
    n /= np.linalg.norm(n)
    d0 = F(np.dot(n, np.array(point, F)))
    return SDF(lambda p: p @ n - d0)


def convex_polyhedron(points):
    """Convex hull of points; exact inside, plane-max outside."""
    from scipy.spatial import ConvexHull
    hull = ConvexHull(np.asarray(points, float))
    eq = hull.equations.astype(F)       # n.x + d <= 0 inside
    n = eq[:, :3]
    d = eq[:, 3]
    return SDF(lambda p: (p @ n.T + d).max(axis=1)), hull


def extrude(shape2d, z0, z1, plane='xy'):
    """Extrude a 2D SDF along the remaining axis between z0 and z1."""
    h = F((z1 - z0) / 2)
    zc = F((z1 + z0) / 2)
    idx = {'xy': (0, 1, 2), 'xz': (0, 2, 1), 'yz': (1, 2, 0)}[plane]

    def g(p):
        d2 = shape2d(p[:, idx[0]], p[:, idx[1]])
        dz = np.abs(p[:, idx[2]] - zc) - h
        out = np.sqrt(np.maximum(d2, 0) ** 2 + np.maximum(dz, 0) ** 2)
        return out + np.minimum(np.maximum(d2, dz), 0)
    return SDF(g)


def revolve(shape2d):
    """Revolve a 2D SDF given in (r, z) around the Z axis."""
    def g(p):
        r = np.sqrt(p[:, 0] ** 2 + p[:, 1] ** 2)
        return shape2d(r, p[:, 2])
    return SDF(g)


# --------------------------------------------------------------------------- 2D primitives (u, v arrays)

def c2(r, c=(0, 0)):
    cu, cv, r = F(c[0]), F(c[1]), F(r)
    return lambda u, v: np.sqrt((u - cu) ** 2 + (v - cv) ** 2) - r


def ell2(ru, rv, c=(0, 0)):
    cu, cv = F(c[0]), F(c[1])
    ru, rv = F(ru), F(rv)

    def g(u, v):
        x = (u - cu) / ru
        y = (v - cv) / rv
        k0 = np.sqrt(x * x + y * y)
        k1 = np.sqrt((x / ru) ** 2 + (y / rv) ** 2)
        return k0 * (k0 - 1) / np.maximum(k1, 1e-6)
    return g


def rect2(w, h, c=(0, 0), r=0.0, angle=0.0):
    hw, hh = F(w / 2 - r), F(h / 2 - r)
    cu, cv, r = F(c[0]), F(c[1]), F(r)
    ca, sa = F(math.cos(math.radians(angle))), F(math.sin(math.radians(angle)))

    def g(u, v):
        x = u - cu
        y = v - cv
        x, y = x * ca + y * sa, -x * sa + y * ca
        qx = np.abs(x) - hw
        qy = np.abs(y) - hh
        out = np.sqrt(np.maximum(qx, 0) ** 2 + np.maximum(qy, 0) ** 2)
        return out + np.minimum(np.maximum(qx, qy), 0) - r
    return g


def seg2(a, b, r):
    ax, ay = F(a[0]), F(a[1])
    bx, by = F(b[0]), F(b[1])
    dx, dy = bx - ax, by - ay
    dd = F(dx * dx + dy * dy)
    r = F(r)

    def g(u, v):
        px, py = u - ax, v - ay
        h = np.clip((px * dx + py * dy) / dd, 0, 1)
        return np.sqrt((px - h * dx) ** 2 + (py - h * dy) ** 2) - r
    return g


def poly2_line(points, r):
    segs = [seg2(points[i], points[i + 1], r) for i in range(len(points) - 1)]
    return u2(*segs)


def arc2(R, r, a0, a1, c=(0, 0)):
    """Stroke of radius r along a circular arc (angles in degrees, CCW)."""
    cu, cv = F(c[0]), F(c[1])
    R, r = F(R), F(r)
    a0, a1 = math.radians(a0), math.radians(a1)
    mid = (a0 + a1) / 2
    half = (a1 - a0) / 2
    pa = (cu + R * math.cos(a0), cv + R * math.sin(a0))
    pb = (cu + R * math.cos(a1), cv + R * math.sin(a1))
    ea, eb = c2(r, pa), c2(r, pb)

    def g(u, v):
        x, y = u - cu, v - cv
        ang = np.arctan2(y, x) - mid
        ang = (ang + math.pi) % (2 * math.pi) - math.pi
        on = np.abs(ang) <= half
        d_ring = np.abs(np.sqrt(x * x + y * y) - R) - r
        d_end = np.minimum(ea(u, v), eb(u, v))
        return np.where(on, d_ring, d_end)
    return g


def polygon2(points):
    """Exact SDF of a simple polygon (list of (u, v))."""
    pts = np.array(points, F)
    n = len(pts)

    def g(u, v):
        d = (u - pts[0, 0]) ** 2 + (v - pts[0, 1]) ** 2
        s = np.ones_like(u)
        j = n - 1
        for i in range(n):
            ex, ey = pts[j, 0] - pts[i, 0], pts[j, 1] - pts[i, 1]
            wx, wy = u - pts[i, 0], v - pts[i, 1]
            h = np.clip((wx * ex + wy * ey) / (ex * ex + ey * ey), 0, 1)
            bx, by = wx - ex * h, wy - ey * h
            d = np.minimum(d, bx * bx + by * by)
            c1 = v >= pts[i, 1]
            c2_ = v < pts[j, 1]
            c3 = ex * wy > ey * wx
            flip = (c1 & c2_ & c3) | (~c1 & ~c2_ & ~c3)
            s = np.where(flip, -s, s)
            j = i
        return s * np.sqrt(d)
    return g


def u2(*fs, k=0.0):
    k = F(k)

    def g(u, v):
        d = fs[0](u, v)
        for f in fs[1:]:
            d = smin(d, f(u, v), k)
        return d
    return g


def sub2(a, *bs):
    def g(u, v):
        d = a(u, v)
        for b in bs:
            d = np.maximum(d, -b(u, v))
        return d
    return g


def i2(*fs):
    def g(u, v):
        d = fs[0](u, v)
        for f in fs[1:]:
            d = np.maximum(d, f(u, v))
        return d
    return g


def move2(f, du, dv):
    du, dv = F(du), F(dv)
    return lambda u, v: f(u - du, v - dv)


def scale2(f, s):
    s = F(s)
    return lambda u, v: f(u / s, v / s) * s


def rot2(f, deg, c=(0, 0)):
    a = math.radians(deg)
    ca, sa = F(math.cos(a)), F(math.sin(a))
    cu, cv = F(c[0]), F(c[1])

    def g(u, v):
        x, y = u - cu, v - cv
        return f(x * ca + y * sa + cu, -x * sa + y * ca + cv)
    return g


# --------------------------------------------------------------------------- meshing

_FIELD = None


def _eval_blocks(args):
    starts, origin, h, B = args
    n = len(starts)
    o = np.arange(B + 1, dtype=F) * F(h)
    gx, gy, gz = np.meshgrid(o, o, o, indexing='ij')
    local = np.stack([gx.ravel(), gy.ravel(), gz.ravel()], axis=1)
    base = origin + starts.astype(F) * F(h)
    pts = (base[:, None, :] + local[None, :, :]).reshape(-1, 3)
    d = _FIELD(pts).astype(F)
    return d.reshape(n, B + 1, B + 1, B + 1)


def mesh_sdf(f, bounds, h=0.2, B=8, band=2.5, workers=None):
    """Mesh the zero set of f inside bounds=((x0,y0,z0),(x1,y1,z1)).

    Returns (verts, faces) with outward-facing triangles.
    """
    global _FIELD
    from skimage import measure

    lo = np.array(bounds[0], F)
    hi = np.array(bounds[1], F)
    nb = np.ceil((hi - lo) / (h * B)).astype(int)
    n = nb * B + 1
    # coarse pass: block centres
    ix, iy, iz = np.meshgrid(*[np.arange(k) for k in nb], indexing='ij')
    bidx = np.stack([ix.ravel(), iy.ravel(), iz.ravel()], axis=1)
    centers = lo + (bidx.astype(F) + 0.5) * F(h * B)
    dc = np.concatenate([f(centers[i:i + 500000]) for i in range(0, len(centers), 500000)])
    thresh = band * (h * B) * math.sqrt(3) / 2
    near = np.abs(dc) <= thresh

    vol = np.empty(tuple(n), F)
    dcg = dc.reshape(tuple(nb))
    # fill with block-centre values (sign is all marching cubes needs there)
    fill = np.repeat(np.repeat(np.repeat(dcg, B, 0), B, 1), B, 2)
    vol[:-1, :-1, :-1] = fill
    vol[-1, :, :] = vol[-2, :, :] if n[0] > 1 else 1
    vol[:, -1, :] = vol[:, -2, :]
    vol[:, :, -1] = vol[:, :, -2]
    del fill

    starts = bidx[near] * B
    _FIELD = f
    workers = workers or os.cpu_count() or 1
    chunk = 400
    jobs = [(starts[i:i + chunk], lo, h, B) for i in range(0, len(starts), chunk)]
    if workers > 1:
        import multiprocessing as mp
        with ProcessPoolExecutor(workers, mp_context=mp.get_context('fork')) as ex:
            results = list(ex.map(_eval_blocks, jobs))
    else:
        results = [_eval_blocks(j) for j in jobs]
    for job, res in zip(jobs, results):
        for s, blk in zip(job[0], res):
            vol[s[0]:s[0] + B + 1, s[1]:s[1] + B + 1, s[2]:s[2] + B + 1] = blk

    # make sure the mesh is closed at the domain boundary
    vol[0, :, :] = np.maximum(vol[0, :, :], h)
    vol[-1, :, :] = np.maximum(vol[-1, :, :], h)
    vol[:, 0, :] = np.maximum(vol[:, 0, :], h)
    vol[:, -1, :] = np.maximum(vol[:, -1, :], h)
    vol[:, :, 0] = np.maximum(vol[:, :, 0], h)
    vol[:, :, -1] = np.maximum(vol[:, :, -1], h)

    verts, faces, _, _ = measure.marching_cubes(
        vol, level=0.0, spacing=(h, h, h), gradient_direction='ascent',
        allow_degenerate=False)
    verts = verts.astype(np.float64) + lo
    return verts, faces.astype(np.int64), int(near.sum())


def write_ply(path, verts, faces):
    with open(path, 'wb') as fh:
        fh.write((
            'ply\nformat binary_little_endian 1.0\n'
            f'element vertex {len(verts)}\n'
            'property float x\nproperty float y\nproperty float z\n'
            f'element face {len(faces)}\n'
            'property list uchar int vertex_indices\nend_header\n').encode())
        fh.write(np.asarray(verts, '<f4').tobytes())
        fd = np.empty(len(faces), dtype=[('n', 'u1'), ('i', '<i4', (3,))])
        fd['n'] = 3
        fd['i'] = faces
        fh.write(fd.tobytes())


# --------------------------------------------------------------------------- helpers

def bounded(s, center, radius, margin=3.0):
    """Skip evaluating `s` far away from its bounding sphere."""
    c = np.array(center, F)
    radius, margin = F(radius), F(margin)
    f = s.f

    def g(p):
        out = _len(p - c) - radius
        m = out < margin
        if m.any():
            out[m] = f(p[m])
        return out
    return SDF(g)


def pulse(x, period, half_width):
    """Triangular pulse train in [0, 1], peaks at multiples of `period`."""
    t = np.mod(x, period)
    dist = np.minimum(t, period - t)
    return np.clip(1 - dist / half_width, 0, 1)


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)
