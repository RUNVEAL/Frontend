package com.swyp.runveal.designsystem.component.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealIconButton
import com.swyp.runveal.designsystem.component.button.RunvealIconButtonSize
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 탭 첫 화면의 큰 제목(내 코스 등). 상태바 바로 아래. */
@Composable
fun RunvealTopAppBarLarge(
    title: String,
    savedCourse: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    val colors = RunvealTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(vertical = RunvealTheme.spacing.space12, horizontal = RunvealTheme.spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        ) {
            Text(title, style = RunvealTheme.typography.heading1Semibold, color = colors.text.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                Text(savedCourse, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.secondary)
                if (subtitle != null) {
                    Text(subtitle, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                }
            }
        }
        if (action != null) {
            action()
        }
    }
}

@Preview
@Composable
private fun RunvealTopAppBarLargePreview() {
    RunvealTheme {
        RunvealTopAppBarLarge(
            title = "내 코스",
            savedCourse = "저장한 코스 4개",
            subtitle = "| 아직 안 달린 코스 2개",
            action = {
                RunvealIconButton(icon = RunvealIcons.Plus, contentDescription = "코스 추가", onClick = {}, size = RunvealIconButtonSize.M)
            },
        )
    }
}
