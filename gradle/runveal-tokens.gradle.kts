/*
 * Runveal Design Tokens 코드 생성
 *
 * design/runveal-tokens.json(W3C Design Tokens 형식)을 읽어서
 * build/generated/runvealTokens/commonMain/kotlin 아래에 Kotlin 파일을 만든다.
 * JSON만 바꾸고 빌드하면 앱에 반영된다. 생성된 파일은 직접 수정하지 않는다.
 *
 * 생성 결과 (package com.swyp.runveal.designsystem)
 *  - RunvealPalette     ← primitive.color
 *  - RunvealSpacing     ← primitive.space
 *  - RunvealRadius      ← primitive.radius
 *  - RunvealColors      ← semantic.* (색상만, 그룹 구조 유지)
 *  - RunvealMapStroke   ← semantic.map.width
 *  - RunvealTypography  ← typography.* (font.family는 생성자 파라미터)
 *  - RunvealEffects     ← effect.*
 *  - RunvealLayout      ← layout.*
 *
 * 이름 규칙: 경로를 camelCase로 이어 붙인다. (예: typography.heading.1-semibold → heading1Semibold)
 */

import groovy.json.JsonSlurper
import java.math.BigDecimal

abstract class GenerateRunvealTokensTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensFile: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        @Suppress("UNCHECKED_CAST")
        val root = JsonSlurper().parse(tokensFile.get().asFile, "UTF-8") as Map<String, Any?>
        val pkg = packageName.get()
        val out = outputDir.get().asFile
        out.deleteRecursively()
        val pkgDir = File(out, pkg.replace('.', '/')).apply { mkdirs() }
        RunvealTokenGenerator(root, pkg).files().forEach { (name, content) ->
            File(pkgDir, "$name.kt").writeText(content)
        }
    }
}

class RunvealTokenGenerator(private val root: Map<String, Any?>, private val pkg: String) {

    fun files(): Map<String, String> {
        val result = linkedMapOf<String, String>()
        group("primitive", "color")?.let { result["RunvealPalette"] = flatObject("RunvealPalette", "Primitive colors (primitive.color). 화면 코드에서는 RunvealTheme.colors를 사용한다.", it, emptyList()) }
        group("primitive", "space")?.let { result["RunvealSpacing"] = flatObject("RunvealSpacing", "Spacing (primitive.space)", it, listOf("space")) }
        group("primitive", "radius")?.let { result["RunvealRadius"] = flatObject("RunvealRadius", "Radius (primitive.radius). Shape는 RunvealShapes", it, listOf("radius")) }
        group("semantic")?.let { result["RunvealColors"] = colorsFile(it) }
        group("semantic", "map", "width")?.let { result["RunvealMapStroke"] = flatObject("RunvealMapStroke", "지도 위 길 선 두께 (semantic.map.width)", it, emptyList()) }
        group("typography")?.let { result["RunvealTypography"] = typographyFile(it) }
        group("effect")?.let { result["RunvealEffects"] = flatObject("RunvealEffects", desc(it) ?: "Effects (effect)", it, emptyList()) }
        group("layout")?.let { result["RunvealLayout"] = flatObject("RunvealLayout", desc(it) ?: "Layout (layout)", it, emptyList()) }
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

    private fun resolveRef(ref: String): Map<String, Any?> {
        val path = ref.removePrefix("{").removeSuffix("}").split(".")
        return group(*path.toTypedArray()) ?: error("Unknown token reference: $ref")
    }

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

    // ---------- literals ----------

    private fun num(n: Any?): String {
        val bd = BigDecimal(n.toString()).stripTrailingZeros()
        return if (bd.scale() <= 0) bd.toBigInteger().toString() else bd.toPlainString()
    }

    private fun unit(n: Any?, suffix: String): String {
        val s = num(n)
        return if (s.startsWith("-")) "($s).$suffix" else "$s.$suffix"
    }

    private fun floatLit(n: Any?) = num(n) + "f"

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
        val hex = (m["hex"] as String).removePrefix("#").uppercase()
        val alpha = BigDecimal(m["alpha"]?.toString() ?: "1")
        val base = "Color(0xFF$hex)"
        return if (alpha.compareTo(BigDecimal.ONE) == 0) base else "$base.copy(alpha = ${floatLit(alpha)})"
    }

