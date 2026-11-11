import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

fun networkConfigValue(key: String): String = localProperties.getProperty(key) ?: System.getenv(key) ?: ""

plugins {
    id("runveal.android.library")
}

android {
    namespace = "com.swyp.runveal.core.network"
}

// local.properties(network.baseUrl)를 읽어 NetworkConfig.kt 생성 — sharedLogic의 generateValhallaConfig와
// 같은 패턴(BuildConfig 대신 Kotlin object로 생성해서 로컬 전용 값이 git에 안 들어가게 한다). 서버 주소는
// 빌드 스크립트에 기본값으로 박아두지 않고 local.properties에만 둔다 — 거기 없으면 빈 문자열이다.
val networkConfigFileContent = """
    |// 자동 생성 파일 - 직접 수정하지 마세요. 원본: local.properties (androidApp/core/network/build.gradle.kts)
    |package com.swyp.runveal.network
    |
    |/** local.properties의 network.baseUrl 값. */
    |object NetworkConfig {
    |    const val BASE_URL: String = "${networkConfigValue("network.baseUrl")}"
    |}
    |
""".trimMargin()

abstract class GenerateNetworkConfigTask : DefaultTask() {
    @get:Input
    abstract val fileContent: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val pkgDir = outputDir.get().asFile.resolve("com/swyp/runveal/network")
        pkgDir.mkdirs()
        pkgDir.resolve("NetworkConfig.kt").writeText(fileContent.get())
    }
}

val generateNetworkConfig = tasks.register<GenerateNetworkConfigTask>("generateNetworkConfig") {
    group = "runveal"
    description = "local.properties -> NetworkConfig.kt"
    fileContent.set(networkConfigFileContent)
    outputDir.set(layout.buildDirectory.dir("generated/networkConfig/kotlin"))
}

// AGP의 소스셋 API는 Kotlin 소스 디렉터리에 Provider/TaskProvider를 직접 받지 않는다(static/generated
// 구분이 안 돼서) — 그래서 resolve된 디렉터리 값을 넘기고, 생성 순서는 preBuild 의존으로 따로 보장한다.
android {
    sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/networkConfig/kotlin").get())
}

tasks.named("preBuild") {
    dependsOn(generateNetworkConfig)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.okhttp)
}
