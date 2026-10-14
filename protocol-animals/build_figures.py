"""Сетевые протоколы в виде животных — однотонные фигурки для 3D-печати.

Скрипт строит в Blender шесть фигурок на подставках с подписью, делает из каждой
одну замкнутую (watertight) сетку для печати и сохраняет:

  stl/<protocol>_<animal>.stl   — модели для слайсера (1 единица = 1 мм)
  protocol_animals.blend        — сцена: исходные детали + сетки для печати
  renders/*.png                 — превью (каждая фигурка одним цветом пластика)

Фигурки рассчитаны на печать одним цветом: все детали читаются формой,
а не раскраской (глаза-пуговки, рельефные ленты и надписи, крупные иглы).

Запуск (любой из вариантов):
  blender -b -P build_figures.py
  python3 build_figures.py        # с установленным модулем bpy (pip install bpy)

Флаги: --no-render (без превью), --only TCP,DNS (только выбранные фигурки).
"""

import math
import os
import sys

import bpy  # noqa: I001 — bpy должен импортироваться раньше bmesh
import bmesh
from mathutils import Euler, Matrix, Vector

HERE = os.path.dirname(os.path.abspath(__file__))
STL_DIR = os.path.join(HERE, "stl")
RENDER_DIR = os.path.join(HERE, "renders")

VOXEL = 0.22           # мм, шаг воксельного ремеша (детализация печатной сетки)
TARGET_TRIS = 150_000  # примерный бюджет треугольников на фигурку
BASE_TOP = 8.0         # высота верхней площадки подставки, мм

FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/opentype/inter/Inter-ExtraBold.otf",
    "C:/Windows/Fonts/arialbd.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/Library/Fonts/Arial Bold.ttf",
]


# --------------------------------------------------------------------------- #
#  Сцена и примитивы
# --------------------------------------------------------------------------- #

def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.unit_settings.system = "METRIC"
    scene.unit_settings.scale_length = 0.001
    scene.unit_settings.length_unit = "MILLIMETERS"


def plastic(name, rgb):
    mat = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*rgb, 1.0)
    bsdf.inputs["Roughness"].default_value = 0.42
    mat.diffuse_color = (*rgb, 1.0)
    return mat


class Figure:
    """Собирает детали одной фигурки в отдельную коллекцию."""

    def __init__(self, key, title):
        self.key = key
        self.title = title
        self.coll = bpy.data.collections.new(f"{key} — детали")
        bpy.context.scene.collection.children.link(self.coll)
        self.parts = []

    def add_bm(self, name, bm, smooth=True):
        mesh = bpy.data.meshes.new(name)
        bm.to_mesh(mesh)
        bm.free()
        if smooth:
            mesh.shade_smooth()
        obj = bpy.data.objects.new(name, mesh)
        self.coll.objects.link(obj)
        self.parts.append(obj)
        return obj


FIG = None  # текущая фигурка


def trs(loc=(0, 0, 0), rot=(0, 0, 0), scale=(1, 1, 1)):
    """Матрица из положения, поворота (градусы, XYZ) и масштаба."""
    r = Euler([math.radians(a) for a in rot], "XYZ").to_matrix().to_4x4()
    s = Matrix.Diagonal((*scale, 1.0))
    return Matrix.Translation(loc) @ r @ s


def align_z(direction):
    return Vector((0, 0, 1)).rotation_difference(Vector(direction).normalized()).to_matrix().to_4x4()


def ellipsoid(loc, radii, rot=(0, 0, 0), name="ellipsoid", seg=48):
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=seg, v_segments=seg // 2, radius=1.0,
                              matrix=trs(loc, rot, radii))
    return FIG.add_bm(name, bm)


def sphere(loc, r, name="sphere", seg=32):
    return ellipsoid(loc, (r, r, r), name=name, seg=seg)


def box(loc, size, rot=(0, 0, 0), name="box"):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0, matrix=trs(loc, rot, size))
    return FIG.add_bm(name, bm, smooth=False)


def cone(p1, p2, r1, r2, flat=1.0, name="cone", seg=32, caps=True):
    """Конус между двумя точками со скруглёнными концами — лапы, шеи, уши.

    flat < 1 сплющивает сечение (уши, когти).
    """
    p1, p2 = Vector(p1), Vector(p2)
    d = p2 - p1
    m = Matrix.Translation((p1 + p2) / 2) @ align_z(d) @ Matrix.Diagonal((1, flat, 1, 1))
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, segments=seg, radius1=r1, radius2=r2,
                          depth=d.length, matrix=m)
    FIG.add_bm(name, bm)
    if caps:
        rot = m.to_3x3().normalized()
        for p, r in ((p1, r1), (p2, r2)):
            bm = bmesh.new()
            bmesh.ops.create_uvsphere(bm, u_segments=seg, v_segments=seg // 2, radius=1.0,
                                      matrix=Matrix.Translation(p) @ rot.to_4x4()
                                      @ Matrix.Diagonal((r, r * flat, r, 1)))
            FIG.add_bm(name + "_cap", bm)


