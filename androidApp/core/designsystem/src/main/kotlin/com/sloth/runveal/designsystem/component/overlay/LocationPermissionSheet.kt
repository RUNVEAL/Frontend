package com.sloth.runveal.designsystem.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.R
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupPrimaryText
import com.sloth.runveal.designsystem.theme.RunvealTheme

/**
 * 위치 권한 미승인 시 지도 위에 띄우는 바텀시트 — 권한이 왜 필요한지 안내하고
 * '허용하기'를 누르면 시스템 권한 다이얼로그를 띄우는 흐름으로 이어진다.
 * 시트를 띄우고 닫는 건(ModalBottomSheet 등) 호출하는 쪽이 맡는다. 배경은 OS 내비게이션 바
 * 뒤까지 그대로 채워야 해서(호출하는 쪽에서 inset을 끄고 호출), 버튼만 내부에서 그 inset만큼 띄운다.
 */
@Composable
fun RunvealLocationPermissionSheet(
    onAllowClick: () -> Unit,
    onLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RunvealBottomSheet(
        modifier = modifier,
        header = {
            RunvealBottomSheetHeader(
                type = RunvealBottomSheetHeaderType.Hero,
                title = stringResource(R.string.location_permission_sheet_title),
                description = stringResource(R.string.location_permission_sheet_description),
                heroIcon = RunvealIcons.Pin,
            )
        },
        footer = {
            RunvealButtonGroup(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                RunvealButtonGroupPrimaryText(
                    primaryLabel = stringResource(R.string.location_permission_sheet_allow),
                    onPrimaryClick = onAllowClick,
                    textLabel = stringResource(R.string.location_permission_sheet_later),
                    onTextClick = onLaterClick,
                )
            }
        },
        content = {
            LocationPermissionBenefits(
                benefits = listOf(
                    stringResource(R.string.location_permission_benefit_explore_rate),
                    stringResource(R.string.location_permission_benefit_realtime_route),
                    stringResource(R.string.location_permission_benefit_background_record),
                ),
            )
        },
    )
}

@Composable
private fun LocationPermissionBenefits(benefits: List<String>, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg.canvas, RunvealTheme.shapes.radius16)
            .padding(RunvealTheme.spacing.space20),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
            benefits.forEach { benefit ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.icon.tertiary, CircleShape),
                    )
                    Text(benefit, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                }
            }
        }
    }
}

@Preview
@Composable
private fun RunvealLocationPermissionSheetPreview() {
    RunvealTheme {
        RunvealLocationPermissionSheet(onAllowClick = {}, onLaterClick = {})
    }
}
