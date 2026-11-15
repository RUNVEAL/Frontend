package com.swyp.runveal.network.maps

import kotlinx.serialization.Serializable

/** `GET /maps/{mapVersion}/manifest.json` 응답 — `docs/api/API.md` §3.1.2. `files`에 적힌 이름으로
 * `MapsApi.fetchFile(mapVersion, fileName)`을 하나씩 호출해서 받는다. */
@Serializable
data class ManifestResponse(
    val version: String,
    val files: List<String>,
    val matchDistanceM: Int,
    val segmentCoverage: Double,
    val trimMinM: Int,
    val trimMaxM: Int,
)

/** `GET /maps/{mapVersion}/index.json` 응답 — `docs/api/API.md` §3.1.2. 전국 행정동/구 총 길이(m) —
 * 완주율(%) 계산의 분모. 키는 행정동 코드(`adm_cd2`)/구 코드 문자열. */
@Serializable
data class IndexResponse(
    val dongLengthM: Map<String, Double>,
    val guLengthM: Map<String, Double> = emptyMap(),
)
