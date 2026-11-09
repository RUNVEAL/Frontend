package com.swyp.runveal.dong.region

/** 행정동 하나(계층 파일의 리프 노드). */
data class Dong(val code: String, val name: String)

/** 일반구가 있는 시(수원시 등)에서, 구의 상위 시. 일반구가 아닌 시군구는 null. */
data class ParentCity(val code: String, val name: String)

/** 시군구 하나. [parentCity]는 일반구(수원시 장안구 등)에서만 값이 있다. */
data class Sigungu(val code: String, val name: String, val parentCity: ParentCity?, val dongs: List<Dong>)

/** 시도 하나. */
data class Sido(val code: String, val name: String, val sigungu: List<Sigungu>)

/**
 * 시도 16 → 시군구 256 → 행정동 3,559 계층(`docs/map/refactoring/REGION_HIERARCHY.md`). 행정동
 * `adm_cd2`의 앞 5자리/앞 2자리가 각각 시군구/시도 코드와 같다는 규칙이 전 행정동에서 성립하지만,
 * 코드가 아니라 **이 계층 파일 기준으로 조회**한다 — 규칙이 깨지는 갱신이 와도 파일만 바꾸면 되게.
 * 조회는 전부 파싱 시점에 만든 `Map`으로 O(1)에 한다.
 */
class RegionHierarchy(val version: String, val sido: List<Sido>) {
    private val sidoByCode: Map<String, Sido>
    private val sigunguByCode: Map<String, Sigungu>
    private val sidoBySigunguCode: Map<String, Sido>
    private val sigunguByDongCode: Map<String, Sigungu>

    init {
        val sidoByCodeMap = mutableMapOf<String, Sido>()
        val sigunguByCodeMap = mutableMapOf<String, Sigungu>()
        val sidoBySigunguCodeMap = mutableMapOf<String, Sido>()
        val sigunguByDongCodeMap = mutableMapOf<String, Sigungu>()
        for (s in sido) {
            sidoByCodeMap[s.code] = s
            for (sg in s.sigungu) {
                sigunguByCodeMap[sg.code] = sg
                sidoBySigunguCodeMap[sg.code] = s
                for (d in sg.dongs) {
                    sigunguByDongCodeMap[d.code] = sg
                }
            }
        }
        sidoByCode = sidoByCodeMap
        sigunguByCode = sigunguByCodeMap
        sidoBySigunguCode = sidoBySigunguCodeMap
        sigunguByDongCode = sigunguByDongCodeMap
    }

    fun sigunguOf(dongCode: String): Sigungu? = sigunguByDongCode[dongCode]

    fun sidoOf(dongCode: String): Sido? = sigunguOf(dongCode)?.let { sidoBySigunguCode[it.code] }

    fun sidoOfSigungu(sigunguCode: String): Sido? = sidoBySigunguCode[sigunguCode]

    fun dongsIn(sigunguCode: String): List<Dong> = sigunguByCode[sigunguCode]?.dongs ?: emptyList()

    fun sigunguIn(sidoCode: String): List<Sigungu> = sidoByCode[sidoCode]?.sigungu ?: emptyList()
}
