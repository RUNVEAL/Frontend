package com.sloth.runveal.designsystem.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.runvealPressOverlay
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 더보기(⋯) 메뉴 패널. 글래스 없이 불투명. [content]에 [RunvealMenuItem]을 나열한다. */
@Composable
fun RunvealMenu(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16

    Column(
        modifier = modifier
            .width(184.dp)
            .clip(shape)
            .background(colors.surface.raised, shape)
            .border(1.dp, colors.border.default, shape)
            .padding(RunvealTheme.spacing.space8),
        content = { content() },
    )
}

enum class RunvealMenuItemType { Default, Danger }

/** 메뉴 한 줄. 아이콘 20 + 라벨. */
@Composable
fun RunvealMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: RunvealMenuItemType = RunvealMenuItemType.Default,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius8
    val contentColor = if (type == RunvealMenuItemType.Danger) colors.status.error else colors.text.primary
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .runvealPressOverlay(shape, colors.state.pressedOnSurface, pressed)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = RunvealTheme.spacing.space8),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = RunvealTheme.typography.bodyMRegular,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // 라벨이 길어도 메뉴 폭(184)을 밀어내지 않게 남는 자리를 라벨이 가져간다.
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun RunvealMenuPreview() {
    RunvealTheme {
        RunvealMenu {
            RunvealMenuItem(label = "이름 수정", icon = RunvealIcons.Edit, onClick = {})
            RunvealMenuItem(label = "GPX 내보내기", icon = RunvealIcons.Download, onClick = {})
            RunvealMenuItem(label = "삭제", icon = RunvealIcons.Delete, onClick = {}, type = RunvealMenuItemType.Danger)
        }
    }
}
