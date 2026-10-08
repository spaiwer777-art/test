"""Preview renders (Cycles, CPU):

    blender -b --factory-startup -P render.py -- [keys...] [--group] [--cutaway KEY] [--fast]
"""
import json
import math
import os
import sys

import bpy
from mathutils import Vector

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import common as C  # noqa: E402

BUILD = os.path.join(HERE, '..', 'build')
STL = os.path.join(HERE, '..', 'stl')
OUT = os.path.join(HERE, '..', 'renders')
PLINTH_RGB = (0.05, 0.055, 0.065)
FAST = False
PLY = False


def reset(samples):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = 'CYCLES'
    sc.cycles.device = 'CPU'
    sc.cycles.samples = samples
    sc.cycles.use_denoising = True
    sc.cycles.max_bounces = 6
    sc.view_settings.view_transform = 'AgX'
    sc.view_settings.look = 'AgX - Medium High Contrast'
    sc.render.film_transparent = False
    world = bpy.data.worlds.new('w')
    world.use_nodes = True
    bg = world.node_tree.nodes['Background']
    bg.inputs[0].default_value = (0.82, 0.84, 0.88, 1)
    bg.inputs[1].default_value = 0.35
    sc.world = world
    return sc


def material(name, rgb, plinth_split=True, rough=0.42):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    bsdf = nt.nodes['Principled BSDF']
    bsdf.inputs['Roughness'].default_value = rough
    if not plinth_split:
        bsdf.inputs['Base Color'].default_value = (*rgb, 1)
        return m
    tc = nt.nodes.new('ShaderNodeTexCoord')
    sep = nt.nodes.new('ShaderNodeSeparateXYZ')
    gt = nt.nodes.new('ShaderNodeMath')
    gt.operation = 'GREATER_THAN'
    gt.inputs[1].default_value = C.PLINTH_H + 0.05
    mix = nt.nodes.new('ShaderNodeMix')
    mix.data_type = 'RGBA'
    mix.inputs['A'].default_value = (*PLINTH_RGB, 1)
    mix.inputs['B'].default_value = (*rgb, 1)
    nt.links.new(tc.outputs['Object'], sep.inputs[0])
    nt.links.new(sep.outputs['Z'], gt.inputs[0])
    nt.links.new(gt.outputs[0], mix.inputs['Factor'])
    nt.links.new(mix.outputs['Result'], bsdf.inputs['Base Color'])
    return m


def load(key, loc=(0, 0, 0), rot_z=0.0):
    if PLY:
        bpy.ops.wm.ply_import(filepath=os.path.join(BUILD, key + '.ply'))
    else:
        bpy.ops.wm.stl_import(filepath=os.path.join(STL, key + '.stl'))
    ob = bpy.context.selected_objects[0]
    ob.location = loc
    ob.rotation_euler.z = rot_z
    bpy.ops.object.shade_auto_smooth(angle=math.radians(35))
    return ob


def studio(size=1.0, floor_rgb=(0.78, 0.8, 0.84)):
    bpy.ops.mesh.primitive_plane_add(size=2000, location=(0, 0, 0))
    floor = bpy.context.active_object
    fm = bpy.data.materials.new('floor')
    fm.use_nodes = True
    fm.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value = (*floor_rgb, 1)
    fm.node_tree.nodes['Principled BSDF'].inputs['Roughness'].default_value = 0.6
    floor.data.materials.append(fm)

    def area(name, loc, energy, s, color=(1, 1, 1)):
        bpy.ops.object.light_add(type='AREA', location=loc)
        L = bpy.context.active_object
        L.data.energy = energy * size * size
        L.data.size = s * size
        L.data.color = color
        d = Vector((0, 0, 60 * size)) - L.location
        L.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()
        return L
    area('key', (-180 * size, -220 * size, 260 * size), 2.2e6, 160, (1.0, 0.96, 0.9))
    area('fill', (260 * size, -160 * size, 120 * size), 0.7e6, 220, (0.85, 0.9, 1.0))
    area('rim', (60 * size, 300 * size, 240 * size), 1.4e6, 120)


def camera(target, dist, elev_deg=14, az_deg=-22, lens=85):
    cam_data = bpy.data.cameras.new('cam')
    cam_data.lens = lens
    cam_data.clip_end = 10000
    cam = bpy.data.objects.new('cam', cam_data)
    bpy.context.collection.objects.link(cam)
    t = Vector(target)
    e, a = math.radians(elev_deg), math.radians(az_deg)
    cam.location = t + Vector((dist * math.cos(e) * math.sin(a), -dist * math.cos(e) * math.cos(a), dist * math.sin(e)))
    cam.rotation_euler = (t - cam.location).to_track_quat('-Z', 'Y').to_euler()
    bpy.context.scene.camera = cam
    return cam


