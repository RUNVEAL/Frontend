package com.sloth.runveal.core.map.dong.region

import com.sloth.runveal.dong.DongBoundaryParser
import com.sloth.runveal.dong.region.RegionHierarchyParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `korea_sigungu`/`korea_sido` 경계의 `adm_cd2` 집합이 `korea_region_hierarchy.json`의 코드 집합과
 * 정확히 같은지 확인한다. 셋 다 `tools/gen_region_assets.py`가 **같은 서버 `index.geojson`에서**
 * 만들어 내므로 어긋나면 안 된다 — 어긋나면 줌아웃 시 테두리는 있는데 완주율이 안 붙거나 그 반대가
 * 된다. 동 경계는 더 이상 번들에 없다(서버에서만 받는다).
 */
class RegionHierarchyConsistencyTest {

    private val assetsDir = File(findRepoRoot(File(".").absoluteFile), "androidApp/core/map/src/main/assets/dong")

    @Test
    fun everyDongCodeBelongsToItsSigunguAndSidoByPrefix() {
        val hierarchy = RegionHierarchyParser.parse(File(assetsDir, "korea_region_hierarchy.json").readText())

        val dongCodes = hierarchy.sido.flatMap { s -> s.sigungu.flatMap { sg -> sg.dongs.map { it.code } } }
        assertEquals(3556, dongCodes.size)

        // 행정동 코드 앞 5자리=시군구, 앞 2자리=시도 규칙이 전 동에서 성립해야 한다 — 완주율 집계가
        // 이 계층을 타고 동→시군구→시도로 올라간다.
        hierarchy.sido.forEach { sido ->
            sido.sigungu.forEach { sigungu ->
                assertEquals(sido.code, sigungu.code.take(2))
                sigungu.dongs.forEach { dong -> assertEquals(sigungu.code, dong.code.take(5)) }
            }
        }
    }

    @Test
    fun sigunguBoundaryCodesMatchHierarchyExactly() {
        val sigunguCodes = DongBoundaryParser.parse(File(assetsDir, "korea_sigungu.geojson").readText())
            .map { it.admCd2 }
            .toSet()
        val hierarchy = RegionHierarchyParser.parse(File(assetsDir, "korea_region_hierarchy.json").readText())
        val hierarchySigunguCodes = hierarchy.sido.flatMap { s -> s.sigungu.map { it.code } }.toSet()

        assertEquals(252, sigunguCodes.size)
        assertEquals(hierarchySigunguCodes, sigunguCodes)
    }

    @Test
    fun sidoBoundaryCodesMatchHierarchyExactly() {
        val sidoCodes = DongBoundaryParser.parse(File(assetsDir, "korea_sido.geojson").readText())
            .map { it.admCd2 }
            .toSet()
        val hierarchy = RegionHierarchyParser.parse(File(assetsDir, "korea_region_hierarchy.json").readText())
        val hierarchySidoCodes = hierarchy.sido.map { it.code }.toSet()

        assertEquals(17, sidoCodes.size)
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
