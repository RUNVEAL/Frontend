package com.swyp.runveal.running

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.core.map.LocalMapState
import com.swyp.runveal.core.map.MapLayerSpec
import com.swyp.runveal.core.map.MapLayers
import com.swyp.runveal.core.map.RecommendedRoute
import com.swyp.runveal.core.map.buildRecommendedRouteGeoJson
import com.swyp.runveal.core.map.rememberMapState
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupEqual
import com.swyp.runveal.designsystem.component.button.RunvealRunPauseButton
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.running.R
import com.swyp.runveal.map.GeoMath
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.map.MapConfig
import com.swyp.runveal.recording.TrackRecorder
import com.swyp.runveal.running.contract.RecordingEffect
import com.swyp.runveal.running.contract.RecordingIntent
import com.swyp.runveal.running.contract.RecordingPhase
import com.swyp.runveal.running.contract.RecordingState
import com.swyp.runveal.tracking.TrackingService
import com.swyp.runveal.ui.mvi.CollectEffect
import kotlinx.coroutines.delay

private const val COURSE_GUIDE_TICKER_INTERVAL_MS = 1000L

/** "이 코스로 러닝 시작"일 때만 패널에 더 보여주는 코스 진행 정보 — [RecordingScreen]이 매초 계산해서
 * [RecordingPanel]까지 내려준다. */
private data class CourseProgress(val remainingDistanceLabel: String, val progressPercent: Int)

/** [RunvealRunPauseButton]의 실제 크기(80dp, 토큰 없이 하드코딩됨)와 맞춘다. */
private val RECORDING_ACTION_HEIGHT = 80.dp

/** 디자인의 locate 버튼 크기(보이는 원 44dp, 터치 영역 48dp) — [RunvealIconButton]의 M/L 어느 쪽도
 * 배경(glass)이 정확히 맞지 않아 토큰을 직접 조합해 하드코딩했다. */
private val LOCATE_BUTTON_VISIBLE_SIZE = 44.dp
private val LOCATE_BUTTON_TOUCH_SIZE = 48.dp

/** 바텀시트 드래그 핸들([RunvealBottomSheet])과 같은 모양의 범례 선 스와치 크기 — 핸들은 36x4지만
 * 12sp 라벨 옆에 쓰기엔 너무 커서 절반 정도로 줄였다. */
private val LEGEND_SWATCH_WIDTH = 16.dp
private val LEGEND_SWATCH_HEIGHT = 3.dp
private val LEGEND_SWATCH_HALO_SIZE = 10.dp
private val LEGEND_SWATCH_HALO_PADDING = 3.dp
private val LEGEND_SWATCH_HALO_BLUR = 4.dp
private val LEGEND_SWATCH_LABEL_GAP = 6.dp

/**
 * "기록 시작/종료" 오버레이 — Map Shell(`feature:home`) 위에 얹힌다. `recording-lifecycle.md` §3/§5.
 * 지도 위 꼬리·확정 구간 렌더링(§4)은 실시간 매칭 레이어(이번 범위 밖)가 붙기 전까지는 비워 둔다.
 *
 * 이 화면에 들어오는 경로는 홈 지도의 "러닝 시작" 버튼, 또는 코스 상세의 "이 코스로 러닝 시작"
 * 버튼 — 어느 쪽이든 진입 자체를 "시작" 의도로 본다. 별도의 "시작" 버튼을 다시 보여주지 않고
 * [LaunchedEffect]로 바로 기록을 시작한다.
 */
