"""Diver point-of-view: float at the surface, duck under, descend, meet a fish school and a humpback whale.

Fully procedural Blender scene rendered headless with EEVEE:
  EGL_PLATFORM=surfaceless python3 diver_pov.py --out frames/ [--frames 0,200] [--scale 66] [--samples 10]
Writes the exhale timings to <out>/breaths.txt so the soundtrack and the bubbles stay in sync.
"""
import argparse
import math
import random
import sys
import time

import bpy  # noqa: I001  (bpy must be imported before bmesh)
import bmesh
from mathutils import Euler, Matrix, Vector

argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
ap = argparse.ArgumentParser()
ap.add_argument("--out", required=True)
ap.add_argument("--frames", default="")
ap.add_argument("--samples", type=int, default=10)
ap.add_argument("--scale", type=int, default=100)
ap.add_argument("--save-blend", default="")
args = ap.parse_args(argv)

FPS = 30
END = 479
SUBMERGE = 82            # frame the mask goes under
FOG_DIST = 20.0
SHALLOW = (0.02, 0.16, 0.19, 1)
DEEP = (0.003, 0.03, 0.06, 1)
EXHALES = [128, 248, 368]   # breathing every 4 s once underwater
random.seed(11)

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


def M(tree, op, a=None, b=None, c=None):
    n = tree.nodes.new("ShaderNodeMath")
    n.operation = op
    for i, v in enumerate((a, b, c)):
        if v is None:
            continue
        if isinstance(v, (int, float)):
            n.inputs[i].default_value = v
        else:
            L(tree, v, n.inputs[i])
    return n.outputs[0]


def ramp(tree, stops, fac):
    r = tree.nodes.new("ShaderNodeValToRGB")
    els = r.color_ramp.elements
    while len(els) < len(stops):
        els.new(0.5)
    for e, (pos, col) in zip(els, stops):
        e.position = pos
        e.color = col
    L(tree, fac, r.inputs[0])
    return r.outputs[0]


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


def key_step(socket, frame, before, after):
    socket.default_value = before
    socket.keyframe_insert("default_value", frame=frame - 1)
    socket.default_value = after
    socket.keyframe_insert("default_value", frame=frame)


# ---------------------------------------------------- shared shader groups
def group(name, inputs, outputs):
    g = bpy.data.node_groups.new(name, "ShaderNodeTree")
    for nm, kind in inputs:
        g.interface.new_socket(nm, in_out="INPUT", socket_type=kind)
    for nm, kind in outputs:
        g.interface.new_socket(nm, in_out="OUTPUT", socket_type=kind)
    return g, g.nodes.new("NodeGroupInput"), g.nodes.new("NodeGroupOutput")


# Caustics: animated light net on up-facing surfaces, fading with depth.
caus, ci, co = group("Caustics", [], [("Light", "NodeSocketFloat")])
geo = caus.nodes.new("ShaderNodeNewGeometry")
sep = caus.nodes.new("ShaderNodeSeparateXYZ"); L(caus, geo.outputs["Position"], sep.inputs[0])
nsep = caus.nodes.new("ShaderNodeSeparateXYZ"); L(caus, geo.outputs["Normal"], nsep.inputs[0])
# Two 3D Voronoi layers drifting in different directions read as animated caustics and are far
# cheaper than 4D smooth Voronoi on a CPU renderer.
vors = []
for scale, drift in ((1.1, (0.35, 0.2, 0.5)), (1.7, (-0.25, 0.3, -0.4))):
    off = caus.nodes.new("ShaderNodeVectorMath"); off.operation = "ADD"
    L(caus, geo.outputs["Position"], off.inputs[0])
    off.inputs[1].default_value = (0, 0, 0); off.inputs[1].keyframe_insert("default_value", frame=0)
    off.inputs[1].default_value = tuple(d * END / FPS for d in drift); off.inputs[1].keyframe_insert("default_value", frame=END)
    v = caus.nodes.new("ShaderNodeTexVoronoi"); v.voronoi_dimensions = "3D"; v.feature = "F1"
    v.inputs["Scale"].default_value = scale
    L(caus, off.outputs[0], v.inputs["Vector"])
    vors.append(v)
vor, vor2 = vors
net = M(caus, "MULTIPLY", ramp(caus, [(0.0, (1, 1, 1, 1)), (0.09, (0, 0, 0, 1))], vor.outputs["Distance"]),
        ramp(caus, [(0.0, (1, 1, 1, 1)), (0.12, (0.25, 0.25, 0.25, 1))], vor2.outputs["Distance"]))
up = M(caus, "POWER", M(caus, "MAXIMUM", nsep.outputs["Z"], 0.0), 1.5)
depth = M(caus, "EXPONENT", M(caus, "DIVIDE", sep.outputs["Z"], 9.0))
underwater = M(caus, "LESS_THAN", sep.outputs["Z"], -0.2)
L(caus, M(caus, "MULTIPLY", M(caus, "MULTIPLY", net, up), M(caus, "MULTIPLY", depth, underwater)), co.inputs[0])

