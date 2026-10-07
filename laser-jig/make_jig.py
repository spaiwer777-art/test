"""Генератор STL-рамки (джига) для гравировки визиток на лазерном гравере.

Модель — плита с карманами под визитки. Визитка ложится в карман с минимальным
зазором и не двигается; пальцевые выемки по коротким сторонам позволяют легко
её достать. Запуск: python3 make_jig.py  (нужен только numpy).
"""
import struct
import numpy as np

# ---------------- Параметры (мм) ----------------
CARD_W, CARD_H = 90.0, 50.0   # размер визитки (РФ стандарт 90x50)
CLEAR = 0.3                   # зазор с каждой стороны кармана
COLS, ROWS = 2, 3             # раскладка карманов
BORDER = 8.0                  # внешняя рамка
GAP_X, GAP_Y = 8.0, 6.0       # перегородки между карманами
HEIGHT = 3.0                  # общая толщина плиты
POCKET_DEPTH = 0.8            # глубина кармана (визитка ~0.3-0.5 мм)
NOTCH_W, NOTCH_IN, NOTCH_OUT = 16.0, 5.0, 3.0  # пальцевая выемка
NOTCH_FLOOR = 1.0             # высота дна выемки
RELIEF = 1.0                  # угловые «подрезы» под скругления печати

PW, PH = CARD_W + 2 * CLEAR, CARD_H + 2 * CLEAR
TOTAL_W = 2 * BORDER + COLS * PW + (COLS - 1) * GAP_X
TOTAL_H = 2 * BORDER + ROWS * PH + (ROWS - 1) * GAP_Y
FLOOR = HEIGHT - POCKET_DEPTH


def pockets():
    for r in range(ROWS):
        for c in range(COLS):
            x0 = BORDER + c * (PW + GAP_X)
            y0 = BORDER + r * (PH + GAP_Y)
            yield x0, y0


def regions():
    """Прямоугольники (x0, y0, x1, y1, высота). Высота ячейки = минимум."""
    out = []
    for x0, y0 in pockets():
        x1, y1 = x0 + PW, y0 + PH
        out.append((x0, y0, x1, y1, FLOOR))
        for cx, cy in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
            out.append((cx - RELIEF, cy - RELIEF, cx + RELIEF, cy + RELIEF, FLOOR))
        ym = (y0 + y1) / 2
        out.append((x0 - NOTCH_OUT, ym - NOTCH_W / 2, x0 + NOTCH_IN, ym + NOTCH_W / 2, NOTCH_FLOOR))
        out.append((x1 - NOTCH_IN, ym - NOTCH_W / 2, x1 + NOTCH_OUT, ym + NOTCH_W / 2, NOTCH_FLOOR))
    return out


