package com.swyp.runveal.core.map.dong.region

import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.dong.region.RegionHierarchyParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `docs/map/refactoring/REGION_HIERARCHY.md` Step 2 — `korea_dong`/`korea_sigungu`/`korea_sido`의
 * `adm_cd2` 집합이 `korea_region_hierarchy.json`의 코드 집합과 정확히 같은지 확인한다(세 경계가
 * 같은 원본에서 나왔다는 걸 코드 레벨로도 검증).
 */
class RegionHierarchyConsistencyTest {

    private val assetsDir = File(findRepoRoot(File(".").absoluteFile), "androidApp/core/map/src/main/assets/dong")

    @Test
    fun everyDongCodeInBoundaryFileExistsInHierarchy() {
        val dongCodes = DongBoundaryParser.parse(File(assetsDir, "korea_dong.geojson").readText())
            .map { it.admCd2 }
            .toSet()
        val hierarchy = RegionHierarchyParser.parse(File(assetsDir, "korea_region_hierarchy.json").readText())
        val hierarchyDongCodes = hierarchy.sido.flatMap { s -> s.sigungu.flatMap { sg -> sg.dongs.map { it.code } } }.toSet()

        assertEquals(3559, dongCodes.size)
        assertEquals(hierarchyDongCodes, dongCodes)
    }

    @Test
    fun sigunguBoundaryCodesMatchHierarchyExactly() {
        val sigunguCodes = DongBoundaryParser.parse(File(assetsDir, "korea_sigungu.geojson").readText())
            .map { it.admCd2 }
            .toSet()
        val hierarchy = RegionHierarchyParser.parse(File(assetsDir, "korea_region_hierarchy.json").readText())
        val hierarchySigunguCodes = hierarchy.sido.flatMap { s -> s.sigungu.map { it.code } }.toSet()

        assertEquals(256, sigunguCodes.size)
        assertEquals(hierarchySigunguCodes, sigunguCodes)
    }

    @Test
    fun sidoBoundaryCodesMatchHierarchyExactly() {
        val sidoCodes = DongBoundaryParser.parse(File(assetsDir, "korea_sido.geojson").readText())
            .map { it.admCd2 }
            .toSet()
        val hierarchy = RegionHierarchyParser.parse(File(assetsDir, "korea_region_hierarchy.json").readText())
        val hierarchySidoCodes = hierarchy.sido.map { it.code }.toSet()

        assertEquals(16, sidoCodes.size)
        assertEquals(hierarchySidoCodes, sidoCodes)
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
