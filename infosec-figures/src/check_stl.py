"""Stage 3: independent sanity check of the exported STL files (trimesh)."""
import glob
import os
import sys

import trimesh

HERE = os.path.dirname(os.path.abspath(__file__))
STL = os.path.join(HERE, '..', 'stl')


def main():
    ok = True
    rows = []
    for path in sorted(glob.glob(os.path.join(STL, '*.stl'))):
        m = trimesh.load_mesh(path, process=True)
        parts = m.split(only_watertight=False)
        good = m.is_watertight and m.is_winding_consistent and m.volume > 0 and len(parts) == 1
        ok &= good
        ext = m.bounds[1] - m.bounds[0]
        rows.append((os.path.basename(path), len(m.faces), m.is_watertight, len(parts),
                     m.volume / 1000.0, ext, os.path.getsize(path) / 1e6, good))
    print(f"{'file':28} {'tris':>8} {'closed':>6} {'parts':>5} {'cm3':>7}  {'size X x Y x Z, mm':>22} {'MB':>5}")
    for name, nf, wt, np_, vol, ext, mb, good in rows:
        flag = '' if good else '   <-- PROBLEM'
        print(f"{name:28} {nf:8d} {str(wt):>6} {np_:5d} {vol:7.1f}  {ext[0]:6.1f} x {ext[1]:5.1f} x {ext[2]:5.1f} {mb:5.1f}{flag}")
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
