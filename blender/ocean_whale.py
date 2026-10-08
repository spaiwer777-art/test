"""Ocean dive with a fish school and a humpback whale — fully procedural Blender scene.

Render (EEVEE, headless):
  EGL_PLATFORM=surfaceless python3 ocean_whale.py --out frames/ [--frames 0,120,300] [--scale 75]
"""
import argparse
import math
import random
import sys
import time

import bpy  # noqa: I001  (bpy must be imported before bmesh)
import bmesh
from mathutils import Euler, Vector

argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
ap = argparse.ArgumentParser()
ap.add_argument("--out", required=True)
ap.add_argument("--frames", default="")
ap.add_argument("--samples", type=int, default=12)
ap.add_argument("--scale", type=int, default=100)
ap.add_argument("--save-blend", default="")
args = ap.parse_args(argv)

FPS = 30
END = 420
DIVE = 121           # frame the camera crosses the surface
FOG_DIST = 16.0      # metres of water for ~63% fog
FOG_COLOR = (0.010, 0.085, 0.12, 1)
random.seed(4)

bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene
scene.frame_start, scene.frame_end = 0, END
scene.render.fps = FPS


# ---------------------------------------------------------------- helpers
def new_material(name, blended=False):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    m.node_tree.nodes.clear()
    if blended:
        m.surface_render_method = "BLENDED"
    return m


def N(tree, kind, **inputs):
    n = tree.nodes.new(kind)
    for k, v in inputs.items():
        n.inputs[k].default_value = v
    return n


def L(tree, a, b):
    tree.links.new(a, b)


def math_node(tree, op, a=None, b=None):
    n = tree.nodes.new("ShaderNodeMath")
    n.operation = op
    if isinstance(a, (int, float)):
        n.inputs[0].default_value = a
    elif a is not None:
        L(tree, a, n.inputs[0])
    if isinstance(b, (int, float)):
        n.inputs[1].default_value = b
    elif b is not None:
        L(tree, b, n.inputs[1])
    return n


def ramp(tree, stops, fac):
    r = tree.nodes.new("ShaderNodeValToRGB")
    els = r.color_ramp.elements
    while len(els) < len(stops):
        els.new(0.5)
    for e, (pos, col) in zip(els, stops):
        e.position = pos
        e.color = col
    L(tree, fac, r.inputs[0])
    return r


def mesh_object(name, bm):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    ob = bpy.data.objects.new(name, me)
    scene.collection.objects.link(ob)
    return ob


def smooth(ob):
    for p in ob.data.polygons:
        p.use_smooth = True


def key_step(socket):
    """0 above water, 1 from the dive frame on."""
    socket.default_value = 0
    socket.keyframe_insert("default_value", frame=DIVE - 1)
    socket.default_value = 1
    socket.keyframe_insert("default_value", frame=DIVE)


# ------------------------------------------------- underwater fog group
fog = bpy.data.node_groups.new("UnderwaterFog", "ShaderNodeTree")
fog.interface.new_socket("Shader", in_out="INPUT", socket_type="NodeSocketShader")
fog.interface.new_socket("Shader", in_out="OUTPUT", socket_type="NodeSocketShader")
fog.interface.new_socket("Fog", in_out="OUTPUT", socket_type="NodeSocketFloat")
gi = fog.nodes.new("NodeGroupInput")
go = fog.nodes.new("NodeGroupOutput")
cam = fog.nodes.new("ShaderNodeCameraData")
fac = math_node(fog, "SUBTRACT", 1, math_node(fog, "EXPONENT", math_node(fog, "MULTIPLY", cam.outputs["View Distance"], -1 / FOG_DIST).outputs[0]).outputs[0])
em = N(fog, "ShaderNodeEmission", Color=FOG_COLOR)
mix = fog.nodes.new("ShaderNodeMixShader")
L(fog, fac.outputs[0], mix.inputs[0]); L(fog, gi.outputs[0], mix.inputs[1]); L(fog, em.outputs[0], mix.inputs[2])
L(fog, mix.outputs[0], go.inputs[0]); L(fog, fac.outputs[0], go.inputs[1])


def fogged(mat, shader_out):
    t = mat.node_tree
    g = t.nodes.new("ShaderNodeGroup"); g.node_tree = fog
    out = t.nodes.new("ShaderNodeOutputMaterial")
    L(t, shader_out, g.inputs[0])
    L(t, g.outputs[0], out.inputs["Surface"])
    return g


