package com.sloth.runveal.courserecommend

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
import com.sloth.runveal.courserecommend.component.FilterBottomSheet
import com.sloth.runveal.courserecommend.contract.CourseRecommendResultIntent
import com.sloth.runveal.courserecommend.contract.CourseRecommendResultState
import com.sloth.runveal.courserecommend.contract.CustomDistanceValues
import com.sloth.runveal.courserecommend.contract.DistanceOptions
import com.sloth.runveal.courserecommend.contract.FilterOptions
import com.sloth.runveal.courserecommend.contract.RouteCardUiModel
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButton
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupSingle
import com.sloth.runveal.designsystem.component.button.RunvealButtonSize
import com.sloth.runveal.designsystem.component.button.RunvealButtonStyle
import com.sloth.runveal.designsystem.component.input.RunvealChipCondition
import com.sloth.runveal.designsystem.component.input.RunvealChipConditionIcon
import com.sloth.runveal.designsystem.component.input.RunvealChipConditionUsage
import com.sloth.runveal.designsystem.component.input.RunvealChipDropdown
import com.sloth.runveal.designsystem.component.input.RunvealChipDropdownUsage
import com.sloth.runveal.designsystem.component.list.RunvealCourseCard
import com.sloth.runveal.designsystem.component.list.RunvealCourseCardState
import com.sloth.runveal.designsystem.component.list.RunvealEmptyState
import com.sloth.runveal.designsystem.component.list.RunvealListItem
import com.sloth.runveal.designsystem.component.list.RunvealListItemSize
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerType
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.sloth.runveal.designsystem.component.overlay.RunvealLoading
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.courserecommend.R
import com.sloth.runveal.map.MapConfig
import kotlin.math.abs
import kotlinx.coroutines.launch
import com.sloth.runveal.designsystem.R as DesignSystemR