    private fun blendMode(figma: String?) = when (figma) {
        null, "NORMAL", "PASS_THROUGH" -> null
        "LINEAR_DODGE" -> "BlendMode.Plus"
        "MULTIPLY" -> "BlendMode.Multiply"
        "SCREEN" -> "BlendMode.Screen"
        "OVERLAY" -> "BlendMode.Overlay"
        "DARKEN" -> "BlendMode.Darken"
        "LIGHTEN" -> "BlendMode.Lighten"
        "COLOR_DODGE" -> "BlendMode.ColorDodge"
        "COLOR_BURN" -> "BlendMode.ColorBurn"
        else -> error("Unsupported blend mode: $figma")
    }

    @Suppress("UNCHECKED_CAST")
    private fun shadowExpr(token: Map<String, Any?>, indent: String): String {
        val raw = token["\$value"]
        val layers = (if (raw is List<*>) raw else listOf(raw)) as List<Map<String, Any?>>
        val blendModes = ext(token)["blendModes"] as? List<String>
        val items = layers.mapIndexed { i, l ->
            val args = mutableListOf("color = ${colorExpr(l["color"])}", "blur = ${unit(dimValue(l["blur"]), "dp")}")
            val ox = dimValue(l["offsetX"]); val oy = dimValue(l["offsetY"]); val sp = dimValue(l["spread"])
            if (ox != null && num(ox) != "0") args += "offsetX = ${unit(ox, "dp")}"
            if (oy != null && num(oy) != "0") args += "offsetY = ${unit(oy, "dp")}"
            if (sp != null && num(sp) != "0") args += "spread = ${unit(sp, "dp")}"
            if (l["inset"] == true) args += "inset = true"
            blendMode(blendModes?.getOrNull(i))?.let { args += "blendMode = $it" }
            "$indent    RunvealShadow(${args.joinToString(", ")}),"
        }
        return "listOf(\n${items.joinToString("\n")}\n$indent)"
    }

    /** 토큰 하나를 (Kotlin 타입, 식)으로. 지원하지 않는 타입은 null. */
    private fun valueOf(token: Map<String, Any?>, indent: String): Pair<String, String>? = when (type(token)) {
        "color" -> "Color" to colorExpr(token["\$value"])
        "dimension" -> "Dp" to unit(dimValue(token["\$value"]), "dp")
        "shadow" -> "List<RunvealShadow>" to shadowExpr(token, indent)
        else -> null
    }

    // ---------- output ----------

    private val header = "// 자동 생성 파일 - 직접 수정하지 마세요. 원본: design/runveal-tokens.json (gradle/runveal-tokens.gradle.kts)\n"

    private fun kdoc(text: String?, indent: String): String =
        if (text.isNullOrBlank()) "" else "$indent/** ${text.replace("*/", "* /")} */\n"

    private val imports = """
        |import androidx.compose.runtime.Immutable
        |import androidx.compose.ui.graphics.BlendMode
        |import androidx.compose.ui.graphics.Color
        |import androidx.compose.ui.text.TextStyle
        |import androidx.compose.ui.text.font.FontFamily
        |import androidx.compose.ui.text.font.FontWeight
        |import androidx.compose.ui.text.style.LineHeightStyle
        |import androidx.compose.ui.unit.Dp
        |import androidx.compose.ui.unit.dp
        |import androidx.compose.ui.unit.sp
    """.trimMargin()

    private fun fileStart() = "$header@file:Suppress(\"unused\", \"RedundantVisibilityModifier\")\n\npackage $pkg\n\n$imports\n\n"

