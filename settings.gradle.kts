rootProject.name = "Runveal"

pluginManagement {
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

include(":androidApp")
include(":sharedLogic")
