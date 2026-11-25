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
    /**
     * 탐험률이 가장 높은 행정동 코드 — 홈이 앱 시작 포커스와 "내 동네" 꼬리표의 기준으로 쓴다.
     *
     * [percents]는 0%인 동을 담지 않으므로([percents] 참고), 비어 있으면 **기록이 아직 없다**는 뜻이다.
     *
     * 같은 퍼센트가 여럿이면 코드가 작은 쪽으로 고정한다 — 안 그러면 묶음을 다시 만들 때마다 기준
     * 동이 바뀌어 포커스와 꼬리표가 흔들린다.
     */
    fun topDongCode(percents: Map<String, Int>): String? =
        percents.entries.minWithOrNull(
            // 퍼센트 내림차순 → 코드 오름차순으로 세워 첫 번째를 고른다(= 최고 퍼센트, 같으면 작은 코드).
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key },
        )?.key

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
