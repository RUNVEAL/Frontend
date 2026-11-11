package com.swyp.runveal.courserecommend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.courserecommend.contract.CourseRecommendResultState
import com.swyp.runveal.courserecommend.contract.RouteCardUiModel
import com.swyp.runveal.designsystem.component.button.RunvealButton
import com.swyp.runveal.designsystem.component.button.RunvealButtonSize
import com.swyp.runveal.designsystem.component.button.RunvealButtonStyle
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.courserecommend.R

/** B1 — `/api/v1/routes` 응답을 보여주는 결과 화면. [CourseRecommendResultHolder]를 구독해서 그대로
 * 그린다(네비게이션 인자로 응답 객체를 못 넘겨서). */
@Composable
fun CourseRecommendResultScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseRecommendResultViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = RunvealTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg.canvas)) {
        Column(modifier = Modifier.fillMaxSize()) {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Close,
                onLeadingClick = onClose,
                leadingContentDescription = stringResource(R.string.course_recommend_result_close_content_description),
                title = stringResource(R.string.course_recommend_result_title),
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars),
            )

            when (val current = state) {
                is CourseRecommendResultState.Loading -> Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CourseRecommendResultState.Ready -> {
                    if (current.emptyMessage != null) {
                        EmptyResult(message = current.emptyMessage, onRetry = onClose, modifier = Modifier.weight(1f).fillMaxWidth())
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(
                                horizontal = RunvealTheme.layout.marginScreen,
                                vertical = RunvealTheme.spacing.space16,
                            ),
                            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
                        ) {
                            items(current.routes, key = { it.routeId }) { route -> RouteCard(route) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResult(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier.padding(horizontal = RunvealTheme.layout.marginScreen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = RunvealTheme.typography.bodyLRegular, color = colors.text.secondary)
        Spacer(modifier = Modifier.padding(top = RunvealTheme.spacing.space20))
        RunvealButton(
            label = stringResource(R.string.course_recommend_result_empty_action),
            onClick = onRetry,
            style = RunvealButtonStyle.Gold,
            size = RunvealButtonSize.L,
        )
    }
}

@Composable
private fun RouteCard(route: RouteCardUiModel, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.default, shape)
            .border(1.dp, colors.border.default, shape)
            .padding(RunvealTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(route.typeLabel, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.accent)
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                Text(route.distanceLabel, style = RunvealTheme.typography.numM, color = colors.text.primary)
                Text(route.durationLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
            }
        }

        if (route.elevationGainLabel != null || route.newRoadLabel != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
                route.elevationGainLabel?.let {
                    Text(it, style = RunvealTheme.typography.captionMRegular, color = colors.text.tertiary)
                }
                route.newRoadLabel?.let {
                    Text(it, style = RunvealTheme.typography.captionMRegular, color = colors.text.accent)
                }
            }
        }

        if (route.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                route.tags.forEach { tag ->
                    Text(
                        tag,
                        style = RunvealTheme.typography.captionSRegular,
                        color = colors.text.secondary,
                        modifier = Modifier
                            .clip(RunvealTheme.shapes.full)
                            .background(colors.surface.raised, RunvealTheme.shapes.full)
                            .padding(horizontal = RunvealTheme.spacing.space8, vertical = RunvealTheme.spacing.space4),
                    )
                }
            }
        }

        if (route.milestoneNames.isNotEmpty()) {
            Text(
                route.milestoneNames.joinToString(" · "),
                style = RunvealTheme.typography.captionMRegular,
                color = colors.text.tertiary,
            )
        }
    }
}
