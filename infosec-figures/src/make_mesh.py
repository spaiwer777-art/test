"""Stage 1: evaluate every figure's SDF, check the walls around the flash
drive cavity and write a dense marching-cubes mesh to ../build/<key>.ply."""
import argparse
import json
import os
import sys
import time

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
from sdf import difference, mesh_sdf, write_ply  # noqa: E402
from common import cavity_sdf, check_envelope  # noqa: E402
import figures  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, '..', 'build')


def auto_bounds(f, step=1.5, margin=3.0):
    xs = np.arange(-70, 70.01, step)
    ys = np.arange(-70, 70.01, step)
    zs = np.arange(-2, 170.01, step)
    g = np.stack(np.meshgrid(xs, ys, zs, indexing='ij'), -1).reshape(-1, 3).astype(np.float32)
    d = np.concatenate([f(g[i:i + 400000]) for i in range(0, len(g), 400000)])
    inside = g[d < step]
    lo = inside.min(0) - margin
    hi = inside.max(0) + margin
    for a in range(3):
        assert (a == 2 or lo[a] > [xs, ys, zs][a][0] + 1) and hi[a] < [xs, ys, zs][a][-1] - 1, 'figure exceeds search box'
    lo[2] = -1.0
    return lo, hi


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('names', nargs='*')
    ap.add_argument('--h', type=float, default=0.2, help='voxel size, mm')
    args = ap.parse_args()
    os.makedirs(BUILD, exist_ok=True)
    builders = [figures.BY_KEY[n] for n in args.names] if args.names else figures.ALL
    for b in builders:
        t0 = time.time()
        fig = b()
        bad, d = check_envelope(fig['solid'])
        if len(bad):
            print(f"[{fig['key']}] WALL CHECK FAILED at {len(bad)} points, worst d={d.max():.2f}")
            zs = np.unique(np.round(bad[:, 2]))
            print('   z levels:', zs[:40])
        else:
            print(f"[{fig['key']}] wall check ok")
        solid = difference(fig['solid'], cavity_sdf())
        lo, hi = auto_bounds(fig['solid'])
        v, f, nblk = mesh_sdf(solid, (lo, hi), h=args.h)
        path = os.path.join(BUILD, fig['key'] + '.ply')
        write_ply(path, v, f)
        meta = {k: fig[k] for k in ('key', 'title', 'sub', 'plinth', 'color', 'back')}
        meta['bounds'] = [lo.tolist(), hi.tolist()]
        meta['wall_ok'] = not len(bad)
        with open(os.path.join(BUILD, fig['key'] + '.json'), 'w') as fh:
            json.dump(meta, fh, indent=1)
        print(f"[{fig['key']}] {len(f)} tris, size {np.round(hi - lo, 1)}, {nblk} blocks, {time.time() - t0:.1f}s")


if __name__ == '__main__':
    main()
