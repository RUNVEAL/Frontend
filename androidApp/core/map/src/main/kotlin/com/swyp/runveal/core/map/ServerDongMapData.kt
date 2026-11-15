package com.swyp.runveal.core.map

/** [DongAssetRepository.syncFromServer]이 내려받은 서버 지도 파일(`index.geojson`/`index.json`,
 * `docs/api/API.md` §3.1.2)에서 바로 쓸 수 있게 뽑아낸 값. [boundaryGeoJson]은 전국 행정동 경계
 * 원본 텍스트, [totalLengthsByDong]은 완주율(%) 분모(행정동 코드 → 총 길이m).
 *
 * [mapVersion]은 이 경계가 어느 지도 버전에서 왔는지 — 빛 이미지 디스크 캐시 키에 들어간다. 서버가
 * 경계를 고쳐 새 버전으로 배포하면 키가 달라져서 옛 경계로 만든 빛 이미지가 자동으로 버려진다. */
data class ServerDongMapData(
    val boundaryGeoJson: String,
    val totalLengthsByDong: Map<String, Double>,
    val mapVersion: String,
)
