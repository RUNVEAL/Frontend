plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(buildLogicLibs.android.gradlePlugin)
    compileOnly(buildLogicLibs.kotlin.gradlePlugin)
    compileOnly(buildLogicLibs.compose.compiler.gradlePlugin)
    compileOnly(buildLogicLibs.kotlin.serialization.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "runveal.android.library"
            implementationClass = "runveal.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "runveal.android.library.compose"
            implementationClass = "runveal.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "runveal.android.application"
            implementationClass = "runveal.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "runveal.kotlin.serialization"
            implementationClass = "runveal.buildlogic.KotlinSerializationConventionPlugin"
        }
        register("kmpLibrary") {
            id = "runveal.kmp.library"
            implementationClass = "runveal.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpIosFramework") {
            id = "runveal.kmp.ios.framework"
            implementationClass = "runveal.buildlogic.KmpIosFrameworkConventionPlugin"
        }
    }
}