# Water column: depth darkening + distance fog whose colour deepens with depth.
fog, fi, fo = group("Underwater", [("Shader", "NodeSocketShader")], [("Shader", "NodeSocketShader"), ("Fog", "NodeSocketFloat")])
cam = fog.nodes.new("ShaderNodeCameraData")
fgeo = fog.nodes.new("ShaderNodeNewGeometry")
fsep = fog.nodes.new("ShaderNodeSeparateXYZ"); L(fog, fgeo.outputs["Position"], fsep.inputs[0])
below = M(fog, "LESS_THAN", fsep.outputs["Z"], 0.02)
fogfac = M(fog, "MULTIPLY", M(fog, "SUBTRACT", 1, M(fog, "EXPONENT", M(fog, "MULTIPLY", cam.outputs["View Distance"], -1 / FOG_DIST))), below)
depthfac = M(fog, "MINIMUM", M(fog, "DIVIDE", M(fog, "MULTIPLY", fsep.outputs["Z"], -1), 22.0), 1.0)
fcol = fog.nodes.new("ShaderNodeMix"); fcol.data_type = "RGBA"
L(fog, depthfac, fcol.inputs[0]); fcol.inputs[6].default_value = SHALLOW; fcol.inputs[7].default_value = DEEP
fem = fog.nodes.new("ShaderNodeEmission"); L(fog, fcol.outputs[2], fem.inputs["Color"])
# light lost with depth (reds first — handled by the teal sun underwater)
dark = M(fog, "MULTIPLY", M(fog, "SUBTRACT", 1, M(fog, "EXPONENT", M(fog, "DIVIDE", M(fog, "MINIMUM", fsep.outputs["Z"], 0.0), 14.0))), 0.85)
black = fog.nodes.new("ShaderNodeEmission"); black.inputs["Strength"].default_value = 0
dmix = fog.nodes.new("ShaderNodeMixShader"); L(fog, dark, dmix.inputs[0]); L(fog, fi.outputs[0], dmix.inputs[1]); L(fog, black.outputs[0], dmix.inputs[2])
mix = fog.nodes.new("ShaderNodeMixShader"); L(fog, fogfac, mix.inputs[0]); L(fog, dmix.outputs[0], mix.inputs[1]); L(fog, fem.outputs[0], mix.inputs[2])
L(fog, mix.outputs[0], fo.inputs[0]); L(fog, fogfac, fo.inputs[1])


def finish(mat, shader_out, caustic=0.0):
    """Add caustic light and the water column on top of a surface shader."""
    t = mat.node_tree
    if caustic > 0:
        cg = t.nodes.new("ShaderNodeGroup"); cg.node_tree = caus
        ce = N(t, "ShaderNodeEmission", Color=(0.75, 0.95, 1.0, 1))
        L(t, M(t, "MULTIPLY", cg.outputs[0], caustic), ce.inputs["Strength"])
        add = t.nodes.new("ShaderNodeAddShader"); L(t, shader_out, add.inputs[0]); L(t, ce.outputs[0], add.inputs[1])
        shader_out = add.outputs[0]
    g = t.nodes.new("ShaderNodeGroup"); g.node_tree = fog
    L(t, shader_out, g.inputs[0])
    L(t, g.outputs[0], t.nodes.new("ShaderNodeOutputMaterial").inputs["Surface"])


# ------------------------------------------------------------- world / sun
SUN_ELEV = math.radians(38)
world = bpy.data.worlds.new("World"); scene.world = world
world.use_nodes = True
wt = world.node_tree; wt.nodes.clear()
sky = wt.nodes.new("ShaderNodeTexSky")
types = [e.identifier for e in sky.bl_rna.properties["sky_type"].enum_items]
sky.sky_type = next((t for t in ("MULTIPLE_SCATTERING", "NISHITA", "SINGLE_SCATTERING") if t in types), types[0])
for prop, val in (("sun_elevation", SUN_ELEV), ("sun_rotation", math.radians(-25)), ("altitude", 30), ("air_density", 1.0),
                  ("dust_density", 1.5), ("sun_intensity", 0.5), ("sun_size", math.radians(1.0))):
    if hasattr(sky, prop):
        setattr(sky, prop, val)
bg_sky = N(wt, "ShaderNodeBackground", Strength=0.12); L(wt, sky.outputs[0], bg_sky.inputs[0])
tc = wt.nodes.new("ShaderNodeTexCoord")
wsep = wt.nodes.new("ShaderNodeSeparateXYZ"); L(wt, tc.outputs["Generated"], wsep.inputs[0])
mr = N(wt, "ShaderNodeMapRange", **{"From Min": -1.0, "From Max": 1.0}); L(wt, wsep.outputs["Z"], mr.inputs["Value"])
bg_uw = wt.nodes.new("ShaderNodeBackground")
L(wt, ramp(wt, [(0.1, (0.002, 0.012, 0.025, 1)), (0.45, (0.012, 0.09, 0.12, 1)), (0.6, SHALLOW)], mr.outputs[0]), bg_uw.inputs[0])
wuw = wt.nodes.new("ShaderNodeValue")
wmix = wt.nodes.new("ShaderNodeMixShader")
L(wt, wuw.outputs[0], wmix.inputs[0]); L(wt, bg_sky.outputs[0], wmix.inputs[1]); L(wt, bg_uw.outputs[0], wmix.inputs[2])
L(wt, wmix.outputs[0], wt.nodes.new("ShaderNodeOutputWorld").inputs[0])
key_step(wuw.outputs[0], SUBMERGE, 0, 1)

sun_data = bpy.data.lights.new("Sun", "SUN"); sun_data.angle = math.radians(1.0)
sun = bpy.data.objects.new("Sun", sun_data); scene.collection.objects.link(sun)
# light comes from ahead-right and above
sun.rotation_euler = Euler((math.pi / 2 - SUN_ELEV, 0, math.pi + math.radians(-25)), "XYZ")
sun_data.energy = 4.5; sun_data.keyframe_insert("energy", frame=SUBMERGE - 1)
sun_data.energy = 3.0; sun_data.keyframe_insert("energy", frame=SUBMERGE)
sun_data.color = (1.0, 0.95, 0.88); sun_data.keyframe_insert("color", frame=SUBMERGE - 1)
sun_data.color = (0.6, 0.92, 1.0); sun_data.keyframe_insert("color", frame=SUBMERGE)