# ------------------------------------------------------------- world / sun
SUN_ELEV = math.radians(4.5)

world = bpy.data.worlds.new("World"); scene.world = world
world.use_nodes = True
wt = world.node_tree; wt.nodes.clear()
sky = wt.nodes.new("ShaderNodeTexSky")
types = [e.identifier for e in sky.bl_rna.properties["sky_type"].enum_items]
sky.sky_type = next((t for t in ("MULTIPLE_SCATTERING", "NISHITA", "SINGLE_SCATTERING") if t in types), types[0])
for prop, val in (("sun_elevation", SUN_ELEV), ("sun_rotation", 0.0), ("altitude", 50), ("air_density", 1.0),
                  ("dust_density", 6.0), ("sun_intensity", 0.45), ("sun_size", math.radians(2.0))):
    if hasattr(sky, prop):
        setattr(sky, prop, val)
bg_sky = N(wt, "ShaderNodeBackground", Strength=0.22)
L(wt, sky.outputs[0], bg_sky.inputs[0])
tc = wt.nodes.new("ShaderNodeTexCoord")
sep = wt.nodes.new("ShaderNodeSeparateXYZ"); L(wt, tc.outputs["Generated"], sep.inputs[0])
mr = N(wt, "ShaderNodeMapRange", **{"From Min": -1.0, "From Max": 1.0}); L(wt, sep.outputs["Z"], mr.inputs["Value"])
uw_grad = ramp(wt, [(0.2, (0.0005, 0.004, 0.008, 1)), (0.5, FOG_COLOR), (0.85, (0.12, 0.45, 0.5, 1))], mr.outputs[0])
bg_uw = wt.nodes.new("ShaderNodeBackground"); L(wt, uw_grad.outputs[0], bg_uw.inputs[0])
wuw = wt.nodes.new("ShaderNodeValue")
wmix = wt.nodes.new("ShaderNodeMixShader")
L(wt, wuw.outputs[0], wmix.inputs[0]); L(wt, bg_sky.outputs[0], wmix.inputs[1]); L(wt, bg_uw.outputs[0], wmix.inputs[2])
wout = wt.nodes.new("ShaderNodeOutputWorld"); L(wt, wmix.outputs[0], wout.inputs[0])
key_step(wuw.outputs[0])

sun_data = bpy.data.lights.new("Sun", "SUN"); sun_data.angle = math.radians(1.2)
sun = bpy.data.objects.new("Sun", sun_data); scene.collection.objects.link(sun)
# Light travels from +Y toward the camera, slightly downward.
sun.rotation_euler = Euler((math.pi / 2 - SUN_ELEV, 0, math.pi), "XYZ")
sun_data.energy = 4.0; sun_data.keyframe_insert("energy", frame=DIVE - 1)
sun_data.energy = 3.4; sun_data.keyframe_insert("energy", frame=DIVE)
# Underwater the sun is filtered teal.
sun_data.color = (1.0, 0.78, 0.55); sun_data.keyframe_insert("color", frame=DIVE - 1)
sun_data.color = (0.55, 0.9, 1.0); sun_data.keyframe_insert("color", frame=DIVE)

# --------------------------------------------------------------- ocean
bpy.ops.mesh.primitive_plane_add(size=2)
ocean = bpy.context.object; ocean.name = "Ocean"
om = ocean.modifiers.new("Ocean", "OCEAN")
om.geometry_mode = "GENERATE"
om.resolution = 14
om.spatial_size = 50
om.repeat_x = om.repeat_y = 5
om.wave_scale = 1.0
om.choppiness = 1.2
om.wind_velocity = 10
om.wave_alignment = 0.5
om.wave_direction = math.radians(80)
om.use_normals = True
prefs = bpy.context.preferences.edit
prefs.keyframe_new_interpolation_type = "LINEAR"
om.time = 0; om.keyframe_insert("time", frame=0)
om.time = END / FPS; om.keyframe_insert("time", frame=END)
prefs.keyframe_new_interpolation_type = "BEZIER"
ocean.location = (-125, -80, 0)
smooth(ocean)
ocean.visible_shadow = False
wm = new_material("Water")
t = wm.node_tree
# From above: deep, glossy sea.
top = N(t, "ShaderNodeBsdfPrincipled", **{"Base Color": (0.004, 0.03, 0.05, 1), "Roughness": 0.04, "IOR": 1.333,
                                         "Coat Weight": 0.3})
