package com.sloth.runveal.courserecommend.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupSingle
import com.sloth.runveal.designsystem.component.button.RunvealIconButton
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheet
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheetHeader
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheetHeaderType
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.courserecommend.R

/**
 * 출발/도착 위치 선택 모드일 때 지도 위에 뜨는 하단 시트 — 선택한 지점 정보 + 확정 버튼.
 * 배경은 OS NavigationBar 영역까지 그대로 채우고(색이 비치지 않게), 버튼만 그 inset만큼 띄운다.
 * [onMyLocationClick]을 주면 시트 우측 상단 바로 위에 "내 위치" 아이콘 버튼이 떠 있는 형태로 추가된다
 * ([MakeCoursePanel]과 같은 자리 — 다만 시트는 좌우 여백 없이 가장자리에 붙으므로 화면 여백만큼 띄운다).
 */
@Composable
fun LocationConfirmPanel(
    placeName: String,
    distanceLabel: String,
    confirmLabel: String,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMyLocationClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier) {
        RunvealBottomSheet(
            header = {
                RunvealBottomSheetHeader(
                    type = RunvealBottomSheetHeaderType.Title,
                    title = placeName,
                    subtitle = distanceLabel,
                )
            },
            footer = {
                RunvealButtonGroup(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                ) {
                    RunvealButtonGroupSingle(label = confirmLabel, onClick = onConfirmClick)
                }
            },
            content = {},
        )

        if (onMyLocationClick != null) {
            RunvealIconButton(
                icon = RunvealIcons.Locate,
                contentDescription = stringResource(R.string.course_create_my_location_content_description),
                onClick = onMyLocationClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = -(RunvealTheme.layout.sizeIconButtonL + RunvealTheme.spacing.space12))
                    .padding(end = RunvealTheme.layout.marginScreen),
            )
        }
    }
}

@Preview
@Composable
private fun LocationConfirmPanelPreview() {
    RunvealTheme {
        LocationConfirmPanel(
            placeName = "성동구 성수 1가 1동",
            distanceLabel = "0.1km",
            confirmLabel = "이 위치를 출발지로 설정하기",
            onConfirmClick = {},
            onMyLocationClick = {},
        )
    }
}