# --------------------------------------------------------------- ocean
bpy.ops.mesh.primitive_plane_add(size=2)
ocean = bpy.context.object; ocean.name = "Ocean"
om = ocean.modifiers.new("Ocean", "OCEAN")
om.geometry_mode = "GENERATE"
om.resolution = 16
om.spatial_size = 40
om.repeat_x = om.repeat_y = 5
om.wave_scale = 0.8
om.choppiness = 1.3
om.wind_velocity = 8
om.wave_alignment = 0.3
om.use_normals = True
prefs = bpy.context.preferences.edit
prefs.keyframe_new_interpolation_type = "LINEAR"
om.time = 0; om.keyframe_insert("time", frame=0)
om.time = END / FPS; om.keyframe_insert("time", frame=END)
prefs.keyframe_new_interpolation_type = "BEZIER"
ocean.location = (-100, -90, 0)
smooth(ocean)
ocean.visible_shadow = False
wm = new_material("Water")
t = wm.node_tree
topw = N(t, "ShaderNodeBsdfPrincipled", **{"Base Color": (0.005, 0.045, 0.06, 1), "Roughness": 0.07, "IOR": 1.333, "Coat Weight": 0.4})
lw = t.nodes.new("ShaderNodeLayerWeight"); lw.inputs["Blend"].default_value = 0.42
# Snell's window from below: bright sky disk overhead, total internal reflection (dark teal) beyond it
win = ramp(t, [(0.0, (1.0, 1.0, 0.98, 1)), (0.3, (0.55, 0.85, 0.9, 1)), (0.42, (0.06, 0.28, 0.32, 1)), (1.0, (0.01, 0.06, 0.08, 1))], lw.outputs["Facing"])
under = N(t, "ShaderNodeEmission", Strength=2.2); L(t, win, under.inputs["Color"])
g_under = t.nodes.new("ShaderNodeGroup"); g_under.node_tree = fog; L(t, under.outputs[0], g_under.inputs[0])
gg = t.nodes.new("ShaderNodeNewGeometry")
side = t.nodes.new("ShaderNodeMixShader")
L(t, gg.outputs["Backfacing"], side.inputs[0]); L(t, topw.outputs[0], side.inputs[1]); L(t, g_under.outputs[0], side.inputs[2])
L(t, side.outputs[0], t.nodes.new("ShaderNodeOutputMaterial").inputs["Surface"])
ocean.data.materials.append(wm)

# --------------------------------------------------------------- seabed + rocks
bm = bmesh.new()
bmesh.ops.create_grid(bm, x_segments=150, y_segments=150, size=70)
for v in bm.verts:
    x, y = v.co.x, v.co.y
    v.co.z = (1.5 * math.sin(x * 0.09) * math.cos(y * 0.07) + 0.6 * math.sin(x * 0.31 + y * 0.23)
              + 0.25 * math.sin(x * 1.3) * math.sin(y * 1.1) + random.uniform(-0.05, 0.05))
seabed = mesh_object("Seabed", bm); smooth(seabed)
seabed.location = (0, 25, -15)
sm = new_material("Sand")
t = sm.node_tree
stc2 = t.nodes.new("ShaderNodeTexCoord")
sandn = N(t, "ShaderNodeTexNoise", Scale=6.0, Detail=8.0); L(t, stc2.outputs["Object"], sandn.inputs["Vector"])
sandc = ramp(t, [(0.35, (0.22, 0.2, 0.15, 1)), (0.65, (0.42, 0.38, 0.28, 1))], sandn.outputs["Fac"])
sand = N(t, "ShaderNodeBsdfPrincipled", Roughness=0.95); L(t, sandc, sand.inputs["Base Color"])
ripple = N(t, "ShaderNodeTexWave", Scale=1.2, Distortion=4.0); L(t, stc2.outputs["Object"], ripple.inputs["Vector"])
sb = N(t, "ShaderNodeBump", Strength=0.4); L(t, ripple.outputs["Fac"], sb.inputs["Height"]); L(t, sb.outputs[0], sand.inputs["Normal"])
finish(sm, sand.outputs[0], caustic=2.2)
seabed.data.materials.append(sm)

rockm = new_material("Rock")
t = rockm.node_tree
rtc = t.nodes.new("ShaderNodeTexCoord")
rn = N(t, "ShaderNodeTexNoise", Scale=2.5, Detail=10.0, Roughness=0.7); L(t, rtc.outputs["Object"], rn.inputs["Vector"])
rc = ramp(t, [(0.3, (0.05, 0.06, 0.04, 1)), (0.55, (0.16, 0.17, 0.12, 1)), (0.7, (0.12, 0.2, 0.08, 1))], rn.outputs["Fac"])
rp = N(t, "ShaderNodeBsdfPrincipled", Roughness=0.9); L(t, rc, rp.inputs["Base Color"])
rb = N(t, "ShaderNodeBump", Strength=0.8); L(t, rn.outputs["Fac"], rb.inputs["Height"]); L(t, rb.outputs[0], rp.inputs["Normal"])
finish(rockm, rp.outputs[0], caustic=1.8)
for i in range(40):
    bm = bmesh.new()
    bmesh.ops.create_icosphere(bm, subdivisions=3, radius=1)
    sx, sy, sz = random.uniform(0.6, 2.4), random.uniform(0.6, 2.2), random.uniform(0.4, 1.4)
    ph = [random.uniform(0, 6) for _ in range(3)]
    for v in bm.verts:
        d = 1 + 0.18 * math.sin(v.co.x * 3 + ph[0]) * math.sin(v.co.y * 3 + ph[1]) + 0.1 * math.sin(v.co.z * 5 + ph[2])
        v.co = Vector((v.co.x * sx * d, v.co.y * sy * d, v.co.z * sz * d))
    rock = mesh_object(f"Rock{i}", bm); smooth(rock)
    rx, ry = random.uniform(-25, 25), random.uniform(-5, 45)
    rock.location = (rx, ry, -15.3 + 1.5 * math.sin(rx * 0.09) * math.cos((ry - 25) * 0.07))
    rock.rotation_euler = (0, 0, random.uniform(0, 6.3))
    rock.data.materials.append(rockm)