# From below: bright, rippled underside (Snell's window) fading into fog.
lw = t.nodes.new("ShaderNodeLayerWeight"); lw.inputs["Blend"].default_value = 0.35
win = ramp(t, [(0.0, (0.9, 1.0, 1.0, 1)), (0.55, (0.12, 0.45, 0.5, 1)), (1.0, (0.01, 0.06, 0.08, 1))], lw.outputs["Facing"])
under = N(t, "ShaderNodeEmission", Strength=1.6); L(t, win.outputs[0], under.inputs["Color"])
g_under = t.nodes.new("ShaderNodeGroup"); g_under.node_tree = fog; L(t, under.outputs[0], g_under.inputs[0])
geo = t.nodes.new("ShaderNodeNewGeometry")
side = t.nodes.new("ShaderNodeMixShader")
L(t, geo.outputs["Backfacing"], side.inputs[0]); L(t, top.outputs[0], side.inputs[1]); L(t, g_under.outputs[0], side.inputs[2])
L(t, side.outputs[0], t.nodes.new("ShaderNodeOutputMaterial").inputs["Surface"])
ocean.data.materials.append(wm)

# --------------------------------------------------------------- seabed
bm = bmesh.new()
bmesh.ops.create_grid(bm, x_segments=120, y_segments=120, size=90)
for v in bm.verts:
    x, y = v.co.x, v.co.y
    v.co.z = 1.8 * math.sin(x * 0.11) * math.cos(y * 0.08) + 0.7 * math.sin(x * 0.37 + y * 0.21) + random.uniform(-0.08, 0.08)
seabed = mesh_object("Seabed", bm); smooth(seabed)
seabed.location = (0, 30, -20)
sm = new_material("Sand")
t = sm.node_tree
sand = N(t, "ShaderNodeBsdfPrincipled", **{"Base Color": (0.3, 0.27, 0.19, 1), "Roughness": 0.9})
vor = t.nodes.new("ShaderNodeTexVoronoi"); vor.voronoi_dimensions = "4D"
vor.inputs["Scale"].default_value = 0.5
vor.inputs["W"].default_value = 0; vor.inputs["W"].keyframe_insert("default_value", frame=0)
vor.inputs["W"].default_value = 5; vor.inputs["W"].keyframe_insert("default_value", frame=END)
caus = ramp(t, [(0.0, (0.5, 0.85, 0.9, 1)), (0.1, (0, 0, 0, 1))], vor.outputs["Distance"])
cem = t.nodes.new("ShaderNodeEmission"); cem.inputs["Strength"].default_value = 0.8; L(t, caus.outputs[0], cem.inputs[0])
add = t.nodes.new("ShaderNodeAddShader"); L(t, sand.outputs[0], add.inputs[0]); L(t, cem.outputs[0], add.inputs[1])
fogged(sm, add.outputs[0])
seabed.data.materials.append(sm)

# ------------------------------------------------------- god rays
rm = new_material("Ray", blended=True)
t = rm.node_tree
gc = t.nodes.new("ShaderNodeTexCoord")
gsep = t.nodes.new("ShaderNodeSeparateXYZ"); L(t, gc.outputs["Generated"], gsep.inputs[0])
along = math_node(t, "POWER", gsep.outputs["Z"], 2.2)
rlw = t.nodes.new("ShaderNodeLayerWeight"); rlw.inputs["Blend"].default_value = 0.5
edge = math_node(t, "POWER", math_node(t, "SUBTRACT", 1, rlw.outputs["Facing"]).outputs[0], 2.5)
g_ray = t.nodes.new("ShaderNodeGroup"); g_ray.node_tree = fog
clear = math_node(t, "SUBTRACT", 1, g_ray.outputs["Fog"])
strength = math_node(t, "MULTIPLY", math_node(t, "MULTIPLY", along.outputs[0], edge.outputs[0]).outputs[0], clear.outputs[0])
strength = math_node(t, "MULTIPLY", strength.outputs[0], 0.14)
rem = N(t, "ShaderNodeEmission", Color=(0.6, 0.95, 1.0, 1)); L(t, strength.outputs[0], rem.inputs["Strength"])
rtr = t.nodes.new("ShaderNodeBsdfTransparent")
radd = t.nodes.new("ShaderNodeAddShader"); L(t, rem.outputs[0], radd.inputs[0]); L(t, rtr.outputs[0], radd.inputs[1])
L(t, radd.outputs[0], t.nodes.new("ShaderNodeOutputMaterial").inputs["Surface"])
for i in range(9):
    bpy.ops.mesh.primitive_cone_add(vertices=24, radius1=random.uniform(1.0, 2.2), radius2=random.uniform(0.3, 0.6), depth=24,
                                    end_fill_type="NOTHING")
    r = bpy.context.object; r.name = f"Ray{i}"
    smooth(r)
    r.location = (random.uniform(-12, 12), random.uniform(2, 34), -12.3)
    base = Euler((math.radians(-14 + random.uniform(-3, 3)), math.radians(random.uniform(-5, 5)), 0))
    r.data.materials.append(rm)
    r.visible_shadow = False
    for f in range(0, END + 1, 70):
        r.rotation_euler = (base.x + math.radians(random.uniform(-2, 2)), base.y + math.radians(random.uniform(-2, 2)), 0)
        r.keyframe_insert("rotation_euler", frame=f)

