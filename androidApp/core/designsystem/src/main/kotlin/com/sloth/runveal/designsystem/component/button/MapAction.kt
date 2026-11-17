package com.sloth.runveal.designsystem.component.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 지도 하단 코스 만들기 / 내 위치 — IconButton(L) + 아래 라벨. */
@Composable
fun RunvealMapAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RunvealIconButton(
            icon = icon,
            contentDescription = label,
            onClick = onClick,
            size = RunvealIconButtonSize.L,
        )
        Text(
            label,
            style = RunvealTheme.typography.captionMRegular,
            color = RunvealTheme.colors.text.secondary,
        )
    }
}

@Preview
@Composable
private fun RunvealMapActionPreview() {
    RunvealTheme {
        RunvealMapAction(icon = RunvealIcons.Course, label = "코스 만들기", onClick = {})
    }
}