def tube(points, r, name="tube"):
    """Трубка по ломаной."""
    for a, b in zip(points, points[1:]):
        cone(a, b, r, r, name=name, caps=False, seg=16)
    for p in points:
        sphere(p, r, name=name + "_joint", seg=16)


def torus(loc, R, r, rot=(0, 0, 0), scale=(1, 1, 1), name="torus", seg=48, rseg=16,
          arc=None):
    """Тор (или его дуга arc=(a0, a1) в градусах — с закрытыми торцами)."""
    bm = bmesh.new()
    closed = arc is None
    a0, a1 = (0.0, 2 * math.pi) if closed else (math.radians(arc[0]), math.radians(arc[1]))
    n = seg if closed else max(4, int(seg * abs(a1 - a0) / (2 * math.pi)) + 1)
    rings = []
    for i in range(n if closed else n + 1):
        a = a0 + (a1 - a0) * i / n
        ring = []
        for j in range(rseg):
            b = 2 * math.pi * j / rseg
            ring.append(bm.verts.new(((R + r * math.cos(b)) * math.cos(a),
                                      (R + r * math.cos(b)) * math.sin(a),
                                      r * math.sin(b))))
        rings.append(ring)
    for i in range(n):
        a, b = rings[i], rings[(i + 1) % len(rings)]
        for j in range(rseg):
            bm.faces.new((a[j], b[j], b[(j + 1) % rseg], a[(j + 1) % rseg]))
    if not closed:
        bm.faces.new(rings[0])
        bm.faces.new(list(reversed(rings[-1])))
    bmesh.ops.transform(bm, matrix=trs(loc, rot, scale), verts=bm.verts)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    return FIG.add_bm(name, bm)


def arc(center, radius, a0, a1, r, plane="xz", height=1.0, name="arc"):
    """Дуга-валик с круглыми концами. Углы в градусах.

    height > 1 вытягивает сечение вдоль нормали плоскости (для xy — вверх): так
    получаются стоящие «стенки» волн.
    """
    rot = {"xy": (0, 0, 0), "xz": (90, 0, 0), "yz": (90, 0, 90)}[plane]
    torus(center, radius, r, rot=rot, scale=(1, 1, height), name=name, arc=(a0, a1))
    m = trs(center, rot)
    for a in (a0, a1):
        a = math.radians(a)
        p = m @ Vector((radius * math.cos(a), radius * math.sin(a), 0))
        bm = bmesh.new()
        bmesh.ops.create_uvsphere(bm, u_segments=16, v_segments=8, radius=1.0,
                                  matrix=Matrix.Translation(p) @ trs(rot=rot, scale=(r, r, r * height)))
        FIG.add_bm(name + "_end", bm)


def prism(outline, thickness, matrix, name="prism"):
    """Плоская фигура по контуру (u, v), выдавленная на thickness; matrix ставит её в мир."""
    bm = bmesh.new()
    face = bm.faces.new([bm.verts.new((u, v, -thickness / 2)) for u, v in outline])
    ext = bmesh.ops.extrude_face_region(bm, geom=[face])
    top = [e for e in ext["geom"] if isinstance(e, bmesh.types.BMVert)]
    bmesh.ops.translate(bm, vec=(0, 0, thickness), verts=top)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bmesh.ops.triangulate(bm, faces=bm.faces)
    bmesh.ops.transform(bm, matrix=matrix, verts=bm.verts)
    return FIG.add_bm(name, bm, smooth=False)


def hull(points, name="hull"):
    bm = bmesh.new()
    for p in points:
        bm.verts.new(p)
    bmesh.ops.convex_hull(bm, input=bm.verts)
    loose = [v for v in bm.verts if not v.link_faces]
    bmesh.ops.delete(bm, geom=loose, context="VERTS")
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    return FIG.add_bm(name, bm, smooth=False)


def eye(center, direction, r):
    """Глаз-пуговка: приплюснутый купол, выступает из головы примерно на 0.6 r."""
    m = Matrix.Translation(center) @ align_z(direction) @ Matrix.Diagonal((r, r, r * 0.8, 1))
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=32, v_segments=16, radius=1.0, matrix=m)
    FIG.add_bm("eye", bm)


def ellipsoid_point(c, radii, theta, phi):
    """Точка и нормаль на эллипсоиде; theta — азимут от -Y (перед), phi — высота."""
    ux = math.sin(theta) * math.cos(phi)
    uy = -math.cos(theta) * math.cos(phi)
    uz = math.sin(phi)
    a, b, cz = radii
    p = Vector(c) + Vector((a * ux, b * uy, cz * uz))
    n = Vector((ux / a, uy / b, uz / cz)).normalized()
    return p, n


# --------------------------------------------------------------------------- #
#  Подставка с подписью
# --------------------------------------------------------------------------- #

def load_font():
    for path in FONT_CANDIDATES:
        if os.path.exists(path):
            return bpy.data.fonts.load(path, check_existing=True)
    return None  # встроенный шрифт Blender


