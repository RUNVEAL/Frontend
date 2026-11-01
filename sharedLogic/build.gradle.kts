import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

fun authConfigValue(key: String): String = localProperties.getProperty(key) ?: System.getenv(key) ?: ""

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
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

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }

    android {
       namespace = "com.swyp.runveal.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
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
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