# ------------------------------------------------------- plankton
bm = bmesh.new()
for i in range(1200):
    c = Vector((random.uniform(-14, 14), random.uniform(-10, 40), random.uniform(-15, -0.5)))
    res = bmesh.ops.create_icosphere(bm, subdivisions=0, radius=random.uniform(0.006, 0.02))
    for v in res["verts"]:
        v.co += c
plankton = mesh_object("Plankton", bm)
pm = new_material("Plankton")
pe = N(pm.node_tree, "ShaderNodeEmission", Color=(0.75, 0.95, 1, 1), Strength=2.0)
fogged(pm, pe.outputs[0])
plankton.data.materials.append(pm)
plankton.visible_shadow = False
plankton.location = (0, 0, 0); plankton.keyframe_insert("location", frame=0)
plankton.location = (0.8, -1.5, 1.2); plankton.keyframe_insert("location", frame=END)

# ------------------------------------------------------- bubbles on entry
bub = new_material("Bubble")
bp = N(bub.node_tree, "ShaderNodeBsdfPrincipled", **{"Base Color": (0.85, 0.95, 1, 1), "Metallic": 1.0, "Roughness": 0.08})
fogged(bub, bp.outputs[0])
for i in range(90):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=16, ring_count=10, radius=random.uniform(0.01, 0.07))
    b = bpy.context.object; b.name = f"Bubble{i}"; smooth(b)
    b.data.materials.append(bub)
    b.visible_shadow = False
    start = DIVE + random.randint(0, 8)
    p0 = Vector((random.uniform(-0.8, 0.8), random.uniform(-9.0, -6.5), random.uniform(-1.8, -0.5)))
    b.location = p0
    b.scale = (0, 0, 0); b.keyframe_insert("scale", frame=start - 1)
    b.scale = (1, 1, 1); b.keyframe_insert("scale", frame=start)
    b.keyframe_insert("location", frame=start)
    for k in range(1, 4):
        b.location = p0 + Vector((random.uniform(-0.3, 0.3), random.uniform(-0.2, 0.4), 0.8 * k * random.uniform(0.6, 1.4)))
        b.keyframe_insert("location", frame=start + k * 14)


# ------------------------------------------------------- fish
def build_fish(name, phase):
    bm = bmesh.new()
    res = bmesh.ops.create_uvsphere(bm, u_segments=16, v_segments=10, radius=1)
    for v in res["verts"]:
        x, y, z = v.co
        taper = 0.45 + 0.55 * ((1 + x) / 2) ** 0.6
        v.co = Vector((x * 0.2, y * 0.04 * taper, z * 0.07 * taper))
    tail = [bm.verts.new(c) for c in ((-0.17, 0, 0), (-0.3, 0, 0.075), (-0.26, 0, 0), (-0.3, 0, -0.075))]
    bm.faces.new(tail)
    dorsal = [bm.verts.new(c) for c in ((0.05, 0, 0.055), (-0.04, 0, 0.1), (-0.08, 0, 0.045))]
    bm.faces.new(dorsal)
    ob = mesh_object(name, bm)
    smooth(ob)
    ob.shape_key_add(name="Basis")
    sk = ob.shape_key_add(name="Wag")
    sk.slider_min = -1
    for i, v in enumerate(ob.data.vertices):
        if v.co.x < 0.05:
            sk.data[i].co.y = v.co.y + 2.4 * (0.05 - v.co.x) ** 2
    d = sk.driver_add("value").driver
    d.type = "SCRIPTED"
    d.expression = f"sin(frame*1.1+{phase:.2f})"
    return ob


