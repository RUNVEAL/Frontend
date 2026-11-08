package com.swyp.runveal.dong.region

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegionHierarchyParserTest {

    // 실제 파일의 일부(종로구, 수원시 장안구 — parentCity 있는 사례)를 그대로 옮긴 소형 fixture.
    private val sampleJson = """
        {
          "version": "HangJeongDong_ver20261001",
          "sido": [
            { "code": "11", "name": "서울특별시", "sigungu": [
              { "code": "11110", "name": "종로구", "dongs": [
                { "code": "1111053000", "name": "사직동" },
                { "code": "1111054000", "name": "삼청동" }
              ] }
            ]},
            { "code": "41", "name": "경기도", "sigungu": [
              { "code": "41111", "name": "수원시 장안구",
                "parentCity": { "code": "41110", "name": "수원시" },
                "dongs": [
                  { "code": "4111156000", "name": "파장동" }
                ]
              },
              { "code": "41113", "name": "수원시 팔달구", "dongs": [
                { "code": "4111354000", "name": "매산동" }
              ]}
            ]}
          ]
        }
    """.trimIndent()

    @Test
    fun parsesSigunguAndSidoLookups() {
        val hierarchy = RegionHierarchyParser.parse(sampleJson)

        val jongno = hierarchy.sigunguOf("1111053000")
        assertEquals("종로구", jongno?.name)
        assertEquals("서울특별시", hierarchy.sidoOf("1111053000")?.name)
        assertEquals("서울특별시", hierarchy.sidoOfSigungu("11110")?.name)
    }

    @Test
    fun parsesParentCityOnlyForGeneralDistricts() {
        val hierarchy = RegionHierarchyParser.parse(sampleJson)

        val jangan = hierarchy.sigunguOf("4111156000")
        assertEquals("수원시", jangan?.parentCity?.name)
        assertEquals("41110", jangan?.parentCity?.code)

        val paldal = hierarchy.sigunguOf("4111354000")
        assertNull(paldal?.parentCity)
    }

    @Test
    fun dongsInReturnsAllDongsOfThatSigungu() {
        val hierarchy = RegionHierarchyParser.parse(sampleJson)

        val dongs = hierarchy.dongsIn("11110")
        assertEquals(listOf("사직동", "삼청동"), dongs.map { it.name })
    }

    @Test
    fun sigunguInReturnsAllSigunguOfThatSido() {
        val hierarchy = RegionHierarchyParser.parse(sampleJson)

        val sigunguNames = hierarchy.sigunguIn("41").map { it.name }
        assertEquals(listOf("수원시 장안구", "수원시 팔달구"), sigunguNames)
    }

    @Test
    fun unknownCodesReturnNullOrEmpty() {
        val hierarchy = RegionHierarchyParser.parse(sampleJson)

        assertNull(hierarchy.sigunguOf("0000000000"))
        assertNull(hierarchy.sidoOf("0000000000"))
        assertEquals(emptyList(), hierarchy.dongsIn("00000"))
        assertEquals(emptyList(), hierarchy.sigunguIn("00"))
    }
}
