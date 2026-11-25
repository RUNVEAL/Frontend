package com.sloth.runveal.ios

import com.sloth.runveal.recording.SplitPoint
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.timeIntervalSince1970

/**
 * 저장해 둔 GPX에서 좌표와 시각만 뽑는다 — 구간 기록(km별 페이스) 계산용.
 *
 * Android `GpxPoints`의 iOS판이다. 끊는 방식(한 줄짜리 `<trkpt>`를 정규식으로)은 같고, 시각 파싱만
 * 다르다 — 저쪽은 `java.time.Instant`를 쓰는데 그건 Android 전용이라 여기서는
 * [NSISO8601DateFormatter]를 쓴다. 구간을 나누는 계산 자체는 공용
 * [com.sloth.runveal.recording.RunSplits]에 있어 양쪽이 같은 결과를 낸다.
 *
 * 시각이 없거나 모양이 다른 점은 조용히 건너뛴다 — 한 점 때문에 기록 전체를 못 보여줄 이유가 없다.
 */
@OptIn(ExperimentalForeignApi::class)
object IosGpxPoints {

    private val TRACK_POINT = Regex("""<trkpt lat="([^"]+)" lon="([^"]+)">.*?<time>([^<]+)</time>""")

    /**
     * 소수점 초가 있는 형식과 없는 형식을 모두 받는다 — `TrackingService`는 `2026-11-20T12:34:56Z`로
     * 쓰지만 Health Connect에서 들여온 기록은 소수점이 붙어 올 수 있다. 하나로는 다른 쪽을 통째로
     * 놓쳐서 둘 다 둔다.
     */
    private val formatters: List<NSISO8601DateFormatter> by lazy {
        listOf(
            NSISO8601DateFormatter().apply {
                setFormatOptions(
                    NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds,
                )
            },
            NSISO8601DateFormatter().apply {
                setFormatOptions(NSISO8601DateFormatWithInternetDateTime)
            },
        )
    }

    fun parse(gpx: String): List<SplitPoint> =
        TRACK_POINT.findAll(gpx).mapNotNull { match ->
            val (lat, lon, time) = match.destructured
            val latValue = lat.toDoubleOrNull() ?: return@mapNotNull null
            val lonValue = lon.toDoubleOrNull() ?: return@mapNotNull null
            val epochMs = parseEpochMs(time) ?: return@mapNotNull null
            SplitPoint(lat = latValue, lon = lonValue, timeEpochMs = epochMs)
        }.toList()

    private fun parseEpochMs(text: String): Long? {
        formatters.forEach { formatter ->
            formatter.dateFromString(text)?.let { return (it.timeIntervalSince1970 * 1000).toLong() }
        }
        return null
    }
}
