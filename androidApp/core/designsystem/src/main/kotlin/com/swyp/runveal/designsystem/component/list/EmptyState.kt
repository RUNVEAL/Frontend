package com.swyp.runveal.designsystem.component.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.component.button.RunvealButton
import com.swyp.runveal.designsystem.component.button.RunvealButtonSize
import com.swyp.runveal.designsystem.component.button.RunvealButtonStyle
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 빈 상태. 그래픽(선택) → 제목 + 설명(가운데 정렬) → Gold 버튼 M(선택). */
@Composable
fun RunvealEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    graphic: @Composable (() -> Unit)? = null,
) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
    ) {
        if (graphic != null) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center, content = { graphic() })
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            Text(title, style = RunvealTheme.typography.heading2, color = colors.text.primary, textAlign = TextAlign.Center)
            if (description != null) {
                Text(description, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary, textAlign = TextAlign.Center)
            }
        }
        if (actionLabel != null) {
            RunvealButton(label = actionLabel, onClick = onActionClick, style = RunvealButtonStyle.Gold, size = RunvealButtonSize.M)
        }
    }
}

@Preview
@Composable
private fun RunvealEmptyStatePreview() {
    RunvealTheme {
        RunvealEmptyState(
            title = "아직 저장한 코스가 없어요",
            description = "원하는 코스를 만들어 보세요",
            actionLabel = "코스 만들기",
        )
    }
}
