package com.swyp.runveal.designsystem.component.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.theme.RunvealTheme

/**
 * 바텀시트 머리. Toolbar=뒤로 + 제목 + 오른쪽 글자 행동(초기화), Title=제목 + 부제,
 * Hero=큰 아이콘 + 제목 + 설명(권한 요청 등).
 */
enum class RunvealBottomSheetHeaderType { Toolbar, Title, Hero }

@Composable
fun RunvealBottomSheetHeader(
    type: RunvealBottomSheetHeaderType,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    description: String? = null,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    heroIcon: ImageVector? = null,
) {
    val colors = RunvealTheme.colors
    when (type) {
        RunvealBottomSheetHeaderType.Toolbar -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                Box(
                    modifier = Modifier.size(RunvealTheme.layout.sizeTouchTargetMin).clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = RunvealIcons.Back, contentDescription = null, tint = colors.icon.primary, modifier = Modifier.size(RunvealTheme.layout.sizeIcon))
                }
            }
            Text(title, style = RunvealTheme.typography.heading3, color = colors.text.primary, modifier = Modifier.weight(1f))
            if (actionLabel != null) {
                Text(
                    actionLabel,
                    style = RunvealTheme.typography.bodyMRegular,
                    color = colors.text.tertiary,
                    modifier = Modifier.clickable(onClick = onActionClick),
                )
            }
        }

        RunvealBottomSheetHeaderType.Title -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
            Text(title, style = RunvealTheme.typography.heading3, color = colors.text.primary)
            if (subtitle != null) {
                Text(subtitle, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
            }
        }

        RunvealBottomSheetHeaderType.Hero -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12)) {
            if (heroIcon != null) {
                Icon(imageVector = heroIcon, contentDescription = null, tint = colors.icon.primary, modifier = Modifier.size(32.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
                if (eyebrow != null) {
                    Text(eyebrow, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.accent)
                }
                Text(title, style = RunvealTheme.typography.heading2, color = colors.text.primary)
                if (description != null) {
                    Text(description, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                }
            }
        }
    }
}

@Preview
@Composable
private fun RunvealBottomSheetHeaderPreview() {
    RunvealTheme {
        Column {
            RunvealBottomSheetHeader(
                type = RunvealBottomSheetHeaderType.Toolbar,
                title = "정렬",
                showBack = true,
                onBackClick = {},
                actionLabel = "초기화",
                onActionClick = {},
            )
            RunvealBottomSheetHeader(
                type = RunvealBottomSheetHeaderType.Hero,
                title = "위치 권한이 필요해요",
                eyebrow = "권한 요청",
                description = "내 주변 탐험 지도를 보여드릴게요",
                heroIcon = RunvealIcons.Locate,
            )
        }
    }
}
