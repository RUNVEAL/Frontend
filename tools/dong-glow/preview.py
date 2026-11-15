import json, sys, numpy as np
from PIL import Image
from shapely.geometry import shape, MultiPolygon
import figma_fx as F, gen_dong_glow as G
gj, outdir, png = sys.argv[1], sys.argv[2], sys.argv[3]
stages = json.loads(sys.argv[4]); mine = sys.argv[5] if len(sys.argv) > 5 else None
BG = np.array(F.hex_rgb(sys.argv[6] if len(sys.argv) > 6 else '#121212'))
man = json.load(open(f'{outdir}/manifest.json'))
feats = {f['properties']['adm_cd2']: f for f in json.load(open(gj))['features'] if f['properties']['adm_cd2'] in man['dongs']}
geoms = {k: shape(f['geometry']).buffer(0) for k, f in feats.items()}
merc = {k: G.transform(lambda x, y, z=None: G.to_merc(np.asarray(x), np.asarray(y)), g) for k, g in geoms.items()}
minx = min(g.bounds[0] for g in merc.values()); miny = min(g.bounds[1] for g in merc.values())
maxx = max(g.bounds[2] for g in merc.values()); maxy = max(g.bounds[3] for g in merc.values())
W = int(sys.argv[7]) if len(sys.argv) > 7 else 1100; pad = 40
s = (W - 2 * pad) / (maxx - minx); H = int((maxy - miny) * s) + 2 * pad
px = lambda cs: [((x - minx) * s + pad, (maxy - y) * s + pad) for x, y in cs]
out = np.ones((H, W, 3), np.float32) * BG
def rings(g):
    ps = g.geoms if isinstance(g, MultiPolygon) else [g]
    return [(px(p.exterior.coords), [px(i.coords) for i in p.interiors]) for p in ps]
# 1) fills
for k, g in merc.items():
    lvl = stages.get(k, 0)
    if lvl:
        c, a = F.stage_spec(lvl)['fill']
        m = F.raster_polys(rings(g), W, H)[..., None] * a
        out = out * (1 - m) + c * m
# 2) glow images placed by their corners (mercator-aligned => resize + paste)
for k in merc:
    lvl = stages.get(k, 0)
    if not lvl: continue
    e = man['dongs'][k]['levels'][str(lvl)]
    (lon0, lat1), _, (lon1, lat0), _ = e['corners']
    x0, y1 = G.to_merc(lon0, lat1); x1, y0 = G.to_merc(lon1, lat0)
    X0, Y0 = (x0 - minx) * s + pad, (maxy - y1) * s + pad
    X1, Y1 = (x1 - minx) * s + pad, (maxy - y0) * s + pad
    im = Image.open(f'{outdir}/{k}/{lvl}.webp').convert('RGBA')
    tw, th = max(1, round(X1 - X0)), max(1, round(Y1 - Y0))
    im = np.asarray(im.resize((tw, th), Image.BILINEAR), np.float32) / 255
    ix, iy = round(X0), round(Y0)
    sx0, sy0 = max(0, -ix), max(0, -iy); dx0, dy0 = max(0, ix), max(0, iy)
    dx1, dy1 = min(W, ix + tw), min(H, iy + th)
    if dx1 <= dx0 or dy1 <= dy0: continue
    crop = im[sy0:sy0 + dy1 - dy0, sx0:sx0 + dx1 - dx0]
    a = crop[..., 3:4]
    out[dy0:dy1, dx0:dx1] = out[dy0:dy1, dx0:dx1] * (1 - a) + crop[..., :3] * a
# 3) strokes (screen px, vector)
for k, g in merc.items():
    lvl = stages.get(k, 0); sp = F.stage_spec(lvl)
    w = 2 if k == mine else sp['stroke_w']
    m = F.raster_stroke(rings(g), W, H, w)[..., None]
    out = out * (1 - m) + sp['stroke'] * m
Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8)).save(png)
print(W, H)
