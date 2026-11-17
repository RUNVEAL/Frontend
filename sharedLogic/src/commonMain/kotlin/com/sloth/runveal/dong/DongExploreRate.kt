package com.sloth.runveal.dong

/**
 * 행정동 탐험률(%) 계산. 분자는 "가본 길" 길이(`explored_segment` 누적), 분모는 그 동의 전체 도로
 * 길이(서버 `index.json`의 `dongLengthM` 또는 로컬 `road_segment` 인덱스 집계)다.
 *
 * 홈 지도의 동 밝기 단계(`RunvealDong.levelOf`)와 러닝 결과 화면의 "동네 탐험률" 변화가 같은 숫자를
 * 보여줘야 해서 공식을 한곳에 둔다 — 결과 화면에서 42%로 올랐다고 알려 놓고 지도는 다른 단계로
 * 빛나면 안 된다.
 */
object DongExploreRate {
    fun percentOf(walkedMeters: Double, totalMeters: Double): Int =
        if (totalMeters <= 0.0) 0 else ((walkedMeters / totalMeters) * 100).toInt().coerceIn(0, 100)

    /**
     * 동별 탐험률. 분모가 없는 동(아직 구간 인덱스를 안 들여온 지역)과 0%인 동은 결과에서 빠진다 —
     * 지도 쪽 호출부가 "없으면 0%"로 읽고, 전국 단위에서 0% 항목까지 들고 다니면 쓸데없이 커진다.
     */
    fun percents(
        walkedLengthsByDong: Map<String, Double>,
        totalLengthsByDong: Map<String, Double>,
    ): Map<String, Int> =
        walkedLengthsByDong.mapNotNull { (admCd2, walked) ->
            val total = totalLengthsByDong[admCd2] ?: return@mapNotNull null
            val percent = percentOf(walked, total)
            if (percent > 0) admCd2 to percent else null
        }.toMap()
}
