/*
 * Runveal Design Tokens 코드 생성 (iOS/Swift)
 *
 * design/runveal-tokens.json(W3C Design Tokens 형식)을 읽어서
 * iosApp/iosApp/DesignSystem/Generated/Tokens 아래에 Swift 파일을 만든다 (아이콘·이미지 생성기와
 * 출력 폴더가 겹치지 않게 Generated 밑에서 한 번 더 갈라 둔다 — 이 태스크는 자기 출력 폴더를
 * 통째로 비우고 다시 쓴다).
 * Xcode가 빌드할 때 "Generate Runveal Design Tokens" Run Script 빌드 단계가
 * `./gradlew :generateRunvealTokensSwift`로 이 태스크를 호출한다
 * (iosApp/iosApp.xcodeproj/project.pbxproj). JSON만 바꾸고 다시 빌드하면 앱에 반영된다.
 * 생성된 파일은 직접 수정하지 않는다. gradle/runveal-tokens.gradle.kts(Kotlin용)의 Swift 버전.
 *
 * iosApp/iosApp/ 폴더 전체가 Xcode의 File System Synchronized Group이라
 * Generated 밑에 파일을 새로 만들어도 pbxproj를 따로 손대지 않고 바로 컴파일 대상에 들어간다.
 *
 * 생성 결과 (손으로 쓴 RunvealShadow.swift, RunvealTextStyle.swift 등과 같은 폴더에 공존)
 *  - RunvealPalette     ← primitive.color
 *  - RunvealSpacing     ← primitive.space
 *  - RunvealRadius      ← primitive.radius
 *  - RunvealColors      ← semantic.* (색상만, 그룹 구조 유지)
 *  - RunvealMapStroke   ← semantic.map.width
 *  - RunvealTypography  ← typography.* (RunvealTextStyle 값으로. 폰트 이름은 고정 매핑, swiftFontName 참고)
 *  - RunvealEffects     ← effect.* (RunvealShadow 값으로)
 *  - RunvealLayout      ← layout.*
 *
 * 이름 규칙: 경로를 camelCase로 이어 붙인다. (예: typography.heading.1-semibold → heading1Semibold)
 */

import groovy.json.JsonSlurper
import java.math.BigDecimal

abstract class GenerateRunvealTokensSwiftTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        @Suppress("UNCHECKED_CAST")
        val root = JsonSlurper().parse(tokensFile.get().asFile, "UTF-8") as Map<String, Any?>
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        RunvealSwiftTokenGenerator(root).files().forEach { (name, content) ->
            File(out, "$name.swift").writeText(content)
        }
    }
}

class RunvealSwiftTokenGenerator(private val root: Map<String, Any?>) {

    fun files(): Map<String, String> {
        val result = linkedMapOf<String, String>()
        group("primitive", "color")?.let { result["RunvealPalette"] = flatEnum("RunvealPalette", "Primitive colors (primitive.color). 화면 코드에서는 RunvealTheme.colors를 사용한다.", it, emptyList()) }
        group("primitive", "space")?.let { result["RunvealSpacing"] = flatEnum("RunvealSpacing", "Spacing (primitive.space)", it, listOf("space")) }
        group("primitive", "radius")?.let { result["RunvealRadius"] = flatEnum("RunvealRadius", "Radius (primitive.radius). Shape는 RunvealShapes", it, listOf("radius")) }
        group("semantic")?.let { result["RunvealColors"] = colorsFile(it) }
        group("semantic", "map", "width")?.let { result["RunvealMapStroke"] = flatEnum("RunvealMapStroke", "지도 위 길 선 두께 (semantic.map.width)", it, emptyList()) }
        group("typography")?.let { result["RunvealTypography"] = typographyFile(it) }
        group("effect")?.let { result["RunvealEffects"] = flatEnum("RunvealEffects", desc(it) ?: "Effects (effect)", it, emptyList()) }
        group("layout")?.let { result["RunvealLayout"] = flatEnum("RunvealLayout", desc(it) ?: "Layout (layout)", it, emptyList()) }
        return result
    }

    // ---------- tree helpers ----------