@Composable
fun RecordingScreen(
    onClose: () -> Unit,
    onDiscarded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordingViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            RecordingEffect.StartTrackingService -> TrackingService.start(context)
            RecordingEffect.PauseTrackingService -> TrackingService.pause(context)
            RecordingEffect.ResumeTrackingService -> TrackingService.resume(context)
            // 결과 화면으로 넘어가는 건 서비스를 실제로 멈춘 **뒤**에 한다. 종료 버튼에서 바로
            // onClose()를 부르면 이 화면이 pop되면서 effect 수집기가 취소돼, Channel에 들어간
            // StopTrackingService가 전달되지 못한다 — 서비스가 계속 돌아 기록이 저장되지도,
            // TrackResultHolder가 publish되지도 않아서 결과 화면이 무한 로딩에 빠졌다.
            is RecordingEffect.StopTrackingService -> {
                TrackingService.stop(context, saveRecord = effect.saveRecord)
                // 100m/1분을 못 채운 러닝은 저장하지 않으니 보여줄 결과도 없다 — 결과 화면을 건너뛰고
                // 홈으로 돌려보낸다.
                if (effect.saveRecord) onClose() else onDiscarded()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (state.phase == RecordingPhase.IDLE) viewModel.onIntent(RecordingIntent.StartClicked)
    }

    // 뒤로가기로 화면을 못 벗어나게 한다 — 종료는 반드시 "종료하기" 버튼으로.
    BackHandler(enabled = true) {}

    // "이 코스로 러닝 시작"으로 들어왔으면(selectedCoursePath) 그 코스를 흰 점선 안내선으로 그린다 —
    // 이미 지나온 구간은 매초 지금 위치 기준으로 잘라내서 "오늘 달린 길"과 안 겹치게 한다. 같은 주기로
    // 남은 거리·진행률([CourseProgress])도 계산해서 패널에 넘긴다.
    val selectedCoursePath = state.selectedCoursePath
    var courseProgress by remember { mutableStateOf<CourseProgress?>(null) }
    if (selectedCoursePath != null && selectedCoursePath.size >= 2) {
        val mapState = LocalMapState.current
        // 러닝이 시작되는 시점에 한 번 코스 전체가 보이게 카메라를 맞춘다 — 이후 위치가 움직여도
        // 다시 맞추지 않는다(계속 다시 맞추면 러닝 중 카메라가 제멋대로 움직이는 것처럼 보인다).
        LaunchedEffect(selectedCoursePath) { mapState.focusOnBounds(selectedCoursePath) }

        val totalCourseDistanceMeters = remember(selectedCoursePath) { GeoMath.totalDistanceMeters(selectedCoursePath) }
        var courseGuideGeoJson by remember {
            mutableStateOf(buildRecommendedRouteGeoJson(listOf(RecommendedRoute(id = "selected-course", points = selectedCoursePath, selected = true))))
        }
        LaunchedEffect(selectedCoursePath) {
            while (true) {
                val currentLocation = TrackRecorder.currentFilteredPoints().lastOrNull()?.let { LatLon(lat = it.lat, lon = it.lon) }
                val remainingPath = currentLocation?.let { remainingCoursePath(selectedCoursePath, it) } ?: selectedCoursePath
                courseGuideGeoJson = buildRecommendedRouteGeoJson(listOf(RecommendedRoute(id = "selected-course", points = remainingPath, selected = true)))
                val remainingDistanceMeters = GeoMath.totalDistanceMeters(remainingPath)
                val progressPercent = if (totalCourseDistanceMeters <= 0.0) {
                    0
                } else {
                    (((totalCourseDistanceMeters - remainingDistanceMeters) / totalCourseDistanceMeters) * 100).toInt().coerceIn(0, 100)
                }
                courseProgress = CourseProgress(
                    remainingDistanceLabel = "%.2f".format(remainingDistanceMeters / 1000.0),
                    progressPercent = progressPercent,
                )
                delay(COURSE_GUIDE_TICKER_INTERVAL_MS)
            }
        }
        MapLayers(MapLayerSpec(MapConfig.SOURCE_RECORDING_COURSE_GUIDE, courseGuideGeoJson))
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.phase != RecordingPhase.IDLE) {
            NewRoadBadge(
                distanceLabel = state.newRoadLabel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = RunvealTheme.layout.marginScreen, top = RunvealTheme.spacing.space16),
            )
        }
        when (state.phase) {
            // 시작 직후 TrackRecorder.state가 아직 안 바뀐 찰나 — 그릴 게 없다.
            RecordingPhase.IDLE -> Unit
            RecordingPhase.RECORDING -> RecordingOverlay(
                state = state,
                courseProgress = courseProgress,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                RunvealRunPauseButton(onClick = { viewModel.onIntent(RecordingIntent.PauseClicked) })
            }
            RecordingPhase.PAUSED -> RecordingOverlay(
                state = state,
                courseProgress = courseProgress,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                RunvealButtonGroupEqual(
                    firstLabel = stringResource(R.string.recording_stop_button),
                    // 화면 이동은 StopTrackingService effect를 처리한 뒤에 한다(위 CollectEffect).
                    onFirstClick = { viewModel.onIntent(RecordingIntent.StopClicked) },
                    secondLabel = stringResource(R.string.recording_resume_button),
                    onSecondClick = { viewModel.onIntent(RecordingIntent.ResumeClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** [coursePath]에서 [currentLocation]과 가장 가까운 점부터 끝까지만 남긴다 — 이미 지나온 구간은
 * 점선 안내선에서 빼서 "오늘 달린 길"과 안 겹쳐 보이게 한다. 가장 가까운 점 기준이라 코스를
 * 역주행하면 오탐할 수 있지만, 정방향으로 뛴다는 전제의 단순 추정이다. */
private fun remainingCoursePath(coursePath: List<LatLon>, currentLocation: LatLon): List<LatLon> {
    val nearestIndex = coursePath.indices.minByOrNull { GeoMath.distanceMeters(coursePath[it], currentLocation) } ?: 0
    return coursePath.subList(nearestIndex, coursePath.size)
}

/** 좌상단 "새로 탐험한 길" 배지 — 총 거리가 아니라 이미 가본 구간을 뺀 거리다. 지도가 금색으로 칠하는
 * 구간과 같은 `LiveRoadClassifier` 결과를 쓴다(전부 로컬 판정, `docs/api/API.md` §3.5). 서버 매칭이
 * 아니라서 종료 후 최종 매칭(§6)으로 계산하는 결과 화면 값과는 조금 다를 수 있다. */
@Composable
private fun NewRoadBadge(distanceLabel: String, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Row(
        modifier = modifier
            .background(colors.surface.raised, RunvealTheme.shapes.full)
            .padding(horizontal = RunvealTheme.spacing.space12, vertical = RunvealTheme.spacing.space8),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.recording_legend_new_label),
            style = RunvealTheme.typography.bodyMSemibold,
            color = colors.text.secondary,
        )
        Text(
            stringResource(R.string.recording_new_distance_format, distanceLabel),
            style = RunvealTheme.typography.bodyMSemibold,
            color = colors.text.accent,
        )
    }
}

/** 범례+내 위치 줄 + 패널(16dp 간격) — 기록 중/일시정지 공통. */
@Composable
private fun RecordingOverlay(
    state: RecordingState,
    courseProgress: CourseProgress? = null,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
    ) {
        val mapState = LocalMapState.current
        MapLegendRow(
            onLocateClick = { mapState.recenterToCurrentLocation() },
            showCourseGuide = state.selectedCoursePath != null,
            modifier = Modifier.padding(horizontal = RunvealTheme.layout.marginScreen),
        )
        RecordingPanel(state = state, courseProgress = courseProgress, action = action)
    }
}

/** 지도 범례 칩([showCourseGuide]면 추천 코스 칩이 그 위에 따로 한 줄 더) + 내 위치 버튼. */
@Composable
private fun MapLegendRow(onLocateClick: () -> Unit, showCourseGuide: Boolean, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            if (showCourseGuide) {
                LegendChip {
                    // 지도 위 안내선([CourseGuideLayer])과 같은 색(text.primary, 흰색) · 같은 점선.
                    LegendItem(
                        swatchColor = colors.text.primary,
                        label = stringResource(R.string.recording_legend_course_label),
                        dashed = true,
                    )
                }
            }
            LegendChip {
                LegendItem(swatchColor = colors.map.roadToday, label = stringResource(R.string.recording_legend_today_label))
                LegendItem(
                    swatchColor = colors.map.roadNew,
                    label = stringResource(R.string.recording_legend_new_label),
                    haloColor = colors.map.roadNewHalo,
                )
            }
        }

        Box(modifier = Modifier.size(LOCATE_BUTTON_TOUCH_SIZE), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(LOCATE_BUTTON_VISIBLE_SIZE)
                    .clip(RunvealTheme.shapes.full)
                    .background(colors.surface.default, RunvealTheme.shapes.full)
                    .border(1.dp, colors.border.default, RunvealTheme.shapes.full)
                    .clickable(onClick = onLocateClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = RunvealIcons.Locate,
                    contentDescription = stringResource(R.string.recording_locate_content_description),
                    tint = colors.icon.primary,
                    modifier = Modifier.size(RunvealTheme.layout.sizeIcon),
                )
            }
        }
    }
}

