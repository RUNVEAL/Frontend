package com.sloth.runveal.tabcourse.component

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

/**
 * "저장한 코스" 목록 섹션 머리 — 제목 + 정렬 기준.
 *
 * [onSortClick]이 null이면 **현재 정렬을 보여주기만 한다** — 화살표를 빼고 탭도 받지 않는다.
 * 고를 정렬이 없는데 화살표만 달려 있으면 눌러 보게 되고, 눌러도 아무 일이 없어 고장으로 보인다.
 */
@Composable
fun SavedCourseSectionHeader(
    title: String,
    sortLabel: String,
    onSortClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors

    Row(
        modifier = modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.primary)
        Row(
            modifier = if (onSortClick != null) Modifier.clickable(onClick = onSortClick) else Modifier,
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sortLabel, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.primary)
            if (onSortClick != null) {
                Icon(
                    imageVector = RunvealIcons.Expand,
                    contentDescription = null,
                    tint = colors.icon.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SavedCourseSectionHeaderPreview() {
    RunvealTheme {
        SavedCourseSectionHeader(title = "저장한 코스", sortLabel = "최근 저장순", onSortClick = null)
    }
}
