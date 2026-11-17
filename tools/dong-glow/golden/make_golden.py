import json, os
from shapely.geometry import shape
import figma_fx as F, gen_dong_glow as G
F.TOKENS = json.load(open('runveal-tokens.json'))
G.QUALITY = 1.0
feats = {f['properties']['adm_cd2']: f for f in json.load(open('/mnt/user-data/uploads/runveal/androidApp/core/map/src/main/assets/dong/korea_dong.geojson'))['features']}
codes = ['44200101', '4420025300', '44200118', '11680101']  # 온천동(작음), 배방읍(큼), 신동, 역삼동
out = {'quality': G.QUALITY, 'refInradiusPx': G.REF_INRADIUS_PX, 'refEffectScale': G.REF_EFFECT_SCALE,
       'maxSide': G.MAX_SIDE, 'note': 'Python 기준 출력. PNG, straight alpha RGBA. corners = [TL,TR,BR,BL] [lon,lat]', 'dongs': {}}
os.makedirs('golden', exist_ok=True)
for c in codes:
    f = feats[c]; g = shape(f['geometry']).buffer(0)
    e = {'name': f['properties']['name'], 'levels': {}}
    for l in range(1, 7):
        img, corners, meta = G.render_dong(g, l)
        img.save(f'golden/{c}_{l}.png', optimize=True)
        e['levels'][str(l)] = {'corners': corners, **meta}
    e['inradiusMercatorM'] = e['levels']['1']['inradius_m']
    out['dongs'][c] = e
json.dump(out, open('golden/golden.json', 'w'), ensure_ascii=False, indent=1)
print({k: (v['name'], v['inradiusMercatorM'], v['levels']['6']['size']) for k, v in out['dongs'].items()})
