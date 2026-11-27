package com.sloth.runveal.courserecommend

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.sloth.runveal.analytics.RunvealAnalyticsLogger
import com.sloth.runveal.core.map.LocalMapState
import com.sloth.runveal.core.map.MapDotMarkers
import com.sloth.runveal.core.map.MapLayerSpec
import com.sloth.runveal.core.map.MapLayers
import com.sloth.runveal.core.map.MapPin
import com.sloth.runveal.core.map.RecommendedRoute
import com.sloth.runveal.core.map.buildRecommendedRouteGeoJson
import com.sloth.runveal.courserecommend.component.ElevationChart
import com.sloth.runveal.courserecommend.contract.CourseDetailIntent
import com.sloth.runveal.courserecommend.contract.CourseDetailSource
import com.sloth.runveal.courserecommend.contract.CourseDetailState
import com.sloth.runveal.courserecommend.contract.RouteStepUiModel
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupSecondaryPrimary
import com.sloth.runveal.designsystem.component.list.RunvealRouteStep
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerType
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.sloth.runveal.designsystem.component.overlay.RunvealLoading
import com.sloth.runveal.designsystem.component.stat.RunvealStat
import com.sloth.runveal.designsystem.component.stat.RunvealStatType
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheet
import com.sloth.runveal.designsystem.R as DesignSystemR
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.courserecommend.R
import com.sloth.runveal.map.MapConfig
import com.sloth.runveal.result.SelectedCourseHolder

/** 공용 로딩 모션 크기(시안 Z0의 80). */
private val LOADING_SIZE = 80.dp

/** B2 — 코스 상세. 지도(Map Shell)는 그대로 보이고 이 위에 뒤로가기와 바텀시트만 얹는다 — 배경을
 * 깔지 않는다. 추천 결과에서 들어온 코스와 "내 코스"에서 들어온 코스가 같은 화면을 쓰고, 어느 쪽을
 * 어디서 읽을지는 [source]가 가른다([CourseDetailSource]). */
@Composable
fun CourseDetailScreen(
    source: CourseDetailSource,
    onBack: () -> Unit,
    onStartRunning: () -> Unit,
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

    LaunchedEffect(source) { viewModel.onIntent(CourseDetailIntent.Load(source)) }

    val readyState = state as? CourseDetailState.Ready
    if (readyState != null) {
        MapLayers(
            MapLayerSpec(
                MapConfig.SOURCE_COURSE_DETAIL_ROUTE,
                buildRecommendedRouteGeoJson(listOf(RecommendedRoute(id = source.id, points = readyState.path, selected = true))),
            ),
        )
    }
    val courseMarkers = remember(readyState?.path, readyState?.isLoop) {
        val detail = readyState ?: return@remember emptyList()
        listOfNotNull(
            detail.path.firstOrNull()?.let { MapPin(it, RunvealMapMarkerType.Start) },
            detail.path.lastOrNull()?.takeIf { !detail.isLoop }?.let { MapPin(it, RunvealMapMarkerType.End) },
        )
    }
    MapDotMarkers(courseMarkers)
    LaunchedEffect(readyState?.path, bottomSheetHeightPx) {
        val detail = readyState ?: return@LaunchedEffect
        val extraBottomPadding = with(density) { bottomSheetHeightPx.toDp() }
        mapState.focusOnBounds(detail.path, extraBottomPadding = extraBottomPadding)
    }

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
            // 공용 로딩 모션(시안 Z0) — Material 기본 스피너는 이 앱 어디에도 안 쓴다.
            is CourseDetailState.Loading -> RunvealLoading(modifier = Modifier.align(Alignment.Center).size(LOADING_SIZE))
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
                                // 저장된 코스는 저장해 둔 이름을 그대로 쓴다 — "코스 {이름}"으로
                                // 조합하면 "코스 코스 A"가 된다.
                                current.savedCourseName
                                    ?: stringResource(R.string.course_recommend_result_course_name, current.courseLabel),
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
                                    secondaryLabel = if (current.isSaved) {
                                        stringResource(R.string.course_detail_saved_label)
                                    } else {
                                        stringResource(R.string.course_detail_save_action)
                                    },
                                    onSecondaryClick = { viewModel.onIntent(CourseDetailIntent.SaveCourse) },
                                    primaryLabel = stringResource(R.string.course_detail_start_running_action),
                                    // 코스 경로를 기록 화면에 넘기기만 한다 — 가이드/오버레이 적용은 보류.
                                    onPrimaryClick = {
                                        RunvealAnalyticsLogger.logCourseStart(fromRecommend = true)
                                        // 저장된 코스일 때만 id를 같이 넘긴다 — 저장 전 추천 코스는
                                        // course 행이 없어서 "달림"을 적을 데가 없다.
                                        SelectedCourseHolder.publish(
                                            path = current.path,
                                            isLoop = current.isLoop,
                                            courseId = (source as? CourseDetailSource.Saved)?.id,
                                        )
                                        onStartRunning()
                                    },
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
