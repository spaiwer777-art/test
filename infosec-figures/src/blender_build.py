"""Stage 2 (runs inside Blender):

    blender -b --factory-startup -P blender_build.py -- [keys...] [--cap]

* imports ../build/<key>.ply (dense marching-cubes mesh from make_mesh.py)
* decimates it to a print-friendly triangle count
* cuts the female thread for the screw cap (exact boolean)
* engraves the name plate on the plinth front and a line on the back
* checks the result is a closed 2-manifold and exports ../stl/<key>.stl
* with --cap, also builds the universal screw cap and a thread test ring
"""
import json
import math
import os
import sys

import bmesh
import bpy
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import common as C  # noqa: E402

BUILD = os.path.join(HERE, '..', 'build')
STL = os.path.join(HERE, '..', 'stl')
TARGET_TRIS = 260_000

FONT_PATHS = ['/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
              '/usr/share/fonts/TTF/DejaVuSans-Bold.ttf',
              '/Library/Fonts/Arial Bold.ttf',
              'C:/Windows/Fonts/arialbd.ttf']


# ----------------------------------------------------------------- thread geometry

def _profile(n=2000):
    """Male thread profile over one pitch, 0 = root, 1 = crest (trapezoid,
    60° included angle)."""
    flank = C.TH_DEPTH * math.tan(math.radians(30)) / C.PITCH
    crest = 0.5 / C.PITCH
    u = np.linspace(0, 1, n, endpoint=False)
    p = np.zeros(n)
    a, b, c = crest, crest + flank, 1 - flank
    p[u < a] = 1
    m = (u >= a) & (u < b)
    p[m] = 1 - (u[m] - a) / flank
    m = u >= c
    p[m] = (u[m] - c) / flank
    return u, p


def _dilate(p, frac):
    n = len(p)
    k = int(round(frac * n))
    out = p.copy()
    for s in range(-k, k + 1):
        out = np.maximum(out, np.roll(p, s))
    return out


_U, _P = _profile()
_PF = _dilate(_P, C.CLR_AX / C.PITCH)


def thread_r(theta, z, r_root, depth, prof, taper):
    u = np.mod(z / C.PITCH - theta / (2 * math.pi), 1.0)
    idx = (u * len(prof)).astype(int) % len(prof)
    return r_root + depth * prof[idx] * taper


def revolve_grid(name, rows, n_theta):
    """rows: list of (z, r) where r is a scalar, an (n_theta,) array or None
    for an axis point.  Rows run along the profile; returns a closed mesh."""
    th = np.linspace(0, 2 * math.pi, n_theta, endpoint=False)
    verts, faces, ring_idx = [], [], []
    for z, r in rows:
        if r is None:
            ring_idx.append(len(verts))
            verts.append((0.0, 0.0, z))
            continue
        r = np.broadcast_to(np.asarray(r, float), (n_theta,))
        zz = np.broadcast_to(np.asarray(z, float), (n_theta,))
        start = len(verts)
        verts.extend(zip(r * np.cos(th), r * np.sin(th), zz))
        ring_idx.append(list(range(start, start + n_theta)))
    for a, b in zip(ring_idx[:-1], ring_idx[1:]):
        if isinstance(a, int):
            faces.extend((a, b[(i + 1) % n_theta], b[i]) for i in range(n_theta))
        elif isinstance(b, int):
            faces.extend((a[i], a[(i + 1) % n_theta], b) for i in range(n_theta))
        else:
            faces.extend((a[i], a[(i + 1) % n_theta], b[(i + 1) % n_theta], b[i]) for i in range(n_theta))
    me = bpy.data.meshes.new(name)
    me.from_pydata(verts, [], faces)
    me.update()
    ob = bpy.data.objects.new(name, me)
    bpy.context.collection.objects.link(ob)
    _fix_normals(ob)
    return ob


