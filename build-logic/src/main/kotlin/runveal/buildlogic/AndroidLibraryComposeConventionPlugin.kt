package runveal.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<LibraryExtension> {
            compileSdk = catalogLibs.version("android-compileSdk").toInt()
            defaultConfig {
                minSdk = catalogLibs.version("android-minSdk").toInt()
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
            buildFeatures {
                compose = true
            }
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        }

        dependencies {
            add("implementation", catalogLibs.findLibrary("compose-runtime").get())
            add("implementation", catalogLibs.findLibrary("compose-foundation").get())
            add("implementation", catalogLibs.findLibrary("compose-ui").get())
            add("implementation", catalogLibs.findLibrary("compose-uiToolingPreview").get())
            add("debugImplementation", catalogLibs.findLibrary("compose-uiTooling").get())
        }
    }
}