# ------------------------------------------------------- marine snow
bm = bmesh.new()
for i in range(2600):
    c = Vector((random.uniform(-9, 9), random.uniform(-12, 22), random.uniform(-14, -0.4)))
    res = bmesh.ops.create_icosphere(bm, subdivisions=0, radius=random.uniform(0.003, 0.014))
    for v in res["verts"]:
        v.co += c
snow = mesh_object("MarineSnow", bm)
snm = new_material("Snow")
sne = N(snm.node_tree, "ShaderNodeEmission", Color=(0.8, 0.95, 1, 1), Strength=1.4)
finish(snm, sne.outputs[0])
snow.data.materials.append(snm)
snow.visible_shadow = False
snow.location = (0, 0, 0); snow.keyframe_insert("location", frame=0)
snow.location = (0.5, -0.8, -0.5); snow.keyframe_insert("location", frame=END)

# ------------------------------------------------------- bubbles
bubm = new_material("Bubble")
t = bubm.node_tree
bpb = N(t, "ShaderNodeBsdfPrincipled", **{"Base Color": (0.55, 0.72, 0.76, 1), "Metallic": 1.0, "Roughness": 0.06})
finish(bubm, bpb.outputs[0])


def bubble(name, start, p0, rise, life, size):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=14, ring_count=8, radius=size)
    b = bpy.context.object; b.name = name; smooth(b)
    b.data.materials.append(bubm)
    b.visible_shadow = False
    b.scale = (0, 0, 0); b.keyframe_insert("scale", frame=start - 1)
    b.scale = (1, 1, 0.8); b.keyframe_insert("scale", frame=start)
    steps = 5
    for k in range(steps + 1):
        f = start + k * life / steps
        wob = Vector((math.sin(k * 1.7 + size * 90) * 0.04 * k, math.cos(k * 1.3) * 0.03 * k, 0))
        b.location = p0 + Vector((0, 0, rise * k / steps)) + wob
        b.keyframe_insert("location", frame=int(f))
    b.scale = (0, 0, 0); b.keyframe_insert("scale", frame=int(start + life) + 1)


# Splash bubbles when the head goes under
for i in range(70):
    p0 = Vector((random.uniform(-0.9, 0.9), random.uniform(-8.6, -7.4), random.uniform(-1.6, -0.4)))
    bubble(f"Splash{i}", SUBMERGE + random.randint(0, 6), p0, random.uniform(0.6, 1.4), 40, random.uniform(0.008, 0.05))

# ------------------------------------------------------- fish school
def build_fish(name, phase):
    bm = bmesh.new()
    res = bmesh.ops.create_uvsphere(bm, u_segments=18, v_segments=10, radius=1)
    for v in res["verts"]:
        x, y, z = v.co
        taper = 0.4 + 0.6 * ((1 + x) / 2) ** 0.55
        v.co = Vector((x * 0.16, y * 0.028 * taper, z * 0.05 * taper - 0.004 * (1 - x)))
    tail = [bm.verts.new(c) for c in ((-0.14, 0, 0), (-0.25, 0, 0.06), (-0.22, 0, 0), (-0.25, 0, -0.06))]
    bm.faces.new(tail)
    for c in (((0.03, 0, 0.04), (-0.03, 0, 0.075), (-0.06, 0, 0.035)), ((-0.05, 0, -0.03), (-0.09, 0, -0.05), (-0.1, 0, -0.025))):
        bm.faces.new([bm.verts.new(p) for p in c])
    ob = mesh_object(name, bm)
    smooth(ob)
    ob.shape_key_add(name="Basis")
    sk = ob.shape_key_add(name="Wag")
    sk.slider_min = -1
    for i, v in enumerate(ob.data.vertices):
        if v.co.x < 0.04:
            sk.data[i].co.y = v.co.y + 3.0 * (0.04 - v.co.x) ** 2
    d = sk.driver_add("value").driver
    d.type = "SCRIPTED"
    d.expression = f"sin(frame*1.3+{phase:.2f})"
    return ob


fmat = new_material("Fish")
t = fmat.node_tree
fp = N(t, "ShaderNodeBsdfPrincipled", Metallic=0.95, Roughness=0.18, **{"Anisotropic": 0.5})
ftc = t.nodes.new("ShaderNodeTexCoord"); fsp = t.nodes.new("ShaderNodeSeparateXYZ"); L(t, ftc.outputs["Object"], fsp.inputs[0])
frr = N(t, "ShaderNodeMapRange", **{"From Min": -0.02, "From Max": 0.03}); L(t, fsp.outputs["Z"], frr.inputs["Value"])
stripes = N(t, "ShaderNodeTexWave", Scale=18.0, Distortion=2.0); stripes.bands_direction = "X"
L(t, ftc.outputs["Object"], stripes.inputs["Vector"])
fmix = M(t, "ADD", frr.outputs[0], M(t, "MULTIPLY", stripes.outputs["Fac"], 0.15))
L(t, ramp(t, [(0.0, (0.88, 0.9, 0.92, 1)), (0.5, (0.55, 0.66, 0.7, 1)), (0.85, (0.06, 0.16, 0.24, 1))], fmix), fp.inputs["Base Color"])
finish(fmat, fp.outputs[0], caustic=0.6)
templates = [build_fish(f"FishT{i}", i * 1.7) for i in range(4)]
for tpl in templates:
    tpl.data.materials.append(fmat)
    tpl.hide_render = True
    tpl.location = (0, 0, -100)

