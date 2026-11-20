package com.sloth.runveal.tabrecord

import com.sloth.runveal.recording.SplitPoint
import java.time.Instant

/**
 * 저장해 둔 GPX에서 좌표와 시각만 뽑는다 — 구간 기록(km별 페이스) 계산용.
 *
 * `TrackingService`/`HealthConnectImporter`가 쓰는 한 줄짜리 `<trkpt>` 형식만 읽으면 되므로 XML
 * 파서를 들이지 않고 정규식으로 끊는다(줄을 넘나들지 않게 `.`이 개행에 안 걸리는 기본 모드를 쓴다).
 * 시각이 없거나 모양이 다른 점은 조용히 건너뛴다 — 한 점 때문에 기록 전체를 못 보여줄 이유가 없다.
 *
 * 시각 파싱에 `java.time`을 쓰므로 Android 전용이다. iOS에서 같은 화면을 만들 땐 그쪽 파서가 필요하다
 * (구간을 나누는 계산 자체는 공용 [com.sloth.runveal.recording.RunSplits]에 있다).
 */
internal object GpxPoints {
    private val TRACK_POINT = Regex("""<trkpt lat="([^"]+)" lon="([^"]+)">.*?<time>([^<]+)</time>""")

    fun parse(gpx: String): List<SplitPoint> =
        TRACK_POINT.findAll(gpx).mapNotNull { match ->
            val (lat, lon, time) = match.destructured
            runCatching {
                SplitPoint(
                    lat = lat.toDouble(),
                    lon = lon.toDouble(),
                    timeEpochMs = Instant.parse(time).toEpochMilli(),
                )
            }.getOrNull()
        }.toList()
}