def female_cutter(n_theta=480, dz=0.1):
    th = np.linspace(0, 2 * math.pi, n_theta, endpoint=False)
    r0 = C.TH_MINOR + C.CLR_R
    rows = [(C.CB_H - 0.8, None), (C.CB_H - 0.8, r0 + C.TH_DEPTH + 1.4)]
    z1 = C.FEM_TOP
    for z in np.arange(C.CB_H - 0.3, z1 + 1e-6, dz):
        taper = np.clip((z1 - z) / 1.2, 0, 1)
        r = thread_r(th, z, r0, C.TH_DEPTH, _PF, taper)
        chamfer = r0 + C.TH_DEPTH + 0.6 - (z - C.CB_H)
        rows.append((z, np.maximum(r, chamfer)))
    top = z1 + (r0 - (C.CAV_R - 0.5))
    rows.append((top, C.CAV_R - 0.5))
    rows.append((top, None))
    return revolve_grid('female_cutter', rows, n_theta)


def build_cap(n_theta=480, dz=0.1):
    th = np.linspace(0, 2 * math.pi, n_theta, endpoint=False)
    z0, zf = C.FLANGE_Z0, C.CB_H
    R = C.FLANGE_R
    knurl = R - 0.45 * (1 - np.abs(((th * 60 / (2 * math.pi)) % 1.0) * 2 - 1))
    rows = [(z0, None), (z0, R - 0.6), (z0 + 0.6, knurl)]
    for z in np.linspace(z0 + 0.6, zf - 0.4, 6)[1:]:
        rows.append((z, knurl))
    rows.append((zf, R - 0.4))
    zt = zf + C.TH_LEN
    for i, z in enumerate(np.arange(zf, zt + 1e-6, dz)):
        taper = np.clip((zt - z) / 1.2, 0, 1)
        r = thread_r(th, z, C.TH_MINOR, C.TH_DEPTH, _P, taper)
        rows.append((z, r))
    rows.append((zt + 0.0, C.CUP_R + 0.6))
    rows.append((zt - 0.6, C.CUP_R))
    rows.append((zf + 0.5, C.CUP_R))
    rows.append((zf, C.CUP_R - 0.5))
    rows.append((zf, None))
    cap = revolve_grid('cap', rows, n_theta)
    # coin / screwdriver slot in the bottom face
    slot = add_box('slot', (26.0, 2.6, 3.0), (0, 0, z0))
    boolean(cap, slot, 'DIFFERENCE')
    return cap


# ----------------------------------------------------------------- blender helpers

def _fix_normals(ob):
    bm = bmesh.new()
    bm.from_mesh(ob.data)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(ob.data)
    bm.free()


def add_box(name, size, center):
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=center)
    ob = bpy.context.active_object
    ob.name = name
    ob.scale = size
    bpy.ops.object.transform_apply(scale=True)
    return ob


def add_cyl(name, r, z0, z1, n=256):
    bpy.ops.mesh.primitive_cylinder_add(vertices=n, radius=r, depth=z1 - z0, location=(0, 0, (z0 + z1) / 2))
    ob = bpy.context.active_object
    ob.name = name
    return ob


def select_only(ob):
    bpy.ops.object.select_all(action='DESELECT')
    ob.select_set(True)
    bpy.context.view_layer.objects.active = ob


def boolean(target, cutter, op, self_intersect=False):
    select_only(target)
    m = target.modifiers.new('bool', 'BOOLEAN')
    m.operation = op
    m.solver = 'EXACT'
    m.use_self = self_intersect
    m.object = cutter
    bpy.ops.object.modifier_apply(modifier=m.name)
    bpy.data.objects.remove(cutter, do_unlink=True)


def decimate(ob, target):
    n = len(ob.data.polygons)
    if n <= target:
        return
    select_only(ob)
    m = ob.modifiers.new('dec', 'DECIMATE')
    m.decimate_type = 'COLLAPSE'
    m.ratio = target / n
    m.use_collapse_triangulate = True
    bpy.ops.object.modifier_apply(modifier=m.name)


def load_font():
    for p in FONT_PATHS:
        if os.path.exists(p):
            return bpy.data.fonts.load(p)
    return None