school = bpy.data.objects.new("School", None); scene.collection.objects.link(school)
# The school drifts up from below during the descent and swirls past the diver.
SCHOOL_KEYS = [(90, (4, 2.0, -6.0), 160), (170, (1.0, -1.0, -6.0), 200), (240, (-2.5, 0.5, -6.6), 245), (330, (-5, 5, -7.2), 280), (END, (-6, 14, -8), 300)]
for f, loc, yaw in SCHOOL_KEYS:
    school.location = loc
    school.rotation_euler = (0, 0, math.radians(yaw))
    school.keyframe_insert("location", frame=f)
    school.keyframe_insert("rotation_euler", frame=f)
for i in range(240):
    tpl = templates[i % 4]
    ob = bpy.data.objects.new(f"Fish{i}", tpl.data)
    scene.collection.objects.link(ob)
    ob.parent = school
    ob.visible_shadow = False
    ob.scale = [random.uniform(0.8, 1.25)] * 3
    off = Vector((random.gauss(0, 1.2), random.gauss(0, 0.9), random.gauss(0, 0.5)))
    for f in range(80, END + 1, 16):
        ob.location = off + Vector((random.uniform(-0.2, 0.2), random.uniform(-0.15, 0.15), random.uniform(-0.1, 0.1)))
        ob.rotation_euler = (0, math.radians(random.uniform(-4, 4)), math.radians(random.uniform(-12, 12)))
        ob.keyframe_insert("location", frame=f)
        ob.keyframe_insert("rotation_euler", frame=f)

# ------------------------------------------------------- humpback whale
WL = 14.0


def lerp_table(table, u):
    for (u0, v0), (u1, v1) in zip(table, table[1:]):
        if u <= u1:
            k = (u - u0) / (u1 - u0)
            k = k * k * (3 - 2 * k)
            return v0 + (v1 - v0) * k
    return table[-1][1]


WIDTH = [(0, 0.03), (0.03, 0.42), (0.12, 0.9), (0.25, 1.35), (0.4, 1.5), (0.58, 1.2), (0.76, 0.6), (0.9, 0.24), (1.0, 0.12)]
TOP = [(0, 0.03), (0.03, 0.2), (0.18, 0.55), (0.3, 0.95), (0.42, 1.2), (0.58, 1.12), (0.76, 0.75), (0.9, 0.4), (1.0, 0.18)]
BOTTOM = [(0, 0.03), (0.03, 0.35), (0.15, 0.95), (0.27, 1.45), (0.42, 1.3), (0.58, 1.0), (0.76, 0.66), (0.9, 0.38), (1.0, 0.16)]

bm = bmesh.new()
RINGS, SEG = 90, 40
rings = []
for r in range(RINGS + 1):
    u = (r / RINGS) ** 1.12
    x = WL * (0.5 - u)
    w, ht, hb = lerp_table(WIDTH, u), lerp_table(TOP, u), lerp_table(BOTTOM, u)
    ring = []
    for s in range(SEG):
        a = 2 * math.pi * s / SEG
        c, sn = math.cos(a), math.sin(a)
        e = 2.3
        y = math.copysign(abs(c) ** (2 / e), c) * w
        z = math.copysign(abs(sn) ** (2 / e), sn) * (ht if sn > 0 else hb)
        # mouth line: a shallow groove from the snout to below the eye
        if u < 0.24 and abs(sn + 0.15) < 0.12:
            k = 1 - abs(sn + 0.15) / 0.12
            z -= 0.0
            y *= 1 - 0.035 * k * (1 - u / 0.24)
        # keel on the tail stock
        if u > 0.75 and abs(c) < 0.25:
            z *= 1 + 0.25 * (u - 0.75) / 0.25
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


def loft(root, span_dir, chord_dir, up_dir, sections, K=16):
    span_dir, chord_dir, up_dir = Vector(span_dir).normalized(), Vector(chord_dir).normalized(), Vector(up_dir).normalized()
    rs = []
    for s, chord, thick, sweep in sections:
        centre = Vector(root) + span_dir * s + chord_dir * sweep
        ring = []
        for k in range(K):
            a = 2 * math.pi * k / K
            cx = math.cos(a)
            th = thick * math.sin(a) * (1 - 0.65 * max(0, -cx))
            ring.append(bm.verts.new(centre + chord_dir * (0.5 * chord * cx) + up_dir * th))
        rs.append(ring)
    for i in range(len(rs) - 1):
        for k in range(K):
            bm.faces.new((rs[i][k], rs[i][(k + 1) % K], rs[i + 1][(k + 1) % K], rs[i + 1][k]))
    bm.faces.new(list(reversed(rs[0])))
    bm.faces.new(rs[-1])


# Pectoral fins with tubercle bumps on the leading edge
for side in (1, -1):
    secs = []
    for i in range(17):
        s = i / 16 * 4.6
        chord = 0.95 * (1 - i / 16) ** 0.6 + 0.12
        bumps = 0.06 * max(0, math.sin(i * 2.6))
        secs.append((s, chord + bumps, 0.09 * chord + 0.02, -0.25 * s + bumps * 0.5))
    loft((2.7, side * 1.2, -0.8), (-0.25, side * 0.9, -0.38), (1, 0, -0.05), (0, -side * 0.35, 0.9), secs)
