package com.sloth.runveal.courserecommend

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.core.map.LocalMapState
import com.sloth.runveal.core.map.MapDotMarkers
import com.sloth.runveal.core.map.MapLayerSpec
import com.sloth.runveal.core.map.MapLayers
import com.sloth.runveal.core.map.MapPin
import com.sloth.runveal.core.map.RecommendedRoute
import com.sloth.runveal.core.map.buildRecommendedRouteGeoJson
import com.sloth.runveal.courserecommend.contract.CourseRecommendResultIntent
import com.sloth.runveal.courserecommend.contract.CourseRecommendResultState
import com.sloth.runveal.courserecommend.contract.RouteCardUiModel
import com.sloth.runveal.designsystem.component.button.RunvealButton
import com.sloth.runveal.designsystem.component.button.RunvealButtonSize
import com.sloth.runveal.designsystem.component.button.RunvealButtonStyle
import com.sloth.runveal.designsystem.component.input.RunvealChipCondition
import com.sloth.runveal.designsystem.component.input.RunvealChipConditionIcon
import com.sloth.runveal.designsystem.component.input.RunvealChipConditionUsage
import com.sloth.runveal.designsystem.component.input.RunvealChipDropdown
import com.sloth.runveal.designsystem.component.input.RunvealChipDropdownUsage
import com.sloth.runveal.designsystem.component.list.RunvealCourseCard
import com.sloth.runveal.designsystem.component.list.RunvealCourseCardState
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerType
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.courserecommend.R
import com.sloth.runveal.map.MapConfig
import kotlin.math.abs
import kotlinx.coroutines.launch

/** [RunvealCourseCard]의 고정 너비(240dp)와 맞춰야 캐러셀 가운데 정렬 계산이 들어맞는다. */
private val COURSE_CARD_WIDTH = 240.dp

/** B1 — `/api/v1/routes` 응답을 지도 위 오버레이로 보여주는 결과 화면. 지도(Map Shell)는 그대로 보이고
 * 이 위에 앱바와 하단 코스 카드 리스트만 얹는다 — 배경을 깔지 않는다.
 * [CourseRecommendResultHolder]를 구독해서 그대로 그린다(네비게이션 인자로 응답 객체를 못 넘겨서). */