def text_mesh(body, cap_h, max_w, font, depth):
    cu = bpy.data.curves.new('txt', 'FONT')
    cu.body = body
    if font:
        cu.font = font
    cu.align_x = 'CENTER'
    cu.size = 10.0
    cu.extrude = depth / 2
    cu.resolution_u = 6
    ob = bpy.data.objects.new('txt', cu)
    bpy.context.collection.objects.link(ob)
    select_only(ob)
    bpy.ops.object.convert(target='MESH')
    ob = bpy.context.active_object
    co = np.array([v.co[:] for v in ob.data.vertices])
    # cap height measured on upper-case letters: use the glyph box height
    h = co[:, 1].max() - co[:, 1].min()
    w = co[:, 0].max() - co[:, 0].min()
    s = min(cap_h / h, max_w / w)
    for v in ob.data.vertices:
        v.co.x *= s
        v.co.y = (v.co.y - co[:, 1].min()) * s
    bm = bmesh.new()
    bm.from_mesh(ob.data)
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=1e-4)
    bm.to_mesh(ob.data)
    bm.free()
    return ob, h * s


def place_text(lines, face_y, back, font, plinth_w, depth=0.9):
    """lines: [(text, cap_height)], laid out top to bottom inside the plinth
    text band.  Returns one joined cutter object."""
    band_lo, band_hi = 1.5, C.PLINTH_H - 3.2 - 0.9
    max_w = plinth_w - 2 * 8.0 - 4.0
    objs = []
    for body, ch in lines:
        ob, real_h = text_mesh(body, ch, max_w, font, depth + 1.5)
        objs.append((ob, real_h))
    gap = 1.3
    total = sum(h for _, h in objs) + gap * (len(objs) - 1)
    z = (band_lo + band_hi) / 2 + total / 2
    for ob, h in objs:
        z -= h
        ob.rotation_euler = (math.radians(90), 0, math.radians(180) if back else 0)
        # extrusion spans local z in [-e, e]; rotated, local z -> -y (front)
        off = (depth + 1.5) / 2 - depth
        ob.location = (0, face_y + (off if back else -off), z)
        z -= gap
    select_only(objs[0][0])
    for ob, _ in objs[1:]:
        ob.select_set(True)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    if len(objs) > 1:
        bpy.ops.object.join()
    return bpy.context.active_object


def clean_mesh(ob):
    """Weld, dissolve zero-area slivers left by the booleans and drop every
    shell except the main one (marching-cubes crumbs, empty bubbles)."""
    bm = bmesh.new()
    bm.from_mesh(ob.data)
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=1e-5)
    bmesh.ops.dissolve_degenerate(bm, edges=bm.edges, dist=1e-5)
    bm.faces.ensure_lookup_table()
    seen, comps = set(), []
    for f in bm.faces:
        if f.index in seen:
            continue
        stack, comp = [f], []
        seen.add(f.index)
        while stack:
            g = stack.pop()
            comp.append(g)
            for e in g.edges:
                for h in e.link_faces:
                    if h.index not in seen:
                        seen.add(h.index)
                        stack.append(h)
        comps.append(comp)
    comps.sort(key=len, reverse=True)
    drop = [f for c in comps[1:] for f in c]
    if drop:
        bmesh.ops.delete(bm, geom=drop, context='FACES')
    loose = [v for v in bm.verts if not v.link_faces]
    if loose:
        bmesh.ops.delete(bm, geom=loose, context='VERTS')
    bmesh.ops.triangulate(bm, faces=bm.faces)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(ob.data)
    bm.free()
    return len(comps) - 1


def place_decal(dc, font):
    """Engraving on a flat face of the figure itself, facing -Y."""
    depth = dc.get('depth', 0.9)
    ob, h = text_mesh(dc['text'], dc['cap'], dc['width'], font, depth + 1.5)
    ob.rotation_euler = (math.radians(90), 0, 0)
    off = (depth + 1.5) / 2 - depth
    ob.location = (dc['x'], dc['y'] - off, dc['z'] - h / 2)
    select_only(ob)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    return ob


