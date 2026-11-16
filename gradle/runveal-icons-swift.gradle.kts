/*
 * Runveal 아이콘 코드 생성 (iOS/Swift)
 *
 * design/icons 안의 svg 파일(전부 24x24, <path>/<circle>만 쓰는 단순한 stroke/fill 아이콘)을 읽어서
 * iosApp/iosApp/DesignSystem/Generated/Icons/RunvealIcons.swift를 만든다. Xcode가 빌드할 때
 * "Generate Runveal Design Tokens" Run Script 빌드 단계가 이 태스크를 함께 호출한다
 * (iosApp/iosApp.xcodeproj/project.pbxproj). gradle/runveal-icons.gradle.kts(Android용)의 Swift 버전.
 *
 * Android는 SVG를 Vector Drawable XML로 바꾸지만, iOS는 패스 문자열을 그대로 Swift 상수로 내려서
 * 런타임에 RunvealSvgPath가 SwiftUI Path로 파싱한다(iosApp/iosApp/DesignSystem/RunvealIcon.swift).
 * 이렇게 하면 Android의 `Icon(tint = ...)` / `tint = Color.Unspecified`와 같은 의미의 tint 처리를
 * 호출하는 쪽에서 그대로 쓸 수 있다 — Asset Catalog의 template/original 렌더링 모드는 에셋마다
 * 하나로 고정돼서 같은 아이콘을 어떤 화면에선 틴트하고 어떤 화면에선 원본으로 쓸 수가 없다.
 *
 * 파일명(확장자 제외)이 그대로 lowerCamelCase 프로퍼티 이름이 된다
 * (예: my-location.svg → RunvealIcons.myLocation). 자동 생성 토큰(RunvealPalette.gold100 등)과
 * 같은 이름 규칙이다.
 *
 * RunvealSvgPath가 지원하는 명령(M/L/H/V/C/Z, 소문자 포함) 밖의 명령이 들어오면 여기서 빌드를
 * 실패시킨다 — 런타임에 아이콘이 조용히 깨지는 대신 아이콘을 추가한 사람이 바로 알게 한다.
 */

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

abstract class GenerateRunvealIconsSwiftTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val iconsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val svgFiles = iconsDir.get().asFile.listFiles { f -> f.extension == "svg" }
            ?.sortedBy { it.name } ?: emptyList()

        val out = outputDir.get().asFile.apply { mkdirs() }
        val icons = svgFiles.map { RunvealSwiftIconParser.parse(it) }
        File(out, "RunvealIcons.swift").writeText(RunvealSwiftIconParser.toSwift(icons))
    }
}

/** SVG(<path>/<circle>만) → Swift `RunvealIconSpec` 상수. */
object RunvealSwiftIconParser {

    /** iosApp/iosApp/DesignSystem/RunvealIcon.swift의 RunvealSvgPath가 파싱할 수 있는 명령. */
    private val SUPPORTED_PATH_COMMANDS = "MmLlHhVvCcZz".toSet()

    data class Layer(
        val geometry: String,
        val fillColor: String?,
        val strokeColor: String?,
        val strokeWidth: String?,
        val strokeLineCap: String?,
        val strokeLineJoin: String?,
    )

    data class Icon(val key: String, val layers: List<Layer>)

