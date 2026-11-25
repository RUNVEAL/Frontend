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
include(":androidApp:core:analytics")
include(":androidApp:core:designsystem")
include(":androidApp:core:devicestate")
include(":androidApp:core:health")
include(":androidApp:core:map")
include(":androidApp:core:navigation")
include(":androidApp:core:permission")
include(":androidApp:core:tracking")
include(":androidApp:core:ui")
include(":androidApp:core:ui-core")
include(":androidApp:feature:course-recommend")
include(":androidApp:feature:home")
include(":androidApp:feature:main")
include(":androidApp:feature:onboarding")
include(":androidApp:feature:running")
include(":androidApp:feature:sign")
include(":androidApp:feature:tab-course")
include(":androidApp:feature:tab-home")
include(":androidApp:feature:tab-mypage")
include(":androidApp:feature:tab-record")
include(":sharedLogic")
