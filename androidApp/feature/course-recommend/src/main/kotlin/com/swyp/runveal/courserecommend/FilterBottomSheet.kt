package com.swyp.runveal.courserecommend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupSingle
import com.swyp.runveal.designsystem.component.input.RunvealChipOption
import com.swyp.runveal.designsystem.component.input.RunvealToggleSwitch
import com.swyp.runveal.designsystem.component.overlay.RunvealBottomSheet
import com.swyp.runveal.designsystem.component.overlay.RunvealBottomSheetHeader
import com.swyp.runveal.designsystem.component.overlay.RunvealBottomSheetHeaderType
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 코스 조건 설정 바텀시트 — 거리, 안 가본 길 우선, 러닝 필터. */
@Composable
fun FilterBottomSheet(
    distanceOptions: List<String>,
    selectedDistance: String,
    onDistanceSelect: (String) -> Unit,
    avoidVisitedPreferred: Boolean,
    onAvoidVisitedPreferredChange: (Boolean) -> Unit,
    filterOptions: List<String>,
    selectedFilters: Set<String>,
    onFilterToggle: (String) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors

    RunvealBottomSheet(
        modifier = modifier,
        header = {
            RunvealBottomSheetHeader(
                type = RunvealBottomSheetHeaderType.Toolbar,
                title = "코스 조건 설정하기",
                showBack = true,
                onBackClick = onBack,
                actionLabel = "초기화",
                onActionClick = onReset,
            )
        },
        footer = {
            RunvealButtonGroup {
                RunvealButtonGroupSingle(label = "코스 조건 적용하기", onClick = onApply)
            }
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
            ) {
                FilterSection(title = "원하는 거리") {
                    distanceOptions.forEach { option ->
                        RunvealChipOption(
                            label = option,
                            selected = option == selectedDistance,
                            onClick = { onDistanceSelect(option) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space20),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
                    ) {
                        Text("안 가본 길 우선 추천", style = RunvealTheme.typography.bodyLSemibold, color = colors.text.primary)
                        Text("안 가본 길을 우선으로 추천해요", style = RunvealTheme.typography.captionMRegular, color = colors.text.secondary)
                    }
                    RunvealToggleSwitch(checked = avoidVisitedPreferred, onCheckedChange = onAvoidVisitedPreferredChange)
                }

                FilterSection(title = "러닝 필터") {
                    filterOptions.forEach { option ->
                        RunvealChipOption(
                            label = option,
                            selected = option in selectedFilters,
                            onClick = { onFilterToggle(option) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun FilterSection(title: String, options: @Composable () -> Unit) {
    val colors = RunvealTheme.colors

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        Text(title, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.primary)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            options()
        }
    }
}