/** [RunvealCourseCard]의 고정 너비(240dp)와 맞춰야 캐러셀 가운데 정렬 계산이 들어맞는다. */
/** 공용 로딩 모션 크기(시안 Z0의 80). */
private val LOADING_SIZE = 80.dp

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
        // 추천 결과가 없을 땐 한 장짜리 안내 화면이다(시안 B1-1) — 뒤에 지도가 비치면 "코스를 못
        // 찾았다"면서 지도만 덩그러니 보이게 돼서, 화면 배경색으로 덮는다.
        if (readyState?.emptyMessage != null) {
            Box(modifier = Modifier.matchParentSize().background(RunvealTheme.colors.bg.canvas))
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                // 결과가 없을 때만 머리 영역을 불투명하게 둔다(시안 B1-1) — 코스가 있을 땐 지도 위에
                // 그대로 얹히는 앱바다.
                .then(
                    if (readyState?.emptyMessage != null) {
                        Modifier.background(RunvealTheme.colors.surface.raised)
                    } else {
                        Modifier
                    },
                )
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = onClose,
                // 지도 위에 얹힐 때만 글래스 원형 배경을 쓴다 — 결과가 없을 땐 불투명한 한 장짜리
                // 화면이라 아이콘만 둔다(시안 B1-1).
                style = if (readyState?.emptyMessage != null) {
                    RunvealTopAppBarStyle.Default
                } else {
                    RunvealTopAppBarStyle.Map
                },
                title = stringResource(R.string.course_recommend_result_title),
                modifier = Modifier.fillMaxWidth(),
            )
            // 결과가 없을 땐 칩을 숨긴다(시안 B1-1) — 보여 줄 코스가 없는데 조건만 떠 있으면 설명과
            // "조건 다시 설정하기" 버튼이 하는 말과 겹친다.
            if (readyState != null && readyState.emptyMessage == null) {
                SettingsSummaryRow(
                    settings = readyState.settings,
                    onSettingsClick = { viewModel.onIntent(CourseRecommendResultIntent.OpenFilterSheet) },
                    modifier = Modifier.padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space12),
                )
            }
        }

        when (val current = state) {
            is CourseRecommendResultState.Loading ->
                RunvealLoading(modifier = Modifier.align(Alignment.Center).size(LOADING_SIZE))
            is CourseRecommendResultState.Ready -> {
                if (current.emptyMessage != null) {
                    EmptyResult(
                        message = current.emptyMessage,
                        settings = current.settings,
                        onIntent = viewModel::onIntent,
                        onRetry = onClose,
                        modifier = Modifier.align(Alignment.TopStart),
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

        // 조건을 바꿔 다시 받는 중 — 맨 위에 덮어 카드·칩까지 가리고 탭도 막는다. 지도와 화면 뼈대는
        // 그대로 둬서, 화면이 통째로 비어 "뒤로 간 것처럼" 보이지 않게 한다.
        if (readyState?.isReloading == true) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(RunvealTheme.colors.bg.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) {
                RunvealLoading(modifier = Modifier.size(LOADING_SIZE))
            }
        }
    }

    readyState?.filterDraft?.let { draft ->
        FilterSheet(draft = draft, onIntent = viewModel::onIntent)
    }
}

/** B0에서 고른 거리·안 가본 길 우선·필터를 그대로 보여주는 요약 칩 — 코스 만들기 바텀 패널과 같은
 * 칩이지만 지도 위에 바로 떠서 글래스(OnMap) 스타일을 쓴다.
 *
 * 셋 중 아무거나 누르면 코스 조건 설정 시트가 열린다 — 어느 칩을 눌렀든 바꿀 수 있는 조건은 같다. */
@Composable
private fun SettingsSummaryRow(
    settings: CourseRecommendSettings,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
        RunvealChipDropdown(
            label = settings.distanceLabel,
            onClick = onSettingsClick,
            usage = RunvealChipDropdownUsage.OnMap,
        )
        // 코스 만들기 패널과 같은 칩 세 개를 **항상** 보여준다 — 켠 것만 남기면 "안 가본 길 우선을
        // 끄고 추천받은 건지, 애초에 안 보여준 건지"를 결과 화면에서 알 수 없다.
        RunvealChipCondition(
            checked = settings.avoidVisitedPreferred,
            onCheckedChange = { onSettingsClick() },
            label = stringResource(R.string.course_recommend_result_avoid_visited_label),
            usage = RunvealChipConditionUsage.OnMap,
        )
        RunvealChipCondition(
            checked = settings.selectedFilterCount > 0,
            onCheckedChange = { onSettingsClick() },
            icon = RunvealChipConditionIcon.Filter,
            // 고른 필터가 없으면 아이콘만 — 코스 만들기 패널과 같은 규칙이다.
            label = settings.selectedFilterCount.takeIf { it > 0 }?.toString(),
            usage = RunvealChipConditionUsage.OnMap,
        )
    }
}

@Composable
private fun EmptyResult(
    message: String,
    settings: CourseRecommendSettings,
    onIntent: (CourseRecommendResultIntent) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                // 앱바(56) 아래 24 — 시안(B1-1)의 Contents y=188 자리다.
                .padding(top = RunvealTheme.layout.sizeTopAppBar + RunvealTheme.spacing.space24)
                .padding(horizontal = RunvealTheme.layout.marginScreen),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
        ) {
            RunvealEmptyState(
                title = stringResource(R.string.course_recommend_result_empty_title),
                // 왜 못 찾았는지는 서버·필터 상황마다 달라서 그대로 받아 설명 줄에 쓴다.
                description = message,
                graphic = {
                    Image(painter = painterResource(R.drawable.empty_course), contentDescription = null)
                },
            )
            EmptyResultHints(settings = settings, onIntent = onIntent)
        }

        RunvealButtonGroup(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) {
            RunvealButtonGroupSingle(
                label = stringResource(R.string.course_recommend_result_empty_action),
                onClick = onRetry,
            )
        }
    }
}

/** 안내 줄 하나 — 문구와 "누르면 할 일"을 묶어 둔다. */
private class EmptyHint(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit,
)

/**
 * 조건을 어떻게 바꾸면 되는지 알려주는 줄(시안 B1-1 "설정변경 안내"). 각 줄은 **그 줄이 말하는 것을
 * 그대로 해 주는 버튼**이다 — 순환 코스는 도착지를 비워 바로 다시 받고, 거리·필터는 코스 조건 설정
 * 시트를 띄운다(상단 조건 칩과 같은 시트).
 *
 * 이미 그렇게 되어 있어서 누를 게 없는 줄은 빼놓는다 — 도착지가 없으면(이미 순환) 순환 줄을, 고른
 * 필터가 없으면 필터 줄을 숨긴다. 눌러도 아무 일도 안 나는 줄을 남기면 고장으로 보인다.
 */
