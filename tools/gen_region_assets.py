"""서버 `index.geojson`에서 시군구·시도 경계와 지역 계층 에셋을 만든다.

왜 필요한가: 완주율의 분자(구간→동 매핑)·분모(`index.json`)·지도 경계가 **같은 행정동 코드 체계**를
써야 한다. 예전엔 번들 `korea_dong.geojson`(vuski/admdongkor 스냅샷)으로 동을 판정했는데, 서버와
코드 체계가 달라서(광주·전남 통합 `12` vs 분리 `29`/`46`, 인천 중구 재배정 등 약 13%) 그 지역 완주율이
어긋났다. 이제 동 경계는 서버 것만 쓰고, 서버가 주지 않는 시군구·시도 경계와 계층만 여기서 서버
동 폴리곤을 합쳐(dissolve) 만든다.

사용법:
  pip install shapely
  python3 tools/gen_region_assets.py <index.geojson> <assets/dong 경로> [version]

입력 `index.geojson`의 각 feature properties:
  code  10자리 행정동 코드 (앞 5자리=시군구, 앞 2자리=시도)
  gu    5자리 시군구 코드 (code[:5]와 같음)
  name  "서울특별시 종로구 사직동" — 공백 3토큰

출력(기존 에셋과 같은 파일명·속성 이름을 유지해 앱 코드는 그대로 쓴다):
  korea_sigungu.geojson       properties: adm_cd2(5자리), name, sido(2자리)
  korea_sido.geojson          properties: adm_cd2(2자리), name
  korea_region_hierarchy.json 시도 → 시군구 → 동 목록
"""
import json
import sys
from collections import defaultdict

import shapely
from shapely.geometry import shape, mapping
from shapely.ops import unary_union

# 위도 1도 ≈ 111km 기준. 시군구 ~22m, 시도 ~55m 오차까지 허용해 꼭짓점을 줄인다.
SIGUNGU_SIMPLIFY_DEG = 0.0002
SIDO_SIMPLIFY_DEG = 0.0005

# 합친 결과에서 버릴 내부 구멍 넓이(m²). 서버 동 폴리곤은 동마다 따로 단순화돼서 맞닿은 경계가
# 몇 m씩 어긋나는데(결과 카드 이중선 이슈와 같은 원인), 그대로 합치면 그 틈이 수천 개의 가느다란
# 구멍으로 남는다. 실측 결과 전국 시군구에서 나온 구멍 7,745개가 **전부 0.1km² 미만**이었고
# (최대 99,345m²) 진짜 월경지는 하나도 없었다 — 그래서 이 크기 미만은 전부 메운다.
MAX_SLIVER_HOLE_M2 = 500_000

# 출력 좌표 격자(도). 소수점 6자리 ≈ 0.1m.
OUTPUT_PRECISION_DEG = 1e-6
METERS_PER_DEGREE = 111_000.0