def label_text(body, loc, rot, size, depth):
    curve = bpy.data.curves.new("label", "FONT")
    curve.body = body
    font = load_font()
    if font:
        curve.font = font
    curve.size = size
    curve.extrude = depth / 2
    curve.align_x = "CENTER"
    curve.align_y = "CENTER"
    curve.resolution_u = 6
    tmp = bpy.data.objects.new("label_tmp", curve)
    bpy.context.scene.collection.objects.link(tmp)
    tmp.matrix_world = trs(loc, rot)
    deps = bpy.context.evaluated_depsgraph_get()
    mesh = bpy.data.meshes.new_from_object(tmp.evaluated_get(deps))
    mesh.transform(tmp.matrix_world)
    bpy.data.objects.remove(tmp)
    bpy.data.curves.remove(curve)
    mesh.materials.clear()
    obj = bpy.data.objects.new("label_" + body, mesh)
    FIG.coll.objects.link(obj)
    FIG.parts.append(obj)
    return obj


def base(label):
    """Подставка: диск со скошенной передней гранью, на скосе — выпуклая надпись.

    Верх подставки — z = BASE_TOP, свободная площадка для фигурки: y > -13.
    """
    pts = []
    for z, r, chord in ((0.0, 27.0, -21.0), (1.2, 27.0, -21.0),
                        (BASE_TOP - 1.0, 26.2, -14.0), (BASE_TOP, 25.2, -13.0)):
        for i in range(128):
            a = 2 * math.pi * i / 128
            x, y = r * math.sin(a), -r * math.cos(a)
            if y >= chord:
                pts.append((x, y, z))
        hw = math.sqrt(r * r - chord * chord)
        pts += [(-hw, chord, z), (hw, chord, z)]
    hull(pts, name="base")

    # скос: от (y=-21, z=1.2) до (y=-14, z=7) — нормаль смотрит вперёд-вверх
    p0, p1 = Vector((0, -21.0, 1.2)), Vector((0, -14.0, BASE_TOP - 1.0))
    slope = p1 - p0
    tilt = math.degrees(math.atan2(slope.z, slope.y))
    normal = Vector((0, -slope.z, slope.y)).normalized()
    label_text(label, (p0 + p1) / 2 + normal * 0.25, (90 - tilt, 0, 0), size=8.5, depth=1.8)


# --------------------------------------------------------------------------- #
#  Фигурки
# --------------------------------------------------------------------------- #
B = BASE_TOP


def tcp_turtle():
    """TCP — черепаха: медленно, но надёжно. Везёт посылку, перевязанную лентой."""
    base("TCP")
    sc, sr = (0, 5, B + 6), (14.5, 16.5, 12.5)
    ellipsoid(sc, sr, name="shell")
    ellipsoid((0, 5, B + 5.5), (15.5, 17.5, 3.4), name="plastron")
    torus((0, 5, B + 6.6), 15.2, 1.8, scale=(1, 17.2 / 15.2, 1), name="shell_rim")
    # щитки панциря — шестигранники, читаются рельефом
    hexes = [(0.0, 90)] + [(math.radians(a), 50) for a in range(0, 360, 60)] + \
            [(math.radians(a + 30), 18) for a in range(0, 360, 60)]
    for th, ph in hexes:
        p, n = ellipsoid_point(sc, sr, th, math.radians(ph))
        size = 4.6 if ph == 90 else 4.0
        bm = bmesh.new()
        bmesh.ops.create_cone(bm, cap_ends=True, segments=6, radius1=size,
                              radius2=size * 0.85, depth=2.2,
                              matrix=Matrix.Translation(p) @ align_z(n))
        FIG.add_bm("scute", bm, smooth=False)
    # шея и голова тянутся вперёд-вверх под 45° — печатается без поддержек
    cone((0, -7, B + 7), (0, -13, B + 13), 4.6, 4.2, name="neck")
    hc = Vector((0, -15.5, B + 16.5))
    ellipsoid(hc, (6.6, 7.2, 6.2), name="head")
    for sx in (-1, 1):
        eye(hc + Vector((sx * 2.9, -5.2, 2.4)), (sx * 0.45, -1, 0.35), 1.9)
    arc(hc + Vector((0, -6.6, 0.3)), 2.6, 200, 340, 0.6, name="smile")
    # лапы
    for sx in (-1, 1):
        for y0, y1 in ((-6, -10), (14, 17)):
            cone((sx * 10, y0, B + 5), (sx * 14.5, y1, B + 1.6), 4.0, 3.6, name="leg")
            ellipsoid((sx * 15, y1 + (-1 if y1 < 0 else 1), B + 1.4), (4.2, 4.8, 1.8), name="foot")
    cone((0, 20, B + 4), (0, 24.5, B + 1.6), 2.4, 1.0, name="tail")
    # посылка с выпуклой лентой и бантом из двух «лепестков» (без отверстий)
    top = sc[2] + sr[2]
    pc = Vector((0, 6, top + 3.2))
    box(pc, (11, 11, 8), rot=(0, 0, 8), name="packet")
    box(pc, (2.4, 12.2, 8.9), rot=(0, 0, 8), name="ribbon")
    box(pc, (12.2, 2.4, 8.9), rot=(0, 0, 8), name="ribbon")
    for sx in (-1, 1):
        ellipsoid(pc + Vector((sx * 2.3, 0, 5.4)), (2.6, 1.6, 1.9), rot=(0, sx * -25, 8),
                  name="bow")


