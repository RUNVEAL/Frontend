package com.swyp.runveal.core.map.dong.glow

import com.swyp.runveal.core.map.internal.glow.DongGlowEffects
import com.swyp.runveal.dong.DongBoundaryParser
import com.swyp.runveal.dong.glow.DongGlowRenderer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * `docs/DONG_GLOW_ON_DEVICE.md` Step 2 — `tools/dong-glow/gen_dong_glow.py`가 만든
 * `tools/dong-glow/golden/`과 `sharedLogic`의 Kotlin 구현 결과를 비교한다. `tools/dong-glow/`는
 * 건드리지 않고 읽기만 한다.
 */
class DongGlowGoldenTest {

    private val repoRoot: File = findRepoRoot(File(".").absoluteFile)
    private val boundaryGeoJson: String by lazy {
        File(repoRoot, "androidApp/core/map/src/main/assets/dong/korea_dong.geojson").readText()
    }
    private val goldenDir = File(repoRoot, "tools/dong-glow/golden")
    private val golden: GoldenRoot by lazy {
        Json { ignoreUnknownKeys = true }.decodeFromString<GoldenRoot>(File(goldenDir, "golden.json").readText())
    }

    // Step 3의 운영 변환 코드를 그대로 쓴다 — 골든 테스트가 그 코드까지 같이 검증한다.
    private val strokeWidthsPx = DongGlowEffects.strokeWidthPxByLevel
    private val effectsByLevel = DongGlowEffects.effectsByLevel

    @Test
    fun matchesPythonGoldenImages() {
        val admCd2List = listOf("44200101", "4420025300", "44200118", "11680101")
        val partsByAdmCd2 = DongBoundaryParser.parsePolygonParts(boundaryGeoJson)

        val report = StringBuilder()
        val failures = mutableListOf<String>()

        for (admCd2 in admCd2List) {
            val parts = partsByAdmCd2[admCd2] ?: error("$admCd2 폴리곤을 korea_dong.geojson에서 못 찾음")
            val goldenDong = golden.dongs[admCd2] ?: error("$admCd2 가 golden.json에 없음")
            report.append("## ${goldenDong.name} ($admCd2)\n")

            for (level in 1..6) {
                val goldenLevel = goldenDong.levels[level.toString()] ?: error("$admCd2 level=$level 골든 없음")
                val result = DongGlowRenderer.render(parts, strokeWidthsPx[level], effectsByLevel[level])

                val sizeDiffW = abs(result.width - goldenLevel.size[0])
                val sizeDiffH = abs(result.height - goldenLevel.size[1])
                val sizeOk = sizeDiffW <= 1 && sizeDiffH <= 1

                // 1e-5도 같은 고정 각도 기준 대신, 이 동의 실제 해상도(이미지 px / 경위도 폭)로 환산한
                // "몇 px 어긋났는지"로 비교한다. 폴리라벨 수렴점이 Python과 달라 내접원이 0.1~0.3% 정도
                // 갈리는 건 정상이고(둘 다 같은 느슨한 정밀도 공식을 쓰므로), 중요한 건 화면에 보이는 크기다.
                val lonSpanDeg = abs(goldenLevel.corners[1][0] - goldenLevel.corners[0][0])
                val latSpanDeg = abs(goldenLevel.corners[0][1] - goldenLevel.corners[3][1])
                val pxPerDegLon = result.width / lonSpanDeg
                val pxPerDegLat = result.height / latSpanDeg
                val cornerDiffsPx = result.corners.zip(goldenLevel.corners).map { (ours, golden) ->
                    maxOf(abs(ours.lon - golden[0]) * pxPerDegLon, abs(ours.lat - golden[1]) * pxPerDegLat)
                }
                val cornerMaxDiffPx = cornerDiffsPx.max()
                val cornersOk = cornerMaxDiffPx < 1.0

                val goldenImage = ImageIO.read(File(goldenDir, "${admCd2}_$level.png"))
                var pixelAvg = Double.NaN
                var pixelP99 = Double.NaN
                var pixelOk = false
                // 크기가 ±1px 어긋나도(내접원 계산 오차 허용) 겹치는 영역만 비교한다.
                if (sizeOk) {
                    val diffs = pixelDiffs(result.rgba, result.width, result.height, goldenImage)
                    pixelAvg = diffs.average()
                    pixelP99 = percentile(diffs, 99.0)
                    pixelOk = pixelAvg <= 3.0 && pixelP99 <= 12.0
                }

                report.append(
                    "  level=$level size=${result.width}x${result.height}(golden ${goldenLevel.size[0]}x${goldenLevel.size[1]}) " +
                        "inradius=${"%.2f".format(result.inradiusM)}(golden ${goldenLevel.inradius_m}) " +
                        "cornerMaxDiffPx=${"%.3f".format(cornerMaxDiffPx)} pixelAvg=${"%.3f".format(pixelAvg)} pixelP99=${"%.3f".format(pixelP99)}\n",
                )

                if (!sizeOk) failures += "$admCd2 level=$level 크기 불일치 (diffW=$sizeDiffW diffH=$sizeDiffH)"
                if (!cornersOk) failures += "$admCd2 level=$level corners 오차 초과 (${cornerMaxDiffPx}px)"
                if (sizeOk && !pixelOk) failures += "$admCd2 level=$level 픽셀 오차 초과 (avg=$pixelAvg p99=$pixelP99)"
            }
        }

        val reportFile = File("build/dong-glow-golden-report.txt").apply { parentFile?.mkdirs() }
        reportFile.writeText(report.toString())
        println(report.toString())

        assertTrue("골든 테스트 실패:\n${failures.joinToString("\n")}\n\n전체 결과는 $reportFile 참고", failures.isEmpty())
    }