fmat = new_material("Fish")
t = fmat.node_tree
fp = N(t, "ShaderNodeBsdfPrincipled", Metallic=0.9, Roughness=0.25)
ftc = t.nodes.new("ShaderNodeTexCoord"); fsep = t.nodes.new("ShaderNodeSeparateXYZ"); L(t, ftc.outputs["Object"], fsep.inputs[0])
fr = N(t, "ShaderNodeMapRange", **{"From Min": -0.03, "From Max": 0.04}); L(t, fsep.outputs["Z"], fr.inputs["Value"])
fcol = ramp(t, [(0.0, (0.85, 0.88, 0.9, 1)), (0.55, (0.5, 0.6, 0.65, 1)), (1.0, (0.03, 0.1, 0.2, 1))], fr.outputs[0])
L(t, fcol.outputs[0], fp.inputs["Base Color"])
fogged(fmat, fp.outputs[0])

templates = [build_fish(f"FishT{i}", i * 2.1) for i in range(3)]
for tpl in templates:
    tpl.data.materials.append(fmat)
    tpl.hide_render = True
    tpl.location = (0, 0, -100)

school = bpy.data.objects.new("School", None); scene.collection.objects.link(school)
SCHOOL_KEYS = [(100, (10, 4.5, -3.8), 172), (190, (2.5, 3.6, -5.2), 186), (280, (-5.5, 7.5, -4.6), 205), (END, (-15, 15, -3.8), 215)]
for f, loc, yaw in SCHOOL_KEYS:
    school.location = loc
    school.rotation_euler = (0, 0, math.radians(yaw))
    school.keyframe_insert("location", frame=f)
    school.keyframe_insert("rotation_euler", frame=f)
for i in range(160):
    tpl = templates[i % 3]
    ob = bpy.data.objects.new(f"Fish{i}", tpl.data)
    scene.collection.objects.link(ob)
    ob.parent = school
    ob.scale = [random.uniform(0.8, 1.3)] * 3
    off = Vector((random.gauss(0, 1.4), random.gauss(0, 1.0), random.gauss(0, 0.55)))
    for f in range(100, END + 1, 20):
        ob.location = off + Vector((random.uniform(-0.25, 0.25), random.uniform(-0.2, 0.2), random.uniform(-0.12, 0.12)))
        ob.rotation_euler = (0, 0, math.radians(random.uniform(-10, 10)))
        ob.keyframe_insert("location", frame=f)
        ob.keyframe_insert("rotation_euler", frame=f)

# ------------------------------------------------------- humpback whale
WL = 14.0   # body length (snout at +x)


def lerp_table(table, u):
    for (u0, v0), (u1, v1) in zip(table, table[1:]):
        if u <= u1:
            k = (u - u0) / (u1 - u0)
            k = k * k * (3 - 2 * k)
            return v0 + (v1 - v0) * k
    return table[-1][1]


# Body half-width, back height and belly depth (metres) along u = 0 (snout) … 1 (tail stock)
WIDTH = [(0, 0.02), (0.04, 0.45), (0.15, 1.05), (0.3, 1.45), (0.45, 1.5), (0.62, 1.15), (0.8, 0.55), (0.93, 0.2), (1.0, 0.12)]
TOP = [(0, 0.02), (0.04, 0.22), (0.2, 0.62), (0.33, 1.05), (0.45, 1.25), (0.62, 1.1), (0.8, 0.7), (0.93, 0.36), (1.0, 0.18)]
BOTTOM = [(0, 0.02), (0.04, 0.38), (0.18, 1.05), (0.3, 1.45), (0.45, 1.3), (0.62, 1.0), (0.8, 0.62), (0.93, 0.34), (1.0, 0.16)]

bm = bmesh.new()
RINGS, SEG = 72, 32
rings = []
for r in range(RINGS + 1):
    u = (r / RINGS) ** 1.15
    x = WL * (0.5 - u)
    w, ht, hb = lerp_table(WIDTH, u), lerp_table(TOP, u), lerp_table(BOTTOM, u)
    ring = []
    for s in range(SEG):
        a = 2 * math.pi * s / SEG
        c, sn = math.cos(a), math.sin(a)
        e = 2.4   # superellipse exponent → fuller cross-section
        y = math.copysign(abs(c) ** (2 / e), c) * w
        z = math.copysign(abs(sn) ** (2 / e), sn) * (ht if sn > 0 else hb)
        ring.append(bm.verts.new((x, y, z)))
    rings.append(ring)
