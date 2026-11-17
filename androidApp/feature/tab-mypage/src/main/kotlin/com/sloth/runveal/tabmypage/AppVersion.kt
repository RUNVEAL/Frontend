package com.sloth.runveal.tabmypage

import android.content.Context

/**
 * 설치된 앱의 버전. `BuildConfig`는 모듈마다 따로 생성돼서 라이브러리 모듈에서는 앱 버전을 알 수
 * 없다 — PackageManager로 실제 설치본의 값을 읽는다.
 */
internal fun Context.appVersionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
        .getOrNull()
        ?.let { "v$it" }
        .orEmpty()
