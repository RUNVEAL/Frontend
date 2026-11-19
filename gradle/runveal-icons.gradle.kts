/*
 * Runveal 아이콘 코드 생성
 *
 * design/icons 안의 svg 파일(<path>/<circle>만 쓰는 단순한 stroke/fill 아이콘)을 읽어서
 * androidApp/core/designsystem에 두 가지를 만든다.
 *  - Android Vector Drawable XML (build/generated/runvealIcons/res/drawable/ic_<name>.xml)
 *  - RunvealIcons.kt (build/generated/runvealIcons/kotlin) — 아이콘마다
 *    `@Composable val X: ImageVector` 하나씩, PascalCase 이름.
 *
 * design/runveal-components.json의 `icons` 섹션(figma id, size, stroke, color 등)은 문서/추적용
 * 메타데이터일 뿐 이 생성기가 읽지는 않는다 — design/icons 폴더에 있는 SVG 파일 전부를 대상으로
 * 생성한다. 파일명(확장자 제외)이 그대로 PascalCase 프로퍼티 이름이 된다
 * (예: my-location.svg → RunvealIcons.MyLocation).
 */

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

abstract class GenerateRunvealIconsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val iconsDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val resOutputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val kotlinOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val pkg = packageName.get()
        val svgFiles = iconsDir.get().asFile.listFiles { f -> f.extension == "svg" }
            ?.sortedBy { it.name } ?: emptyList()

        val resDir = resOutputDir.get().asFile.resolve("drawable").apply { deleteRecursively(); mkdirs() }
        val kotlinDir = kotlinOutputDir.get().asFile.resolve(pkg.replace('.', '/')).apply { deleteRecursively(); mkdirs() }

        val properties = StringBuilder()
        for (svg in svgFiles) {
            val icon = RunvealIconParser.parse(svg)
            val resourceName = "ic_" + icon.key.replace('-', '_')
            resDir.resolve("$resourceName.xml").writeText(RunvealIconParser.toVectorDrawableXml(icon))

            val propertyName = icon.key.split('-', '_').filter { it.isNotEmpty() }
                .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            properties.append("    val $propertyName: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.$resourceName)\n")
        }

        kotlinDir.resolve("RunvealIcons.kt").writeText(
            """
            |// 자동 생성 파일 - 직접 수정하지 마세요. 원본: design/icons/*.svg (gradle/runveal-icons.gradle.kts)
            |package $pkg
            |
            |import androidx.compose.runtime.Composable
            |import androidx.compose.ui.graphics.vector.ImageVector
            |import androidx.compose.ui.res.vectorResource
            |
            |object RunvealIcons {
            |$properties}
            |
            """.trimMargin(),
        )
    }
}

/** SVG(<path>/<circle>만) → Android Vector Drawable 변환. */
object RunvealIconParser {
    data class Layer(
        val pathData: String,
        val fillColor: String?,
        val strokeColor: String?,
        val strokeWidth: String?,
        val strokeLineCap: String?,
        val strokeLineJoin: String?,
    )

    data class Icon(val key: String, val layers: List<Layer>, val width: String, val height: String)

    fun parse(file: File): Icon {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file)
        val layers = mutableListOf<Layer>()
        val children = doc.documentElement.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i) as? Element ?: continue
            when (node.tagName) {
                "path" -> layers += layerOf(node, node.getAttribute("d"))
                "circle" -> {
                    val cx = node.getAttribute("cx").toDouble()
                    val cy = node.getAttribute("cy").toDouble()
                    val r = node.getAttribute("r").toDouble()
                    val d = "M${num(cx - r)} ${num(cy)}A${num(r)} ${num(r)} 0 1 0 ${num(cx + r)} ${num(cy)}" +
                        "A${num(r)} ${num(r)} 0 1 0 ${num(cx - r)} ${num(cy)}Z"
                    layers += layerOf(node, d)
                }
                else -> error("${file.name}: 지원하지 않는 SVG 엘리먼트 <${node.tagName}> — path/circle만 지원")
            }
        }
        // viewBox("0 0 W H")를 그대로 캔버스 크기로 쓴다 — 대부분 24x24지만 그렇지 않은 아이콘도 있다
        // (지도 위 방향 화살표 polygon2는 16x15). 없으면 width/height, 그것도 없으면 24.
        val viewBox = doc.documentElement.getAttribute("viewBox").trim().split(Regex("[ ,]+"))
        val width = viewBox.getOrNull(2) ?: doc.documentElement.getAttribute("width").ifBlank { "24" }
        val height = viewBox.getOrNull(3) ?: doc.documentElement.getAttribute("height").ifBlank { "24" }
        return Icon(key = file.nameWithoutExtension, layers = layers, width = width, height = height)
    }

    private fun layerOf(node: Element, pathData: String) = Layer(
        pathData = pathData,
        fillColor = node.getAttribute("fill").ifBlank { null },
        strokeColor = node.getAttribute("stroke").ifBlank { null },
        strokeWidth = node.getAttribute("stroke-width").ifBlank { null },
        strokeLineCap = node.getAttribute("stroke-linecap").ifBlank { null },
        strokeLineJoin = node.getAttribute("stroke-linejoin").ifBlank { null },
    )

    private fun num(d: Double): String {
        val s = d.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    fun toVectorDrawableXml(icon: Icon): String {
        val paths = icon.layers.joinToString("\n") { layer ->
            buildString {
                append("    <path\n")
                append("        android:pathData=\"${layer.pathData}\"")
                layer.fillColor?.let { append("\n        android:fillColor=\"$it\"") }
                layer.strokeColor?.let { append("\n        android:strokeColor=\"$it\"") }
                layer.strokeWidth?.let { append("\n        android:strokeWidth=\"$it\"") }
                layer.strokeLineCap?.let { append("\n        android:strokeLineCap=\"$it\"") }
                layer.strokeLineJoin?.let { append("\n        android:strokeLineJoin=\"$it\"") }
                append(" />")
            }
        }
        return """
            |<?xml version="1.0" encoding="utf-8"?>
            |<!-- 자동 생성 파일 - 직접 수정하지 마세요. 원본: design/icons/${icon.key}.svg -->
            |<vector xmlns:android="http://schemas.android.com/apk/res/android"
            |    android:width="${icon.width}dp"
            |    android:height="${icon.height}dp"
            |    android:viewportWidth="${icon.width}"
            |    android:viewportHeight="${icon.height}">
            |$paths
            |</vector>
            |
        """.trimMargin()
    }
}

val generateRunvealIcons = tasks.register<GenerateRunvealIconsTask>("generateRunvealIcons") {
    group = "runveal"
    description = "design/icons/*.svg -> Android Vector Drawable + RunvealIcons.kt"
    iconsDir.set(rootProject.layout.projectDirectory.dir("design/icons"))
    packageName.set("com.sloth.runveal.designsystem")
    resOutputDir.set(layout.buildDirectory.dir("generated/runvealIcons/res"))
    kotlinOutputDir.set(layout.buildDirectory.dir("generated/runvealIcons/kotlin"))
}