def udp_rabbit():
    """UDP — кролик: быстро, без рукопожатий; один пакет держит, другой уже обронил."""
    base("UDP")
    ellipsoid((0, 5, B + 11), (11, 12.5, 12), name="body")
    hc = Vector((0, -1, B + 26))
    ellipsoid(hc, (9.5, 8.5, 8.5), name="head")
    for sx in (-1, 1):
        ellipsoid(hc + Vector((sx * 3.6, -6, -2.5)), (3.6, 3, 3), name="cheek")
        eye(hc + Vector((sx * 3.6, -6.4, 2.0)), (sx * 0.4, -1, 0.15), 2.1)
        box(hc + Vector((sx * 0.8, -8.0, -4.8)), (1.5, 1.5, 2.6), name="tooth")
        # уши: толщина ≥ 3.5 мм, чуть разведены и откинуты назад
        ellipsoid(hc + Vector((sx * 4.2, 2.0, 13.0)), (3.5, 2.2, 10.5), rot=(-12, sx * 14, 0),
                  name="ear")
    sphere(hc + Vector((0, -8.7, -0.6)), 1.7, name="nose")
    # передние лапки обнимают пакет, стоящий между лап
    box((0, -8.5, B + 3.8), (8.4, 7.5, 8.0), name="packet")
    for sx in (-1, 1):
        cone((sx * 6, -2.5, B + 18), (sx * 4.6, -9.0, B + 8.6), 2.8, 2.5, name="arm")
        ellipsoid((sx * 9, 8, B + 6), (5, 8, 6.5), name="thigh")
        ellipsoid((sx * 8.5, -1, B + 2.0), (3.6, 8.5, 2.6), name="foot")
    sphere((0, 17.5, B + 9), 4.8, name="tail")
    # оброненный пакет — UDP не переспрашивает
    box((15, 13, B + 2.8), (7, 7, 7), rot=(0, 0, 25), name="lost_packet")


def http_dog():
    """HTTP — пёс: на запрос GET приносит ответ — конверт в зубах."""
    base("HTTP")
    ellipsoid((0, 6, B + 7), (10.5, 11, 8), name="haunch")
    ellipsoid((0, 2, B + 15), (9.5, 9, 12), rot=(-12, 0, 0), name="chest")
    for sx in (-1, 1):
        ellipsoid((sx * 8.5, 9, B + 6), (4.5, 8, 6.5), name="thigh")
        ellipsoid((sx * 8, 0, B + 1.8), (3.4, 6.5, 2.4), name="hind_paw")
        cone((sx * 4.6, -3, B + 15), (sx * 4.6, -6.5, B + 2.4), 3.0, 2.8, name="leg")
        ellipsoid((sx * 4.6, -8.2, B + 1.9), (3.3, 4.2, 2.2), name="paw")
    hc = Vector((0, -4, B + 31))
    ellipsoid(hc, (9.5, 9, 8.5), name="head")
    ellipsoid(hc + Vector((0, -7.5, -3)), (5.2, 6.5, 4.2), name="snout")
    sphere(hc + Vector((0, -13.4, -1.6)), 2.0, name="nose")
    for sx in (-1, 1):
        eye(hc + Vector((sx * 3.9, -7.3, 2.2)), (sx * 0.35, -1, 0.2), 2.0)
        ellipsoid(hc + Vector((sx * 8.5, 0.5, -2.5)), (2.8, 4.8, 8.5), rot=(8, sx * -20, 0),
                  name="ear")
    torus((0, -2.5, B + 23), 7.0, 1.5, rot=(-25, 0, 0), scale=(1.05, 1, 1), name="collar")
    # конверт в зубах: клапан и печать — рельефом
    env = Vector((0, -13.0, B + 25.5))
    box(env, (16, 2.2, 9.0), rot=(-10, 0, 0), name="envelope")
    tube([env + Vector((-7.2, -1.6, 3.2)), env + Vector((0, -2.1, -1.2)),
          env + Vector((7.2, -1.6, 3.2))], 0.6, name="flap")
    sphere(env + Vector((0, -2.0, -1.2)), 1.6, name="seal")
    tube([(0, 15, B + 8), (0, 19, B + 11), (0, 20, B + 16), (0, 17.5, B + 19)], 2.3, name="tail")


