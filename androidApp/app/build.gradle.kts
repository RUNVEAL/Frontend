import java.util.Properties

plugins {
    id("runveal.android.application")
}

/*
 * 서명 키·버전 같은 빌드 설정값은 local.properties(로컬) 또는 환경변수(CI)에서 읽는다 —
 * sharedLogic이 소셜 로그인 키를 읽는 방식과 같다. 저장소에는 아무 값도 들어가지 않는다.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun buildSecret(key: String): String? =
    localProperties.getProperty(key) ?: System.getenv(key)

/*
 * Firebase 연결(google-services 플러그인). google-services.json을 읽어 `google_app_id` 같은 리소스를
 * 만들어 주고, 그 위에 Analytics·Crashlytics 같은 SDK를 붙일 수 있게 된다.
 *
 * 파일이 있을 때만 붙이는 이유: google-services.json은 저장소에 없다(.gitignore). 무조건 붙이면
 * 파일을 아직 안 받은 사람이나 CI에서 빌드가 통째로 깨진다. Firebase 콘솔에서 받아 이 폴더
 * (`androidApp/app/`)에 두면 그때부터 적용된다.
 */
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
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
        // CI가 실행 번호를 넘겨 자동으로 올린다(`-PversionCode=123`). 로컬 빌드는 1 고정.
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "1.0"
    }

    /*
     * 업로드 키 서명. 키스토어가 있을 때만 설정을 만들고, 없으면 release도 debug 키로 서명된다
     * (= 로컬에서 키 없이도 `assembleRelease`가 돌아간다). 다만 **debug 키로 서명된 배포본은
     * 카카오·구글 로그인이 실패한다** — 콘솔에 등록된 SHA가 아니기 때문이다. 배포용 빌드는 반드시
     * 키스토어를 넣고 돌려야 한다.
     */
    signingConfigs {
        val storeFileName = buildSecret("RELEASE_STORE_FILE")
        val storeFile = storeFileName?.let { file(it) }?.takeIf { it.exists() }
        if (storeFile != null) {
            create("release") {
                this.storeFile = storeFile
                storePassword = buildSecret("RELEASE_STORE_PASSWORD")
                keyAlias = buildSecret("RELEASE_KEY_ALIAS")
                keyPassword = buildSecret("RELEASE_KEY_PASSWORD")
            }
        } else if (storeFileName != null) {
            logger.warn("RELEASE_STORE_FILE=$storeFileName 파일이 없습니다 — release가 debug 키로 서명됩니다.")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            // R8로 코드 축소·난독화 + 안 쓰는 리소스 제거. 규칙은 proguard-rules.pro 참고 —
            // 라이브러리 대부분은 자기 consumer 규칙을 들고 오고, 리플렉션을 쓰는
            // kotlinx.serialization(서버 모델 + 네비게이션 라우트)만 직접 지켜 준다.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
