import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

fun authConfigValue(key: String): String = localProperties.getProperty(key) ?: System.getenv(key) ?: ""

plugins {
    id("runveal.kmp.library")
    id("runveal.kmp.ios.framework")
    // `runveal.kotlin.serialization`은 단일 플랫폼(Android/JVM) 모듈의 flat "implementation" 설정을
    // 전제로 해서 KMP 모듈(소스셋별 설정, commonMainImplementation 등)인 sharedLogic에는 못 쓴다.
    // kotlinx-serialization-json 런타임은 이미 commonMain.dependencies에 있으니 컴파일러 플러그인만
    // 바로 적용한다 — @Serializable 데이터 클래스(RegionHierarchyParser)를 쓰려면 이게 필요하다.
    id("org.jetbrains.kotlin.plugin.serialization")
    id("app.cash.sqldelight")
}

// docs/local-db/local-db-schema-design.md §2 패키지 배치.
sqldelight {
    databases {
        create("RunvealDatabase") {
            packageName.set("com.swyp.runveal.db")
        }
    }
}

// local.properties(KAKAO_NATIVE_APP_KEY 등)를 읽어 androidMain에 AuthConfig.kt 생성.
// 값이 비어 있어도 빌드는 그대로 성공한다 (런타임에만 필요).
val authConfigKeys = listOf(
    "KAKAO_NATIVE_APP_KEY",
    "GOOGLE_WEB_CLIENT_ID",
)

abstract class GenerateAuthConfigTask : DefaultTask() {
    @get:Input
    abstract val fileContent: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val pkgDir = outputDir.get().asFile.resolve("com/swyp/runveal/auth")
        pkgDir.mkdirs()
        pkgDir.resolve("AuthConfig.kt").writeText(fileContent.get())
    }
}

val authConfigFileContent = """
    |// 자동 생성 파일 - 직접 수정하지 마세요. 원본: local.properties (sharedLogic/build.gradle.kts)
    |package com.swyp.runveal.auth
    |
    |/** local.properties에서 읽은 소셜 로그인/Firebase 설정값. 값은 로컬 전용이며 git에 커밋되지 않는다. */
    |object AuthConfig {
    |${authConfigKeys.joinToString("\n    |") { key -> "const val $key: String = \"${authConfigValue(key)}\"" }}
    |}
    |
""".trimMargin()

val generateAuthConfig = tasks.register<GenerateAuthConfigTask>("generateAuthConfig") {
    group = "runveal"
    description = "local.properties -> AuthConfig.kt (androidMain)"
    fileContent.set(authConfigFileContent)
    outputDir.set(layout.buildDirectory.dir("generated/authConfig/androidMain/kotlin"))
}

// local.properties(network.baseUrl)를 읽어 commonMain에 NetworkConfig.kt 생성 — androidApp/core/network의
// 같은 파일을 그대로 옮긴 것(androidApp/core/network는 Android 전용이라 iOS에서 API를 못 썼다).
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

val networkConfigFileContent = """
    |// 자동 생성 파일 - 직접 수정하지 마세요. 원본: local.properties (sharedLogic/build.gradle.kts)
    |package com.swyp.runveal.network
    |
    |/** local.properties의 network.baseUrl 값. 끝에 "/" 없는 형태로 넣는다 — 호출부가 "${'$'}BASE_URL/api/..."처럼 직접 이어 붙인다. */
    |object NetworkConfig {
    |    const val BASE_URL: String = "${authConfigValue("network.baseUrl")}"
    |}
    |
""".trimMargin()

val generateNetworkConfig = tasks.register<GenerateNetworkConfigTask>("generateNetworkConfig") {
    group = "runveal"
    description = "local.properties -> NetworkConfig.kt (commonMain)"
    fileContent.set(networkConfigFileContent)
    outputDir.set(layout.buildDirectory.dir("generated/networkConfig/commonMain/kotlin"))
}

kotlin {
    android {
       namespace = "com.swyp.runveal.sharedLogic"
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        commonMain.configure {
            kotlin.srcDir(generateNetworkConfig)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
        }
        androidMain.configure {
            kotlin.srcDir(generateAuthConfig)
        }
        androidMain.dependencies {
            implementation(libs.kakao.sdk.user)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.playServicesAuth)
            implementation(libs.googleid)
            implementation(libs.androidx.security.crypto)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