def dns_owl():
    """DNS — сова: мудрая, знает адреса всех имён. Сидит на телефонной книге, в очках."""
    base("DNS")
    # книга: обложка и чуть утопленный блок страниц
    box((0, 4, B + 2.6), (30, 22, 5.2), name="cover")
    box((1.0, 3.2, B + 2.6), (28.6, 20.8, 3.6), name="pages")
    z0 = B + 5.0
    bc = Vector((0, 5, z0 + 13))
    ellipsoid(bc, (12, 11, 14), name="body")
    ellipsoid(bc + Vector((0, -6, -1.5)), (8.5, 6.5, 10.5), name="belly")
    # «галочки» перьев на груди
    for z, xs in ((-4.0, (-3.3, 3.3)), (-0.2, (-5.0, 0, 5.0)), (3.5, (-3.0, 3.0))):
        for x in xs:
            yb = bc.y - 6 - 6.5 * math.sqrt(max(0.0, 1 - (x / 8.5) ** 2 - (z / 10.5) ** 2)) + 0.3
            tube([(x - 1.6, yb + 0.2, bc.z + z + 1.2), (x, yb, bc.z + z),
                  (x + 1.6, yb + 0.2, bc.z + z + 1.2)], 0.6, name="chevron")
    for sx in (-1, 1):
        ellipsoid(bc + Vector((sx * 10.5, 2.5, 0)), (3.4, 8.5, 11), rot=(-8, sx * 10, 0),
                  name="wing")
        for t in range(3):
            cone((sx * (1.4 + t * 1.6), -1.5, z0 + 0.8), (sx * (0.6 + t * 2.4), -5.0, z0 + 0.6),
                 1.1, 1.0, name="toe")
    hc = Vector((0, 4, z0 + 29))
    ellipsoid(hc, (12.5, 10.5, 9.5), name="head")
    for sx in (-1, 1):
        ellipsoid(hc + Vector((sx * 5.2, -6.6, -0.3)), (5.6, 3.0, 5.8), name="disc")
        cone(hc + Vector((sx * 8.0, 1, 6.0)), hc + Vector((sx * 11.0, 2.5, 12.0)), 3.0, 1.0,
             name="tuft")
        eye(hc + Vector((sx * 5.0, -8.6, 0.3)), (sx * 0.25, -1, 0.05), 3.0)
        torus(hc + Vector((sx * 5.0, -10.2, 0.3)), 4.6, 0.85, rot=(90, 0, 0), name="glasses_rim")
    arc(hc + Vector((0, -10.2, -1.2)), 1.6, 30, 150, 0.8, name="glasses_bridge")
    cone(hc + Vector((0, -8.4, -2.4)), hc + Vector((0, -10.2, -6.2)), 1.9, 0.8, name="beak")


def tls_hedgehog():
    """TLS — ёж: всё под защитой колючек, в лапке — замок."""
    base("TLS")
    bc, br = Vector((0, 6, B + 10)), (13.5, 15, 11.5)
    ellipsoid(bc, br, name="body")
    # иглы по спирали Фибоначчи — реже и толще, чтобы уверенно печатались
    n = 150
    golden = math.pi * (3 - math.sqrt(5))
    for i in range(n):
        z = 1 - (i + 0.5) / n * 2
        rad = math.sqrt(1 - z * z)
        u = Vector((math.cos(golden * i) * rad, math.sin(golden * i) * rad, z))
        if u.z < -0.15 or u.y < -0.35:   # без игл снизу и на мордочке
            continue
        p = bc + Vector((u.x * br[0], u.y * br[1], u.z * br[2]))
        nrm = Vector((u.x / br[0], u.y / br[1], u.z / br[2])).normalized()
        d = (nrm + Vector((0, 0.55, 0.15))).normalized()
        length = 6.5 if u.y > -0.1 else 4.8
        bm = bmesh.new()
        bmesh.ops.create_cone(bm, cap_ends=True, segments=12, radius1=2.5, radius2=0.6,
                              depth=length,
                              matrix=Matrix.Translation(p + d * (length / 2 - 1.2)) @ align_z(d))
        FIG.add_bm("spike", bm)
    # мордочка
    fc = Vector((0, -6, B + 9.5))
    ellipsoid(fc, (8.5, 8, 7.5), name="face")
    cone(fc + Vector((0, -4, -0.5)), fc + Vector((0, -11.5, -1.5)), 4.5, 1.8, name="snout")
    sphere(fc + Vector((0, -12.6, -1.4)), 1.9, name="nose")
    for sx in (-1, 1):
        eye(fc + Vector((sx * 3.6, -6.3, 2.6)), (sx * 0.4, -1, 0.3), 1.8)
        sphere(fc + Vector((sx * 6.0, -1.0, 5.4)), 2.3, name="ear")
        ellipsoid((sx * 6.0, -6.5, B + 1.6), (2.8, 3.8, 2.2), name="front_foot")
        ellipsoid((sx * 9.0, 15.0, B + 1.6), (3.0, 4.0, 2.2), name="back_foot")
    # замок с выпуклой замочной скважиной
    lk = Vector((13.5, -7.0, B + 5.2))
    box(lk, (10, 5.6, 9.5), rot=(0, 0, 25), name="lock_body")
    torus(lk + Vector((0, 0, 4.8)), 3.4, 1.3, rot=(90, 0, 25), name="lock_shackle")
    d = Vector((math.sin(math.radians(25)), -math.cos(math.radians(25)), 0))
    sphere(lk + d * 2.8 + Vector((0, 0, 1.2)), 1.3, name="keyhole")
    cone(lk + d * 2.8 + Vector((0, 0, 1.0)), lk + d * 2.8 + Vector((0, 0, -2.3)), 0.8, 0.8,
         name="keyhole")
    cone(fc + Vector((5.5, 1.0, -3.5)), lk + Vector((-4.0, 1.0, 0.5)), 2.0, 1.8, name="arm")


