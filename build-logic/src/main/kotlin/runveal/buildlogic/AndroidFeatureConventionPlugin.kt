package runveal.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * feature 모듈 전용. Compose 라이브러리 기본 세팅([AndroidLibraryComposeConventionPlugin])에
 * MVI 공통 의존성(core:ui-core, ViewModel/Lifecycle Compose)을 얹는다.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("runveal.android.library.compose")

        dependencies {
            add("implementation", project(":androidApp:core:ui-core"))
            add("implementation", catalogLibs.findLibrary("androidx-lifecycle-viewmodelCompose").get())
            add("implementation", catalogLibs.findLibrary("androidx-lifecycle-runtimeCompose").get())
        }
    }
}
