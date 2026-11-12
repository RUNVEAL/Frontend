package com.swyp.runveal.courserecommend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.courserecommend.contract.CourseRecommendResultIntent
import com.swyp.runveal.courserecommend.contract.CourseRecommendResultState
import com.swyp.runveal.courserecommend.contract.RouteCardUiModel
import com.swyp.runveal.designsystem.component.button.RunvealButton
import com.swyp.runveal.designsystem.component.button.RunvealButtonSize
import com.swyp.runveal.designsystem.component.button.RunvealButtonStyle
import com.swyp.runveal.designsystem.component.list.RunvealCourseCard
import com.swyp.runveal.designsystem.component.list.RunvealCourseCardState
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.courserecommend.R

/** B1 — `/api/v1/routes` 응답을 지도 위 오버레이로 보여주는 결과 화면. 지도(Map Shell)는 그대로 보이고
 * 이 위에 앱바와 하단 코스 카드 리스트만 얹는다 — 배경을 깔지 않는다.
 * [CourseRecommendResultHolder]를 구독해서 그대로 그린다(네비게이션 인자로 응답 객체를 못 넘겨서). */
@Composable
fun CourseRecommendResultScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseRecommendResultViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        RunvealTopAppBar(
            leading = RunvealTopAppBarLeading.Close,
            onLeadingClick = onClose,
            style = RunvealTopAppBarStyle.Map,
            leadingContentDescription = stringResource(R.string.course_recommend_result_close_content_description),
            title = stringResource(R.string.course_recommend_result_title),
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        )

        when (val current = state) {
            is CourseRecommendResultState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is CourseRecommendResultState.Ready -> {
                if (current.emptyMessage != null) {
                    EmptyResult(
                        message = current.emptyMessage,
                        onRetry = onClose,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space16),
                    )
                } else {
                    LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                        contentPadding = PaddingValues(
                            horizontal = RunvealTheme.layout.marginScreen,
                            vertical = RunvealTheme.spacing.space16,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
                    ) {
                        items(current.routes, key = { it.routeId }) { route ->
                            RouteCard(
                                route = route,
                                selected = route.routeId == current.selectedRouteId,
                                onClick = { viewModel.onIntent(CourseRecommendResultIntent.SelectRoute(route.routeId)) },
                            )
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
    val shape = RunvealTheme.shapes.radius16

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface.raised, shape)
            .padding(RunvealTheme.spacing.space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space20),
    ) {
        Text(message, style = RunvealTheme.typography.bodyLRegular, color = colors.text.secondary, textAlign = TextAlign.Center)
        RunvealButton(
            label = stringResource(R.string.course_recommend_result_empty_action),
            onClick = onRetry,
            style = RunvealButtonStyle.Gold,
            size = RunvealButtonSize.L,
        )
    }
}

@Composable
private fun RouteCard(route: RouteCardUiModel, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RunvealCourseCard(
        name = stringResource(R.string.course_recommend_result_course_name, route.courseLabel),
        tags = route.tags.map { tagLabel(it) },
        distanceValue = route.distanceValue,
        distanceUnit = route.distanceUnit,
        timeValue = route.timeValue,
        timeUnit = route.timeUnit,
        elevationValue = route.elevationValue,
        elevationUnit = route.elevationUnit,
        newRoadValue = route.newRoadValue,
        newRoadUnit = route.newRoadUnit,
        onClick = onClick,
        state = if (selected) RunvealCourseCardState.Selected else RunvealCourseCardState.Default,
        modifier = modifier,
    )
}

/** `RouteDto.tags`는 서버 코드(`PARK`/`RIVERSIDE`/`GENTLE_SLOPE`/`FLAT`)로 와서 한글 라벨로 바꿔 보여준다. */
@Composable
private fun tagLabel(code: String): String = when (code) {
    "PARK" -> stringResource(R.string.course_recommend_tag_park)
    "RIVERSIDE" -> stringResource(R.string.course_recommend_tag_riverside)
    "GENTLE_SLOPE" -> stringResource(R.string.course_recommend_tag_gentle_slope)
    "FLAT" -> stringResource(R.string.course_recommend_tag_flat)
    else -> code
}
