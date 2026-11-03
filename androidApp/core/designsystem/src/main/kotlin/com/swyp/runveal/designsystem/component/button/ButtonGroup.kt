package com.swyp.runveal.designsystem.component.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.theme.RunvealTheme

/**
 * 화면·시트 하단에 고정되는 버튼 묶음 레이아웃(좌우 margin.screen, 내비게이션 바 WindowInsets 위 8).
 * 안의 버튼 배치는 [RunvealButtonGroupSingle]/[RunvealButtonGroupStacked]/[RunvealButtonGroupPrimaryText]/
 * [RunvealButtonGroupSecondaryPrimary]/[RunvealButtonGroupEqual] 중 하나를 쓴다.
 */
@Composable
fun RunvealButtonGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RunvealTheme.layout.marginScreen)
            .padding(bottom = RunvealTheme.spacing.space8),
        content = content,
    )
}

/** 주 버튼 하나(Gold). */
@Composable
fun RunvealButtonGroupSingle(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RunvealButton(label = label, onClick = onClick, modifier = modifier, style = RunvealButtonStyle.Gold, fillWidth = true)
}

/** 같은 무게의 선택지 세로 나열(로그인 수단 등). */
@Composable
fun RunvealButtonGroupStacked(
    firstLabel: String,
    onFirstClick: () -> Unit,
    secondLabel: String,
    onSecondClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
        RunvealButton(label = firstLabel, onClick = onFirstClick, style = RunvealButtonStyle.Secondary, fillWidth = true)
        RunvealButton(label = secondLabel, onClick = onSecondClick, style = RunvealButtonStyle.Secondary, fillWidth = true)
    }
}

/** 주 버튼(Gold) + 아래 보조 Text 버튼. */
@Composable
fun RunvealButtonGroupPrimaryText(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    textLabel: String,
    onTextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
        RunvealButton(label = primaryLabel, onClick = onPrimaryClick, style = RunvealButtonStyle.Gold, fillWidth = true)
        RunvealButton(
            label = textLabel,
            onClick = onTextClick,
            style = RunvealButtonStyle.Text,
            fillWidth = true,
            modifier = Modifier.height(RunvealTheme.layout.sizeTouchTargetMin),
        )
    }
}

/** 보조 버튼(내용 너비) + 주 버튼(나머지 채움). */
@Composable
fun RunvealButtonGroupSecondaryPrimary(
    secondaryLabel: String,
    onSecondaryClick: () -> Unit,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
        RunvealButton(label = secondaryLabel, onClick = onSecondaryClick, style = RunvealButtonStyle.Secondary)
        RunvealButton(
            label = primaryLabel,
            onClick = onPrimaryClick,
            style = RunvealButtonStyle.Gold,
            fillWidth = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 같은 너비 둘(보조 1/2, 주 1/2). */
@Composable
fun RunvealButtonGroupEqual(
    firstLabel: String,
    onFirstClick: () -> Unit,
    secondLabel: String,
    onSecondClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
        RunvealButton(
            label = firstLabel,
            onClick = onFirstClick,
            style = RunvealButtonStyle.Secondary,
            fillWidth = true,
            modifier = Modifier.weight(1f),
        )
        RunvealButton(
            label = secondLabel,
            onClick = onSecondClick,
            style = RunvealButtonStyle.Gold,
            fillWidth = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun RunvealButtonGroupPreview() {
    RunvealTheme {
        RunvealButtonGroup {
            RunvealButtonGroupSecondaryPrimary(
                secondaryLabel = "취소",
                onSecondaryClick = {},
                primaryLabel = "확인",
                onPrimaryClick = {},
            )
        }
    }
}
