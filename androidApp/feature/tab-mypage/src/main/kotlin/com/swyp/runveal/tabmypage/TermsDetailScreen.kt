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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabmypage.R
import com.swyp.runveal.tabmypage.contract.OPEN_SOURCE_LICENSES
import com.swyp.runveal.tabmypage.contract.TermsPolicyItem

/**
 * 약관 한 건의 본문. 상단 제목은 목록에서 누른 약관 이름([TermsPolicyItem])을 그대로 쓴다.
 *
 * 오픈소스 라이선스만 앱이 직접 들고 있다 — 서버에서 받아올 성질이 아니라 빌드에 들어간
 * 의존성에서 나오는 값이기 때문이다.
 *
 * TODO: 나머지 약관 본문은 비워 뒀다 — 약관 목록 API(`docs/api/API.md` §3.8)가 붙으면 받아 온
 *  내용을 여기 채운다. 본문이 길어질 것을 전제로 스크롤은 미리 걸어 뒀다.
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RunvealTheme.layout.marginScreen)
                .padding(top = RunvealTheme.spacing.space24),
        ) {
            when (item) {
                TermsPolicyItem.OpenSource -> OpenSourceLicenseContent()
                // 나머지는 본문 자리 — 위 TODO 참고.
                else -> Unit
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
