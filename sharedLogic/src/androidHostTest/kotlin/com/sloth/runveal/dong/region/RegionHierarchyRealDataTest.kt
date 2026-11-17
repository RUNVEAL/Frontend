package com.sloth.runveal.dong.region

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `docs/map/refactoring/REGION_HIERARCHY.md` Step 1 — 실제 계층 파일(16/256/3,559)로 개수·조회를
 * 검증한다. commonTest는 모든 KMP 타겟에서 돌아 파일 IO가 안 되므로(iOS 등), 이 실데이터 검증만 JVM인
 * androidHostTest에 둔다 (`:sharedLogic:allTests`에 포함됨). 아직 asset으로 옮기기 전(Step 2 이전)이라
 * `docs/map/refactoring/`의 준비된 파일을 직접 읽는다 — `tools/dong-glow/golden/`을 읽는
 * `DongGlowGoldenTest`와 같은 패턴.
 */
class RegionHierarchyRealDataTest {

    private val hierarchy: RegionHierarchy by lazy {
        val repoRoot = findRepoRoot(File(".").absoluteFile)
        val jsonFile = File(repoRoot, "docs/map/refactoring/korea_region_hierarchy.json")
        RegionHierarchyParser.parse(jsonFile.readText())
    }

    @Test
    fun countsMatchHangJeongDong20261001() {
        val sigunguCount = hierarchy.sido.sumOf { it.sigungu.size }
        val dongCount = hierarchy.sido.sumOf { s -> s.sigungu.sumOf { it.dongs.size } }

        assertEquals(16, hierarchy.sido.size, "시도 개수")
        assertEquals(256, sigunguCount, "시군구 개수")
        assertEquals(3559, dongCount, "행정동 개수")
    }

    @Test
    fun jongnoLookupsResolveCorrectly() {
        assertEquals("종로구", hierarchy.sigunguOf("1111053000")?.name)
        assertEquals("서울특별시", hierarchy.sidoOf("1111053000")?.name)
    }

    @Test
    fun asanLookupResolvesCorrectly() {
        assertEquals("아산시", hierarchy.sigunguOf("4420057000")?.name)
        assertEquals("44200", hierarchy.sigunguOf("4420057000")?.code)
    }

    @Test
    fun suwonJanganHasParentCity() {
        val jangan = hierarchy.sigunguIn("41").first { it.code == "41111" }
        assertEquals("수원시", jangan.parentCity?.name)
        assertEquals("41110", jangan.parentCity?.code)
    }

    @Test
    fun everyDongBelongsToExactlyOneSigungu() {
        val dongCodeToSigunguCodes = mutableMapOf<String, MutableSet<String>>()
        for (s in hierarchy.sido) {
            for (sg in s.sigungu) {
                for (d in sg.dongs) {
                    dongCodeToSigunguCodes.getOrPut(d.code) { mutableSetOf() }.add(sg.code)
                }
            }
        }
        val duplicates = dongCodeToSigunguCodes.filterValues { it.size > 1 }
        assertEquals(emptyMap(), duplicates, "두 개 이상 시군구에 속한 행정동이 있으면 안 됨")
    }

    private fun findRepoRoot(start: File): File {
        var dir: File? = start
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile
        }
        error("settings.gradle.kts를 찾을 수 없음 (시작: $start)")
    }
}
