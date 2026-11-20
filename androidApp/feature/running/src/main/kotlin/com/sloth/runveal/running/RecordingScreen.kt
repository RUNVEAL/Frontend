package com.sloth.runveal.running

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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.core.map.LocalMapState
import com.sloth.runveal.core.map.MapDotMarkers
import com.sloth.runveal.core.map.MapLayerSpec
import com.sloth.runveal.core.map.MapLayers
import com.sloth.runveal.core.map.MapPin
import com.sloth.runveal.core.map.RecommendedRoute
import com.sloth.runveal.core.map.buildRecommendedRouteGeoJson
import com.sloth.runveal.core.map.rememberMapState
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupEqual
import com.sloth.runveal.designsystem.component.button.RunvealRunPauseButton
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerType
import com.sloth.runveal.designsystem.component.overlay.RunvealDialog
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.running.R
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.MapConfig
import com.sloth.runveal.permission.RequestNotificationPermissionOnce
import com.sloth.runveal.recording.CourseProgressTracker
import com.sloth.runveal.recording.TrackRecorder
import com.sloth.runveal.running.contract.RecordingEffect
import com.sloth.runveal.running.contract.RecordingIntent
import com.sloth.runveal.running.contract.RecordingPhase
import com.sloth.runveal.running.contract.RecordingState
import com.sloth.runveal.tracking.TrackingService
import com.sloth.runveal.ui.mvi.CollectEffect
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
    val mapState = LocalMapState.current

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

    // 이 화면에 들어온 것 자체가 "러닝 시작"이라, 알림 권한을 묻기에 맥락이 가장 분명한 자리다
    // (앱 첫 실행에 몰아서 묻지 않는다). 러닝을 막지 않으므로 결과를 기다리지도, 보지도 않는다 —
    // 거부되면 지속 알림이 안 보일 뿐 TrackingService는 그대로 돈다.
    RequestNotificationPermissionOnce()

    LaunchedEffect(Unit) {
        if (state.phase == RecordingPhase.IDLE) viewModel.onIntent(RecordingIntent.StartClicked)
    }

    // 뒤로가기로 화면을 못 벗어나게 한다 — 종료는 반드시 "종료하기" 버튼으로. 확인 다이얼로그가
    // 떠 있을 때만 뒤로가기가 그걸 닫는다(Dialog가 자기 뒤로가기를 먼저 먹지만, 그 전에 이 화면이
    // 가로채는 경우에도 같이 닫히게 둔다).
    BackHandler(enabled = true) {
        if (state.finishDialog != null) viewModel.onIntent(RecordingIntent.StopDismissed)
    }

    // 러닝 중 지도 카메라 — 달리는 동안엔 현재 위치를 계속 따라가고(북쪽 고정), 일시정지하면 멈춘다.
    // 줌은 **시작할 때 한 번만** 맞춘다: 재개할 때 다시 맞추면 그 사이 사용자가 보려고 조절해 둔
    // 배율이 멋대로 되돌아간다("내 위치" 버튼도 같은 이유로 줌을 안 건드린다).
    var isFirstStart by remember { mutableStateOf(true) }
    LaunchedEffect(state.phase) {
        when (state.phase) {
            RecordingPhase.RECORDING -> {
                mapState.startFollowingCurrentLocation(zoom = if (isFirstStart) MapConfig.RECORDING_ZOOM else null)
                isFirstStart = false
            }
            RecordingPhase.PAUSED -> mapState.stopFollowingCurrentLocation()
            RecordingPhase.IDLE -> Unit
        }
    }
    // 하단 패널이 지도를 가리는 높이를 지도에 알려준다 — 현재 위치가 화면 가운데가 아니라 **패널 위
    // 보이는 영역의 가운데**에 오게 한다. 패널 높이는 기록/일시정지·코스 진행 표시 유무로 달라져서
    // 실제로 재서(onSizeChanged) 넘긴다.
    var panelHeight by remember { mutableStateOf(0.dp) }
    LaunchedEffect(panelHeight) { mapState.followBottomInset = panelHeight }
    // 지도(Map Shell)는 이 화면보다 오래 살아 있다 — 나갈 때 끄지 않으면 홈 지도가 계속 현재 위치를
    // 따라다닌다. 패널만큼 아래를 비워 두던 카메라 여백도 이때 같이 되돌린다(러닝 중에는 그대로 둬야
    // 일시정지·드래그 때 지도가 튀지 않는다 — stopFollowingCurrentLocation KDoc).
    DisposableEffect(Unit) {
        // 러닝 중에는 내 위치 마커에 지금 바라보는 방향을 화살표로 띄운다.
        mapState.showsHeadingIndicator = true
        onDispose {
            mapState.showsHeadingIndicator = false
            mapState.stopFollowingCurrentLocation(resetAnchor = true)
            mapState.followBottomInset = 0.dp
        }
    }

    // "이 코스로 러닝 시작"으로 들어왔으면(selectedCoursePath) 그 코스를 흰 점선 안내선으로 그린다.
    // 안내선은 현위치와 무관하게 추천 코스 **전체**를 그대로 유지한다 — MVP 1차에서는 현위치에 따라
    // 코스를 바꾸지 않는다. 매초 남은 거리·진행률([CourseProgress])만 따로 계산해서 패널에 넘긴다.
    val selectedCoursePath = state.selectedCoursePath
    var courseProgress by remember { mutableStateOf<CourseProgress?>(null) }
    if (selectedCoursePath != null && selectedCoursePath.size >= 2) {
        // 코스 전체가 보이게 카메라를 맞추지는 않는다 — 러닝 중 카메라는 현재 위치를 따라가는 게
        // 우선이라(위 추적 블록) 코스에 맞춰 봐도 곧 현재 위치로 끌려간다. 코스는 안내선으로만
        // 보여주고, 전체 모양을 보고 싶으면 지도를 직접 줌아웃하면 된다(그때 추적도 풀린다).

        // 출발·도착 핀 — 코스 상세/추천 결과 화면과 같은 규칙으로, 순환 코스는 도착이 곧 출발이라
        // 같은 자리에 마커가 겹치지 않게 출발만 띄운다.
        val courseMarkers = remember(selectedCoursePath, state.selectedCourseIsLoop) {
            listOfNotNull(
                selectedCoursePath.firstOrNull()?.let { MapPin(it, RunvealMapMarkerType.Start) },
                selectedCoursePath.lastOrNull()?.takeIf { !state.selectedCourseIsLoop }
                    ?.let { MapPin(it, RunvealMapMarkerType.End) },
            )
        }
        MapDotMarkers(courseMarkers)

        // 코스가 바뀌지 않는 한 안내선도 그대로다 — 매초 다시 만들 이유가 없다.
        val courseGuideGeoJson = remember(selectedCoursePath) {
            buildRecommendedRouteGeoJson(listOf(RecommendedRoute(id = "selected-course", points = selectedCoursePath, selected = true)))
        }
        // 진행률은 "달린 거리"가 아니라 **코스를 실제로 지나간 만큼**으로 센다([CourseProgressTracker]).
        // 달린 거리로 세면 코스와 상관없는 곳을 달려도 진행률이 오르고, 코스를 다 안 돌았는데 거리만
        // 채워도 100%가 됐다. 순환 코스는 출발점과 도착점이 같은 자리라 "현위치에서 가장 가까운 점"
        // 방식도 못 쓴다 — 출발하자마자 도착점이 잡혀서 100%로 튄다.
        val progressTracker = remember(selectedCoursePath) { CourseProgressTracker(selectedCoursePath) }
        LaunchedEffect(selectedCoursePath) {
            var consumedPointCount = 0
            while (true) {
                // 매초 "마지막 위치"만 보면 그 사이 지나친 코스 구간을 놓칠 수 있어, 아직 안 본 점을 전부 넘긴다.
                val filteredPoints = TrackRecorder.currentFilteredPoints()
                if (filteredPoints.size > consumedPointCount) {
                    progressTracker.advance(
                        filteredPoints.drop(consumedPointCount).map { LatLon(lat = it.lat, lon = it.lon) },
                    )
                    consumedPointCount = filteredPoints.size
                }
                courseProgress = CourseProgress(
                    remainingDistanceLabel = "%.2f".format(progressTracker.remainingMeters / 1000.0),
                    progressPercent = progressTracker.progressPercent,
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
                onPanelHeightChange = { panelHeight = it },
                courseProgress = courseProgress,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                RunvealRunPauseButton(onClick = { viewModel.onIntent(RecordingIntent.PauseClicked) })
            }
            RecordingPhase.PAUSED -> RecordingOverlay(
                state = state,
                onPanelHeightChange = { panelHeight = it },
                courseProgress = courseProgress,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                RunvealButtonGroupEqual(
                    firstLabel = stringResource(R.string.recording_stop_button),
                    // 바로 끝내지 않고 한 번 더 묻는다 — 실제 종료는 다이얼로그의 "끝내기"에서.
                    onFirstClick = { viewModel.onIntent(RecordingIntent.StopRequested) },
                    secondLabel = stringResource(R.string.recording_resume_button),
                    onSecondClick = { viewModel.onIntent(RecordingIntent.ResumeClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // RunvealDialog는 내용만 그리는 컴포넌트라(그 KDoc 참고) Compose의 Dialog로 감싼다 —
        // 바깥을 누르거나 뒤로가기로 닫히는 동작을 거기서 얻는다(기록 상세의 삭제 다이얼로그와 같다).
        state.finishDialog?.let { finishDialog ->
            // 100m·1분을 못 채운 러닝은 저장되지 않으니, 끝내면 기록이 남는다는 안내를 그대로 쓸 수
            // 없다 — 문구와 버튼을 "저장 없이 종료"로 바꿔 끝내기 전에 알린다.
            val willSave = finishDialog.willSaveRecord
            Dialog(onDismissRequest = { viewModel.onIntent(RecordingIntent.StopDismissed) }) {
                RunvealDialog(
                    title = stringResource(
                        if (willSave) {
                            R.string.recording_finish_dialog_title
                        } else {
                            R.string.recording_finish_dialog_unsaved_title
                        },
                    ),
                    description = stringResource(
                        if (willSave) {
                            R.string.recording_finish_dialog_description
                        } else {
                            R.string.recording_finish_dialog_unsaved_description
                        },
                    ),
                    confirmLabel = stringResource(
                        if (willSave) {
                            R.string.recording_finish_dialog_confirm
                        } else {
                            R.string.recording_finish_dialog_unsaved_confirm
                        },
                    ),
                    cancelLabel = stringResource(
                        if (willSave) {
                            R.string.recording_finish_dialog_cancel
                        } else {
                            R.string.recording_finish_dialog_unsaved_cancel
                        },
                    ),
                    // 화면 이동은 StopTrackingService effect를 처리한 뒤에 한다(위 CollectEffect).
                    onConfirm = { viewModel.onIntent(RecordingIntent.StopClicked) },
                    onCancel = { viewModel.onIntent(RecordingIntent.StopDismissed) },
                )
            }
        }
    }
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

/** 범례+내 위치 줄 + 패널(16dp 간격) — 기록 중/일시정지 공통.
 *
 * [onPanelHeightChange]로 패널(지도를 실제로 가리는 불투명한 부분)의 높이를 알려준다 — 러닝 중
 * 카메라가 현재 위치를 "패널 위 보이는 영역의 가운데"에 두는 데 쓴다. 범례·내 위치 줄은 지도가
 * 그대로 보이는 자리라 높이에서 뺀다. */
@Composable
private fun RecordingOverlay(
    state: RecordingState,
    onPanelHeightChange: (Dp) -> Unit,
    courseProgress: CourseProgress? = null,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
    ) {
        val mapState = LocalMapState.current
        MapLegendRow(
            // 현재 줌은 그대로 두고 위치로 복귀하면서 추적을 다시 켠다(드래그·핀치로 풀린 뒤 복귀하는 길).
            onLocateClick = { mapState.startFollowingCurrentLocation() },
            showCourseGuide = state.selectedCoursePath != null,
            modifier = Modifier.padding(horizontal = RunvealTheme.layout.marginScreen),
        )
        RecordingPanel(
            state = state,
            courseProgress = courseProgress,
            action = action,
            modifier = Modifier.onSizeChanged { onPanelHeightChange(with(density) { it.height.toDp() }) },
        )
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
                    onPanelHeightChange = {},
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
                    onPanelHeightChange = {},
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