    /**
     * 진단 전용(문서 Step 2: "다르면 Python 쪽 inradius_m을 주입해서 한 번 더 비교해 원인을 분리한다").
     * 폴리라벨이 계산한 내접원 대신 golden.json의 `inradius_m`을 그대로 넣었을 때 corners가 1e-5도
     * 안으로 들어오면, corner 오차의 원인이 배치·코너 공식이 아니라 폴리라벨 수렴 정밀도라는 뜻이다.
     */
    @Test
    fun cornersMatchWhenGoldenInradiusIsInjected() {
        val admCd2List = listOf("44200101", "4420025300", "44200118", "11680101")
        val partsByAdmCd2 = DongBoundaryParser.parsePolygonParts(boundaryGeoJson)
        val report = StringBuilder()
        var maxDiff = 0.0

        for (admCd2 in admCd2List) {
            val parts = partsByAdmCd2[admCd2] ?: error("$admCd2 폴리곤을 못 찾음")
            val goldenDong = golden.dongs[admCd2] ?: error("$admCd2 가 golden.json에 없음")
            for (level in 1..6) {
                val goldenLevel = goldenDong.levels[level.toString()] ?: error("$admCd2 level=$level 골든 없음")
                val result = DongGlowRenderer.render(
                    parts, strokeWidthsPx[level], effectsByLevel[level],
                    inradiusOverrideM = goldenLevel.inradius_m,
                )
                val cornerMaxDiff = result.corners.zip(goldenLevel.corners).maxOf { (ours, golden) ->
                    maxOf(abs(ours.lon - golden[0]), abs(ours.lat - golden[1]))
                }
                maxDiff = maxOf(maxDiff, cornerMaxDiff)
                report.append("${goldenDong.name} level=$level cornerMaxDiff=${"%.8f".format(cornerMaxDiff)}\n")
            }
        }
        println(report.toString())
        assertTrue(
            "inradius를 주입해도 corner 오차($maxDiff)가 남음 -> 배치/코너 공식 자체에 문제가 있을 수 있음",
            maxDiff < 1e-5,
        )
    }

    /**
     * [ourRgba]([ourWidth]x[ourHeight])와 [golden]의 크기가 ±1px 달라도(내접원 계산 오차 허용), 둘 다
     * 좌상단 기준 같은 좌표계라 겹치는 영역(min(width), min(height))만 비교하면 된다.
     */
    private fun pixelDiffs(ourRgba: FloatArray, ourWidth: Int, ourHeight: Int, golden: java.awt.image.BufferedImage): DoubleArray {
        val w = minOf(ourWidth, golden.width)
        val h = minOf(ourHeight, golden.height)
        val diffs = DoubleArray(w * h * 4)
        var k = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = (y * ourWidth + x) * 4
                val ourR = (ourRgba[idx] * 255f)
                val ourG = (ourRgba[idx + 1] * 255f)
                val ourB = (ourRgba[idx + 2] * 255f)
                val ourA = (ourRgba[idx + 3] * 255f)

                val argb = golden.getRGB(x, y)
                val goldenA = (argb ushr 24) and 0xFF
                val goldenR = (argb ushr 16) and 0xFF
                val goldenG = (argb ushr 8) and 0xFF
                val goldenB = argb and 0xFF

                val alphaWeight = maxOf(ourA, goldenA.toFloat()) / 255.0
                diffs[k++] = abs(ourR - goldenR) * alphaWeight
                diffs[k++] = abs(ourG - goldenG) * alphaWeight
                diffs[k++] = abs(ourB - goldenB) * alphaWeight
                diffs[k++] = abs(ourA - goldenA).toDouble()
            }
        }
        return diffs
    }

    private fun percentile(values: DoubleArray, p: Double): Double {
        val sorted = values.sortedArray()
        val index = ((p / 100.0) * (sorted.size - 1)).roundToInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
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

@Serializable
private data class GoldenRoot(val quality: Double, val dongs: Map<String, GoldenDong>)

@Serializable
private data class GoldenDong(val name: String, val levels: Map<String, GoldenLevel>)

@Serializable
private data class GoldenLevel(
    val corners: List<List<Double>>,
    val inradius_m: Double,
    val quality: Double,
    val size: List<Int>,
)
