package com.sloth.runveal.result

import com.sloth.runveal.dong.DongExploreRate

/**
 * "동네 탐험률 전/후" 한 줄씩 만들기. 러닝을 끝낸 뒤(`TrackingService`)와 Health Connect 기록을
 * 가져온 뒤(`HealthConnectImporter`)가 같은 숫자를 보여 줘야 해서 공식을 한곳에 둔다 — 한쪽만
 * 고치면 같은 동이 두 화면에서 다른 탐험률로 보인다.
 */
object DongRateChanges {
    /**
     * [touchedAdmCodes] 중 전체 길이([totalLengths])와 이름([namesByCode])을 둘 다 아는 동만
     * 돌려준다. 전체 길이가 없는 동은 아직 구간 인덱스를 안 들여온 지역이라 분모가 없어 탐험률을
     * 낼 수 없고, 이름을 못 찾은 동은 화면에 적을 말이 없다.
     */
    fun build(
        touchedAdmCodes: Set<String>,
        beforeWalked: Map<String, Double>,
        afterWalked: Map<String, Double>,
        totalLengths: Map<String, Double>,
        namesByCode: Map<String, String>,
    ): List<DongRateChange> {
        if (touchedAdmCodes.isEmpty()) return emptyList()
        return touchedAdmCodes.mapNotNull { admCd2 ->
            val total = totalLengths[admCd2]?.takeIf { it > 0.0 } ?: return@mapNotNull null
            val name = namesByCode[admCd2] ?: return@mapNotNull null
            DongRateChange(
                admCd2 = admCd2,
                name = name,
                beforePercent = DongExploreRate.percentOf(beforeWalked[admCd2] ?: 0.0, total),
                afterPercent = DongExploreRate.percentOf(afterWalked[admCd2] ?: 0.0, total),
            )
        }
    }
}