pect_end = len(bm.faces)
# Flukes with a scalloped trailing edge
tx = -WL / 2 + 0.1
for side in (1, -1):
    secs = []
    for i in range(15):
        k = i / 14
        s = k * 2.5
        chord = 1.1 * (1 - k ** 1.6) + 0.1 + 0.05 * math.sin(i * 2.2)
        secs.append((s, chord, 0.07 * chord + 0.015, -0.6 * k ** 1.4 * 1.4))
    loft((tx - 0.45, side * 0.05, 0), (0, side, 0), (1, 0, 0), (0, 0, 1), secs)
loft((-2.4, 0, 0.95), (-0.3, 0, 1), (1, 0, 0), (0, 1, 0), [(0.0, 1.0, 0.1, 0), (0.25, 0.7, 0.08, -0.15), (0.45, 0.3, 0.05, -0.35)])
# Tubercles (knobs) along the rostrum and lower jaw
for i in range(46):
    u = random.uniform(0.02, 0.22)
    x = WL * (0.5 - u)
    ang = random.choice([random.uniform(0.35, 1.25), random.uniform(1.9, 2.8), random.uniform(-0.7, -0.35), random.uniform(-2.8, -2.45)])
    w, ht, hb = lerp_table(WIDTH, u), lerp_table(TOP, u), lerp_table(BOTTOM, u)
    p = Vector((x, math.cos(ang) * w * 0.97, math.sin(ang) * (ht if math.sin(ang) > 0 else hb) * 0.97))
    res = bmesh.ops.create_icosphere(bm, subdivisions=1, radius=random.uniform(0.045, 0.08))
    for v in res["verts"]:
        v.co += p
# Eyes
for side in (1, -1):
    u = 0.215
    res = bmesh.ops.create_uvsphere(bm, u_segments=12, v_segments=8, radius=0.07)
    for v in res["verts"]:
        v.co += Vector((WL * (0.5 - u), side * lerp_table(WIDTH, u) * 0.96, -0.12))

whale = mesh_object("Whale", bm)
smooth(whale)
sub = whale.modifiers.new("Sub", "SUBSURF"); sub.levels = 1; sub.render_levels = 2

wmat = new_material("WhaleSkin")
t = wmat.node_tree
wp = N(t, "ShaderNodeBsdfPrincipled", Roughness=0.38, **{"Coat Weight": 0.35, "Coat Roughness": 0.2})
wtc = t.nodes.new("ShaderNodeTexCoord"); wsp = t.nodes.new("ShaderNodeSeparateXYZ"); L(t, wtc.outputs["Object"], wsp.inputs[0])
wr = N(t, "ShaderNodeMapRange", **{"From Min": -0.9, "From Max": 0.05}); L(t, wsp.outputs["Z"], wr.inputs["Value"])
noise = N(t, "ShaderNodeTexNoise", Scale=1.4, Detail=10.0, Roughness=0.62); L(t, wtc.outputs["Object"], noise.inputs["Vector"])
fine = N(t, "ShaderNodeTexNoise", Scale=22.0, Detail=6.0); L(t, wtc.outputs["Object"], fine.inputs["Vector"])
mott = M(t, "MULTIPLY_ADD", noise.outputs["Fac"], 0.7, -0.35)
wcol = ramp(t, [(0.22, (0.72, 0.73, 0.72, 1)), (0.42, (0.3, 0.32, 0.33, 1)), (0.62, (0.02, 0.025, 0.032, 1))], M(t, "ADD", wr.outputs[0], mott))
spots = t.nodes.new("ShaderNodeTexVoronoi"); spots.inputs["Scale"].default_value = 7.0; L(t, wtc.outputs["Object"], spots.inputs["Vector"])
spot_m = ramp(t, [(0.0, (1, 1, 1, 1)), (0.14, (0, 0, 0, 1))], spots.outputs["Distance"])
headm = N(t, "ShaderNodeMapRange", **{"From Min": 3.0, "From Max": 6.0}); L(t, wsp.outputs["X"], headm.inputs["Value"])
scar_n = N(t, "ShaderNodeTexWave", Scale=0.6, Distortion=12.0, Detail=4.0); L(t, wtc.outputs["Object"], scar_n.inputs["Vector"])
scars = ramp(t, [(0.0, (1, 1, 1, 1)), (0.04, (0, 0, 0, 1))], scar_n.outputs["Fac"])
spot_f = M(t, "MAXIMUM", M(t, "MULTIPLY", spot_m, headm.outputs[0]), M(t, "MULTIPLY", scars, 0.35))
colm = t.nodes.new("ShaderNodeMix"); colm.data_type = "RGBA"
L(t, spot_f, colm.inputs[0]); L(t, wcol, colm.inputs[6]); colm.inputs[7].default_value = (0.78, 0.76, 0.7, 1)
L(t, colm.outputs[2], wp.inputs["Base Color"])
pleat = N(t, "ShaderNodeTexWave", Scale=2.6); pleat.wave_type = "BANDS"; pleat.bands_direction = "Y"
L(t, wtc.outputs["Object"], pleat.inputs["Vector"])
belly = N(t, "ShaderNodeMapRange", **{"From Min": -0.35, "From Max": -1.1}); L(t, wsp.outputs["Z"], belly.inputs["Value"])
front = N(t, "ShaderNodeMapRange", **{"From Min": -0.5, "From Max": 1.5}); L(t, wsp.outputs["X"], front.inputs["Value"])
height = M(t, "ADD", M(t, "MULTIPLY", M(t, "MULTIPLY", pleat.outputs["Fac"], belly.outputs[0]), front.outputs[0]),
           M(t, "ADD", M(t, "MULTIPLY", noise.outputs["Fac"], 0.25), M(t, "MULTIPLY", fine.outputs["Fac"], 0.12)))