    @Suppress("UNCHECKED_CAST")
    private fun group(vararg path: String): Map<String, Any?>? {
        var cur: Any? = root
        for (p in path) cur = (cur as? Map<String, Any?>)?.get(p) ?: return null
        return cur as? Map<String, Any?>
    }

    private fun isToken(m: Map<String, Any?>) = m.containsKey("\$value")

    @Suppress("UNCHECKED_CAST")
    private fun children(m: Map<String, Any?>): List<Pair<String, Map<String, Any?>>> =
        m.entries.filter { !it.key.startsWith("$") && it.value is Map<*, *> }
            .map { it.key to (it.value as Map<String, Any?>) }

    private fun type(m: Map<String, Any?>) = m["\$type"] as? String

    private fun desc(m: Map<String, Any?>) = m["\$description"] as? String

    @Suppress("UNCHECKED_CAST")
    private fun ext(m: Map<String, Any?>): Map<String, Any?> =
        ((m["\$extensions"] as? Map<String, Any?>)?.get("com.runveal") as? Map<String, Any?>) ?: emptyMap()

    private fun isRef(v: Any?) = v is String && v.startsWith("{") && v.endsWith("}")

    // ---------- naming ----------

    private fun words(s: String) = s.split('-', '_', ' ').filter { it.isNotEmpty() }