    fun parse(file: File): Icon {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file)
        val layers = mutableListOf<Layer>()
        val children = doc.documentElement.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i) as? Element ?: continue
            when (node.tagName) {
                "path" -> {
                    val d = node.getAttribute("d")
                    validatePathCommands(file, d)
                    layers += layerOf(node, "path: \"$d\"")
                }
                "circle" -> {
                    val cx = num(node.getAttribute("cx").toDouble())
                    val cy = num(node.getAttribute("cy").toDouble())
                    val r = num(node.getAttribute("r").toDouble())
                    layers += layerOf(node, "circle: RunvealIconLayer.Circle(cx: $cx, cy: $cy, r: $r)")
                }
                else -> error("${file.name}: 지원하지 않는 SVG 엘리먼트 <${node.tagName}> — path/circle만 지원")
            }
        }
        return Icon(key = file.nameWithoutExtension, layers = layers)
    }

    private fun validatePathCommands(file: File, pathData: String) {
        val unsupported = pathData.filter { it.isLetter() && it !in "eE" }.filterNot { it in SUPPORTED_PATH_COMMANDS }
        require(unsupported.isEmpty()) {
            "${file.name}: iOS 아이콘 렌더러(RunvealSvgPath)가 지원하지 않는 SVG path 명령 " +
                "'${unsupported.toSortedSet().joinToString("")}' — M/L/H/V/C/Z만 지원한다. " +
                "SVG를 그 명령만 쓰도록 내보내거나(Figma: '단순화'), RunvealSvgPath에 해당 명령을 추가해야 한다."
        }
    }

    private fun layerOf(node: Element, geometry: String) = Layer(
        geometry = geometry,
        fillColor = node.getAttribute("fill").ifBlank { null }?.takeIf { it != "none" },
        strokeColor = node.getAttribute("stroke").ifBlank { null }?.takeIf { it != "none" },
        strokeWidth = node.getAttribute("stroke-width").ifBlank { null },
        strokeLineCap = node.getAttribute("stroke-linecap").ifBlank { null },
        strokeLineJoin = node.getAttribute("stroke-linejoin").ifBlank { null },
    )

    private fun num(d: Double): String {
        val s = d.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    /** `#RRGGBB` / `#AARRGGBB` → 자동 생성 토큰과 같은 `Color(red:green:blue:opacity:)` 리터럴. */
    private fun colorExpr(hex: String): String {
        val raw = hex.removePrefix("#")
        require(raw.length == 6 || raw.length == 8) { "지원하지 않는 색 형식: $hex (#RRGGBB / #AARRGGBB만 지원)" }
        val hasAlpha = raw.length == 8
        val alpha = if (hasAlpha) raw.substring(0, 2).toInt(16) / 255.0 else 1.0
        val offset = if (hasAlpha) 2 else 0
        val r = raw.substring(offset, offset + 2).toInt(16) / 255.0
        val g = raw.substring(offset + 2, offset + 4).toInt(16) / 255.0
        val b = raw.substring(offset + 4, offset + 6).toInt(16) / 255.0
        fun f(v: Double) = String.format("%.4f", v).trimEnd('0').trimEnd('.').ifEmpty { "0" }
        return "Color(red: ${f(r)}, green: ${f(g)}, blue: ${f(b)}, opacity: ${f(alpha)})"
    }

    private fun lineCapExpr(svg: String?): String? = when (svg) {
        null, "butt" -> null
        "round" -> ".round"
        "square" -> ".square"
        else -> error("지원하지 않는 stroke-linecap: $svg")
    }

    private fun lineJoinExpr(svg: String?): String? = when (svg) {
        null, "miter" -> null
        "round" -> ".round"
        "bevel" -> ".bevel"
        else -> error("지원하지 않는 stroke-linejoin: $svg")
    }

    private fun propertyName(key: String): String {
        val words = key.split('-', '_', ' ').filter { it.isNotEmpty() }
        return words.mapIndexed { index, word ->
            if (index == 0) word.replaceFirstChar { it.lowercaseChar() } else word.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")
    }

    fun toSwift(icons: List<Icon>): String {
        val body = icons.joinToString("\n") { icon ->
            val layers = icon.layers.joinToString("\n") { layer ->
                val args = mutableListOf(layer.geometry)
                layer.fillColor?.let { args += "fill: ${colorExpr(it)}" }
                layer.strokeColor?.let { args += "stroke: ${colorExpr(it)}" }
                layer.strokeWidth?.let { args += "strokeWidth: $it" }
                lineCapExpr(layer.strokeLineCap)?.let { args += "lineCap: $it" }
                lineJoinExpr(layer.strokeLineJoin)?.let { args += "lineJoin: $it" }
                "        RunvealIconLayer(${args.joinToString(", ")}),"
            }
            """
            |    /// design/icons/${icon.key}.svg
            |    static let ${propertyName(icon.key)} = RunvealIconSpec(layers: [
            |$layers
            |    ])
            """.trimMargin()
        }
        return """
            |// 자동 생성 파일 - 직접 수정하지 마세요. 원본: design/icons/*.svg (gradle/runveal-icons-swift.gradle.kts)
            |
            |import SwiftUI
            |
            |/// design/icons의 아이콘. `RunvealIcon(RunvealIcons.home, tint: ...)`으로 그린다.
            |enum RunvealIcons {
            |$body
            |}
            |
        """.trimMargin()
    }
}

val generateRunvealIconsSwift = tasks.register<GenerateRunvealIconsSwiftTask>("generateRunvealIconsSwift") {
    group = "runveal"
    description = "design/icons/*.svg -> RunvealIcons.swift (iOS)"
    iconsDir.set(rootProject.layout.projectDirectory.dir("design/icons"))
    outputDir.set(rootProject.layout.projectDirectory.dir("iosApp/iosApp/DesignSystem/Generated/Icons"))
}
