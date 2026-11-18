package com.sloth.runveal.ui.web

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.sloth.runveal.designsystem.component.overlay.RunvealLoading
import com.sloth.runveal.designsystem.theme.RunvealTheme

/**
 * 웹에 올려 둔 문서(약관·개인정보 처리방침)를 띄우는 웹뷰.
 *
 * 자바스크립트는 켜지 않는다(WebView 기본값) — 정적 HTML이라 필요 없고, 꺼 두는 쪽이 안전하다.
 *
 * 로드가 끝나기 전까지 캔버스 색으로 덮인 채 로딩 모션만 보인다. 배경색을 지정하지 않으면 로드
 * 직전에 흰 화면이 한 번 번쩍인다 — 앱이 다크 단일 테마라 특히 눈에 띈다.
 *
 * 원문 HTML이 이미 다크 테마라 앱 안에서 겉돌지 않는다(실기기 확인). 원문을 밝은 배경으로 바꾸면
 * 이 화면만 희게 뜨므로, 그때는 원문에 `prefers-color-scheme`을 넣는 쪽이 낫다 — 앱에서
 * 강제 다크닝(`androidx.webkit`)을 걸면 글자가 뭉개진다.
 *
 * 마이페이지의 약관 화면과 Health Connect 권한 근거 화면이 같이 쓴다.
 */
@Composable
fun RunvealWebView(url: String, modifier: Modifier = Modifier) {
    var isLoading by remember(url) { mutableStateOf(true) }
    val backgroundColor = RunvealTheme.colors.bg.canvas.toArgb()

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(backgroundColor)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            isLoading = false
                        }
                    }
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (isLoading) {
            RunvealLoading(modifier = Modifier.align(Alignment.Center))
        }
    }
}
