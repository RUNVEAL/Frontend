package com.sloth.runveal.designsystem.component.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/** 구역 제목 + 정렬 드롭다운. 정렬을 누르면 바텀시트로 정렬 옵션을 고른다. */
@Composable
fun RunvealSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    sortLabel: String? = null,
    onSortClick: () -> Unit = {},
) {
    val colors = RunvealTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.primary)
        if (sortLabel != null) {
            Row(
                modifier = Modifier.clickable(onClick = onSortClick),
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(sortLabel, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.primary)
                Icon(imageVector = RunvealIcons.Expand, contentDescription = null, tint = colors.icon.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Preview
@Composable
private fun RunvealSectionHeaderPreview() {
    RunvealTheme {
        RunvealSectionHeader(title = "성수동", sortLabel = "거리순", onSortClick = {})
    }
}