def render(path, res=(1200, 1500)):
    sc = bpy.context.scene
    if FAST:
        res = (res[0] // 2, res[1] // 2)
    sc.render.resolution_x, sc.render.resolution_y = res
    sc.render.image_settings.file_format = 'JPEG'
    sc.render.image_settings.quality = 90
    sc.render.filepath = path
    bpy.ops.render.render(write_still=True)


def hero(key, samples):
    reset(samples)
    meta = json.load(open(os.path.join(BUILD, key + '.json')))
    ob = load(key)
    ob.data.materials.append(material(key, meta['color']))
    studio()
    h = ob.dimensions.z
    camera((0, 0, h * 0.47), 470, elev_deg=12, az_deg=-24)
    render(os.path.join(OUT, key + '.jpg'))


def group(keys, samples):
    reset(samples)
    studio(size=2.2)
    cols = (len(keys) + 1) // 2
    for i, key in enumerate(keys):
        meta = json.load(open(os.path.join(BUILD, key + '.json')))
        r, c = divmod(i, cols)
        n = cols if r == 0 else len(keys) - cols
        x = (c - (n - 1) / 2) * 96 + (0 if n != cols else (-24 if r == 0 else 24))
        y = r * 260
        ob = load(key, (x, y, 0))
        ob.data.materials.append(material(key, meta['color']))
    camera((0, 125, 50), 1250, elev_deg=33, az_deg=0, lens=68)
    render(os.path.join(OUT, '00_collection.jpg'), res=(2400, 1600))


def cutaway(key, samples):
    """Half-section of a figure with the cap screwed in and a stick inside."""
    reset(samples)
    meta = json.load(open(os.path.join(BUILD, key + '.json')))
    ob = load(key)
    ob.data.materials.append(material(key, meta['color']))
    bpy.ops.wm.stl_import(filepath=os.path.join(STL, 'cap_universal.stl'))
    cap = bpy.context.selected_objects[0]
    cap.location.z = C.FLANGE_Z0
    cap.data.materials.append(material('cap', (0.9, 0.75, 0.1), plinth_split=False))
    # dummy USB stick: 58 x 20 x 9.5 mm body + metal plug
    L, W, T, P = 58.0, 20.0, 9.5, 12.0
    z0 = C.CB_H + 0.2

    def block(size, zc, mat):
        bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, zc))
        b = bpy.context.active_object
        b.scale = size
        bpy.ops.object.transform_apply(scale=True)
        bev = b.modifiers.new('b', 'BEVEL')
        bev.width = 0.8
        bev.segments = 3
        b.data.materials.append(mat)
        return b
    block((W, T, L - P), z0 + (L - P) / 2, material('stick', (0.06, 0.25, 0.75), plinth_split=False, rough=0.3))
    block((12.0, 4.5, P), z0 + L - P / 2, material('plug', (0.8, 0.8, 0.82), plinth_split=False, rough=0.15))
    for target in (ob, cap):
        bpy.ops.mesh.primitive_cube_add(size=1, location=(0, -150, 100))
        cutter = bpy.context.active_object
        cutter.scale = (300, 300, 300)
        bpy.ops.object.select_all(action='DESELECT')
        target.select_set(True)
        bpy.context.view_layer.objects.active = target
        m = target.modifiers.new('cut', 'BOOLEAN')
        m.solver = 'EXACT'
        m.object = cutter
        bpy.ops.object.modifier_apply(modifier=m.name)
        bpy.data.objects.remove(cutter, do_unlink=True)
    studio()
    h = ob.dimensions.z
    camera((0, 0, h * 0.42), 520, elev_deg=10, az_deg=-30)
    render(os.path.join(OUT, key + '_cutaway.jpg'))


if __name__ == '__main__':
    argv = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
    FAST = '--fast' in argv
    PLY = '--ply' in argv
    OUT = os.path.join(BUILD, 'preview') if PLY else OUT
    samples = 32 if FAST else 160
    os.makedirs(OUT, exist_ok=True)
    keys = [a for a in argv if not a.startswith('--')]
    cut = None
    if '--cutaway' in argv:
        cut = argv[argv.index('--cutaway') + 1]
        keys = [k for k in keys if k != cut]
    if not keys and '--group' not in argv and not cut:
        keys = sorted(f[:-4] for f in os.listdir(STL) if f[:2].isdigit() and f.endswith('.stl'))
    for k in keys:
        hero(k, samples)
    if '--group' in argv:
        group(sorted(f[:-4] for f in os.listdir(STL) if f[:2].isdigit() and f.endswith('.stl')), samples)
    if cut:
        cutaway(cut, samples)
