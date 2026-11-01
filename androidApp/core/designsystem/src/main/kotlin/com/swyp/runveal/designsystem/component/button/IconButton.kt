package com.swyp.runveal.designsystem.component.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.designsystem.theme.runvealGlass

/** L=코스 만들기, 내 위치. M=헤더 옆 작은 버튼(보이는 크기는 작아도 터치 영역은 48 확보). */
enum class RunvealIconButtonSize { L, M }

@Composable
fun RunvealIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: RunvealIconButtonSize = RunvealIconButtonSize.L,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val visibleSize = when (size) {
        RunvealIconButtonSize.L -> RunvealTheme.layout.sizeIconButtonL
        RunvealIconButtonSize.M -> RunvealTheme.layout.sizeIconButtonM
    }
    val touchSize = maxOf(visibleSize, RunvealTheme.layout.sizeTouchTargetMin)

    Box(
        modifier = modifier
            .size(touchSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(visibleSize)
                .clip(shape)
                .runvealGlass(shape)
                .runvealPressOverlay(shape, colors.state.pressedOnSurface, pressed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = colors.icon.primary,
                modifier = Modifier.size(RunvealTheme.layout.sizeIcon),
            )
        }
    }
}

@Preview
@Composable
private fun RunvealIconButtonPreview() {
    RunvealTheme {
        RunvealIconButton(icon = RunvealIcons.Locate, contentDescription = "내 위치", onClick = {})
    }
}
