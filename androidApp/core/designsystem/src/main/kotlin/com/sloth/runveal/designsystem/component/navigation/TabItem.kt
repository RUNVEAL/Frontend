package com.sloth.runveal.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** GNB 탭 하나. [RunvealTabBar]가 4개를 Row로 묶어서 쓴다. */
@Composable
fun RunvealTabItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    val background = if (active) colors.accent.default else Color.Transparent
    val iconTint = if (active) colors.icon.onAccent else colors.icon.tertiary
    val labelStyle = if (active) RunvealTheme.typography.captionSSemibold else RunvealTheme.typography.captionSRegular
    val labelColor = if (active) colors.text.onAccent else colors.text.tertiary

    Column(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(background, shape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier)
        Text(label, style = labelStyle, color = labelColor)
    }
}

@Preview
@Composable
private fun RunvealTabItemPreview() {
    RunvealTheme {
        RunvealTabItem(icon = RunvealIcons.Home, label = "홈", active = true, onClick = {})
    }
}