    /** 그룹 아래 모든 토큰을 경로 camelCase 이름으로 펼친 object. */
    private fun flatObject(name: String, doc: String, group: Map<String, Any?>, prefix: List<String>): String {
        val sb = StringBuilder(fileStart())
        sb.append(kdoc(doc, ""))
        sb.append("object $name {\n")
        fun walk(m: Map<String, Any?>, path: List<String>) {
            for ((k, child) in children(m)) {
                val p = path + k
                if (isToken(child)) {
                    val (t, expr) = valueOf(child, "    ") ?: continue
                    sb.append(kdoc(desc(child), "    "))
                    sb.append("    val ${camel(prefix + p)}: $t = $expr\n")
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
        fun writeClass(name: String, m: Map<String, Any?>, indent: String, doc: String?) {
            sb.append(kdoc(doc, indent))
            sb.append("${indent}@Immutable\n${indent}class $name internal constructor() {\n")
            val inner = "$indent    "
            val nested = mutableListOf<Pair<String, Map<String, Any?>>>()
            for ((k, child) in children(m)) {
                if (!hasColor(child)) continue
                if (isToken(child)) {
                    sb.append(kdoc(desc(child), inner))
                    sb.append("${inner}val ${camel(listOf(k))}: Color = ${colorExpr(child["\$value"])}\n")
                } else {
                    sb.append("${inner}val ${camel(listOf(k))}: ${pascal(k)} = ${pascal(k)}()\n")
                    nested += k to child
                }
            }
            for ((k, child) in nested) {
                sb.append("\n")
                writeClass(pascal(k), child, inner, desc(child))
            }
            sb.append("$indent}\n")
        }
        writeClass("RunvealColors", semantic, "", "Semantic colors (semantic.*). 다크 모드 하나만 있음. RunvealTheme.colors로 사용한다.")
        return sb.toString()
    }

    @Suppress("UNCHECKED_CAST")
    private fun typographyFile(typo: Map<String, Any?>): String {
        val families = group("font", "family")?.let { children(it).map { (k, _) -> k } } ?: emptyList()
        val sb = StringBuilder(fileStart())
        sb.append("private val RunvealLineHeightStyle = LineHeightStyle(\n")
        sb.append("    alignment = LineHeightStyle.Alignment.Center,\n")
        sb.append("    trim = LineHeightStyle.Trim.None,\n")
        sb.append(")\n\n")
        sb.append(kdoc(desc(typo) ?: "Typography (typography.*)", ""))
        sb.append("@Immutable\nclass RunvealTypography(\n")
        families.forEach { sb.append("    ${camel(listOf(it))}: FontFamily,\n") }
        sb.append(") {\n")
        fun walk(m: Map<String, Any?>, path: List<String>) {
            for ((k, child) in children(m)) {
                val p = path + k
                if (!isToken(child)) { walk(child, p); continue }
                if (type(child) != "typography") continue
                val v = child["\$value"] as Map<String, Any?>
                val family = (v["fontFamily"] as String).let { ref ->
                    camel(listOf(ref.removePrefix("{").removeSuffix("}").split(".").last()))
                }
                val weight = v["fontWeight"].let { w -> if (isRef(w)) resolveRef(w as String)["\$value"] else w }
                val size = dimValue(v["fontSize"])
                val lineHeightPx = ext(child)["lineHeightPx"]
                    ?: (BigDecimal(size.toString()) * BigDecimal(v["lineHeight"].toString()))
                val letterSpacing = dimValue(v["letterSpacing"]) ?: 0
                sb.append(kdoc(desc(child), "    "))
                sb.append("    val ${camel(p)}: TextStyle = TextStyle(\n")
                sb.append("        fontFamily = $family,\n")
                sb.append("        fontWeight = FontWeight(${num(weight)}),\n")
                sb.append("        fontSize = ${unit(size, "sp")},\n")
                sb.append("        lineHeight = ${unit(lineHeightPx, "sp")},\n")
                sb.append("        letterSpacing = ${unit(letterSpacing, "sp")},\n")
                sb.append("        lineHeightStyle = RunvealLineHeightStyle,\n")
                sb.append("    )\n")
            }
        }
        walk(typo, emptyList())
        sb.append("}\n")
        return sb.toString()
    }
}

val generateRunvealTokens = tasks.register<GenerateRunvealTokensTask>("generateRunvealTokens") {
    group = "runveal"
    description = "design/runveal-tokens.json → Kotlin 디자인 토큰 생성"
    tokensFile.set(rootProject.layout.projectDirectory.file("design/runveal-tokens.json"))
    packageName.set("com.swyp.runveal.designsystem")
    outputDir.set(layout.buildDirectory.dir("generated/runvealTokens/commonMain/kotlin"))
}

// Android Studio Gradle Sync 때도 생성되도록
tasks.matching { it.name == "prepareKotlinIdeaImport" }.configureEach {
    dependsOn(generateRunvealTokens)
}
