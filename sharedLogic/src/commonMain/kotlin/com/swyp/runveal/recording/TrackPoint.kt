package com.swyp.runveal.recording

/** GPS에서 받은 위치 점 하나 — `docs/running/recording-lifecycle.md` §4.1. */
data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
    val speedMps: Float,
    val timeEpochMs: Long,
)