for r in range(RINGS):
    for s in range(SEG):
        bm.faces.new((rings[r][s], rings[r][(s + 1) % SEG], rings[r + 1][(s + 1) % SEG], rings[r + 1][s]))
cap0 = bm.verts.new((WL * 0.5 + 0.02, 0, 0))
for s in range(SEG):
    bm.faces.new((cap0, rings[0][(s + 1) % SEG], rings[0][s]))
cap1 = bm.verts.new((WL * -0.5 - 0.05, 0, 0))
for s in range(SEG):
    bm.faces.new((cap1, rings[-1][s], rings[-1][(s + 1) % SEG]))
body_faces = len(bm.faces)


def loft(root, span_dir, chord_dir, up_dir, sections):
    """Fin as a series of airfoil-like sections along span_dir. sections: (s, chord, thick, sweep)."""
    span_dir, chord_dir, up_dir = Vector(span_dir).normalized(), Vector(chord_dir).normalized(), Vector(up_dir).normalized()
    rs = []
    K = 14
    for s, chord, thick, sweep in sections:
        centre = Vector(root) + span_dir * s + chord_dir * sweep
        ring = []
        for k in range(K):
            a = 2 * math.pi * k / K
            cx = math.cos(a)
            th = thick * math.sin(a) * (1 - 0.6 * max(0, -cx))   # blunt leading edge, thin trailing edge
            ring.append(bm.verts.new(centre + chord_dir * (0.5 * chord * cx) + up_dir * th))
        rs.append(ring)
    for i in range(len(rs) - 1):
        for k in range(K):
            bm.faces.new((rs[i][k], rs[i][(k + 1) % K], rs[i + 1][(k + 1) % K], rs[i + 1][k]))
    bm.faces.new(list(reversed(rs[0])))
    bm.faces.new(rs[-1])


# Long humpback pectoral fins (≈ 1/3 body length), swept back and down, knobbly leading edge
for side in (1, -1):
    secs = []
    for i in range(13):
        s = i / 12 * 4.6
        chord = 0.95 * (1 - i / 12) ** 0.6 + 0.12 + 0.05 * math.sin(i * 2.3)
        secs.append((s, chord, 0.09 * chord + 0.02, -0.25 * s))
    loft((2.6, side * 1.15, -0.75), (0.05, side * 0.8, -0.6), (1, 0, -0.05), (0, -side * 0.6, 0.8), secs)
pect_end = len(bm.faces)
# Tail flukes, horizontal, wide with swept-back tips
tx = -WL / 2 + 0.1
for side in (1, -1):
    secs = []
    for i in range(11):
        s = i / 10 * 2.4
        chord = 1.1 * (1 - (i / 10) ** 1.6) + 0.12
        secs.append((s, chord, 0.07 * chord + 0.015, -0.55 * (i / 10) ** 1.4 * 1.4))
    loft((tx - 0.45, side * 0.05, 0), (0, side, 0), (1, 0, 0), (0, 0, 1), secs)
# Small dorsal fin
loft((-2.6, 0, 0.98), (-0.3, 0, 1), (1, 0, 0), (0, 1, 0), [(0.0, 1.0, 0.1, 0), (0.25, 0.7, 0.08, -0.15), (0.45, 0.3, 0.05, -0.35)])

whale = mesh_object("Whale", bm)
smooth(whale)
sub = whale.modifiers.new("Sub", "SUBSURF"); sub.levels = 1; sub.render_levels = 2

