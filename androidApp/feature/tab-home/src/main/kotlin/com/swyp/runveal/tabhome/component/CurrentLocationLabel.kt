package com.swyp.runveal.tabhome.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.theme.RunvealTheme

@Composable
fun CurrentLocationLabel(locationName: String, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = RunvealIcons.Pin,
            contentDescription = null,
            tint = colors.icon.primary,
        )
        Text(
            text = locationName,
            style = RunvealTheme.typography.bodyLSemibold,
            color = colors.text.primary,
        )
        Icon(
            imageVector = RunvealIcons.Chevron,
            contentDescription = null,
            tint = colors.icon.primary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Preview
@Composable
private fun CurrentLocationLabelPreview() {
    RunvealTheme {
        CurrentLocationLabel(locationName = "현재 위치")
    }
}