WRIST = (10.0, 26.0)
WING_TIPS = [(18.5, 0.0), (13.0, 0.0), (7.5, 0.0)]


def icmp_bat():
    """ICMP — летучая мышь: echo request / echo reply.

    Глаза закрыты — мышь «видит» эхом. Огромные уши ловят ответ, перед ней на
    подставке расходятся три волны ping. Крылья раскрыты плащом за спиной и
    опираются на подставку кончиками пальцев, поэтому поддержки почти не нужны.
    """
    base("ICMP")
    wing_outline = [(0, 5.5), (0, 21)]
    wing_outline += [(WRIST[0] * t, 21 + (WRIST[1] - 21) * t + 1.5 * math.sin(math.pi * t))
                     for t in (0.35, 0.7)] + [WRIST]
    wing_outline += [(WRIST[0] + (WING_TIPS[0][0] - WRIST[0]) * t + 1.6 * math.sin(math.pi * t),
                      WRIST[1] + (WING_TIPS[0][1] - WRIST[1]) * t) for t in (0.33, 0.66)]
    ends = WING_TIPS + [(1.0, 5.5)]
    for (u0, v0), (u1, v1) in zip(ends, ends[1:]):
        wing_outline.append((u0, v0))
        sag = 4.5 if u1 > 2 else 2.0
        wing_outline += [(u0 + (u1 - u0) * t, v0 + (v1 - v0) * t + sag * math.sin(math.pi * t))
                         for t in (0.2, 0.4, 0.6, 0.8)]
    bc = Vector((0, 6, B + 11))
    ellipsoid(bc, (9, 8.5, 11.5), name="body")
    hc = Vector((0, 5, B + 25))
    ellipsoid(hc, (9.2, 8.2, 7.8), name="head")

    for sx in (-1, 1):
        # уши-локаторы: большие, сплющенные, с ребром посередине
        ear0, ear1 = hc + Vector((sx * 4.6, 1.0, 4.2)), hc + Vector((sx * 10.0, 2.2, 17.0))
        cone(ear0, ear1, 4.8, 1.1, flat=0.55, name="ear")
        cone(ear0 + Vector((0, -2.2, 1.2)), ear1 + Vector((-sx * 0.6, -0.9, -2.0)), 1.1, 0.6,
             name="ear_ridge")
        # закрытые глаза-дуги ∪ — мышь ориентируется по эху
        arc(hc + Vector((sx * 3.6, -7.6, 1.6)), 1.8, 200, 340, 0.6, name="closed_eye")
        # клыки: сплющенные, торчат вниз из-под мордочки
        cone(hc + Vector((sx * 2.0, -8.2, -2.9)), hc + Vector((sx * 2.15, -8.7, -5.3)), 1.15, 0.4,
             flat=0.65, name="fang")
        # ножки
        ellipsoid((sx * 4.4, -1.5, B + 1.4), (2.8, 4.0, 2.0), name="foot")

        # крыло раскрыто плащом за спиной: кончики пальцев стоят на подставке,
        # между ними — арки-фестоны (печатаются мостами, без поддержек)
        wm = Matrix.Translation((sx * 3.0, bc.y + 2.5, B - 0.5)) @ trs(rot=(90, 0, sx * 10))
        w = [(sx * u, v) for u, v in wing_outline]
        prism(w if sx > 0 else w[::-1], 2.4, wm, name="wing")
        # кости-рёбра выступают на лицевой стороне крыла (локальная +z смотрит вперёд)
        wrist = wm @ Vector((sx * WRIST[0], WRIST[1], 0.6))
        cone(wrist, wrist + Vector((sx * 0.8, -0.6, 4.2)), 1.8, 0.7, name="thumb")
        for tu, tv in WING_TIPS:
            cone(wrist, wm @ Vector((sx * tu, tv + 1.2, 0.9)), 1.3, 0.9, name="finger")
        cone(wm @ Vector((sx * 1.0, 18.0, 0.6)), wrist, 1.6, 1.3, name="forearm")

    ellipsoid(hc + Vector((0, -7.0, -1.2)), (3.6, 2.6, 2.6), name="muzzle")
    sphere(hc + Vector((0, -9.3, -0.5)), 1.2, name="nose")

    # волны ping: стоячие дуги, к краю подставки затухают
    for r, h, span in ((12.5, 3.4, 22), (15.5, 2.5, 25), (18.5, 1.6, 28)):
        arc((0, 6.0, B), r, 270 - span, 270 + span, 0.8, plane="xy", height=h / 0.8,
            name="ping_wave")


