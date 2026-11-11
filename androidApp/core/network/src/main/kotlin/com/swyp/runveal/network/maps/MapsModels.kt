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
