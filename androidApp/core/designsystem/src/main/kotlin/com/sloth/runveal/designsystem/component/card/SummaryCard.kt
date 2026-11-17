package com.sloth.runveal.designsystem.component.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.runveal.designsystem.R
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.designsystem.component.map.RunvealDongThumbnail

/** 홈 요약 카드. Home Bottom Panel 안에서만 쓴다(배경 없음) — 카드 전체 터치 시 동 상세. */
@Composable
fun RunvealSummaryCard(
    dongName: String,
    ratePercent: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMine: Boolean = false,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius24

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = RunvealTheme.layout.marginCardPadding, vertical = RunvealTheme.spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dongName, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.primary)
                if (isMine) {
                    Text(
                        stringResource(R.string.dong_mine_tag),
                        style = RunvealTheme.typography.captionSRegular,
                        color = colors.text.tertiary,
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$ratePercent",
                    style = RunvealTheme.typography.numDisplay,
                    color = colors.text.primary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    stringResource(R.string.percent_unit),
                    style = RunvealTheme.typography.unitDisplay,
                    color = colors.text.secondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }

        RunvealDongThumbnail()
    }
}

@Preview
@Composable
private fun RunvealSummaryCardPreview() {
    RunvealTheme {
        RunvealSummaryCard(
            dongName = "성수동",
            ratePercent = 37,
            onClick = {},
            isMine = true,
        )
    }
}
