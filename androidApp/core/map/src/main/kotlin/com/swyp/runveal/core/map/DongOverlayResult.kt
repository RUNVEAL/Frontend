package com.swyp.runveal.core.map

import com.swyp.runveal.dong.DongPolygonPart

/**
 * 동 오버레이 한 벌 — 지도에 바로 넣는 GeoJSON 3종과, 그걸 만들 때 쓴 polygon part.
 *
 * part를 같이 돌려주는 이유: 빛 이미지([com.swyp.runveal.core.map.internal.maplibre.DongGlowImageLayer])가
 * 테두리 선과 **같은 정점**을 써야 한다. 경계 GeoJSON을 따로 한 번 더 파싱해서 쓰면 인접 동 공유 정점
 * 스냅([com.swyp.runveal.core.map.internal.maplibre.VertexSnapper])을 안 거친 좌표가 나와서, 같은 동인데
 * 테두리와 빛이 최대 15m까지 어긋난 두 겹으로 보인다. 덤으로 전국 경계를 두 번 파싱하지 않아도 된다.
 */
data class DongOverlayResult(
    val overlay: DongOverlayGeoJson,
    val polygonPartsByAdmCd2: Map<String, List<DongPolygonPart>>,
)
