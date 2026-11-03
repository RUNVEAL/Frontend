package com.swyp.runveal.designsystem.component.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.R
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 지도 동 라벨. 동 면 중앙에 배치. 터치는 라벨이 아닌 동 면(path) 전체가 받는다. */
enum class RunvealDongLabelType { Mine, Other }

@Composable
fun RunvealDongLabel(
    name: String,
    modifier: Modifier = Modifier,
    ratePercent: Int? = null,
    type: RunvealDongLabelType = RunvealDongLabelType.Other,
) {
    val colors = RunvealTheme.colors
    when (type) {
        RunvealDongLabelType.Mine -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                Text(name, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.primary)
                Text(
                    stringResource(R.string.dong_mine_tag),
                    style = RunvealTheme.typography.captionSRegular,
                    color = colors.text.tertiary,
                )
            }
            if (ratePercent != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$ratePercent",
                        style = RunvealTheme.typography.numL,
                        color = colors.text.primary,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        stringResource(R.string.percent_unit),
                        style = RunvealTheme.typography.unitL,
                        color = colors.text.secondary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        RunvealDongLabelType.Other -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(name, style = RunvealTheme.typography.captionSRegular, color = colors.text.secondary)
            if (ratePercent != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$ratePercent",
                        style = RunvealTheme.typography.numS,
                        color = colors.text.primary,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        stringResource(R.string.percent_unit),
                        style = RunvealTheme.typography.unitS,
                        color = colors.text.secondary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RunvealDongLabelPreview() {
    RunvealTheme {
        Column {
            RunvealDongLabel(name = "성수동", ratePercent = 37, type = RunvealDongLabelType.Mine)
            RunvealDongLabel(name = "역삼동", type = RunvealDongLabelType.Other)
        }
    }
}
