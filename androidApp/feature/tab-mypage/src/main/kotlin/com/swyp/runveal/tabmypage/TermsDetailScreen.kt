package com.swyp.runveal.tabmypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.overlay.RunvealLoading
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabmypage.R
import com.swyp.runveal.tabmypage.contract.OPEN_SOURCE_LICENSES
import com.swyp.runveal.tabmypage.contract.TermsPolicyItem

/**
 * 약관 한 건의 본문. 상단 제목은 목록에서 누른 약관 이름([TermsPolicyItem])을 그대로 쓴다.
 *
 * 본문은 두 가지다:
 * - **웹에 올려 둔 약관**([TermsPolicyItem.url]) — 웹뷰로 띄운다. 문구가 바뀌어도 앱을 다시
 *   배포하지 않아도 되고, 두 플랫폼이 같은 원문을 본다([com.swyp.runveal.terms.TermsDocument]).
 * - **오픈소스 라이선스** — 앱이 직접 그린다. 서버에서 받아올 성질이 아니라 빌드에 들어간
 *   의존성에서 나오는 값이기 때문이다.
 */
@Composable
internal fun TermsDetailScreen(
    item: TermsPolicyItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        RunvealTopAppBar(
            leading = RunvealTopAppBarLeading.Back,
            onLeadingClick = onBackClick,
            title = stringResource(item.labelRes),
        )

        val url = item.url
        if (url != null) {
            // 웹뷰는 스스로 스크롤하므로 verticalScroll로 감싸지 않는다 — 감싸면 높이 제약이 없어져
            // 페이지 전체가 펼쳐지고 스크롤이 두 겹이 된다.
            TermsWebView(url = url, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RunvealTheme.layout.marginScreen)
                    .padding(top = RunvealTheme.spacing.space24),
            ) {
                OpenSourceLicenseContent()
            }
        }
    }
}

/**
 * 약관 원문을 띄우는 웹뷰.
 *
 * 자바스크립트는 켜지 않는다(WebView 기본값) — 정적 HTML이라 필요 없고, 꺼 두는 쪽이 안전하다.
 *
 * 로드가 끝나기 전까지 캔버스 색으로 덮인 채 로딩 모션만 보인다. 배경색을 지정하지 않으면 로드
 * 직전에 흰 화면이 한 번 번쩍인다 — 앱이 다크 단일 테마라 특히 눈에 띈다.
 *
 * 원문 HTML이 이미 다크 테마라 앱 안에서 겉돌지 않는다(실기기 확인). 원문을 밝은 배경으로 바꾸면
 * 이 화면만 희게 뜨므로, 그때는 원문에 `prefers-color-scheme`을 넣는 쪽이 낫다 — 앱에서
 * 강제 다크닝(`androidx.webkit`)을 걸면 글자가 뭉개진다.
 */
@Composable
private fun TermsWebView(url: String, modifier: Modifier = Modifier) {
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

/**
 * 오픈소스 고지. 지도를 맨 위에 따로 둔 건 **표시 의무가 있기 때문**이다 — 지도 데이터가
 * OpenStreetMap(ODbL)이라 저작자 표시를 해야 하는데, 지도 화면에서는 MapLibre 기본 표시를 꺼
 * 두었다(`MapLibreMapView`의 `isAttributionEnabled = false`). 그 고지가 사는 자리가 여기다.
 */
@Composable
private fun OpenSourceLicenseContent() {
    val colors = RunvealTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = RunvealTheme.spacing.space32),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
    ) {
        LicenseSection(title = stringResource(R.string.oss_map_section)) {
            LicenseRow(
                name = stringResource(R.string.oss_map_data),
                license = stringResource(R.string.oss_map_data_license),
            )
            LicenseRow(
                name = stringResource(R.string.oss_map_tiles),
                license = stringResource(R.string.oss_map_tiles_license),
            )
        }

        LicenseSection(title = stringResource(R.string.oss_library_section)) {
            OPEN_SOURCE_LICENSES.forEach { LicenseRow(name = it.name, license = it.license) }
        }
    }
}

@Composable
private fun LicenseSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12)) {
        Text(
            text = title,
            style = RunvealTheme.typography.bodyLSemibold,
            color = RunvealTheme.colors.text.primary,
        )
        content()
    }
}

/** 이름은 줄바꿈될 수 있고 라이선스 이름은 한 줄로 붙여 둔다 — 길이가 제각각이라 양끝 정렬은 안 쓴다. */
@Composable
private fun LicenseRow(name: String, license: String) {
    Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2)) {
        Text(
            text = name,
            style = RunvealTheme.typography.bodyMRegular,
            color = RunvealTheme.colors.text.secondary,
        )
        Text(
            text = license,
            style = RunvealTheme.typography.captionMRegular,
            color = RunvealTheme.colors.text.tertiary,
        )
    }
}