# Skin: dark back, white mottled throat/belly, ventral pleats, barnacle spots on the head
wmat = new_material("WhaleSkin")
t = wmat.node_tree
wp = N(t, "ShaderNodeBsdfPrincipled", Roughness=0.4, **{"Coat Weight": 0.25})
wtc = t.nodes.new("ShaderNodeTexCoord"); wsep = t.nodes.new("ShaderNodeSeparateXYZ"); L(t, wtc.outputs["Object"], wsep.inputs[0])
wr = N(t, "ShaderNodeMapRange", **{"From Min": -0.9, "From Max": 0.1}); L(t, wsep.outputs["Z"], wr.inputs["Value"])
noise = N(t, "ShaderNodeTexNoise", Scale=1.6, Detail=8.0, Roughness=0.6); L(t, wtc.outputs["Object"], noise.inputs["Vector"])
mott = math_node(t, "MULTIPLY_ADD", noise.outputs["Fac"], 0.6); mott.inputs[2].default_value = -0.3
mixf = math_node(t, "ADD", wr.outputs[0], mott.outputs[0])
wcol = ramp(t, [(0.25, (0.75, 0.76, 0.76, 1)), (0.6, (0.018, 0.022, 0.03, 1))], mixf.outputs[0])
spots = t.nodes.new("ShaderNodeTexVoronoi"); spots.inputs["Scale"].default_value = 9.0; L(t, wtc.outputs["Object"], spots.inputs["Vector"])
spot_m = ramp(t, [(0.0, (1, 1, 1, 1)), (0.12, (0, 0, 0, 1))], spots.outputs["Distance"])
head = N(t, "ShaderNodeMapRange", **{"From Min": 3.5, "From Max": 6.5}); L(t, wsep.outputs["X"], head.inputs["Value"])
spot_f = math_node(t, "MULTIPLY", spot_m.outputs[0], head.outputs[0])
col = t.nodes.new("ShaderNodeMix"); col.data_type = "RGBA"
L(t, spot_f.outputs[0], col.inputs[0]); L(t, wcol.outputs[0], col.inputs[6]); col.inputs[7].default_value = (0.8, 0.78, 0.72, 1)
L(t, col.outputs[2], wp.inputs["Base Color"])
pleat = N(t, "ShaderNodeTexWave", Scale=2.2); pleat.wave_type = "BANDS"; pleat.bands_direction = "Y"
L(t, wtc.outputs["Object"], pleat.inputs["Vector"])
belly = N(t, "ShaderNodeMapRange", **{"From Min": -0.4, "From Max": -1.0}); L(t, wsep.outputs["Z"], belly.inputs["Value"])
pleat_h = math_node(t, "MULTIPLY", pleat.outputs["Fac"], belly.outputs[0])
hsum = math_node(t, "ADD", pleat_h.outputs[0], math_node(t, "MULTIPLY", noise.outputs["Fac"], 0.3).outputs[0])
hsum = math_node(t, "ADD", hsum.outputs[0], math_node(t, "MULTIPLY", spot_f.outputs[0], 0.4).outputs[0])
bump = N(t, "ShaderNodeBump", Strength=0.35); L(t, hsum.outputs[0], bump.inputs["Height"]); L(t, bump.outputs[0], wp.inputs["Normal"])
fogged(wmat, wp.outputs[0])
whale.data.materials.append(wmat)
# Pectoral fins: mostly white with dark mottling
finm = new_material("WhaleFin")
t = finm.node_tree
fpb = N(t, "ShaderNodeBsdfPrincipled", Roughness=0.45)
ftc2 = t.nodes.new("ShaderNodeTexCoord"); fn = N(t, "ShaderNodeTexNoise", Scale=2.5, Detail=6.0); L(t, ftc2.outputs["Object"], fn.inputs["Vector"])
fc = ramp(t, [(0.4, (0.82, 0.83, 0.82, 1)), (0.62, (0.06, 0.07, 0.08, 1))], fn.outputs["Fac"])
L(t, fc.outputs[0], fpb.inputs["Base Color"])
fogged(finm, fpb.outputs[0])
whale.data.materials.append(finm)
for p in whale.data.polygons[body_faces:pect_end]:
    p.material_index = 1

# Swim rig: bone chain along the body; weights blended smoothly by x position.
arm_data = bpy.data.armatures.new("WhaleRig")
rig = bpy.data.objects.new("WhaleRig", arm_data); scene.collection.objects.link(rig)
bpy.context.view_layer.objects.active = rig
bpy.ops.object.mode_set(mode="EDIT")
NB = 8
xs = [WL / 2 - 3.0 - i * (WL - 3.0) / NB for i in range(NB + 1)]
prev = None
for i in range(NB):
    b = arm_data.edit_bones.new(f"B{i}")
    b.head = (xs[i], 0, 0); b.tail = (xs[i + 1], 0, 0)
    if prev:
        b.parent = prev; b.use_connect = True
    prev = b
bpy.ops.object.mode_set(mode="OBJECT")
groups = [whale.vertex_groups.new(name=f"B{i}") for i in range(NB)]
centres = [(xs[i] + xs[i + 1]) / 2 for i in range(NB)]
for v in whale.data.vertices:
    x = v.co.x
    if x >= centres[0]:
        groups[0].add([v.index], 1.0, "REPLACE")
        continue
    if x <= centres[-1]:
        groups[-1].add([v.index], 1.0, "REPLACE")
        continue
    for i in range(NB - 1):
        if centres[i] >= x >= centres[i + 1]:
            f = (centres[i] - x) / (centres[i] - centres[i + 1])
            groups[i].add([v.index], 1 - f, "REPLACE")
            groups[i + 1].add([v.index], f, "REPLACE")
            break
