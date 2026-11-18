package com.sloth.runveal.designsystem.component.overlay

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.runveal.designsystem.R
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupPrimaryText
import com.sloth.runveal.designsystem.theme.RunvealTheme

/**
 * 기기의 위치 서비스(GPS)가 꺼져 있을 때 띄우는 바텀시트.
 *
 * 위치 **권한**(앱)과는 다른 상태다 — 권한을 허용했어도 기기에서 위치 서비스가 꺼져 있으면 위치를
 * 못 받는다. 생김새는 권한 설정 시트([RunvealLocationPermissionSettingsSheet])와 같고 문구만 다르다.
 *
 * 시트를 띄우고 닫는 건 호출하는 쪽이 맡는다.
 */
@Composable
fun RunvealLocationServicesSheet(
    onSettingsClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RunvealBottomSheet(
        modifier = modifier,
        header = {
            RunvealBottomSheetHeader(
                type = RunvealBottomSheetHeaderType.Hero,
                title = stringResource(R.string.location_services_sheet_title),
                description = stringResource(R.string.location_services_sheet_description),
                heroIcon = RunvealIcons.LocationOff,
            )
        },
        footer = {
            RunvealButtonGroup(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                RunvealButtonGroupPrimaryText(
                    primaryLabel = stringResource(R.string.location_services_sheet_primary),
                    onPrimaryClick = onSettingsClick,
                    textLabel = stringResource(R.string.location_services_sheet_secondary),
                    onTextClick = onCloseClick,
                )
            }
        },
        content = {},
    )
}

@Preview
@Composable
private fun RunvealLocationServicesSheetPreview() {
    RunvealTheme {
        RunvealLocationServicesSheet(onSettingsClick = {}, onCloseClick = {})
    }
}
