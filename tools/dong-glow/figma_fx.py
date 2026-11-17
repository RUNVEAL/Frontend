"""Figma-style effect renderer for dong glow (effect/dong-1~6 tokens)."""
import json
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import gaussian_filter

import os
_DEFAULT_TOKENS = os.environ.get('RUNVEAL_TOKENS', os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../design/runveal-tokens.json'))
TOKENS = json.load(open(_DEFAULT_TOKENS)) if os.path.exists(_DEFAULT_TOKENS) else None  # gen_dong_glow sets it from argv


def hex_rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)])


def stage_spec(level):
    """Read stroke/fill/effect for a stage straight from runveal-tokens.json."""
    prim = TOKENS['primitive']['color']
    sem = TOKENS['semantic']['map']['dong']

    def resolve(v):
        if isinstance(v, str):  # "{primitive.color.gold.700}"
            path = v.strip('{}').split('.')[2:]
            node = prim
            for p in path:
                node = node[p]
            return hex_rgb(node['$value']['hex']), 1.0
        return hex_rgb(v['hex']), v.get('alpha', 1.0)

    stroke_c, _ = resolve(sem[f'stroke-{level}']['$value'])
    width = TOKENS['layout']['map-dong-level']['stroke-width'][str(level)]['$value']['value']
    spec = {'stroke': stroke_c, 'stroke_w': width, 'fill': None, 'effects': []}
    if level == 0:
        return spec
    spec['fill'] = resolve(sem[f'fill-{level}']['$value'])
    eff = TOKENS['effect']['dong'][str(level)]
    modes = eff['$extensions']['com.runveal']['blendModes']
    for v, mode in zip(eff['$value'], modes):
        spec['effects'].append({
            'color': hex_rgb(v['color']['hex']),
            'alpha': v['color']['alpha'],
            'blur': v['blur']['value'],
            'inset': bool(v.get('inset', False)),
            'mode': mode,
        })
    return spec


def raster_polys(polys, w, h, ss=4):
    """polys: list of (exterior, [holes]) in pixel coords -> antialiased mask [0,1]."""
    im = Image.new('L', (w * ss, h * ss), 0)
    d = ImageDraw.Draw(im)
    for ext, holes in polys:
        d.polygon([(x * ss, y * ss) for x, y in ext], fill=255)
        for hole in holes:
            d.polygon([(x * ss, y * ss) for x, y in hole], fill=0)
    im = im.resize((w, h), Image.BOX)
    return np.asarray(im, dtype=np.float32) / 255


def raster_stroke(polys, w, h, width, ss=4):
    im = Image.new('L', (w * ss, h * ss), 0)
    d = ImageDraw.Draw(im)
    lw = max(1, int(round(width * ss)))
    for ext, holes in polys:
        for ring in [ext] + list(holes):
            pts = [(x * ss, y * ss) for x, y in ring]
            d.line(pts + [pts[0]], fill=255, width=lw, joint='curve')
    im = im.resize((w, h), Image.BOX)
    return np.asarray(im, dtype=np.float32) / 255


def sigma(blur, scale):
    # Figma blur value ~= 2 * gaussian sigma
    return max(blur * scale / 2.0, 1e-3)


def glow_layers(mask, spec, scale, stroke=None, inner_uses_fill_alpha=False):
    """Return list of (rgb, alpha, mode) layers in paint order, excluding stroke and fill.

    mask: shape coverage [0,1]. stroke: stroke coverage (for drop-shadow silhouette).
    """
    layers = []
    if not spec['effects']:
        return layers
    fill_c, fill_a = spec['fill']
    # Calibrated against the reference: Figma casts the drop shadow from the shape geometry
    # (not from the 2~36% fill alpha) and hides it behind the shape.
    sil = mask
    if stroke is not None:
        sil = np.maximum(sil, stroke)
    inv = 1.0 - mask
    for e in spec['effects']:
        if not e['inset']:
            a = gaussian_filter(sil, sigma(e['blur'], scale)) * e['alpha']
            a = a * (1.0 - mask)  # "show behind transparent areas" off
        else:
            a = gaussian_filter(inv, sigma(e['blur'], scale), mode='constant', cval=1.0) * e['alpha'] * mask
            if inner_uses_fill_alpha:
                a = a * fill_a
        layers.append((e['color'], a, e['mode']))
    return layers


def composite(bg, layers):
    out = bg.copy()
    for rgb, a, mode in layers:
        a3 = a[..., None]
        if mode == 'LINEAR_DODGE':
            out = np.clip(out + rgb * a3, 0, 1)
        else:
            out = out * (1 - a3) + rgb * a3
    return out


def render_on_bg(polys, w, h, level, scale=1.0, bg=(14, 14, 14), **kw):
    spec = stage_spec(level)
    mask = raster_polys(polys, w, h)
    stroke = raster_stroke(polys, w, h, spec['stroke_w'] * scale)
    out = np.ones((h, w, 3), np.float32) * (np.array(bg) / 255)
    layers = []
    fx = glow_layers(mask, spec, scale, stroke, **kw)
    drops = [l for l, e in zip(fx, spec['effects']) if not e['inset']]
    inners = [l for l, e in zip(fx, spec['effects']) if e['inset']]
    layers += drops
    if spec['fill'] is not None:
        layers.append((spec['fill'][0], mask * spec['fill'][1], 'NORMAL'))
    layers += inners
    layers.append((spec['stroke'], stroke, 'NORMAL'))
    return composite(out, layers)