arm_mod = whale.modifiers.new("Rig", "ARMATURE"); arm_mod.object = rig
whale.modifiers.move(1, 0)
whale.parent = rig

# Swimming stroke: vertical wave travelling to the flukes. Bones run along -x with roll 0,
# so their local X axis is world +Y and rotating about it bends the body up/down.
for pb in rig.pose.bones:
    pb.rotation_mode = "XYZ"
for f in range(140, END + 1, 2):
    tsec = f / FPS
    for i, pb in enumerate(rig.pose.bones):
        amp = 0.012 + 0.085 * (i / (NB - 1)) ** 2
        pb.rotation_euler = (amp * math.sin(2 * math.pi * 0.42 * tsec - i * 0.65), 0, 0)
        pb.keyframe_insert("rotation_euler", frame=f)

# Path: emerges from the blue on the right, glides across in front of the lens, banking slightly.
WHALE_KEYS = [
    (140, (27, 12.5, -7.6), 182, 0),
    (230, (11.5, 9.6, -7.0), 189, -5),
    (330, (-0.5, 8.1, -6.4), 195, -9),
    (400, (-10.5, 7.7, -6.0), 199, -7),
    (END, (-13.2, 7.6, -5.9), 200, -6),
]
for f, loc, yaw, roll in WHALE_KEYS:
    rig.location = loc
    rig.rotation_euler = (math.radians(roll), math.radians(-3), math.radians(yaw))
    rig.keyframe_insert("location", frame=f)
    rig.keyframe_insert("rotation_euler", frame=f)

# ------------------------------------------------------- camera
cam_data = bpy.data.cameras.new("Cam"); cam_data.lens = 22; cam_data.sensor_fit = "AUTO"
cam_data.clip_start = 0.02; cam_data.clip_end = 500
camera = bpy.data.objects.new("Camera", cam_data); scene.collection.objects.link(camera); scene.camera = camera
CAM_KEYS = [
    # frame, location, rotation XYZ in degrees (x=90 looks horizontally along +Y)
    (0, (0, -26, 2.8), (86, 0, 0)),
    (70, (0, -17, 2.3), (82, 0, 0)),
    (102, (0, -12.8, 1.4), (55, 0, 0)),
    (117, (0, -10.8, 0.3), (58, 0, 0)),
    (DIVE + 5, (0, -9.4, -1.0), (66, 0, 0)),
    (170, (0, -7.5, -4.5), (88, 0, 0)),
    (240, (0, -4.2, -6.1), (95, 0, -4)),
    (320, (0, -2.8, -6.5), (99, 0, 3)),
    (END, (0, -2.0, -6.7), (100, 0, 14)),
]
for f, loc, rot in CAM_KEYS:
    camera.location = loc
    camera.rotation_euler = Euler([math.radians(a) for a in rot], "XYZ")
    camera.keyframe_insert("location", frame=f)
    camera.keyframe_insert("rotation_euler", frame=f)

# ------------------------------------------------------- render settings
r = scene.render
r.engine = "BLENDER_EEVEE"
r.resolution_x, r.resolution_y = 1080, 1920
r.resolution_percentage = args.scale
ee = scene.eevee
ee.taa_render_samples = args.samples
for prop, val in (("use_shadows", True), ("shadow_ray_count", 1), ("shadow_step_count", 4), ("use_raytracing", False)):
    if hasattr(ee, prop):
        setattr(ee, prop, val)
scene.view_settings.view_transform = "AgX"
try:
    scene.view_settings.look = "AgX - Medium High Contrast"
except TypeError:
    pass
scene.view_settings.exposure = 0.0
r.image_settings.file_format = "PNG"
r.image_settings.color_mode = "RGB"

if args.save_blend:
    bpy.ops.wm.save_as_mainfile(filepath=args.save_blend)

frames = [int(x) for x in args.frames.split(",") if x] if args.frames else list(range(0, END + 1))
for f in frames:
    scene.frame_set(f)
    r.filepath = f"{args.out}/f{f:04d}.png"
    t0 = time.time()
    bpy.ops.render.render(write_still=True)
    print(f"FRAME {f} {time.time() - t0:.1f}s", flush=True)
