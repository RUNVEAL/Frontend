package com.swyp.runveal.designsystem.component.overlay

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.R
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupPrimaryText
import com.swyp.runveal.designsystem.theme.RunvealTheme

/**
 * 위치 권한을 2회 이상(또는 '다시 묻지 않음') 거부해 시스템 권한 다이얼로그를 더 띄울 수 없을 때
 * 지도 위에 띄우는 바텀시트 — 시스템 다이얼로그 대신 앱 설정 화면으로 보낸다.
 */
@Composable
fun RunvealLocationPermissionSettingsSheet(
    onSettingsClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RunvealBottomSheet(
        modifier = modifier,
        header = {
            RunvealBottomSheetHeader(
                type = RunvealBottomSheetHeaderType.Hero,
                title = stringResource(R.string.location_permission_settings_sheet_title),
                description = stringResource(R.string.location_permission_settings_sheet_description),
                heroIcon = RunvealIcons.LocationOff,
            )
        },
        footer = {
            RunvealButtonGroup(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                RunvealButtonGroupPrimaryText(
                    primaryLabel = stringResource(R.string.location_permission_settings_sheet_primary),
                    onPrimaryClick = onSettingsClick,
                    textLabel = stringResource(R.string.location_permission_settings_sheet_secondary),
                    onTextClick = onCloseClick,
                )
            }
        },
        content = {},
    )
}

@Preview
@Composable
private fun RunvealLocationPermissionSettingsSheetPreview() {
    RunvealTheme {
        RunvealLocationPermissionSettingsSheet(onSettingsClick = {}, onCloseClick = {})
    }
}