height = M(t, "ADD", height, M(t, "MULTIPLY", spot_f, 0.5))
bump = N(t, "ShaderNodeBump", Strength=0.45, Distance=0.05); L(t, height, bump.inputs["Height"]); L(t, bump.outputs[0], wp.inputs["Normal"])
finish(wmat, wp.outputs[0], caustic=1.6)
whale.data.materials.append(wmat)
finm = new_material("WhaleFin")
t = finm.node_tree
fpb = N(t, "ShaderNodeBsdfPrincipled", Roughness=0.42, **{"Coat Weight": 0.3})
ftc2 = t.nodes.new("ShaderNodeTexCoord"); fn = N(t, "ShaderNodeTexNoise", Scale=2.2, Detail=8.0); L(t, ftc2.outputs["Object"], fn.inputs["Vector"])
L(t, ramp(t, [(0.42, (0.8, 0.81, 0.8, 1)), (0.6, (0.05, 0.06, 0.07, 1))], fn.outputs["Fac"]), fpb.inputs["Base Color"])
fb = N(t, "ShaderNodeBump", Strength=0.3); L(t, fn.outputs["Fac"], fb.inputs["Height"]); L(t, fb.outputs[0], fpb.inputs["Normal"])
finish(finm, fpb.outputs[0], caustic=1.6)
whale.data.materials.append(finm)
for p in whale.data.polygons[body_faces:pect_end]:
    p.material_index = 1

arm_data = bpy.data.armatures.new("WhaleRig")
rig = bpy.data.objects.new("WhaleRig", arm_data); scene.collection.objects.link(rig)
bpy.context.view_layer.objects.active = rig
bpy.ops.object.mode_set(mode="EDIT")
NB = 9
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
for pb in rig.pose.bones:
    pb.rotation_mode = "XYZ"
for f in range(150, END + 1, 2):
    tsec = f / FPS
    for i, pb in enumerate(rig.pose.bones):
        amp = 0.01 + 0.08 * (i / (NB - 1)) ** 2.2
        pb.rotation_euler = (amp * math.sin(2 * math.pi * 0.36 * tsec - i * 0.6), 0, 0)
        pb.keyframe_insert("rotation_euler", frame=f)

# Close pass, slightly above the diver so it is framed against the bright surface.
WHALE_KEYS = [
    (170, (26, 9.5, -6.8), 184, 0),
    (260, (12, 6.2, -5.6), 191, -6),
    (350, (0, 4.6, -4.9), 197, -10),
    (420, (-9, 4.6, -4.6), 203, -8),
    (END, (-15, 5.4, -4.5), 207, -6),
]
for f, loc, yaw, roll in WHALE_KEYS:
    rig.location = loc
    rig.rotation_euler = (math.radians(roll), math.radians(-2), math.radians(yaw))
    rig.keyframe_insert("location", frame=f)
    rig.keyframe_insert("rotation_euler", frame=f)

# ------------------------------------------------------- diver camera
cam_data = bpy.data.cameras.new("Cam"); cam_data.lens = 18; cam_data.sensor_fit = "AUTO"
cam_data.clip_start = 0.02; cam_data.clip_end = 300
cam_data.dof.use_dof = True
cam_data.dof.aperture_fstop = 2.0
camera = bpy.data.objects.new("Camera", cam_data); scene.collection.objects.link(camera); scene.camera = camera
for f, d in ((0, 8.0), (SUBMERGE, 2.5), (180, 3.0), (250, 6.0), (330, 5.2), (420, 5.5), (END, 7.0)):
    cam_data.dof.focus_distance = d
    cam_data.dof.keyframe_insert("focus_distance", frame=f)

# Base head path (smooth), then layered with breathing and hand-held micro motion.
HEAD_KEYS = [
    (0, (0, -9.0, 0.55), (88, 0, 0)),
    (40, (0, -8.9, 0.52), (86, 0, -6)),
    (66, (0, -8.8, 0.3), (78, 0, -2)),
    (SUBMERGE, (0, -8.6, -0.35), (62, 0, 0)),
    (110, (0, -8.3, -1.4), (42, 0, 2)),
    (170, (0, -7.6, -3.6), (55, 0, -4)),
    (220, (0, -7.0, -5.4), (78, 0, -16)),
    (270, (0, -6.7, -6.0), (92, 0, -26)),
    (330, (0, -6.5, -6.2), (100, 0, -6)),
    (400, (0, -6.4, -6.2), (102, 0, 16)),
    (END, (0, -6.3, -6.1), (97, 0, 32)),
]
def head_at(f):
    """Smooth Catmull-Rom interpolation of HEAD_KEYS → (location, rotation in radians)."""
    keys = HEAD_KEYS
    i = max(j for j in range(len(keys)) if keys[j][0] <= f) if f >= keys[0][0] else 0
    i = min(i, len(keys) - 2)
    f0, f1 = keys[i][0], keys[i + 1][0]
    u = min(max((f - f0) / (f1 - f0), 0.0), 1.0)
    out = []
    for comp in (1, 2):
        p0 = Vector(keys[max(i - 1, 0)][comp]); p1 = Vector(keys[i][comp])
        p2 = Vector(keys[i + 1][comp]); p3 = Vector(keys[min(i + 2, len(keys) - 1)][comp])
        m1 = (p2 - p0) * 0.5 * (f1 - f0) / max(1, keys[i + 1][0] - keys[max(i - 1, 0)][0]) * 2
        m2 = (p3 - p1) * 0.5 * (f1 - f0) / max(1, keys[min(i + 2, len(keys) - 1)][0] - keys[i][0]) * 2
        h00 = 2 * u ** 3 - 3 * u ** 2 + 1; h10 = u ** 3 - 2 * u ** 2 + u; h01 = -2 * u ** 3 + 3 * u ** 2; h11 = u ** 3 - u ** 2
        out.append(p1 * h00 + m1 * h10 + p2 * h01 + m2 * h11)
    return out[0], Euler([math.radians(a) for a in out[1]], "XYZ")


