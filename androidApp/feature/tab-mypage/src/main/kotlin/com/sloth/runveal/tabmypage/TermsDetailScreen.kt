package com.sloth.runveal.tabmypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
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
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.overlay.RunvealLoading
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabmypage.R
import com.sloth.runveal.tabmypage.contract.OPEN_SOURCE_LICENSES
import com.sloth.runveal.ui.web.RunvealWebView
import com.sloth.runveal.tabmypage.contract.TermsPolicyItem

/**
 * 약관 한 건의 본문. 상단 제목은 목록에서 누른 약관 이름([TermsPolicyItem])을 그대로 쓴다.
 *
 * 본문은 두 가지다:
 * - **웹에 올려 둔 약관**([TermsPolicyItem.url]) — 웹뷰로 띄운다. 문구가 바뀌어도 앱을 다시
 *   배포하지 않아도 되고, 두 플랫폼이 같은 원문을 본다([com.sloth.runveal.terms.TermsDocument]).
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
            RunvealWebView(url = url, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RunvealTheme.layout.marginScreen)
                    // 끝까지 내렸을 때 마지막 내용이 OS 내비게이션(제스처 바)에 가리지 않게 한다.
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(top = RunvealTheme.spacing.space24),
            ) {
                OpenSourceLicenseContent()
            }
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
            // 행정동 경계는 앱 번들이 아니라 서버 `index.geojson`에서 받지만, 화면에 그리는 건
            // 앱이라 출처를 여기 함께 적는다.
            LicenseRow(
                name = stringResource(R.string.oss_map_dong),
                license = stringResource(R.string.oss_map_dong_license),
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
