plugins {
    id("runveal.android.application")
}

dependencies {
    implementation(project(":androidApp:feature:main"))
    implementation(project(":androidApp:core:permission"))
    implementation(project(":androidApp:core:tracking"))

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.maplibre.android.sdk)
}

android {
    namespace = "com.swyp.runveal"

    defaultConfig {
        applicationId = "com.swyp.runveal"
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
