package com.sloth.runveal.data.local

/** 기록의 출처(`track.source`, docs/api/API.md §4 runs "출처"). DB에는 [name]이 그대로 들어간다. */
enum class TrackSource {
    /** 앱에서 직접 기록한 러닝(`core:tracking`). */
    LOCAL,

    /** Health Connect에서 가져온 외부 운동 기록(§3.7 `purpose: "IMPORT"`). */
    HEALTH_CONNECT,
    ;

    companion object {
        fun fromDb(value: String?): TrackSource = entries.firstOrNull { it.name == value } ?: LOCAL
    }
}
