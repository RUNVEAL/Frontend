package com.swyp.runveal.courserecommend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.core.map.LocalMapState
import com.swyp.runveal.core.map.MapLayerSpec
import com.swyp.runveal.core.map.MapLayers
import com.swyp.runveal.core.map.MapPin
import com.swyp.runveal.core.map.RecommendedRoute
import com.swyp.runveal.core.map.buildRecommendedRouteGeoJson
import com.swyp.runveal.courserecommend.component.ElevationChart
import com.swyp.runveal.courserecommend.contract.CourseDetailIntent
import com.swyp.runveal.courserecommend.contract.CourseDetailState
import com.swyp.runveal.courserecommend.contract.RouteStepUiModel
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupSecondaryPrimary
import com.swyp.runveal.designsystem.component.list.RunvealRouteStep
import com.swyp.runveal.designsystem.component.map.RunvealMapMarkerType
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.swyp.runveal.designsystem.component.stat.RunvealStat
import com.swyp.runveal.designsystem.component.stat.RunvealStatType
import com.swyp.runveal.designsystem.component.overlay.RunvealBottomSheet
import com.swyp.runveal.designsystem.R as DesignSystemR
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.courserecommend.R
import com.swyp.runveal.map.MapConfig

/** B2 — 추천 코스 결과에서 카드를 눌러 들어가는 코스 상세. 지도(Map Shell)는 그대로 보이고 이 위에
 * 뒤로가기와 바텀시트만 얹는다 — 배경을 깔지 않는다. [routeId]로 [CourseRecommendResultHolder]에서
 * 코스를 다시 찾아온다(네비게이션 인자로 전체 응답을 못 넘겨서). */