    private fun camel(parts: List<String>): String {
        val ws = parts.flatMap { words(it) }
        val name = ws.mapIndexed { i, w ->
            if (i == 0) w.replaceFirstChar { it.lowercaseChar() } else w.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")
        return if (name.first().isDigit()) "_$name" else name
    }

    private fun pascal(key: String) = camel(listOf(key)).replaceFirstChar { it.uppercaseChar() }

    /** 토큰 leaf 이름이 Swift 예약어(예: default)와 겹치면 선언부에 백틱을 씌운다. 점(.) 뒤 참조는 백틱 없이도 된다. */
    private val swiftKeywords = setOf(
        "default", "class", "struct", "enum", "protocol", "extension", "func", "var", "let", "case",
        "self", "Self", "Type", "where", "as", "is", "in", "for", "while", "if", "else", "switch",
        "return", "break", "continue", "import", "internal", "private", "public", "static", "typealias",
        "guard", "repeat", "catch", "throw", "throws", "try", "init", "deinit", "subscript", "operator",
        "associatedtype", "fileprivate", "open", "rethrows", "super", "nil", "true", "false", "do",
    )

    private fun ident(name: String) = if (name in swiftKeywords) "`$name`" else name

    // ---------- literals ----------

    /** Swift는 Kotlin의 .dp/.sp 같은 단위 접미사가 없다. 음수도 괄호 없이 그냥 쓸 수 있다. */
    private fun num(n: Any?): String {
        val bd = BigDecimal(n.toString()).stripTrailingZeros()
        return if (bd.scale() <= 0) bd.toBigInteger().toString() else bd.toPlainString()
    }

    @Suppress("UNCHECKED_CAST")
    private fun dimValue(v: Any?): Any? = (v as Map<String, Any?>)["value"]

    @Suppress("UNCHECKED_CAST")
    private fun colorExpr(v: Any?): String {
        if (isRef(v)) {
            val path = (v as String).removePrefix("{").removeSuffix("}").split(".")
            require(path.size > 2 && path[0] == "primitive" && path[1] == "color") { "Unsupported color reference: $v" }
            return "RunvealPalette.${camel(path.drop(2))}"
        }
        val m = v as Map<String, Any?>
        val comps = m["components"] as List<Any?>
        val alpha = m["alpha"] ?: 1
        return "Color(red: ${num(comps[0])}, green: ${num(comps[1])}, blue: ${num(comps[2])}, opacity: ${num(alpha)})"
    }

    private fun blendMode(figma: String?): String? = when (figma) {
        null, "NORMAL", "PASS_THROUGH" -> null
        // SwiftUI에는 `.plus`가 없다. Figma의 Linear Dodge(= add)에 대응하는 건
        // `.plusLighter`다(`.plusDarker`는 반대 방향).
        "LINEAR_DODGE" -> ".plusLighter"
        "MULTIPLY" -> ".multiply"
        "SCREEN" -> ".screen"
        "OVERLAY" -> ".overlay"
        "DARKEN" -> ".darken"
        "LIGHTEN" -> ".lighten"
        "COLOR_DODGE" -> ".colorDodge"
        "COLOR_BURN" -> ".colorBurn"
        else -> error("Unsupported blend mode: $figma")
    }

    @Suppress("UNCHECKED_CAST")
    private fun shadowExpr(token: Map<String, Any?>, indent: String): String {
        val raw = token["\$value"]
        val layers = (if (raw is List<*>) raw else listOf(raw)) as List<Map<String, Any?>>
        val blendModes = ext(token)["blendModes"] as? List<String>
        val items = layers.mapIndexed { i, l ->
            val args = mutableListOf("color: ${colorExpr(l["color"])}", "blur: ${num(dimValue(l["blur"]))}")
            val ox = dimValue(l["offsetX"]); val oy = dimValue(l["offsetY"]); val sp = dimValue(l["spread"])
            if (ox != null && num(ox) != "0") args += "offsetX: ${num(ox)}"
            if (oy != null && num(oy) != "0") args += "offsetY: ${num(oy)}"
            if (sp != null && num(sp) != "0") args += "spread: ${num(sp)}"
            if (l["inset"] == true) args += "inset: true"
            blendMode(blendModes?.getOrNull(i))?.let { args += "blendMode: $it" }
            "$indent    RunvealShadow(${args.joinToString(", ")}),"
        }
        return "[\n${items.joinToString("\n")}\n$indent]"
    }

    /** 토큰 하나를 (Swift 타입, 식)으로. 지원하지 않는 타입은 null. */
    private fun valueOf(token: Map<String, Any?>, indent: String): Pair<String, String>? = when (type(token)) {
        "color" -> "Color" to colorExpr(token["\$value"])
        "dimension" -> "CGFloat" to num(dimValue(token["\$value"]))
        "shadow" -> "[RunvealShadow]" to shadowExpr(token, indent)
        else -> null
    }

    // ---------- output ----------

    private val header = "// 자동 생성 파일 - 직접 수정하지 마세요. 원본: design/runveal-tokens.json (gradle/runveal-tokens-swift.gradle.kts)\n"

    private fun kdoc(text: String?, indent: String): String =
        if (text.isNullOrBlank()) "" else "$indent/// ${text.replace("\n", " ")}\n"

    private fun fileStart() = "$header\nimport SwiftUI\n\n"

    /** 그룹 아래 모든 토큰을 경로 camelCase 이름으로 펼친 enum(네임스페이스). */
    private fun flatEnum(name: String, doc: String, group: Map<String, Any?>, prefix: List<String>): String {
        val sb = StringBuilder(fileStart())
        sb.append(kdoc(doc, ""))
        sb.append("enum $name {\n")
        fun walk(m: Map<String, Any?>, path: List<String>) {
            for ((k, child) in children(m)) {
                val p = path + k
                if (isToken(child)) {
                    val (t, expr) = valueOf(child, "    ") ?: continue
                    sb.append(kdoc(desc(child), "    "))
                    sb.append("    static let ${ident(camel(prefix + p))}: $t = $expr\n")
                } else {
                    walk(child, p)
                }
            }
        }
        walk(group, emptyList())
        sb.append("}\n")
        return sb.toString()
    }

    private fun hasColor(m: Map<String, Any?>): Boolean =
        if (isToken(m)) type(m) == "color" else children(m).any { hasColor(it.second) }

    private fun colorsFile(semantic: Map<String, Any?>): String {
        val sb = StringBuilder(fileStart())
        fun writeStruct(name: String, m: Map<String, Any?>, indent: String, doc: String?) {
            sb.append(kdoc(doc, indent))
            sb.append("${indent}struct $name {\n")
            val inner = "$indent    "
            val nested = mutableListOf<Pair<String, Map<String, Any?>>>()
            for ((k, child) in children(m)) {
                if (!hasColor(child)) continue
                if (isToken(child)) {
                    sb.append(kdoc(desc(child), inner))
                    sb.append("${inner}let ${ident(camel(listOf(k)))}: Color = ${colorExpr(child["\$value"])}\n")
                } else {
                    sb.append("${inner}let ${ident(camel(listOf(k)))}: ${pascal(k)} = ${pascal(k)}()\n")
                    nested += k to child
                }
            }
            for ((k, child) in nested) {
                sb.append("\n")
                writeStruct(pascal(k), child, inner, desc(child))
            }
            sb.append("$indent}\n")
        }
        writeStruct("RunvealColors", semantic, "", "Semantic colors (semantic.*). 다크 모드 하나만 있음. RunvealTheme.colors로 사용한다.")
        return sb.toString()
    }

    /**
     * (font.family 키, font.weight 값) -> iosApp/iosApp/DesignSystem/Fonts에 실제로 들어있는 폰트의
     * PostScript 이름. 폰트를 추가하면 Fonts 폴더에 파일을 넣고, Info.plist의 UIAppFonts에 등록하고,
     * 여기에도 매핑을 추가한다.
     */
    private fun swiftFontName(familyKey: String, weight: Int): String = when (familyKey to weight) {
        "pretendard" to 400 -> "Pretendard-Regular"
        "pretendard" to 600 -> "Pretendard-SemiBold"
        "geist" to 200 -> "Geist-ExtraLight"
        "geist" to 300 -> "Geist-Light"
        else -> error(
            "No embedded font mapped for family='$familyKey' weight=$weight. " +
                "iosApp/iosApp/DesignSystem/Fonts에 폰트를 추가하고 swiftFontName()에 매핑을 추가하세요."
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun typographyFile(typo: Map<String, Any?>): String {
        val sb = StringBuilder(fileStart())
        sb.append(kdoc(desc(typo) ?: "Typography (typography.*)", ""))
        sb.append("struct RunvealTypography {\n")
        fun walk(m: Map<String, Any?>, path: List<String>) {
            for ((k, child) in children(m)) {
                val p = path + k
                if (!isToken(child)) { walk(child, p); continue }
                if (type(child) != "typography") continue
                val v = child["\$value"] as Map<String, Any?>
                val familyKey = (v["fontFamily"] as String).removePrefix("{").removeSuffix("}").split(".").last()
                val weightRaw = v["fontWeight"]
                val weight = if (isRef(weightRaw)) {
                    val wPath = (weightRaw as String).removePrefix("{").removeSuffix("}").split(".")
                    (group(*wPath.toTypedArray())?.get("\$value") as Number).toInt()
                } else {
                    (weightRaw as Number).toInt()
                }
                val fontName = swiftFontName(familyKey, weight)
                val size = dimValue(v["fontSize"])
                val lineHeightPx = ext(child)["lineHeightPx"]
                    ?: (BigDecimal(size.toString()) * BigDecimal(v["lineHeight"].toString()))
                val letterSpacing = dimValue(v["letterSpacing"]) ?: 0
                sb.append(kdoc(desc(child), "    "))
                sb.append("    let ${ident(camel(p))} = RunvealTextStyle(\n")
                sb.append("        font: .custom(\"$fontName\", size: ${num(size)}),\n")
                sb.append("        lineHeight: ${num(lineHeightPx)},\n")
                sb.append("        tracking: ${num(letterSpacing)}\n")
                sb.append("    )\n")
            }
        }
        walk(typo, emptyList())
        sb.append("}\n")
        return sb.toString()
    }
}

val generateRunvealTokensSwift = tasks.register<GenerateRunvealTokensSwiftTask>("generateRunvealTokensSwift") {
    group = "runveal"
    description = "design/runveal-tokens.json → Swift 디자인 토큰 생성 (iOS, Xcode Run Script에서 호출)"
    tokensFile.set(layout.projectDirectory.file("design/runveal-tokens.json"))
    outputDir.set(layout.projectDirectory.dir("iosApp/iosApp/DesignSystem/Generated/Tokens"))
}