ph = [random.uniform(0, 6.3) for _ in range(8)]


def breath(f):
    """+1 full inhale … -1 empty; 4 s cycle once underwater."""
    if f < SUBMERGE + 10:
        return 0.0
    return math.sin(2 * math.pi * (f - EXHALES[0] + 30) / 120)


camera.rotation_mode = "XYZ"
CAM_POSE = {}
for f in range(0, END + 1):
    loc, rot = head_at(f)
    surf = f < SUBMERGE
    sway = 1.6 if surf else 0.7
    rot.x += math.radians(sway * (0.9 * math.sin(f * 0.045 + ph[0]) + 0.4 * math.sin(f * 0.13 + ph[1])))
    rot.z += math.radians(sway * (0.8 * math.sin(f * 0.037 + ph[2]) + 0.3 * math.sin(f * 0.11 + ph[3])))
    rot.y += math.radians(sway * (0.9 * math.sin(f * 0.05 + ph[4]) + 0.3 * math.sin(f * 0.17 + ph[5])))
    if surf:
        # bobbing with the swell
        loc.z += 0.09 * math.sin(f * 0.21) + 0.04 * math.sin(f * 0.53 + 1)
        rot.y += math.radians(2.0 * math.sin(f * 0.21 + 0.6))
    loc.z += 0.05 * breath(f)
    loc.x += 0.03 * math.sin(f * 0.031 + ph[6])
    camera.location = loc
    camera.rotation_euler = rot
    CAM_POSE[f] = (loc.copy(), rot.copy())
    camera.keyframe_insert("location", frame=f)
    camera.keyframe_insert("rotation_euler", frame=f)

# Exhaled bubbles: from the regulator below the mask, rising past the sides of the face.
for n, ex in enumerate(EXHALES):
    cl, cr = CAM_POSE[ex]
    mw = Matrix.Translation(cl) @ cr.to_matrix().to_4x4()
    for i in range(55):
        start = ex + int(random.uniform(0, 30) ** 1.2 / 2.2)
        side = random.choice((-1, 1))
        local = Vector((side * random.uniform(0.1, 0.32), random.uniform(-0.35, -0.12), -random.uniform(0.15, 0.4)))
        p0 = mw @ local
        size = random.uniform(0.004, 0.028) if random.random() < 0.85 else random.uniform(0.03, 0.06)
        bubble(f"Ex{n}_{i}", start, p0, random.uniform(1.4, 2.4), random.randint(40, 60), size)
with open(f"{args.out}/breaths.txt", "w") as fh:
    fh.write(" ".join(str(e) for e in EXHALES) + f"\n{SUBMERGE}\n")

# ------------------------------------------------------- render settings
r = scene.render
r.engine = "BLENDER_EEVEE"
r.resolution_x, r.resolution_y = 1080, 1920
r.resolution_percentage = args.scale
ee = scene.eevee
ee.taa_render_samples = args.samples
ee.use_shadows = True
ee.shadow_ray_count = 1
ee.shadow_step_count = 2
ee.shadow_resolution_scale = 0.5
ee.use_raytracing = False
scene.view_settings.view_transform = "AgX"
try:
    scene.view_settings.look = "AgX - Medium High Contrast"
except TypeError:
    pass
comp = bpy.data.node_groups.new("Post", "CompositorNodeTree")
comp.interface.new_socket("Image", in_out="OUTPUT", socket_type="NodeSocketColor")
rl = comp.nodes.new("CompositorNodeRLayers")
beams = comp.nodes.new("CompositorNodeGlare")
beams.inputs["Type"].default_value = "Sun Beams"
beams.inputs["Threshold"].default_value = 0.9
key_step(beams.inputs["Strength"], SUBMERGE + 4, 0.0, 0.55)
beams.inputs["Size"].default_value = 0.55
beams.inputs["Sun Position"].default_value = (0.62, 1.25)
bloom = comp.nodes.new("CompositorNodeGlare")
bloom.inputs["Type"].default_value = "Bloom"
bloom.inputs["Threshold"].default_value = 1.4
bloom.inputs["Strength"].default_value = 0.35
lens = comp.nodes.new("CompositorNodeLensdist")
lens.inputs["Distortion"].default_value = 0.035
lens.inputs["Dispersion"].default_value = 0.012
lens.inputs["Fit"].default_value = True
gout = comp.nodes.new("NodeGroupOutput")
comp.links.new(rl.outputs["Image"], beams.inputs["Image"])
comp.links.new(beams.outputs["Image"], bloom.inputs["Image"])
comp.links.new(bloom.outputs["Image"], lens.inputs["Image"])
comp.links.new(lens.outputs["Image"], gout.inputs[0])
scene.compositing_node_group = comp
r.use_compositing = True
r.image_settings.file_format = "PNG"
r.image_settings.color_mode = "RGB"

if args.save_blend:
    bpy.ops.wm.save_as_mainfile(filepath=args.save_blend)

if args.frames == "none":
    frames = []
elif "-" in args.frames:
    a, b = (int(x) for x in args.frames.split("-"))
    frames = list(range(a, b + 1))
else:
    frames = [int(x) for x in args.frames.split(",") if x] if args.frames else list(range(0, END + 1))
for f in frames:
    scene.frame_set(f)
    r.filepath = f"{args.out}/f{f:04d}.png"
    t0 = time.time()
    bpy.ops.render.render(write_still=True)
    print(f"FRAME {f} {time.time() - t0:.1f}s", flush=True)
