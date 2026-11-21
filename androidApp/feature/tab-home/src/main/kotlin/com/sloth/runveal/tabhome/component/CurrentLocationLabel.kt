package com.sloth.runveal.tabhome.component

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
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** 위치 줄 앞 핀 아이콘 크기 — 시안 값이라 아이콘 토큰(24)을 쓰지 않는다. */
private val PIN_ICON_SIZE = 18.dp

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
            // 시안(A3-1)의 핀은 18 — 아이콘 기본 크기(24)보다 작다.
            modifier = Modifier.size(PIN_ICON_SIZE),
        )
        Text(
            text = locationName,
            style = RunvealTheme.typography.bodyLSemibold,
            color = colors.text.primary,
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