/** 범례 칩 하나 — 배경(`bg.scrim`)으로 묶인 [LegendItem] 한 줄. "추천 코스"는 단독 칩으로,
 * "오늘 달린 길"/"새로 탐험한 길"은 같은 칩 안에 묶는다([MapLegendRow]). */
@Composable
private fun LegendChip(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .background(RunvealTheme.colors.bg.scrim, RunvealTheme.shapes.radius8)
            .padding(horizontal = RunvealTheme.spacing.space12, vertical = RunvealTheme.spacing.space8),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        content = content,
    )
}

/** 범례 한 칸 — 지도 선 색(바텀시트 드래그 핸들과 같은 모양의 작은 선 스와치) + 라벨. [haloColor]를 주면
 * 실제 지도에 도로가 칠해질 때와 같은 번짐 효과를 작게 얹는다([VisitedRoadLayer]의 glow와 같은 색
 * 페어 — `map.roadNew`/`map.roadNewHalo`). `오늘 달린 길`(roadToday)은 번짐 토큰이 없어 그대로 둔다.
 * [dashed]면 실선 대신 점선 스와치 — 추천 코스 안내선([CourseGuideLayer])처럼 지도에 점선으로 그리는
 * 레이어의 범례용. */
@Composable
private fun LegendItem(
    swatchColor: Color,
    label: String,
    modifier: Modifier = Modifier,
    haloColor: Color? = null,
    dashed: Boolean = false,
) {
    Row(
        modifier = modifier,
        // 디자인 값 6dp — spacing 스케일에 4/8만 있고 6은 없어 그대로 하드코딩.
        horizontalArrangement = Arrangement.spacedBy(LEGEND_SWATCH_LABEL_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (haloColor != null) {
                Box(
                    modifier = Modifier
                        .size(width = LEGEND_SWATCH_WIDTH + LEGEND_SWATCH_HALO_PADDING * 2, height = LEGEND_SWATCH_HALO_SIZE)
                        .blur(LEGEND_SWATCH_HALO_BLUR)
                        .clip(RunvealTheme.shapes.full)
                        .background(haloColor, RunvealTheme.shapes.full),
                )
            }
            if (dashed) {
                Canvas(modifier = Modifier.size(width = LEGEND_SWATCH_WIDTH, height = LEGEND_SWATCH_HEIGHT)) {
                    // [CourseGuideLayer]의 지도 선과 같은 비율(대시:간격 = 1.4:0.5) — 살짝 긴 대시, 촘촘한 간격.
                    drawLine(
                        color = swatchColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = size.height,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(size.height * 1.4f, size.height * 0.5f), 0f),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(width = LEGEND_SWATCH_WIDTH, height = LEGEND_SWATCH_HEIGHT)
                        .clip(RunvealTheme.shapes.full)
                        .background(swatchColor, RunvealTheme.shapes.full),
                )
            }
        }
        Text(label, style = RunvealTheme.typography.captionMRegular, color = RunvealTheme.colors.text.secondary)
    }
}