def build_mesh():
    regs = regions()
    xs = sorted({0.0, TOTAL_W, *[round(v, 4) for r in regs for v in (r[0], r[2])]})
    ys = sorted({0.0, TOTAL_H, *[round(v, 4) for r in regs for v in (r[1], r[3])]})
    nx, ny = len(xs) - 1, len(ys) - 1
    h = np.full((nx, ny), HEIGHT)
    cxs = [(xs[i] + xs[i + 1]) / 2 for i in range(nx)]
    cys = [(ys[j] + ys[j + 1]) / 2 for j in range(ny)]
    for x0, y0, x1, y1, z in regs:
        for i, cx in enumerate(cxs):
            if x0 < cx < x1:
                for j, cy in enumerate(cys):
                    if y0 < cy < y1:
                        h[i, j] = min(h[i, j], z)
    levels = sorted({0.0, *np.unique(h).tolist()})

    tris = []

    def quad(a, b, c, d):  # против часовой при взгляде снаружи
        tris.append((a, b, c))
        tris.append((a, c, d))

    def H(i, j):
        return h[i, j] if 0 <= i < nx and 0 <= j < ny else 0.0

    for i in range(nx):
        for j in range(ny):
            x0, x1, y0, y1, z = xs[i], xs[i + 1], ys[j], ys[j + 1], h[i, j]
            quad((x0, y0, z), (x1, y0, z), (x1, y1, z), (x0, y1, z))      # верх
            quad((x0, y0, 0), (x0, y1, 0), (x1, y1, 0), (x1, y0, 0))      # низ

    def wall(p, q, lo, hi):
        # стена вдоль ребра p->q, наружная сторона справа от направления p->q
        for k in range(len(levels) - 1):
            a, b = levels[k], levels[k + 1]
            if a >= lo - 1e-9 and b <= hi + 1e-9:
                quad((p[0], p[1], a), (q[0], q[1], a), (q[0], q[1], b), (p[0], p[1], b))

    for i in range(-1, nx):          # вертикальные рёбра x = xs[i+1]
        for j in range(ny):
            hl, hr = H(i, j), H(i + 1, j)
            x, y0, y1 = xs[i + 1], ys[j], ys[j + 1]
            if hl > hr:   # левая ячейка выше, наружу +x
                wall((x, y0), (x, y1), hr, hl)
            elif hr > hl:  # наружу -x
                wall((x, y1), (x, y0), hl, hr)
    for i in range(nx):              # горизонтальные рёбра y = ys[j+1]
        for j in range(-1, ny):
            hb, ht = H(i, j), H(i, j + 1)
            y, x0, x1 = ys[j + 1], xs[i], xs[i + 1]
            if hb > ht:   # наружу +y
                wall((x1, y), (x0, y), ht, hb)
            elif ht > hb:  # наружу -y
                wall((x0, y), (x1, y), hb, ht)
    return tris


def check_watertight(tris):
    edges = {}
    for t in tris:
        for k in range(3):
            e = (t[k], t[(k + 1) % 3])
            edges[e] = edges.get(e, 0) + 1
    bad = [e for e, n in edges.items() if n != 1 or edges.get((e[1], e[0]), 0) != 1]
    return len(bad)


def write_stl(path, tris):
    with open(path, "wb") as f:
        f.write(b"Business card laser jig".ljust(80, b" "))
        f.write(struct.pack("<I", len(tris)))
        for a, b, c in tris:
            a, b, c = map(np.array, (a, b, c))
            n = np.cross(b - a, c - a)
            n = n / (np.linalg.norm(n) or 1)
            f.write(struct.pack("<12fH", *n, *a, *b, *c, 0))


def write_svg(path):
    """Шаблон расположения визиток для LightBurn и т.п. (1 ед. = 1 мм)."""
    rects = "\n".join(
        f'  <rect x="{x0 + CLEAR:.2f}" y="{y0 + CLEAR:.2f}" width="{CARD_W}" height="{CARD_H}" '
        f'fill="none" stroke="red" stroke-width="0.2"/>'
        for x0, y0 in pockets())
    with open(path, "w") as f:
        f.write(f'<svg xmlns="http://www.w3.org/2000/svg" width="{TOTAL_W:.2f}mm" height="{TOTAL_H:.2f}mm" '
                f'viewBox="0 0 {TOTAL_W:.2f} {TOTAL_H:.2f}">\n'
                f'  <rect x="0" y="0" width="{TOTAL_W:.2f}" height="{TOTAL_H:.2f}" fill="none" stroke="blue" stroke-width="0.2"/>\n'
                f'{rects}\n</svg>\n')


if __name__ == "__main__":
    tris = build_mesh()
    bad = check_watertight(tris)
    name = f"business_card_jig_{COLS}x{ROWS}_{int(CARD_W)}x{int(CARD_H)}"
    write_stl(name + ".stl", tris)
    write_svg(name + "_layout.svg")
    print(f"{name}.stl: {len(tris)} треугольников, размер {TOTAL_W:.1f} x {TOTAL_H:.1f} x {HEIGHT} мм, "
          f"незамкнутых рёбер: {bad}")
