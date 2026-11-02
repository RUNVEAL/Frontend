rootProject.name = "Runveal"

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Kakao SDK for Android - Maven Central에 없음
        maven("https://devrepo.kakao.com/nexus/content/groups/public/") {
            mavenContent {
                includeGroup("com.kakao.sdk")
            }
        }
    }
}

include(":androidApp:app")
include(":androidApp:core:designsystem")
include(":androidApp:core:navigation")
include(":androidApp:core:permission")
include(":androidApp:feature:home")
include(":androidApp:feature:main")
include(":sharedLogic")