FIGURES = [
    ("TCP", "tcp_turtle", "TCP — Черепаха", tcp_turtle),
    ("UDP", "udp_rabbit", "UDP — Кролик", udp_rabbit),
    ("HTTP", "http_dog", "HTTP — Пёс", http_dog),
    ("DNS", "dns_owl", "DNS — Сова", dns_owl),
    ("TLS", "tls_hedgehog", "TLS — Ёж", tls_hedgehog),
    ("ICMP", "icmp_bat", "ICMP — Летучая мышь", icmp_bat),
]

# цвета пластика только для превью (на модель не влияют)
FILAMENT = {
    "TCP": (0.30, 0.62, 0.32),
    "UDP": (0.90, 0.90, 0.88),
    "HTTP": (0.92, 0.55, 0.18),
    "DNS": (0.55, 0.38, 0.24),
    "TLS": (0.45, 0.47, 0.52),
    "ICMP": (0.36, 0.26, 0.52),
}


# --------------------------------------------------------------------------- #
#  Печатная сетка: объединение → воксельный ремеш → прореживание
# --------------------------------------------------------------------------- #

def evaluated_copy(obj):
    deps = bpy.context.evaluated_depsgraph_get()
    mesh = bpy.data.meshes.new_from_object(obj.evaluated_get(deps))
    old = obj.data
    obj.modifiers.clear()
    obj.data = mesh
    bpy.data.meshes.remove(old)
    return mesh


def build_print_mesh(fig):
    bm = bmesh.new()
    for obj in fig.parts:
        m = obj.data.copy()
        m.transform(obj.matrix_world)
        bm.from_mesh(m)
        bpy.data.meshes.remove(m)
    joined = bpy.data.meshes.new(fig.key + "_print")
    bm.to_mesh(joined)
    bm.free()

    obj = bpy.data.objects.new(f"{fig.key}_print", joined)
    bpy.context.scene.collection.objects.link(obj)

    rem = obj.modifiers.new("remesh", "REMESH")
    rem.mode = "VOXEL"
    rem.voxel_size = VOXEL
    rem.adaptivity = 0.0
    mesh = evaluated_copy(obj)

    tris = sum(len(p.vertices) - 2 for p in mesh.polygons)
    if tris > TARGET_TRIS:
        dec = obj.modifiers.new("decimate", "DECIMATE")
        dec.decimate_type = "COLLAPSE"
        dec.ratio = TARGET_TRIS / tris
        dec.use_collapse_triangulate = True
        mesh = evaluated_copy(obj)

    drop_debris(mesh)
    flatten_bottom(mesh)
    mesh.name = f"{fig.key}_print"
    mesh.materials.clear()
    mesh.materials.append(plastic(f"filament_{fig.key}", FILAMENT[fig.key]))
    mesh.shade_smooth()
    fig.coll.hide_render = True
    fig.coll.hide_viewport = True
    return obj


def drop_debris(mesh, min_volume=1.0):
    """Удаляет мелкие вырожденные осколки (< 1 мм³), которые иногда оставляет прореживание."""
    bm = bmesh.new()
    bm.from_mesh(mesh)
    seen, junk = set(), []
    for f in bm.faces:
        if f.index in seen:
            continue
        comp, stack = [], [f]
        seen.add(f.index)
        while stack:
            cur = stack.pop()
            comp.append(cur)
            for e in cur.edges:
                for g in e.link_faces:
                    if g.index not in seen:
                        seen.add(g.index)
                        stack.append(g)
        vol = sum(g.calc_area() * g.normal.dot(g.calc_center_median()) for g in comp) / 3
        if abs(vol) < min_volume:
            junk.extend(comp)
    if junk:
        bmesh.ops.delete(bm, geom=junk, context="FACES")
        loose = [v for v in bm.verts if not v.link_faces]
        bmesh.ops.delete(bm, geom=loose, context="VERTS")
        bm.to_mesh(mesh)
    bm.free()


def flatten_bottom(mesh, tol=0.35):
    """Ремеш чуть скругляет нижнюю грань — выравниваем её точно в z = 0."""
    zmin = min(v.co.z for v in mesh.vertices)
    for v in mesh.vertices:
        v.co.z = 0.0 if v.co.z < zmin + tol else v.co.z - zmin
    mesh.update()


def check_mesh(mesh):
    bm = bmesh.new()
    bm.from_mesh(mesh)
    non_manifold = sum(1 for e in bm.edges if not e.is_manifold)
    boundary = sum(1 for e in bm.edges if e.is_boundary)
    volume = bm.calc_volume(signed=True)
    seen, shells = set(), 0
    for v in bm.verts:
        if v.index in seen:
            continue
        shells += 1
        stack = [v]
        seen.add(v.index)
        while stack:
            cur = stack.pop()
            for e in cur.link_edges:
                o = e.other_vert(cur)
                if o.index not in seen:
                    seen.add(o.index)
                    stack.append(o)
    bm.free()
    return non_manifold, boundary, volume, shells


