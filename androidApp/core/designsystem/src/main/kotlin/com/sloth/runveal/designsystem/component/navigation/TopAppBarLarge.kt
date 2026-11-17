package com.sloth.runveal.designsystem.component.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealIconButton
import com.sloth.runveal.designsystem.component.button.RunvealIconButtonSize
import com.sloth.runveal.designsystem.theme.RunvealTheme

/**
 * 탭 첫 화면의 큰 제목(내 코스, 기록 등). 상태바 바로 아래.
 * [onMetaLabelClick]을 주면 metaLabel(+subtitle) 옆에 expand 아이콘이 붙고 탭할 수 있게 된다
 * (기록 탭의 월 선택처럼). 안 주면(기본) 그냥 텍스트다.
 */
@Composable
fun RunvealTopAppBarLarge(
    title: String,
    metaLabel: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = RunvealTheme.typography.heading1Semibold,
    subtitle: String? = null,
    onMetaLabelClick: (() -> Unit)? = null,
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
            Text(title, style = titleStyle, color = colors.text.primary)
            Row(
                modifier = if (onMetaLabelClick != null) Modifier.clickable(onClick = onMetaLabelClick) else Modifier,
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(metaLabel, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.secondary)
                if (subtitle != null) {
                    Text(subtitle, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                }
                if (onMetaLabelClick != null) {
                    Icon(
                        imageVector = RunvealIcons.Expand,
                        contentDescription = null,
                        tint = colors.icon.primary,
                        modifier = Modifier.size(RunvealTheme.layout.sizeIcon),
                    )
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
            metaLabel = "저장한 코스 4개",
            subtitle = "| 아직 안 달린 코스 2개",
            action = {
                RunvealIconButton(icon = RunvealIcons.Plus, contentDescription = "코스 추가", onClick = {}, size = RunvealIconButtonSize.M)
            },
        )
    }
}
