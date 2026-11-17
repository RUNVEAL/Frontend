package com.sloth.runveal.courserecommend.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupSingle
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheet
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheetHeader
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheetHeaderType
import com.sloth.runveal.designsystem.theme.RunvealTheme

/**
 * 출발/도착 위치 선택 모드일 때 지도 위에 뜨는 하단 시트 — 선택한 지점 정보 + 확정 버튼.
 * 배경은 OS NavigationBar 영역까지 그대로 채우고(색이 비치지 않게), 버튼만 그 inset만큼 띄운다.
 */
@Composable
fun LocationConfirmPanel(
    placeName: String,
    distanceLabel: String,
    confirmLabel: String,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RunvealBottomSheet(
        modifier = modifier,
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
        )
    }
}
