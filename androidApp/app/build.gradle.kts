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
    // Crashlytics 플러그인도 같은 조건 안에 둔다 — google-services 플러그인이 만들어 주는 Firebase
    // 앱 정보를 전제로 하므로, 밖으로 빼면 google-services.json 없는 사람·CI에서 빌드가 깨진다.
    //
    // 이 플러그인이 하는 일은 release의 R8 매핑 파일을 Crashlytics에 올려 주는 것이다(기본 켜짐).
    // 난독화된 스택트레이스를 읽을 수 있게 만드는 쪽이라 Crashlytics를 넣는 의미의 절반이 여기 있다 —
    // 대신 릴리즈 빌드에 매핑 업로드 네트워크 단계가 하나 붙는다.
    apply(plugin = libs.plugins.firebaseCrashlytics.get().pluginId)
}

dependencies {
    implementation(project(":androidApp:feature:main"))
    implementation(project(":androidApp:core:devicestate"))
    implementation(project(":androidApp:core:health"))
    implementation(project(":androidApp:core:permission"))
    implementation(project(":androidApp:core:tracking"))

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.maplibre.android.sdk)
    // 이 모듈의 AndroidManifest가 카카오 SDK의 AuthCodeHandlerActivity를 직접 선언하므로 여기서도
    // 의존성을 밝힌다. sharedLogic이 implementation으로 들고 있어 런타임에는 어차피 들어오지만,
    // 그건 전이 의존이라 lint가 못 보고(MissingClass 에러), sharedLogic이 카카오를 걷어내면
    // 매니페스트만 조용히 깨진다.
    implementation(libs.kakao.sdk.user)

    // Firebase Analytics. BOM이 SDK 버전을 맞춘다.
    //
    // 수집은 **릴리즈 빌드에서만** 켜진다 — AndroidManifest의
    // `firebase_analytics_collection_enabled`에 아래 buildTypes가 넣어 주는 값이 그 스위치다.
    //
    // google-services.json이 없으면 위 플러그인이 안 붙어 Firebase 자체가 초기화되지 않고,
    // 그러면 수집도 조용히 안 된다(앱은 그대로 돈다). 의존성만 조건부로 빼지 않는 이유는
    // 코드가 컴파일되는지는 파일 유무와 무관해야 하기 때문이다.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // Crashlytics. 수집은 Analytics와 달리 **모든 빌드에서** 켜 둔다(SDK 기본값) — 그래서
    // `firebase_crashlytics_collection_enabled` 스위치를 따로 두지 않는다. iOS도 Crashlytics는
    // 빌드 구분 없이 켜 둬서 두 플랫폼이 같은 걸 보고하고, 위에서 Analytics를 릴리즈로 묶은 이유
    // (개발 중 이벤트가 사용자 수·세션을 부풀린다)는 크래시엔 적용되지 않는다 — 개발 중 크래시도
    // 봐야 하는 쪽이다.
    //
    // MapLibre가 들고 오는 네이티브 라이브러리의 크래시는 심볼 없이 올라간다 —
    // firebase-crashlytics-ndk는 NDK 설치와 unstripped 라이브러리 경로 설정까지 끌고 와서 범위 밖.
    implementation(libs.firebase.crashlytics)
}

android {
    namespace = "com.sloth.runveal"

    defaultConfig {
        applicationId = "com.sloth.runveal"
        // CI가 실행 번호를 넘겨 자동으로 올린다(`-PversionCode=123`). 로컬 빌드는 1 고정.
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        // 마이페이지 "앱 버전 정보"에 그대로 보이는 값이라 semver 세 자리로 둔다(v1.0.0).
        versionName = providers.gradleProperty("versionName").orNull ?: "1.0.0"

        // 카카오 계정 로그인(카카오톡 미설치·로그인 실패 시 쓰는 웹 로그인)이 돌아올 주소
        // `kakao{NATIVE_APP_KEY}://oauth`를 AndroidManifest의 AuthCodeHandlerActivity에 꽂아 준다.
        // 키가 없으면 빈 문자열이라 스킴이 `kakao://oauth`가 되지만, 그 상태에선 애초에
        // KakaoSdk.init도 건너뛰어서(initAuthPlatform) 카카오 로그인 자체를 못 쓴다.
        manifestPlaceholders["kakaoNativeAppKey"] = buildSecret("KAKAO_NATIVE_APP_KEY") ?: ""

        // Firebase Analytics 수집 스위치(AndroidManifest의 meta-data로 들어간다). 기본은 **꺼짐**이고
        // 아래 release에서만 켠다 — 새 buildType을 만들어도 켜 달라고 적지 않는 한 수집되지 않는다.
        // 개발 중 기기에서 쏘는 이벤트가 지표에 섞이면 사용자 수·세션이 부풀어 보인다.
        manifestPlaceholders["analyticsCollectionEnabled"] = false
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
            // 배포로 나가는 빌드에서만 Analytics를 켠다(`build-debug.yml`이 assembleRelease로
            // 만들어 Firebase App Distribution에 올리는 그 APK다).
            manifestPlaceholders["analyticsCollectionEnabled"] = true
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