@Composable
fun CourseDetailScreen(
    routeId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mapState = LocalMapState.current
    val density = LocalDensity.current
    // 바텀시트가 화면 아래쪽을 덮는 만큼, 그 실제 높이를 측정해서 카메라 포커싱 때 아래 여백으로 더
    // 줘야 코스가 안 덮이는 위쪽(보이는 영역)에 맞춰진다. 처음엔 아직 못 재서 0 — 측정되면 한 번 더
    // 포커싱을 다시 맞춘다.
    var bottomSheetHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(routeId) { viewModel.onIntent(CourseDetailIntent.Load(routeId)) }

    val readyState = state as? CourseDetailState.Ready
    if (readyState != null) {
        MapLayers(
            MapLayerSpec(
                MapConfig.SOURCE_COURSE_DETAIL_ROUTE,
                buildRecommendedRouteGeoJson(listOf(RecommendedRoute(id = routeId, points = readyState.path, selected = true))),
            ),
        )
    }
    LaunchedEffect(readyState?.path, readyState?.isLoop, bottomSheetHeightPx) {
        val detail = readyState ?: return@LaunchedEffect
        mapState.dotMarkers = listOfNotNull(
            detail.path.firstOrNull()?.let { MapPin(it, RunvealMapMarkerType.Start) },
            detail.path.lastOrNull()?.takeIf { !detail.isLoop }?.let { MapPin(it, RunvealMapMarkerType.End) },
        )
        val extraBottomPadding = with(density) { bottomSheetHeightPx.toDp() }
        mapState.focusOnBounds(detail.path, extraBottomPadding = extraBottomPadding)
    }
    DisposableEffect(Unit) { onDispose { mapState.dotMarkers = emptyList() } }

    Box(modifier = modifier.fillMaxSize()) {
        RunvealTopAppBar(
            leading = RunvealTopAppBarLeading.Back,
            onLeadingClick = onBack,
            style = RunvealTopAppBarStyle.Map,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        )

        when (val current = state) {
            is CourseDetailState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is CourseDetailState.NotFound -> Text(
                stringResource(R.string.course_detail_not_found),
                style = RunvealTheme.typography.bodyLRegular,
                color = RunvealTheme.colors.text.secondary,
                modifier = Modifier.align(Alignment.Center),
            )
            is CourseDetailState.Ready -> {
                BoxWithConstraints(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    val sheetMaxHeight = maxHeight
                    val overallSheetCap = sheetMaxHeight * 0.85f
                    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    // "경로 정보" 목록을 뺀 나머지(드래그 핸들·헤더·통계·그래프·버튼과 그 사이 여백들,
                    // 버튼 아래 제스처 바 inset까지)의 대략적인 높이 — 시트 전체 한도에서 이만큼 뺀
                    // 나머지만 목록에 줘야 버튼이 밀려서 찌그러지지 않는다. Column이 비중(weight) 없이
                    // 순서대로 공간을 나눠 가지는 구조라, 다른 요소가 다 차지하면 뒤에 오는 버튼이
                    // 남은 자투리 공간에 맞춰 눌려 찌그러진다 — 그래서 직접 역산해 줘야 한다.
                    val otherContentEstimate = 360.dp + navBarInset
                    val routeStepsMaxHeight = (overallSheetCap - otherContentEstimate).coerceIn(80.dp, 280.dp)
                    RunvealBottomSheet(
                        // 배경은 OS NavigationBar 뒤까지 그대로 채운다(MakeCoursePanel/LocationConfirmPanel과
                        // 같은 컨벤션) — 그래야 제스처 바 영역도 시트와 같은 색으로 보인다. inset은 버튼에만 준다.
                        modifier = Modifier
                            .heightIn(max = overallSheetCap)
                            .onSizeChanged { bottomSheetHeightPx = it.height },
                        header = {
                            Text(
                                stringResource(R.string.course_recommend_result_course_name, current.courseLabel),
                                style = RunvealTheme.typography.heading2,
                                color = RunvealTheme.colors.text.primary,
                            )
                        },
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16)) {
                                StatsRow(current)
                                ElevationChart(points = current.elevationProfile, modifier = Modifier.fillMaxWidth())
                                // 경로 정보만 자체 높이로 스크롤 — 통계·그래프는 항상 고정으로 보이고,
                                // 경유지가 많아져도 그 목록만 안에서 스크롤돼서 시트 전체 높이와 버튼
                                // 크기가 밀려서 찌그러지지 않는다.
                                RouteStepsSection(current.routeSteps, maxListHeight = routeStepsMaxHeight)
                            }
                        },
                        footer = {
                            RunvealButtonGroup(
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                            ) {
                                RunvealButtonGroupSecondaryPrimary(
                                    // TODO: 코스 저장·러닝 시작 실제 동작 연동 추가 예정.
                                    secondaryLabel = stringResource(R.string.course_detail_save_action),
                                    onSecondaryClick = {},
                                    primaryLabel = stringResource(R.string.course_detail_start_running_action),
                                    onPrimaryClick = {},
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(state: CourseDetailState.Ready, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16)) {
        RunvealStat(
            label = stringResource(DesignSystemR.string.course_card_stat_distance),
            value = state.distanceValue,
            unit = state.distanceUnit,
            type = RunvealStatType.Large,
            modifier = Modifier.weight(1f),
        )
        RunvealStat(
            label = stringResource(DesignSystemR.string.course_card_stat_time),
            value = state.timeValue,
            unit = state.timeUnit,
            type = RunvealStatType.Large,
            modifier = Modifier.weight(1f),
        )
        RunvealStat(
            label = stringResource(DesignSystemR.string.course_card_stat_elevation),
            value = state.elevationValue,
            unit = state.elevationUnit,
            type = RunvealStatType.Large,
            modifier = Modifier.weight(1f),
        )
        RunvealStat(
            label = stringResource(DesignSystemR.string.course_card_stat_new_road),
            value = state.newRoadValue,
            unit = state.newRoadUnit,
            type = RunvealStatType.Large,
            accent = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RouteStepsSection(steps: List<RouteStepUiModel>, maxListHeight: Dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
        Text(
            stringResource(R.string.course_detail_route_info_title),
            style = RunvealTheme.typography.bodyLSemibold,
            color = RunvealTheme.colors.text.secondary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxListHeight)
                .verticalScroll(rememberScrollState()),
        ) {
            steps.forEachIndexed { index, step ->
                RunvealRouteStep(
                    place = step.place,
                    distance = step.distanceLabel,
                    type = step.type,
                    note = step.note,
                    showConnector = index != steps.lastIndex,
                )
            }
        }
    }
}