@Composable
fun CourseRecommendResultScreen(
    onClose: () -> Unit,
    onCourseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseRecommendResultViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mapState = LocalMapState.current
    val readyState = state as? CourseRecommendResultState.Ready

    // 코스가 바뀌거나(새 추천) 카드 선택이 바뀌면 지도에 그릴 선을 다시 만들고, 선택된 코스가 전부
    // 보이게 카메라를 맞춘다.
    if (readyState != null) {
        MapLayers(
            MapLayerSpec(
                MapConfig.SOURCE_RECOMMENDED_ROUTE,
                buildRecommendedRouteGeoJson(
                    readyState.routes.map { route ->
                        RecommendedRoute(id = route.routeId, points = route.path, selected = route.routeId == readyState.selectedRouteId)
                    },
                ),
            ),
        )
    }
    // 선택된 코스의 출발/도착(경로의 첫·끝 좌표)을 Dot 마커로 띄우고, 그 코스가 전부 보이게 카메라를 맞춘다.
    // 순환(LOOP) 코스는 도착이 곧 출발이라 — 같은 자리에 마커 2개가 겹쳐 보이지 않게 출발만 띄운다.
    val courseMarkers = remember(readyState?.selectedRouteId, readyState?.routes) {
        val selected = readyState?.routes?.firstOrNull { it.routeId == readyState.selectedRouteId }
            ?: return@remember emptyList()
        listOfNotNull(
            selected.path.firstOrNull()?.let { MapPin(it, RunvealMapMarkerType.Start) },
            selected.path.lastOrNull()?.takeIf { !selected.isLoop }?.let { MapPin(it, RunvealMapMarkerType.End) },
        )
    }
    MapDotMarkers(courseMarkers)
    LaunchedEffect(readyState?.selectedRouteId, readyState?.routes) {
        val selected = readyState?.routes?.firstOrNull { it.routeId == readyState.selectedRouteId } ?: return@LaunchedEffect
        mapState.focusOnBounds(selected.path)
    }

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = onClose,
                style = RunvealTopAppBarStyle.Map,
                title = stringResource(R.string.course_recommend_result_title),
                modifier = Modifier.fillMaxWidth(),
            )
            if (readyState != null) {
                SettingsSummaryRow(
                    settings = readyState.settings,
                    modifier = Modifier.padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space12),
                )
            }
        }

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
                    BoxWithConstraints(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                    ) {
                        // 화면 가운데에 카드 하나가 오게 좌우 패딩을 "뷰포트 - 카드" 절반씩 준다. 화면이
                        // 좁아 음수가 나오면 기본 마진으로 떨어진다.
                        val sidePadding = ((maxWidth - COURSE_CARD_WIDTH) / 2).coerceAtLeast(RunvealTheme.layout.marginScreen)
                        val listState = rememberLazyListState()
                        val coroutineScope = rememberCoroutineScope()

                        // 탭이 아니라 "화면 가운데에 와 있는 카드"가 선택 상태를 정한다 — 스크롤이 멈추면
                        // snapFlingBehavior가 카드 하나를 정확히 가운데로 맞추고, 그 카드의 routeId를
                        // SelectRoute로 보낸다.
                        val centeredRouteId by remember(current.routes) {
                            derivedStateOf {
                                val layoutInfo = listState.layoutInfo
                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                layoutInfo.visibleItemsInfo
                                    .minByOrNull { abs(it.offset + it.size / 2 - viewportCenter) }
                                    ?.key as? String
                            }
                        }
                        LaunchedEffect(centeredRouteId) {
                            centeredRouteId?.let { viewModel.onIntent(CourseRecommendResultIntent.SelectRoute(it)) }
                        }

                        LazyRow(
                            state = listState,
                            flingBehavior = rememberSnapFlingBehavior(listState),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = sidePadding, vertical = RunvealTheme.spacing.space16),
                            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
                        ) {
                            itemsIndexed(current.routes, key = { _, route -> route.routeId }) { index, route ->
                                RouteCard(
                                    route = route,
                                    selected = route.routeId == current.selectedRouteId,
                                    // 이미 가운데(선택된) 카드를 또 탭하면 상세로, 아니면 가운데로 스크롤만 한다
                                    // — 선택 자체는 위 센터 감지가 맡는다.
                                    onClick = {
                                        if (route.routeId == current.selectedRouteId) {
                                            onCourseClick(route.routeId)
                                        } else {
                                            coroutineScope.launch { listState.animateScrollToItem(index) }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** B0에서 고른 거리·안 가본 길 우선·필터 수를 그대로 보여주는 요약 칩 — 코스 만들기 바텀 패널과 같은
 * 칩이지만 지도 위에 바로 떠서 글래스(OnMap) 스타일을 쓴다.
 *
 * **조회용이다.** 그래서 거리 칩도 `onClick`을 주지 않아 화살표가 없고 눌리지도 않는다 —
 * 여기서 조건을 바꾸려면 뒤로 가서 다시 고른다. */
@Composable
private fun SettingsSummaryRow(settings: CourseRecommendSettings, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
        RunvealChipDropdown(
            label = settings.distanceLabel,
            onClick = null,
            usage = RunvealChipDropdownUsage.OnMap,
        )
        if (settings.avoidVisitedPreferred) {
            RunvealChipCondition(
                checked = true,
                onCheckedChange = {},
                label = stringResource(R.string.course_recommend_result_avoid_visited_label),
                usage = RunvealChipConditionUsage.OnMap,
            )
        }
        if (settings.selectedFilterCount > 0) {
            RunvealChipCondition(
                checked = true,
                onCheckedChange = {},
                icon = RunvealChipConditionIcon.Filter,
                label = settings.selectedFilterCount.toString(),
                usage = RunvealChipConditionUsage.OnMap,
            )
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
