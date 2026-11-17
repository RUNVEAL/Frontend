"""Generate per-dong, per-stage glow images (effect/dong-1~6) for MapLibre ImageSource.

Each image contains only the glow (outer drop shadow + inner shadows). The dong fill and
stroke stay as vector layers in the app. Effect size is scaled to each dong's size so every
dong looks like the design reference (heptagon in DONG_EXPLORATION_LAYER design).

Usage:
  python3 gen_dong_glow.py <korea_dong.geojson> <runveal-tokens.json> <out_dir> [--prefix 11680] [--levels 1-6]
"""
import argparse
import json
import math
import os

import numpy as np
from PIL import Image
from shapely.geometry import shape, Polygon, MultiPolygon
from shapely.ops import polylabel, transform  # noqa

import figma_fx as F

# --- calibration against the design reference (see calib.py) ---------------------------
REF_INRADIUS_PX = 38.8      # heptagon inradius in the reference image
REF_EFFECT_SCALE = 0.7      # token blur -> reference px (reference was exported at 0.7x)
QUALITY = 2.0               # image px per reference px (inradius ~ 78px in the image)
MAX_SIDE = 1024             # cap; very elongated/large dongs get a lower QUALITY instead
R = 6378137.0


def to_merc(lon, lat):
    x = np.radians(lon) * R
    y = np.log(np.tan(np.pi / 4 + np.radians(lat) / 2)) * R
    return x, y


def to_lonlat(x, y):
    lon = np.degrees(x / R)
    lat = np.degrees(2 * np.arctan(np.exp(y / R)) - np.pi / 2)
    return float(lon), float(lat)


def inradius(geom):
    polys = geom.geoms if isinstance(geom, MultiPolygon) else [geom]
    best = 0.0
    for p in polys:
        tol = max(math.sqrt(p.area) / 200, 0.5)
        c = polylabel(p, tolerance=tol)
        best = max(best, p.exterior.distance(c) if not p.interiors else p.boundary.distance(c))
    return best


def render_dong(geom_ll, level, tokens_spec_cache={}):
    """Return (RGBA uint8 image, corners[[lon,lat] x4 TL,TR,BR,BL], meta)."""
    g = transform(lambda x, y, z=None: to_merc(np.asarray(x), np.asarray(y)), geom_ll)
    r_m = inradius(g)
    spec = tokens_spec_cache.get(level) or F.stage_spec(level)
    tokens_spec_cache[level] = spec
    max_blur = max(e['blur'] for e in spec['effects'])
    minx, miny, maxx, maxy = g.bounds

    def layout(q):
        px_per_m = (REF_INRADIUS_PX * q) / r_m
        eff_scale = REF_EFFECT_SCALE * q          # token blur -> image px
        margin = int(math.ceil(3 * F.sigma(max_blur, eff_scale))) + 2
        w = int(math.ceil((maxx - minx) * px_per_m)) + 2 * margin
        h = int(math.ceil((maxy - miny) * px_per_m)) + 2 * margin
        return px_per_m, eff_scale, margin, w, h

    q = QUALITY
    px_per_m, eff_scale, margin, w, h = layout(q)
    while max(w, h) > MAX_SIDE:
        q *= MAX_SIDE / max(w, h) * 0.98
        px_per_m, eff_scale, margin, w, h = layout(q)

    def to_px(coords):
        return [((x - minx) * px_per_m + margin, (maxy - y) * px_per_m + margin) for x, y in coords]

    polys = g.geoms if isinstance(g, MultiPolygon) else [g]
    rings = [(to_px(p.exterior.coords), [to_px(i.coords) for i in p.interiors]) for p in polys]
    mask = F.raster_polys(rings, w, h)
    stroke = F.raster_stroke(rings, w, h, spec['stroke_w'] * eff_scale)
    layers = F.glow_layers(mask, spec, eff_scale, stroke)

    # Flatten glow layers into one straight-alpha RGBA (premultiplied "over").
    # LINEAR_DODGE is folded in as normal-over: identical on black, near-identical on the dark map.
    prem = np.zeros((h, w, 3), np.float32)
    alpha = np.zeros((h, w), np.float32)
    for rgb, a, _mode in layers:
        a = np.clip(a, 0, 1)
        prem = prem * (1 - a[..., None]) + rgb * a[..., None]
        alpha = alpha * (1 - a) + a
    rgb = np.where(alpha[..., None] > 1e-6, prem / np.maximum(alpha[..., None], 1e-6), 0)
    img = np.dstack([rgb, alpha])
    img = (np.clip(img, 0, 1) * 255 + 0.5).astype(np.uint8)

    x0 = minx - margin / px_per_m
    x1 = x0 + w / px_per_m
    y1 = maxy + margin / px_per_m
    y0 = y1 - h / px_per_m
    corners = [to_lonlat(x0, y1), to_lonlat(x1, y1), to_lonlat(x1, y0), to_lonlat(x0, y0)]
    meta = {'inradius_m': round(r_m, 1), 'quality': round(q, 3), 'size': [w, h]}
    return Image.fromarray(img, 'RGBA'), corners, meta


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('geojson')
    ap.add_argument('tokens')
    ap.add_argument('out')
    ap.add_argument('--prefix', default='')
    ap.add_argument('--levels', default='1-6')
    ap.add_argument('--format', default='webp', choices=['webp', 'png'])
    a = ap.parse_args()
    F.TOKENS = json.load(open(a.tokens))
    lo, hi = map(int, a.levels.split('-'))
    feats = [f for f in json.load(open(a.geojson))['features']
             if f['properties']['adm_cd2'].startswith(a.prefix)]
    os.makedirs(a.out, exist_ok=True)
    manifest = {
        'description': 'Dong glow images (effect/dong-N). Glow only; fill/stroke are vector layers.',
        'coordinates': 'corners = [topLeft, topRight, bottomRight, bottomLeft] as [lon, lat] (MapLibre LatLngQuad)',
        'path': '{adm_cd2}/{level}.' + a.format,
        'dongs': {},
    }
    for i, f in enumerate(feats):
        code = f['properties']['adm_cd2']
        geom = shape(f['geometry']).buffer(0)
        if geom.is_empty:
            continue
        entry = {'name': f['properties'].get('name'), 'levels': {}}
        os.makedirs(os.path.join(a.out, code), exist_ok=True)
        for lvl in range(lo, hi + 1):
            img, corners, meta = render_dong(geom, lvl)
            p = os.path.join(a.out, code, f'{lvl}.{a.format}')
            if a.format == 'webp':
                img.save(p, 'WEBP', quality=85, method=4)
            else:
                img.save(p, optimize=True)
            entry['levels'][str(lvl)] = {'corners': [[round(c[0], 7), round(c[1], 7)] for c in corners], **meta}
        manifest['dongs'][code] = entry
        if (i + 1) % 100 == 0:
            print(f'{i + 1}/{len(feats)}', flush=True)
    json.dump(manifest, open(os.path.join(a.out, 'manifest.json'), 'w'), ensure_ascii=False, indent=1)
    print('done', len(manifest['dongs']))


if __name__ == '__main__':
    main()
