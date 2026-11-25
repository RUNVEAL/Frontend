package com.sloth.runveal.core.map

import com.sloth.runveal.dong.DongPolygonPart

/**
 * 동 오버레이 한 벌 — 지도에 바로 넣는 GeoJSON 3종과, 그걸 만들 때 쓴 polygon part.
 *
 * part를 같이 돌려주는 이유: 빛 이미지([com.sloth.runveal.core.map.internal.maplibre.DongGlowImageLayer])가
 * 테두리 선과 **같은 정점**을 써야 한다. 경계 GeoJSON을 따로 한 번 더 파싱하면 전국 데이터를 두 번
 * 읽게 되고, 전처리가 어긋나면 테두리와 빛이 두 겹으로 보인다.
 */
data class DongOverlayResult(
    val overlay: DongOverlayGeoJson,
    val polygonPartsByAdmCd2: Map<String, List<DongPolygonPart>>,
    /** 동 코드 -> 행정동 이름("부성2동") — 지도 라벨과 홈 요약 카드가 같은 이름을 쓰게 한다. */
    val nameByAdmCd2: Map<String, String>,
)