def manifold_report(ob):
    bm = bmesh.new()
    bm.from_mesh(ob.data)
    nm = sum(1 for e in bm.edges if not e.is_manifold)
    loose = sum(1 for v in bm.verts if not v.link_edges)
    bm.free()
    return nm, loose


def export_stl(ob, path):
    select_only(ob)
    bpy.ops.wm.stl_export(filepath=path, export_selected_objects=True, ascii_format=False,
                          apply_modifiers=True, global_scale=1.0)


def clear():
    bpy.ops.wm.read_factory_settings(use_empty=True)


# ----------------------------------------------------------------- figure pipeline

def build_figure(key):
    clear()
    meta = json.load(open(os.path.join(BUILD, key + '.json')))
    bpy.ops.wm.ply_import(filepath=os.path.join(BUILD, key + '.ply'))
    ob = bpy.context.selected_objects[0]
    ob.name = key
    n0 = len(ob.data.polygons)
    decimate(ob, TARGET_TRIS)
    font = load_font()
    w, d = meta['plinth']
    # engrave texts first: a later exact boolean can tear the slivers that the
    # thread cut leaves on the counterbore ceiling
    front = place_text([(meta['title'], 5.2), (meta['sub'], 2.6)], -d / 2, False, font, w)
    boolean(ob, front, 'DIFFERENCE', self_intersect=True)
    if meta.get('back'):
        back = place_text([tuple(b) for b in meta['back']], d / 2, True, font, w)
        boolean(ob, back, 'DIFFERENCE', self_intersect=True)
    for dc in meta.get('decals', []):
        boolean(ob, place_decal(dc, font), 'DIFFERENCE', self_intersect=True)
    boolean(ob, female_cutter(), 'DIFFERENCE')
    dropped = clean_mesh(ob)
    nm, loose = manifold_report(ob)
    path = os.path.join(STL, key + '.stl')
    export_stl(ob, path)
    print(f'[{key}] {n0} -> {len(ob.data.polygons)} tris, dropped shells: {dropped}, '
          f'non-manifold edges: {nm}, loose: {loose} -> {path}')


def build_cap_files():
    clear()
    cap = build_cap()
    cap.location.z = -C.FLANGE_Z0
    select_only(cap)
    bpy.ops.object.transform_apply(location=True)
    clean_mesh(cap)
    nm, _ = manifold_report(cap)
    export_stl(cap, os.path.join(STL, 'cap_universal.stl'))
    print(f'[cap] {len(cap.data.polygons)} tris, non-manifold edges: {nm}')

    # thread test ring: the bottom 18 mm of a plinth with the same cut-outs
    clear()
    ring = add_cyl('ring', 27.0, 0.0, 18.0)
    boolean(ring, add_cyl('cb', C.CB_R, -1.0, C.CB_H), 'DIFFERENCE')
    boolean(ring, add_cyl('bore', C.CAV_R, -1.0, 20.0), 'DIFFERENCE')
    boolean(ring, female_cutter(), 'DIFFERENCE')
    clean_mesh(ring)
    nm, _ = manifold_report(ring)
    export_stl(ring, os.path.join(STL, 'thread_test_ring.stl'))
    print(f'[test ring] non-manifold edges: {nm}')


if __name__ == '__main__':
    argv = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
    os.makedirs(STL, exist_ok=True)
    keys = [a for a in argv if not a.startswith('--')]
    if '--clean-stl' in argv:
        # re-clean already exported STL files in place
        for f in sorted(os.listdir(STL)):
            if f.endswith('.stl'):
                clear()
                bpy.ops.wm.stl_import(filepath=os.path.join(STL, f))
                ob = bpy.context.selected_objects[0]
                d = clean_mesh(ob)
                nm, loose = manifold_report(ob)
                export_stl(ob, os.path.join(STL, f))
                print(f'[{f}] dropped shells: {d}, non-manifold edges: {nm}')
        sys.exit(0)
    if '--cap' in argv:
        build_cap_files()
    if not keys and '--cap-only' not in argv:
        keys = sorted(f[:-4] for f in os.listdir(BUILD) if f.endswith('.ply'))
    for k in keys:
        build_figure(k)