def export_stl(obj, path):
    bpy.ops.object.select_all(action="DESELECT")
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.wm.stl_export(filepath=path, export_selected_objects=True, ascii_format=False,
                          apply_modifiers=True, global_scale=1.0, use_scene_unit=False)


# --------------------------------------------------------------------------- #
#  Превью
# --------------------------------------------------------------------------- #

def setup_render(samples=48):
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = samples
    scene.cycles.use_denoising = True
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.exposure = -0.7
    world = bpy.data.worlds.new("world")
    world.use_nodes = True
    bg = world.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (0.80, 0.84, 0.90, 1)
    bg.inputs["Strength"].default_value = 0.65
    scene.world = world
    for name, energy, rot in (("sun", 2.6, (50, 0, -35)), ("fill", 0.8, (65, 0, 140))):
        light = bpy.data.lights.new(name, "SUN")
        light.energy = energy
        light.angle = math.radians(12)
        obj = bpy.data.objects.new(name, light)
        obj.rotation_euler = Euler([math.radians(a) for a in rot])
        scene.collection.objects.link(obj)
    floor_mesh = bpy.data.meshes.new("floor")
    bm = bmesh.new()
    bmesh.ops.create_grid(bm, x_segments=1, y_segments=1, size=2000)
    bm.to_mesh(floor_mesh)
    bm.free()
    floor_mesh.materials.append(plastic("floor", (0.85, 0.87, 0.9)))
    floor = bpy.data.objects.new("floor", floor_mesh)
    floor.location.z = -0.01
    scene.collection.objects.link(floor)
    cam_data = bpy.data.cameras.new("cam")
    cam_data.clip_start = 1
    cam_data.clip_end = 10000
    cam = bpy.data.objects.new("cam", cam_data)
    scene.collection.objects.link(cam)
    scene.camera = cam
    return cam


def aim(cam, target, distance, yaw, pitch, lens=85):
    t = Vector(target)
    d = Vector((math.sin(math.radians(yaw)) * math.cos(math.radians(pitch)),
                -math.cos(math.radians(yaw)) * math.cos(math.radians(pitch)),
                math.sin(math.radians(pitch))))
    cam.location = t + d * distance
    cam.rotation_euler = (t - cam.location).to_track_quat("-Z", "Y").to_euler()
    cam.data.lens = lens


def render(path, res):
    scene = bpy.context.scene
    scene.render.resolution_x, scene.render.resolution_y = res
    scene.render.filepath = path
    bpy.ops.render.render(write_still=True)


# --------------------------------------------------------------------------- #

def main():
    global FIG
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
    do_render = "--no-render" not in argv
    only = None
    if "--only" in argv:
        only = set(argv[argv.index("--only") + 1].upper().split(","))

    os.makedirs(STL_DIR, exist_ok=True)
    os.makedirs(RENDER_DIR, exist_ok=True)
    reset_scene()
    cam = setup_render() if do_render else None

    built = []
    for key, slug, title, fn in FIGURES:
        if only and key not in only:
            continue
        FIG = Figure(key, title)
        fn()
        pobj = build_print_mesh(FIG)
        nm, bd, vol, shells = check_mesh(pobj.data)
        dims = pobj.dimensions
        print(f"{title:24s} {dims.x:5.1f}×{dims.y:5.1f}×{dims.z:5.1f} мм  "
              f"{len(pobj.data.polygons):7d} тр.  объём {vol / 1000:5.1f} см³  "
              f"оболочек {shells}  неманифолд {nm}  края {bd}")
        export_stl(pobj, os.path.join(STL_DIR, f"{slug}.stl"))
        built.append((slug, FIG.coll, pobj))

    if do_render:
        for slug, coll, pobj in built:
            for _, _, other in built:
                other.hide_render = other is not pobj
            aim(cam, (0, -2, pobj.dimensions.z * 0.45), 175, -28, 18)
            render(os.path.join(RENDER_DIR, f"{slug}.png"), (900, 900))
        for _, _, pobj in built:
            pobj.hide_render = False

    # раскладываем фигурки в ряд для общего вида и .blend
    n = len(built)
    for i, (slug, coll, pobj) in enumerate(built):
        x = (i - (n - 1) / 2) * 64
        pobj.location.x = x
        for o in coll.objects:
            o.location.x += x
    if do_render and n > 1:
        aim(cam, (0, 0, 24), 720, 0, 14, lens=60)
        render(os.path.join(RENDER_DIR, "lineup.png"), (1800, 760))

    if not only:
        bpy.context.preferences.filepaths.save_version = 0  # без резервной копии .blend1
        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(HERE, "protocol_animals.blend"),
                                    compress=True)


if __name__ == "__main__":
    main()
