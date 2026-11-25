/*
 * Runveal 이미지(PNG 등 래스터) 공유 (iOS/Swift)
 *
 * design/images 안의 파일을 iosApp/iosApp/DesignSystem/Generated/Images로 복사하고, 이름을 안전하게
 * 참조할 수 있는 RunvealImages.swift를 함께 만든다. gradle/runveal-images.gradle.kts(Android용)의
 * iOS 버전 — Android는 같은 원본을 drawable-xxxhdpi 리소스로 복사한다.
 *
 * **크기는 호출하는 쪽에서 반드시 지정해야 한다.** 원본 PNG는 Android xxxhdpi(4배) 기준인데 iOS는
 * @3x까지만 있어서 배율 접미사로 맞출 수가 없다. 그래서 1배 이미지로 들어가고(예: empty_course.png
 * 320x320 → 자연 크기 320pt), Android가 `Modifier.size(80.dp)`로 감싸 둔 것과 같은 크기를
 * `.frame(width:height:)`로 직접 줘야 한다.
 *
 * Xcode는 File System Synchronized Group 안의 리소스를 번들 루트에 평평하게 복사하므로, 하위 폴더에
 * 있어도 파일 이름만으로 찾을 수 있다. 다만 **`Image("splash")`로는 안 뜬다** — SwiftUI의
 * `Image(_:bundle:)`은 에셋 카탈로그만 보고, 카탈로그 밖 낱개 파일은 조용히 빈 칸으로 남긴다.
 * 그래서 UIKit 조회를 거치는 `RunvealBundleImage`(iosApp/iosApp/Core/RunvealBundleImage.swift)를
 * 부르는 코드를 만든다.
 */

import java.io.File

abstract class GenerateRunvealImagesSwiftTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val imagesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val sourceDir = imagesDir.get().asFile
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()

        val images = sourceDir.listFiles { f -> f.isFile && f.extension.lowercase() in RASTER_EXTENSIONS }
            ?.sortedBy { it.name } ?: emptyList()

        images.forEach { image -> image.copyTo(File(out, image.name), overwrite = true) }
        File(out, "RunvealImages.swift").writeText(toSwift(images))
    }

    private fun toSwift(images: List<File>): String {
        val body = images.joinToString("\n\n") { image ->
            val name = image.nameWithoutExtension
            """
            |    /// design/images/${image.name} — 크기는 호출하는 쪽에서 `.frame(width:height:)`로 지정한다.
            |    static var ${propertyName(name)}: Image { RunvealBundleImage.image("$name", "${image.extension.lowercase()}") }
            """.trimMargin()
        }
        return """
            |// 자동 생성 파일 - 직접 수정하지 마세요. 원본: design/images/* (gradle/runveal-images-swift.gradle.kts)
            |
            |import SwiftUI
            |
            |/// design/images의 래스터 이미지.
            |enum RunvealImages {
            |$body
            |}
            |
        """.trimMargin()
    }

    private fun propertyName(fileName: String): String {
        val words = fileName.split('-', '_', ' ').filter { it.isNotEmpty() }
        return words.mapIndexed { index, word ->
            if (index == 0) word.replaceFirstChar { it.lowercaseChar() } else word.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")
    }

    private companion object {
        val RASTER_EXTENSIONS = setOf("png", "jpg", "jpeg")
    }
}

val generateRunvealImagesSwift = tasks.register<GenerateRunvealImagesSwiftTask>("generateRunvealImagesSwift") {
    group = "runveal"
    description = "design/images/* -> iOS 번들 리소스 + RunvealImages.swift"
    imagesDir.set(rootProject.layout.projectDirectory.dir("design/images"))
    outputDir.set(rootProject.layout.projectDirectory.dir("iosApp/iosApp/DesignSystem/Generated/Images"))
}