/** 기록 중/일시정지 공통 패널 — 거리, 경과 시간/평균 페이스(+[courseProgress]가 있으면 남은 거리·코스
 * 진행), 하단 액션([action])만 상태별로 다르다. 바텀시트처럼 화면 하단에 깔리지만 드래그 핸들은 없다. */
@Composable
private fun RecordingPanel(
    state: RecordingState,
    courseProgress: CourseProgress?,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    val colors = RunvealTheme.colors
    val shape = RoundedCornerShape(topStart = RunvealTheme.radius.radius24, topEnd = RunvealTheme.radius.radius24)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.raised, shape)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(
                start = RunvealTheme.layout.marginScreen,
                top = RunvealTheme.spacing.space24,
                end = RunvealTheme.layout.marginScreen,
                bottom = RunvealTheme.spacing.space32,
            ),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space20),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                Text(
                    state.distanceLabel,
                    style = RunvealTheme.typography.numDisplay,
                    color = colors.text.primary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    stringResource(R.string.recording_distance_unit),
                    style = RunvealTheme.typography.unitDisplay,
                    color = colors.text.secondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            if (courseProgress != null) {
                Text(
                    stringResource(R.string.recording_course_remaining_distance_format, courseProgress.remainingDistanceLabel),
                    style = RunvealTheme.typography.bodyMRegular,
                    color = colors.text.secondary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            RecordingStat(
                label = stringResource(R.string.recording_elapsed_label),
                value = state.elapsedLabel,
                modifier = Modifier.weight(1f),
            )
            RecordingStat(
                label = stringResource(R.string.recording_pace_label),
                value = state.paceLabel,
                modifier = Modifier.weight(1f),
            )
            if (courseProgress != null) {
                RecordingStat(
                    label = stringResource(R.string.recording_course_progress_label),
                    value = courseProgress.progressPercent.toString(),
                    unit = stringResource(R.string.recording_course_progress_unit),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 일시정지 원형 버튼(80dp)과 종료하기/재개 버튼 행(52dp) 높이가 달라, 고정 높이로 감싸지 않으면
        // 상태가 바뀔 때마다 패널 전체 높이가 바뀌어 위 거리/스탯 위치가 같이 움직인다.
        Box(
            modifier = Modifier.fillMaxWidth().height(RECORDING_ACTION_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            action()
        }
    }
}

/** [unit]을 주면(코스 진행의 "%") 값·단위를 한 줄로, 안 주면(경과 시간/평균 페이스) 값만. */
@Composable
private fun RecordingStat(label: String, value: String, modifier: Modifier = Modifier, unit: String? = null) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
        if (unit != null) {
            Row(
                // 디자인 값 2dp — spacing 스케일에 없어 그대로 하드코딩.
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    value,
                    style = RunvealTheme.typography.numM,
                    color = colors.text.primary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    unit,
                    style = RunvealTheme.typography.unitM,
                    color = colors.text.secondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        } else {
            Text(value, style = RunvealTheme.typography.numM, color = colors.text.primary)
        }
    }
}

@Preview
@Composable
private fun RecordingOverlayRecordingPreview() {
    RunvealTheme {
        // RecordingOverlay 안의 "내 위치" 버튼이 LocalMapState.current를 읽어 더미 값을 제공한다.
        CompositionLocalProvider(LocalMapState provides rememberMapState()) {
            Box(Modifier.fillMaxSize()) {
                RecordingOverlay(
                    state = RecordingState(phase = RecordingPhase.RECORDING, elapsedLabel = "14:08", distanceLabel = "2.34", paceLabel = "6'02″"),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    RunvealRunPauseButton(onClick = {})
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecordingOverlayPausedPreview() {
    RunvealTheme {
        CompositionLocalProvider(LocalMapState provides rememberMapState()) {
            Box(Modifier.fillMaxSize()) {
                RecordingOverlay(
                    state = RecordingState(phase = RecordingPhase.PAUSED, elapsedLabel = "14:08", distanceLabel = "2.34", paceLabel = "6'02″"),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    RunvealButtonGroupEqual(
                        firstLabel = "종료하기",
                        onFirstClick = {},
                        secondLabel = "재개",
                        onSecondClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