def main(src_path: str, out_dir: str, version: str) -> None:
    src = json.load(open(src_path, encoding="utf-8"))
    features = src["features"]
    print(f"입력 동 {len(features)}개")

    # 동 → (시도코드, 시도명, 시군구코드, 시군구명, 동명)
    rows = []
    for f in features:
        p = f["properties"]
        code = p["code"]
        parts = p["name"].split()
        if len(parts) != 3:
            sys.exit(f"name 형식이 예상과 다르다: {p['name']!r} ({code})")
        sido_name, sigungu_name, dong_name = parts
        rows.append(
            {
                "code": code,
                "dong": dong_name,
                "sigungu_code": code[:5],
                "sigungu": sigungu_name,
                "sido_code": code[:2],
                "sido": sido_name,
                "geom": shape(f["geometry"]).buffer(0),  # 자가교차 폴리곤 보정
            }
        )

    # ── 계층 ──────────────────────────────────────────────────────────────
    by_sido = defaultdict(lambda: {"name": None, "sigungu": defaultdict(lambda: {"name": None, "dongs": []})})
    for r in rows:
        s = by_sido[r["sido_code"]]
        s["name"] = r["sido"]
        g = s["sigungu"][r["sigungu_code"]]
        g["name"] = r["sigungu"]
        g["dongs"].append({"code": r["code"], "name": r["dong"]})

    hierarchy = {
        # RegionHierarchyParser가 필수로 읽는 필드. 어느 지도 버전에서 뽑았는지 남겨 둔다.
        "version": version,
        "source": "server index.geojson",
        "counts": {"sido": None, "sigungu": None, "dong": len(rows)},
        "sido": [
            {
                "code": sido_code,
                "name": s["name"],
                "sigungu": [
                    {"code": gu_code, "name": g["name"], "dongs": sorted(g["dongs"], key=lambda d: d["code"])}
                    for gu_code, g in sorted(s["sigungu"].items())
                ],
            }
            for sido_code, s in sorted(by_sido.items())
        ]
    }
    n_gu = sum(len(s["sigungu"]) for s in hierarchy["sido"])
    hierarchy["counts"]["sido"] = len(hierarchy["sido"])
    hierarchy["counts"]["sigungu"] = n_gu
    print(f"계층: 시도 {len(hierarchy['sido'])} / 시군구 {n_gu} / 동 {len(rows)}")
    write_json(f"{out_dir}/korea_region_hierarchy.json", hierarchy)

    # ── 경계 합치기(dissolve) ─────────────────────────────────────────────
    # 합친 뒤 단순화한다. 이 경계들은 줌아웃 상태에서 테두리 선으로만 보이고(동 레이어와 동시에
    # 뜨지 않는다 — MapConfig의 줌 전환), 원본 그대로면 시도 17개가 2MB를 넘는다. 시도는 더 멀리서
    # 보므로 더 거칠게 깎는다.
    write_json(
        f"{out_dir}/korea_sigungu.geojson",
        dissolve(rows, "sigungu_code", "sigungu", with_sido=True, simplify_deg=SIGUNGU_SIMPLIFY_DEG),
    )
    write_json(
        f"{out_dir}/korea_sido.geojson",
        dissolve(rows, "sido_code", "sido", with_sido=False, simplify_deg=SIDO_SIMPLIFY_DEG),
    )


def dissolve(rows, code_key: str, name_key: str, with_sido: bool, simplify_deg: float) -> dict:
    groups = defaultdict(list)
    for r in rows:
        groups[r[code_key]].append(r)
    features = []
    for code, members in sorted(groups.items()):
        merged = unary_union([m["geom"] for m in members])
        merged = fill_slivers(merged)
        if simplify_deg > 0:
            # 단순화가 드물게 자가교차를 만든다(실측: 목포시). buffer(0)로 바로 펴 준다 —
            # 잘못된 도형은 MapLibre에서 렌더링이 튀거나 아예 안 그려질 수 있다.
            merged = merged.simplify(simplify_deg, preserve_topology=True)
        # 좌표를 소수점 6자리(≈0.1m) 격자에 맞춘다 — 번들 원본도 mapshaper `precision=0.000001`로
        # 같은 자리수를 썼다. 손으로 반올림하면 꼭짓점이 겹쳐 자가교차가 생길 수 있는데(실측: 목포시),
        # set_precision은 GEOS가 스냅하면서 유효한 도형으로 정리해 준다.
        merged = shapely.set_precision(merged, OUTPUT_PRECISION_DEG)
        if not merged.is_valid:
            merged = merged.buffer(0)
        props = {"adm_cd2": code, "name": members[0][name_key]}
        if with_sido:
            props["sido"] = members[0]["sido_code"]
        features.append({"type": "Feature", "properties": props, "geometry": mapping(merged)})
    print(f"  {code_key}: {len(features)}개 합침")
    return {"type": "FeatureCollection", "features": features}


def fill_slivers(geom):
    """맞닿은 경계가 어긋나 생긴 가느다란 내부 구멍을 메운다([MAX_SLIVER_HOLE_M2] 참고)."""
    from shapely.geometry import MultiPolygon, Polygon

    def clean(poly: Polygon) -> Polygon:
        kept = [
            ring
            for ring in poly.interiors
            if Polygon(ring).area * METERS_PER_DEGREE**2 >= MAX_SLIVER_HOLE_M2
        ]
        return Polygon(poly.exterior, kept)

    polys = [clean(p) for p in (geom.geoms if geom.geom_type == "MultiPolygon" else [geom])]
    return polys[0] if len(polys) == 1 else MultiPolygon(polys)


def write_json(path: str, obj) -> None:
    # separators로 공백을 빼 용량을 더 줄인다(좌표 반올림은 round_coords에서 끝냈다).
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, separators=(",", ":"))
    import os

    print(f"  → {path} ({os.path.getsize(path) / 1024 / 1024:.2f} MB)")


if __name__ == "__main__":
    if len(sys.argv) not in (3, 4):
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) == 4 else "server-2026-11-10")