@Composable
private fun EmptyResultHints(
    settings: CourseRecommendSettings,
    onIntent: (CourseRecommendResultIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16
    val loopHint = EmptyHint(
        icon = RunvealIcons.Circulation,
        title = stringResource(R.string.course_recommend_result_empty_hint_loop_title),
        description = stringResource(R.string.course_recommend_result_empty_hint_loop_description),
        onClick = { onIntent(CourseRecommendResultIntent.RetryAsLoopCourse) },
    )
    val distanceHint = EmptyHint(
        icon = RunvealIcons.Distance,
        title = stringResource(R.string.course_recommend_result_empty_hint_distance_title),
        description = stringResource(R.string.course_recommend_result_empty_hint_distance_description),
        onClick = { onIntent(CourseRecommendResultIntent.OpenFilterSheet) },
    )
    val filterHint = EmptyHint(
        icon = RunvealIcons.Filter,
        title = stringResource(R.string.course_recommend_result_empty_hint_filter_title),
        description = stringResource(R.string.course_recommend_result_empty_hint_filter_description),
        onClick = { onIntent(CourseRecommendResultIntent.OpenFilterSheet) },
    )
    val hints = listOfNotNull(
        loopHint.takeIf { settings.endLatLon != null },
        distanceHint,
        filterHint.takeIf { settings.selectedFilterCount > 0 },
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.default, shape)
            .border(1.dp, colors.border.default, shape)
            .padding(horizontal = RunvealTheme.spacing.space16),
    ) {
        hints.forEachIndexed { index, hint ->
            RunvealListItem(
                title = hint.title,
                description = hint.description,
                leadingIcon = hint.icon,
                size = RunvealListItemSize.L,
                onClick = hint.onClick,
                showDivider = index != hints.lastIndex,
            )
        }
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

/**
 * 상단 필터 칩으로 여는 코스 조건 설정 시트. 코스 만들기(B0)와 **같은 시트**를 쓰고, 고치는 동안은
 * 초안([CourseRecommendResultState.Ready.filterDraft])에만 쌓였다가 "코스 조건 적용하기"를 눌러야
 * 실제로 다시 추천받는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(draft: CourseRecommendSettings, onIntent: (CourseRecommendResultIntent) -> Unit) {
    fun update(block: CourseRecommendSettings.() -> CourseRecommendSettings) {
        onIntent(CourseRecommendResultIntent.UpdateFilterDraft(draft.block()))
    }

    ModalBottomSheet(
        onDismissRequest = { onIntent(CourseRecommendResultIntent.CloseFilterSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        FilterBottomSheet(
            distanceOptions = DistanceOptions,
            selectedDistance = draft.selectedDistance,
            onDistanceSelect = { update { copy(selectedDistance = it, isCustomDistanceSelected = false) } },
            isCustomDistanceSelected = draft.isCustomDistanceSelected,
            customDistanceValues = CustomDistanceValues,
            customDistanceIndex = draft.customDistanceIndex,
            onCustomDistanceChipClick = { update { copy(isCustomDistanceSelected = true) } },
            onCustomDistanceIndexChange = { update { copy(customDistanceIndex = it) } },
            avoidVisitedPreferred = draft.avoidVisitedPreferred,
            onAvoidVisitedPreferredChange = { update { copy(avoidVisitedPreferred = it) } },
            filterOptions = FilterOptions,
            selectedFilters = draft.selectedFilters,
            onFilterToggle = { filter ->
                update {
                    val filters = if (filter in selectedFilters) selectedFilters - filter else selectedFilters + filter
                    copy(selectedFilters = filters)
                }
            },
            onBack = { onIntent(CourseRecommendResultIntent.CloseFilterSheet) },
            onReset = { onIntent(CourseRecommendResultIntent.ResetFilterDraft) },
            onApply = { onIntent(CourseRecommendResultIntent.ApplyFilterDraft) },
        )
    }
}
