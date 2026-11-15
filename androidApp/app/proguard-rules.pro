# R8(= 이 프로젝트의 난독화·축소기) 규칙. AGP 기본 규칙(proguard-android-optimize.txt) 위에 얹힌다.
#
# 라이브러리 대부분(AndroidX·Compose·MapLibre·Kakao SDK 등)은 AAR 안에 자기 consumer 규칙을 넣어 와서
# 여기 다시 쓸 필요가 없다. 여기엔 **그렇게 따라오지 않는 것들**만 적는다 — 주로 런타임에 리플렉션을
# 쓰는 kotlinx.serialization 계열이다.

# ─────────────────────────── 크래시 스택 가독성 ───────────────────────────
# 난독화된 스택에서도 줄 번호를 남긴다. 원본 파일명은 가려서(renamesourcefileattribute) 클래스 이름이
# 새어 나가지 않게 한다. mapping.txt로 복원할 수 있다.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─────────────────────────── kotlinx.serialization ───────────────────────────
# 공식 권장 규칙(kotlinx.serialization README). @Serializable 클래스의 Companion/serializer()는
# 런타임에 이름으로 찾기 때문에, 지워지거나 이름이 바뀌면 직렬화가 통째로 깨진다.
# 이 앱에서 영향받는 곳: 서버 API 모델(MetaResponse·RouteDto·MatchResponse 등), 로컬 저장 포맷,
# 그리고 **Navigation Compose의 타입 안전 라우트**(RunVealRoute — @Serializable object로 선언돼 있고
# NavHost가 직렬화로 목적지를 식별한다).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable object(라우트가 전부 이 형태) — INSTANCE와 serializer()를 같이 남긴다.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ─────────────────────────── 카카오 로그인 SDK ───────────────────────────
# 카카오 SDK는 에러 원인 enum을 이름으로 찾는다(`Class.getField("TokenNotFound")`). 난독화되면
# 앱 시작 직후 `NoSuchFieldException: TokenNotFound`로 죽는다 — 실제로 R8을 켜자마자 재현됐다.
# 모델 클래스는 Gson으로 역직렬화하므로 필드 이름도 같이 지켜야 한다(카카오 공식 가이드 규칙).
-keep class com.kakao.sdk.**.model.* { <fields>; }
-keep class * extends com.google.gson.TypeAdapter

# ─────────────────────────── 경고 억제 ───────────────────────────
# Ktor가 참조하지만 안드로이드 빌드엔 안 들어오는 선택적 의존성들. 실제로 안 쓰이므로 경고만 끈다.
-dontwarn org.slf4j.**
-dontwarn kotlinx.atomicfu.**
-dontwarn io.ktor.client.engine.darwin.**
