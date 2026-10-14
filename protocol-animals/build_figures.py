"""Сетевые протоколы в виде животных — фигурки для 3D-печати.

Скрипт строит в Blender шесть фигурок на подставках с подписью, делает из каждой
одну замкнутую (watertight) сетку для печати и сохраняет:

  stl/<protocol>_<animal>.stl   — модели для слайсера (1 единица = 1 мм)
  protocol_animals.blend        — сцена: цветные детали + сетки для печати
  renders/*.png                 — превью

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

VOXEL = 0.22         # мм, шаг воксельного ремеша (детализация печатной сетки)
TARGET_TRIS = 220_000  # примерный бюджет треугольников на фигурку
BASE_TOP = 8.0       # высота верхней площадки подставки, мм

FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/opentype/inter/Inter-ExtraBold.otf",
    "C:/Windows/Fonts/arialbd.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/Library/Fonts/Arial Bold.ttf",
]

PALETTE = {
    "base": (0.13, 0.15, 0.19),
    "label": (0.95, 0.80, 0.25),
    "white": (0.95, 0.95, 0.93),
    "black": (0.02, 0.02, 0.025),
    "pink": (0.95, 0.45, 0.55),
    "turtle": (0.35, 0.65, 0.30),
    "shell": (0.45, 0.30, 0.15),
    "scute": (0.62, 0.45, 0.22),
    "packet": (0.85, 0.65, 0.40),
    "ribbon": (0.80, 0.15, 0.15),
    "rabbit": (0.88, 0.88, 0.90),
    "packet_udp": (0.30, 0.65, 0.95),
    "dog": (0.85, 0.60, 0.30),
    "dog_light": (0.97, 0.88, 0.72),
    "collar": (0.15, 0.40, 0.85),
    "gold": (0.95, 0.72, 0.15),
    "envelope": (0.97, 0.97, 0.92),
    "owl": (0.50, 0.35, 0.22),
    "owl_light": (0.85, 0.75, 0.55),
    "beak": (0.95, 0.65, 0.10),
    "book": (0.55, 0.12, 0.15),
    "pages": (0.95, 0.93, 0.85),
    "glasses": (0.10, 0.10, 0.12),
    "hedgehog": (0.40, 0.28, 0.20),
    "spike": (0.25, 0.17, 0.12),
    "face": (0.90, 0.78, 0.62),
    "bat": (0.35, 0.30, 0.42),
    "wing": (0.22, 0.18, 0.28),
    "paddle": (0.85, 0.12, 0.15),
    "wood": (0.80, 0.60, 0.35),
    "ball": (1.00, 0.55, 0.10),
    "wave": (0.35, 0.85, 0.75),
}


# --------------------------------------------------------------------------- #
#  Сцена и материалы
# --------------------------------------------------------------------------- #

def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = bpy.context.scene
    scene.unit_settings.system = "METRIC"
    scene.unit_settings.scale_length = 0.001
    scene.unit_settings.length_unit = "MILLIMETERS"
    for name, rgb in PALETTE.items():
        mat = bpy.data.materials.new(name)
        mat.use_nodes = True
        bsdf = mat.node_tree.nodes.get("Principled BSDF")
        bsdf.inputs["Base Color"].default_value = (*rgb, 1.0)
        bsdf.inputs["Roughness"].default_value = 0.45
        mat.diffuse_color = (*rgb, 1.0)
    clay = bpy.data.materials.new("clay")
    clay.use_nodes = True
    bsdf = clay.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (0.50, 0.52, 0.56, 1.0)
    bsdf.inputs["Roughness"].default_value = 0.6


class Figure:
    """Собирает детали одной фигурки в отдельную коллекцию."""

    def __init__(self, key, title):
        self.key = key
        self.title = title
        self.coll = bpy.data.collections.new(f"{key} — цвет")
        bpy.context.scene.collection.children.link(self.coll)
        self.parts = []

    def add_bm(self, name, bm, mat, smooth=True):
        mesh = bpy.data.meshes.new(name)
        bm.to_mesh(mesh)
        bm.free()
        if smooth:
            mesh.shade_smooth()
        mesh.materials.append(bpy.data.materials[mat])
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


def ellipsoid(loc, radii, mat, rot=(0, 0, 0), name="ellipsoid", seg=48):
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=seg, v_segments=seg // 2, radius=1.0,
                              matrix=trs(loc, rot, radii))
    return FIG.add_bm(name, bm, mat)


def sphere(loc, r, mat, name="sphere", seg=32):
    return ellipsoid(loc, (r, r, r), mat, name=name, seg=seg)


def box(loc, size, mat, rot=(0, 0, 0), name="box", smooth=False):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0, matrix=trs(loc, rot, size))
    return FIG.add_bm(name, bm, mat, smooth=smooth)


def cylinder(loc, r, depth, mat, rot=(0, 0, 0), name="cylinder", seg=48, r2=None,
             smooth=True):
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, segments=seg, radius1=r,
                          radius2=r if r2 is None else r2, depth=depth,
                          matrix=trs(loc, rot))
    obj = FIG.add_bm(name, bm, mat, smooth=smooth)
    return obj


def align_z(direction):
    return Vector((0, 0, 1)).rotation_difference(Vector(direction).normalized()).to_matrix().to_4x4()


def limb(p1, p2, r1, r2, mat, name="limb", caps=True, seg=32):
    """Конус между двумя точками со сферами на концах — лапы, шеи, трубки."""
    p1, p2 = Vector(p1), Vector(p2)
    d = p2 - p1
    bm = bmesh.new()
    m = Matrix.Translation((p1 + p2) / 2) @ align_z(d)
    bmesh.ops.create_cone(bm, cap_ends=True, segments=seg, radius1=r1, radius2=r2,
                          depth=d.length, matrix=m)
    FIG.add_bm(name, bm, mat)
    if caps:
        sphere(p1, r1, mat, name=name + "_cap", seg=seg)
        sphere(p2, r2, mat, name=name + "_cap", seg=seg)


def flat_cone(p1, p2, r1, r2, flat, mat, name="flat_cone", seg=32):
    """Сплюснутый конус (уши): flat — коэффициент толщины поперёк."""
    p1, p2 = Vector(p1), Vector(p2)
    d = p2 - p1
    bm = bmesh.new()
    m = Matrix.Translation((p1 + p2) / 2) @ align_z(d) @ Matrix.Diagonal((1, flat, 1, 1))
    bmesh.ops.create_cone(bm, cap_ends=True, segments=seg, radius1=r1, radius2=r2,
                          depth=d.length, matrix=m)
    FIG.add_bm(name, bm, mat)
    ellipsoid(p2, (r2, r2 * flat, r2), mat, name=name + "_tip", seg=16)


def tube(points, r, mat, name="tube"):
    """Трубка по ломаной (бусины + цилиндры)."""
    for a, b in zip(points, points[1:]):
        limb(a, b, r, r, mat, name=name, caps=False, seg=16)
    for p in points:
        sphere(p, r, mat, name=name + "_joint", seg=16)


def arc(center, radius, a0, a1, r, mat, plane="xz", name="arc"):
    """Дуга окружности (часть тора с круглыми концами). Углы в градусах."""
    rot = {"xy": (0, 0, 0), "xz": (90, 0, 0), "yz": (90, 0, 90)}[plane]
    torus(center, radius, r, mat, rot=rot, name=name, arc=(a0, a1))
    m = trs(center, rot)
    for a in (a0, a1):
        a = math.radians(a)
        sphere(m @ Vector((radius * math.cos(a), radius * math.sin(a), 0)), r, mat,
               name=name + "_end", seg=16)


def torus(loc, R, r, mat, rot=(0, 0, 0), scale=(1, 1, 1), name="torus", seg=48, rseg=16,
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
            x = (R + r * math.cos(b)) * math.cos(a)
            y = (R + r * math.cos(b)) * math.sin(a)
            z = r * math.sin(b)
            ring.append(bm.verts.new((x, y, z)))
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
    return FIG.add_bm(name, bm, mat)


def prism(outline, thickness, mat, matrix, name="prism"):
    """Плоская фигура по контуру (u, v), выдавленная на thickness; matrix ставит её в мир."""
    bm = bmesh.new()
    verts = [bm.verts.new((u, v, -thickness / 2)) for u, v in outline]
    face = bm.faces.new(verts)
    ext = bmesh.ops.extrude_face_region(bm, geom=[face])
    top = [e for e in ext["geom"] if isinstance(e, bmesh.types.BMVert)]
    bmesh.ops.translate(bm, vec=(0, 0, thickness), verts=top)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bmesh.ops.triangulate(bm, faces=bm.faces)
    bmesh.ops.transform(bm, matrix=matrix, verts=bm.verts)
    return FIG.add_bm(name, bm, mat, smooth=False)


def hull(points, mat, name="hull"):
    bm = bmesh.new()
    for p in points:
        bm.verts.new(p)
    bmesh.ops.convex_hull(bm, input=bm.verts)
    # убрать «висячие» вершины, не попавшие в оболочку
    loose = [v for v in bm.verts if not v.link_faces]
    bmesh.ops.delete(bm, geom=loose, context="VERTS")
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    return FIG.add_bm(name, bm, mat, smooth=False)


def eye(center, direction, r_white, r_pupil, look=(0, 0, 0)):
    """Глаз: белок + выпуклый зрачок + блик (блик нужен только для цветного превью)."""
    c, d = Vector(center), (Vector(direction).normalized() + Vector(look)).normalized()
    sphere(c, r_white, "white", name="eye_white")
    p = c + d * (r_white * 0.72)
    sphere(p, r_pupil, "black", name="pupil")
    side = d.cross(Vector((0, 0, 1))).normalized()
    sphere(p + d * (r_pupil * 0.75) + Vector((0, 0, r_pupil * 0.35)) - side * r_pupil * 0.3,
           r_pupil * 0.32, "white", name="eye_glint")


def ellipsoid_point(c, radii, theta, phi):
    """Точка и нормаль на эллипсоиде; theta — азимут от -Y (перед) по часовой, phi — высота."""
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


def label_text(body, loc, rot, size, depth, mat="label"):
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
    mesh.materials.append(bpy.data.materials[mat])
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
            if y < chord:
                continue
            pts.append((x, y, z))
        hw = math.sqrt(r * r - chord * chord)
        pts += [(-hw, chord, z), (hw, chord, z)]
    hull(pts, "base", name="base")

    # скос: от (y=-21, z=1.2) до (y=-14, z=7) — нормаль смотрит вперёд-вверх
    p0, p1 = Vector((0, -21.0, 1.2)), Vector((0, -14.0, BASE_TOP - 1.0))
    slope = (p1 - p0)
    tilt = math.degrees(math.atan2(slope.z, slope.y))  # угол скоса к горизонту
    normal = Vector((0, -slope.z, slope.y)).normalized()
    mid = (p0 + p1) / 2 + normal * 0.25
    label_text(label, mid, (90 - tilt, 0, 0), size=8.5, depth=1.6)


# --------------------------------------------------------------------------- #
#  Фигурки
# --------------------------------------------------------------------------- #
B = BASE_TOP


def tcp_turtle():
    """TCP — черепаха: медленно, но надёжно. Несёт посылку, перевязанную лентой."""
    base("TCP")
    sc, sr = (0, 5, B + 6), (14.5, 16.5, 12.5)
    ellipsoid(sc, sr, "shell", name="shell")
    ellipsoid((0, 5, B + 5.5), (15.5, 17.5, 3.4), "turtle", name="plastron")
    torus((0, 5, B + 6.6), 15.2, 1.7, "scute", scale=(1, 17.2 / 15.2, 1), name="shell_rim")
    # щитки панциря
    hexes = [(0, 90)] + [(math.radians(a), 50) for a in range(0, 360, 60)] + \
            [(math.radians(a + 30), 18) for a in range(0, 360, 60)]
    for th, ph in hexes:
        th = th if isinstance(th, float) else 0.0
        p, n = ellipsoid_point(sc, sr, th, math.radians(ph))
        size = 4.6 if ph == 90 else 4.0
        m = Matrix.Translation(p) @ align_z(n)
        bm = bmesh.new()
        bmesh.ops.create_cone(bm, cap_ends=True, segments=6, radius1=size,
                              radius2=size * 0.85, depth=2.0, matrix=m)
        FIG.add_bm("scute", bm, "scute", smooth=False)
    # шея и голова (тянется вперёд-вверх под 45°, удобно печатать)
    limb((0, -7, B + 7), (0, -13, B + 13), 4.6, 4.2, "turtle", name="neck")
    hc = Vector((0, -15.5, B + 16.5))
    ellipsoid(hc, (6.6, 7.2, 6.2), "turtle", name="head")
    eye(hc + Vector((-3.0, -5.0, 2.4)), (-0.45, -1, 0.35), 2.3, 1.25)
    eye(hc + Vector((3.0, -5.0, 2.4)), (0.45, -1, 0.35), 2.3, 1.25)
    arc(hc + Vector((0, -6.6, 0.5)), 2.6, 200, 340, 0.55, "black", plane="xz", name="smile")
    sphere(hc + Vector((-4.2, -4.6, -0.6)), 1.3, "pink", name="cheek")
    sphere(hc + Vector((4.2, -4.6, -0.6)), 1.3, "pink", name="cheek")
    # лапы
    for sx in (-1, 1):
        for y0, y1 in ((-6, -10), (14, 17)):
            limb((sx * 10, y0, B + 5), (sx * 14.5, y1, B + 1.6), 4.0, 3.6, "turtle", name="leg")
            ellipsoid((sx * 15, y1 + (-1 if y1 < 0 else 1), B + 1.4), (4.2, 4.8, 1.8), "turtle",
                      name="foot")
    limb((0, 20, B + 4), (0, 24.5, B + 1.6), 2.4, 1.0, "turtle", name="tail")
    # посылка-пакет на панцире
    top = sc[2] + sr[2]
    box((0, 6, top + 3.2), (11, 11, 8), "packet", rot=(0, 0, 8), name="packet")
    box((0, 6, top + 3.2), (2.2, 11.6, 8.6), "ribbon", rot=(0, 0, 8), name="ribbon")
    box((0, 6, top + 3.2), (11.6, 2.2, 8.6), "ribbon", rot=(0, 0, 8), name="ribbon")
    torus((-2.2, 6.3, top + 8.4), 2.2, 0.9, "ribbon", rot=(90, 0, 98), name="bow")
    torus((2.2, 5.7, top + 8.4), 2.2, 0.9, "ribbon", rot=(90, 0, 98), name="bow")


def udp_rabbit():
    """UDP — кролик: быстро, без рукопожатий; один пакет держит, другой уже обронил."""
    base("UDP")
    bc = (0, 5, B + 11)
    ellipsoid(bc, (11, 12.5, 12), "rabbit", name="body")
    ellipsoid((0, 0, B + 9), (8, 6, 8), "white", name="belly")
    hc = Vector((0, -1, B + 26))
    ellipsoid(hc, (9.5, 8.5, 8.5), "rabbit", name="head")
    ellipsoid(hc + Vector((-4, -6, -2.5)), (3.6, 3, 3), "white", name="cheek")
    ellipsoid(hc + Vector((4, -6, -2.5)), (3.6, 3, 3), "white", name="cheek")
    sphere(hc + Vector((0, -8.6, -0.6)), 1.6, "pink", name="nose")
    for sx in (-1, 1):
        box(hc + Vector((sx * 0.75, -8.0, -4.6)), (1.4, 1.4, 2.4), "white", name="tooth")
    eye(hc + Vector((-3.6, -6.3, 2.0)), (-0.4, -1, 0.15), 2.4, 1.45)
    eye(hc + Vector((3.6, -6.3, 2.0)), (0.4, -1, 0.15), 2.4, 1.45)
    # уши: толщина ≥ 3 мм, чуть разведены и откинуты назад
    for sx in (-1, 1):
        ellipsoid(hc + Vector((sx * 4.2, 2.0, 13.0)), (3.4, 2.1, 10.5), "rabbit",
                  rot=(-12, sx * 14, 0), name="ear")
        ellipsoid(hc + Vector((sx * 4.4, 0.7, 13.2)), (2.0, 1.2, 8.0), "pink",
                  rot=(-12, sx * 14, 0), name="ear_inner")
    # передние лапки обнимают синий пакет, стоящий между лап
    pk = Vector((0, -8.5, B + 3.8))
    box(pk, (8.4, 7.5, 8.0), "packet_udp", name="packet")
    box(pk + Vector((0, -3.6, 0)), (8.6, 0.8, 1.6), "white", name="packet_stripe")
    for sx in (-1, 1):
        limb((sx * 6, -2.5, B + 18), (sx * 4.6, -9.0, B + 8.6), 2.8, 2.5, "rabbit", name="arm")
    # задние лапы
    for sx in (-1, 1):
        ellipsoid((sx * 9, 8, B + 6), (5, 8, 6.5), "rabbit", name="thigh")
        ellipsoid((sx * 8.5, -1, B + 2.0), (3.6, 8.5, 2.6), "rabbit", name="foot")
    sphere((0, 17.5, B + 9), 4.8, "white", name="tail")
    # оброненный пакет — UDP не переспрашивает
    box((15, 13, B + 2.8), (7, 7, 7), "packet_udp", rot=(0, 0, 25), name="lost_packet")
    box((15.4, 12.6, B + 2.8), (7.2, 1.0, 7.2), "black", rot=(0, 0, 25), name="crack")


def http_dog():
    """HTTP — пёс: по запросу GET приносит ответ — конверт «200 OK» в зубах."""
    base("HTTP")
    ellipsoid((0, 6, B + 7), (10.5, 11, 8), "dog", name="haunch")
    ellipsoid((0, 2, B + 15), (9.5, 9, 12), "dog", rot=(-12, 0, 0), name="chest")
    ellipsoid((0, -4, B + 15), (6, 5, 9), "dog_light", rot=(-12, 0, 0), name="bib")
    for sx in (-1, 1):
        ellipsoid((sx * 8.5, 9, B + 6), (4.5, 8, 6.5), "dog", name="thigh")
        ellipsoid((sx * 8, 0, B + 1.8), (3.4, 6.5, 2.4), "dog", name="hind_paw")
        limb((sx * 4.6, -3, B + 15), (sx * 4.6, -6.5, B + 2.4), 3.0, 2.8, "dog", name="leg")
        ellipsoid((sx * 4.6, -8.2, B + 1.9), (3.3, 4.2, 2.2), "dog_light", name="paw")
    hc = Vector((0, -4, B + 31))
    ellipsoid(hc, (9.5, 9, 8.5), "dog", name="head")
    ellipsoid(hc + Vector((0, -7.5, -3)), (5.2, 6.5, 4.2), "dog_light", name="snout")
    sphere(hc + Vector((0, -13.4, -1.6)), 1.9, "black", name="nose")
    eye(hc + Vector((-3.9, -7.0, 2.2)), (-0.35, -1, 0.2), 2.2, 1.4)
    eye(hc + Vector((3.9, -7.0, 2.2)), (0.35, -1, 0.2), 2.2, 1.4)
    for sx in (-1, 1):
        ellipsoid(hc + Vector((sx * 8.5, 0.5, -2.5)), (2.6, 4.8, 8.5), "dog",
                  rot=(8, sx * -20, 0), name="ear")
        ellipsoid(hc + Vector((sx * 3.8, -4.3, 5.4)), (1.8, 1.0, 0.8), "dog",
                  rot=(0, sx * -15, 0), name="brow")
    torus((0, -2.5, B + 23), 7.0, 1.4, "collar", rot=(-25, 0, 0), scale=(1.05, 1, 1),
          name="collar")
    # конверт в зубах
    env = Vector((0, -13.0, B + 25.5))
    box(env, (16, 2.2, 9.0), "envelope", rot=(-10, 0, 0), name="envelope")
    tube([env + Vector((-7.2, -1.6, 3.2)), env + Vector((0, -2.1, -1.2)),
          env + Vector((7.2, -1.6, 3.2))], 0.55, "ribbon", name="flap")
    sphere(env + Vector((0, -2.0, -1.2)), 1.5, "ribbon", name="seal")
    # хвост бубликом
    tube([(0, 15, B + 8), (0, 19, B + 11), (0, 20, B + 16), (0, 17.5, B + 19)], 2.2, "dog",
         name="tail")


def dns_owl():
    """DNS — сова: мудрая, знает адреса всех имён. Сидит на телефонной книге."""
    base("DNS")
    # стопка-книга
    box((0, 4, B + 2.6), (30, 22, 5.2), "book", name="cover")
    box((1.0, 3.2, B + 2.6), (28.6, 20.8, 4.0), "pages", name="pages")
    for k in range(4):
        box((15.2, 3.2, B + 1.2 + k * 0.9), (0.6, 20.0, 0.45), "pages", name="page_line")
    box((-15.0, 4, B + 2.6), (1.6, 22, 5.2), "book", name="spine")
    z0 = B + 5.0
    bc = Vector((0, 5, z0 + 13))
    ellipsoid(bc, (12, 11, 14), "owl", name="body")
    ellipsoid(bc + Vector((0, -6, -1.5)), (8.5, 6.5, 10.5), "owl_light", name="belly")
    # «галочки» на груди
    for row, (z, xs) in enumerate(((-4.0, (-3.3, 3.3)), (-0.2, (-5.0, 0, 5.0)), (3.5, (-3.0, 3.0)))):
        for x in xs:
            yb = bc.y - 6 - 6.5 * math.sqrt(max(0.0, 1 - (x / 8.5) ** 2 - (z / 10.5) ** 2)) + 0.3
            tube([(x - 1.6, yb + 0.2, bc.z + z + 1.2), (x, yb, bc.z + z),
                  (x + 1.6, yb + 0.2, bc.z + z + 1.2)], 0.5, "owl", name="chevron")
    for sx in (-1, 1):
        ellipsoid(bc + Vector((sx * 10.5, 2.5, 0)), (3.4, 8.5, 11), "owl", rot=(-8, sx * 10, 0),
                  name="wing")
        for t in range(3):
            limb((sx * 3 + sx * t * 1.6 - 1.6 * sx, -1.5, z0 + 0.8),
                 (sx * 3 + sx * t * 2.4 - 2.4 * sx, -5.0, z0 + 0.6), 1.0, 0.9, "beak", name="toe")
    hc = Vector((0, 4, z0 + 29))
    ellipsoid(hc, (12.5, 10.5, 9.5), "owl", name="head")
    for sx in (-1, 1):
        ellipsoid(hc + Vector((sx * 5.2, -6.6, -0.3)), (5.6, 3.0, 5.8), "owl_light", name="disc")
        limb(hc + Vector((sx * 8.0, 1, 6.0)), hc + Vector((sx * 11.0, 2.5, 12.0)), 3.0, 0.9,
             "owl", name="tuft")
        eye(hc + Vector((sx * 5.0, -7.6, 0.3)), (sx * 0.25, -1, 0.05), 3.6, 2.2)
        # очки
        torus(hc + Vector((sx * 5.0, -10.2, 0.3)), 4.6, 0.75, "glasses", rot=(90, 0, 0),
              name="glasses_rim")
        limb(hc + Vector((sx * 9.4, -10.0, 0.6)), hc + Vector((sx * 11.6, -5.5, 1.2)), 0.75, 0.75,
             "glasses", name="glasses_arm")
    arc(hc + Vector((0, -10.2, -1.2)), 1.6, 30, 150, 0.7, "glasses", plane="xz",
        name="glasses_bridge")
    limb(hc + Vector((0, -8.4, -2.4)), hc + Vector((0, -10.2, -6.2)), 1.9, 0.7, "beak", name="beak")


def tls_hedgehog():
    """TLS — ёж: шифрует всё колючками и охраняет замочек."""
    base("TLS")
    bc, br = Vector((0, 6, B + 10)), (13.5, 15, 11.5)
    ellipsoid(bc, br, "hedgehog", name="body")
    # иглы по Фибоначчи на спине
    n = 260
    golden = math.pi * (3 - math.sqrt(5))
    for i in range(n):
        z = 1 - (i + 0.5) / n * 2
        rad = math.sqrt(1 - z * z)
        th = golden * i
        u = Vector((math.cos(th) * rad, math.sin(th) * rad, z))
        if u.z < -0.15 or u.y < -0.35:   # без игл снизу и на мордочке
            continue
        p = bc + Vector((u.x * br[0], u.y * br[1], u.z * br[2]))
        nrm = Vector((u.x / br[0], u.y / br[1], u.z / br[2])).normalized()
        d = (nrm + Vector((0, 0.55, 0.15))).normalized()
        length = 6.5 if u.y > -0.1 else 4.5
        bm = bmesh.new()
        m = Matrix.Translation(p + d * (length / 2 - 1.2)) @ align_z(d)
        bmesh.ops.create_cone(bm, cap_ends=True, segments=10, radius1=1.9, radius2=0.45,
                              depth=length, matrix=m)
        FIG.add_bm("spike", bm, "spike")
    # мордочка
    fc = Vector((0, -6, B + 9.5))
    ellipsoid(fc, (8.5, 8, 7.5), "face", name="face")
    limb(fc + Vector((0, -4, -0.5)), fc + Vector((0, -11.5, -1.5)), 4.5, 1.8, "face", name="snout")
    sphere(fc + Vector((0, -12.6, -1.4)), 1.8, "black", name="nose")
    eye(fc + Vector((-3.6, -6.0, 2.6)), (-0.4, -1, 0.3), 1.9, 1.3)
    eye(fc + Vector((3.6, -6.0, 2.6)), (0.4, -1, 0.3), 1.9, 1.3)
    for sx in (-1, 1):
        sphere(fc + Vector((sx * 6.0, -1.0, 5.4)), 2.2, "face", name="ear")
        sphere(fc + Vector((sx * 5.4, -5.6, -1.0)), 1.3, "pink", name="cheek")
        ellipsoid((sx * 6.0, -6.5, B + 1.6), (2.8, 3.8, 2.2), "face", name="front_foot")
        ellipsoid((sx * 9.0, 15.0, B + 1.6), (3.0, 4.0, 2.2), "face", name="back_foot")
    # замок
    lk = Vector((13.5, -7.0, B + 5.2))
    box(lk, (10, 5.6, 9.5), "gold", rot=(0, 0, 25), name="lock_body")
    torus(lk + Vector((0, 0, 4.8)), 3.4, 1.15, "gold", rot=(90, 0, 25), name="lock_shackle")
    d = Vector((math.sin(math.radians(25)), -math.cos(math.radians(25)), 0))
    sphere(lk + d * 2.8 + Vector((0, 0, 1.2)), 1.25, "black", name="keyhole")
    limb(lk + d * 2.8 + Vector((0, 0, 1.0)), lk + d * 2.8 + Vector((0, 0, -2.3)), 0.7, 0.7,
         "black", name="keyhole")
    # лапка держит замок
    limb(fc + Vector((5.5, 1.0, -3.5)), lk + Vector((-4.0, 1.0, 0.5)), 2.0, 1.8, "face", name="arm")


def icmp_bat():
    """ICMP — летучая мышь: ping! Эхолокация и ракетка для пинг-понга."""
    base("ICMP")
    bc = Vector((0, 5, B + 12))
    ellipsoid(bc, (8.5, 8, 12), "bat", name="body")
    ellipsoid(bc + Vector((0, -4.5, -1)), (5.5, 4.5, 8), "wing", name="belly")
    hc = Vector((0, 4, B + 27))
    ellipsoid(hc, (8.5, 8, 7.5), "bat", name="head")
    for sx in (-1, 1):
        # большие острые уши-локаторы
        flat_cone(hc + Vector((sx * 4.2, 1.0, 4.0)), hc + Vector((sx * 9.5, 1.6, 15.5)),
                  4.4, 0.9, 0.5, "bat", name="ear")
        flat_cone(hc + Vector((sx * 4.4, -0.6, 5.0)), hc + Vector((sx * 8.9, -0.3, 13.6)),
                  2.7, 0.5, 0.4, "pink", name="ear_inner")
        eye(hc + Vector((sx * 3.4, -6.0, 1.6)), (sx * 0.35, -1, 0.15), 2.4, 1.5)
        limb(hc + Vector((sx * 1.6, -6.4, -3.0)), hc + Vector((sx * 1.4, -7.6, -5.0)), 0.8, 0.35,
             "white", name="fang")
        # ножки
        limb((sx * 4, 6, B + 3), (sx * 4.5, 4, B + 1.2), 2.2, 2.0, "bat", name="leg")
        ellipsoid((sx * 4.6, 2.5, B + 1.0), (2.6, 3.6, 1.6), "bat", name="foot")
    ellipsoid(hc + Vector((0, -7.0, -1.0)), (3.4, 2.4, 2.4), "bat", name="muzzle")
    sphere(hc + Vector((0, -9.0, -0.4)), 1.0, "pink", name="nose")
    arc(hc + Vector((0, -8.0, -3.1)), 1.8, 200, 340, 0.45, "black", plane="xz",
        name="mouth")
    # крылья — пластины 2.2 мм с «пальцами»-рёбрами и фестонами, распахнуты вверх
    k = 0.92
    W = Vector((13, 19)) * k
    tips = [Vector(t) * k for t in ((27, 25), (25, 11), (16, 2))]
    root, shoulder = Vector((3, -3)) * k, Vector((1, 3)) * k
    outline = [shoulder, Vector((6, 12)) * k, W, tips[0]]
    for a, b in zip(tips, tips[1:] + [root]):
        mid = (a + b) / 2
        inward = (W - mid).normalized()
        for i in range(1, 8):
            t = i / 8
            outline.append(a + (b - a) * t + inward * 3.2 * math.sin(math.pi * t))
        outline.append(b)
    outline.pop()  # root уже последний

    def wing_matrix(sx):
        return Matrix.Translation(bc + Vector((sx * 3.5, 1.5, -4))) @ trs(rot=(90, 0, sx * -18))

    for sx in (-1, 1):
        pts = [(sx * p.x, p.y) for p in outline] + [(sx * root.x, root.y)]
        if sx < 0:
            pts.reverse()
        m = wing_matrix(sx)
        prism(pts, 2.2, "wing", m, name="wing")
        wrist = m @ Vector((sx * W.x, W.y, 0))
        for tip in tips:
            limb(wrist, m @ Vector((sx * tip.x, tip.y, 0)), 1.0, 0.7, "bat", name="finger")
        limb(m @ Vector((sx * 2, 4, 0)), wrist, 1.4, 1.1, "bat", name="arm")
    # ракетка для пинг-понга в правом «большом пальце»
    wrist = wing_matrix(1) @ Vector((W.x, W.y, 0))
    pad = wrist + Vector((5.5, -6.0, 5.0))
    cylinder(pad, 6.0, 2.0, "paddle", rot=(70, 0, -10), name="paddle")
    limb(wrist + Vector((0.5, -1.2, -0.5)), pad + Vector((-2.4, 1.6, -3.6)), 1.3, 1.3, "wood",
         name="handle")
    sphere(pad + Vector((0.4, -2.6, 1.0)), 2.5, "ball", name="ball")
    # эхо-волны «ping» на подставке
    for r in (9.0, 12.5):
        arc((0, 1.0, B), r, 235, 305, 0.9, "wave", plane="xy", name="echo")


FIGURES = [
    ("TCP", "tcp_turtle", "TCP — Черепаха", tcp_turtle),
    ("UDP", "udp_rabbit", "UDP — Кролик", udp_rabbit),
    ("HTTP", "http_dog", "HTTP — Пёс", http_dog),
    ("DNS", "dns_owl", "DNS — Сова", dns_owl),
    ("TLS", "tls_hedgehog", "TLS — Ёж", tls_hedgehog),
    ("ICMP", "icmp_bat", "ICMP — Летучая мышь", icmp_bat),
]


# --------------------------------------------------------------------------- #
#  Печатная сетка: объединение → воксельный ремеш → прореживание
# --------------------------------------------------------------------------- #

def build_print_mesh(fig):
    bm = bmesh.new()
    for obj in fig.parts:
        m = obj.data.copy()
        m.transform(obj.matrix_world)
        bm.from_mesh(m)
        bpy.data.meshes.remove(m)
    joined = bpy.data.meshes.new(fig.key + "_joined")
    bm.to_mesh(joined)
    bm.free()

    coll = bpy.data.collections.new(f"{fig.key} — печать")
    bpy.context.scene.collection.children.link(coll)
    obj = bpy.data.objects.new(f"{fig.key}_print", joined)
    coll.objects.link(obj)

    rem = obj.modifiers.new("remesh", "REMESH")
    rem.mode = "VOXEL"
    rem.voxel_size = VOXEL
    rem.adaptivity = 0.0
    rem.use_smooth_shade = True
    deps = bpy.context.evaluated_depsgraph_get()
    remeshed = bpy.data.meshes.new_from_object(obj.evaluated_get(deps))
    obj.modifiers.clear()
    obj.data = remeshed
    bpy.data.meshes.remove(joined)

    tris = sum(len(p.vertices) - 2 for p in remeshed.polygons)
    if tris > TARGET_TRIS:
        dec = obj.modifiers.new("decimate", "DECIMATE")
        dec.decimate_type = "COLLAPSE"
        dec.ratio = TARGET_TRIS / tris
        dec.use_collapse_triangulate = True
        deps = bpy.context.evaluated_depsgraph_get()
        final = bpy.data.meshes.new_from_object(obj.evaluated_get(deps))
        obj.modifiers.clear()
        obj.data = final
        bpy.data.meshes.remove(remeshed)

    mesh = obj.data
    drop_debris(mesh)
    mesh.name = f"{fig.key}_print"
    flatten_bottom(mesh)
    mesh.materials.clear()
    mesh.materials.append(bpy.data.materials["clay"])
    mesh.shade_smooth()
    return obj, coll


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
    zs = [v.co.z for v in mesh.vertices]
    zmin = min(zs)
    for v in mesh.vertices:
        v.co.z = 0.0 if v.co.z < zmin + tol else v.co.z - zmin
    mesh.update()


def check_mesh(mesh):
    bm = bmesh.new()
    bm.from_mesh(mesh)
    non_manifold = sum(1 for e in bm.edges if not e.is_manifold)
    boundary = sum(1 for e in bm.edges if e.is_boundary)
    volume = bm.calc_volume(signed=True)
    shells = count_shells(bm)
    bm.free()
    return non_manifold, boundary, volume, shells


def count_shells(bm):
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
    return shells


def export_stl(obj, path):
    bpy.ops.object.select_all(action="DESELECT")
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.wm.stl_export(filepath=path, export_selected_objects=True, ascii_format=False,
                          apply_modifiers=True, global_scale=1.0, use_scene_unit=False)


# --------------------------------------------------------------------------- #
#  Превью
# --------------------------------------------------------------------------- #

def setup_render(res=900, samples=48):
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = samples
    scene.cycles.use_denoising = True
    scene.render.resolution_x = res
    scene.render.resolution_y = res
    scene.render.film_transparent = False
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.exposure = -0.35
    world = bpy.data.worlds.new("world")
    world.use_nodes = True
    bg = world.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (0.80, 0.84, 0.90, 1)
    bg.inputs["Strength"].default_value = 0.65
    scene.world = world
    sun = bpy.data.lights.new("sun", "SUN")
    sun.energy = 2.6
    sun.angle = math.radians(12)
    s = bpy.data.objects.new("sun", sun)
    s.rotation_euler = Euler((math.radians(50), 0, math.radians(-35)))
    scene.collection.objects.link(s)
    fill = bpy.data.lights.new("fill", "SUN")
    fill.energy = 0.8
    f = bpy.data.objects.new("fill", fill)
    f.rotation_euler = Euler((math.radians(65), 0, math.radians(140)))
    scene.collection.objects.link(f)
    floor_mesh = bpy.data.meshes.new("floor")
    bm = bmesh.new()
    bmesh.ops.create_grid(bm, x_segments=1, y_segments=1, size=2000)
    bm.to_mesh(floor_mesh)
    bm.free()
    mat = bpy.data.materials.new("floor")
    mat.use_nodes = True
    mat.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = (0.85, 0.87, 0.9, 1)
    floor_mesh.materials.append(mat)
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


def render(path, res=None):
    scene = bpy.context.scene
    if res:
        scene.render.resolution_x, scene.render.resolution_y = res
    scene.render.filepath = path
    bpy.ops.render.render(write_still=True)


def show_only(collections):
    for c in bpy.context.scene.collection.children:
        visible = c in collections
        c.hide_render = not visible
        c.hide_viewport = not visible


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
        pobj, pcoll = build_print_mesh(FIG)
        nm, bd, vol, shells = check_mesh(pobj.data)
        dims = pobj.dimensions
        tris = len(pobj.data.polygons)
        print(f"{title:24s} {dims.x:5.1f}×{dims.y:5.1f}×{dims.z:5.1f} мм  "
              f"{tris:7d} тр.  объём {vol / 1000:5.1f} см³  оболочек {shells}  "
              f"неманифолд {nm}  края {bd}")
        export_stl(pobj, os.path.join(STL_DIR, f"{slug}.stl"))
        built.append((key, slug, title, FIG.coll, pcoll, pobj))

    if do_render:
        for key, slug, title, coll, pcoll, pobj in built:
            h = pobj.dimensions.z
            show_only([coll])
            aim(cam, (0, -2, h * 0.45), 175, -28, 18)
            render(os.path.join(RENDER_DIR, f"{slug}.png"), (900, 900))
            show_only([pcoll])
            render(os.path.join(RENDER_DIR, f"{slug}_print.png"), (900, 900))

    # раскладываем фигурки в ряд для общего вида и .blend
    step = 64
    n = len(built)
    for i, (key, slug, title, coll, pcoll, pobj) in enumerate(built):
        x = (i - (n - 1) / 2) * step
        for o in coll.objects:
            o.location.x += x
        pobj.location = (x, 90, 0)
    if do_render and n > 1:
        show_only([b[3] for b in built])
        aim(cam, (0, 0, 24), 720, 0, 14, lens=60)
        render(os.path.join(RENDER_DIR, "lineup.png"), (1800, 760))
        show_only([b[4] for b in built])
        for b in built:
            b[5].location.y = 0
        render(os.path.join(RENDER_DIR, "lineup_print.png"), (1800, 760))
        for b in built:
            b[5].location.y = 90

    for c in bpy.context.scene.collection.children:
        c.hide_render = c.hide_viewport = False
    if not only:
        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(HERE, "protocol_animals.blend"),
                                    compress=True)


if __name__ == "__main__":
    main()
