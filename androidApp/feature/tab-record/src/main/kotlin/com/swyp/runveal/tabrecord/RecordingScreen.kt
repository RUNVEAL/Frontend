package com.swyp.runveal.tabrecord

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.core.map.LocalMapState
import com.swyp.runveal.core.map.rememberMapState
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupEqual
import com.swyp.runveal.designsystem.component.button.RunvealRunPauseButton
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabrecord.R
import com.swyp.runveal.tabrecord.contract.RecordingEffect
import com.swyp.runveal.tabrecord.contract.RecordingIntent
import com.swyp.runveal.tabrecord.contract.RecordingPhase
import com.swyp.runveal.tabrecord.contract.RecordingState
import com.swyp.runveal.tracking.TrackingService
import com.swyp.runveal.ui.mvi.CollectEffect

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
 * 지도 위 꼬리·확정 구간 렌더링(§4)은 Valhalla 레이어(이번 범위 밖)가 붙기 전까지는 비워 둔다.
 *
 * 이 화면에 들어오는 유일한 경로가 홈 지도의 "러닝 시작" 버튼이라, 진입 자체를 "시작" 의도로 본다 —
 * 별도의 "시작" 버튼을 다시 보여주지 않고 [LaunchedEffect]로 바로 기록을 시작한다.
 */
@Composable
fun RecordingScreen(onClose: () -> Unit, modifier: Modifier = Modifier, viewModel: RecordingViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            RecordingEffect.StartTrackingService -> TrackingService.start(context)
            RecordingEffect.PauseTrackingService -> TrackingService.pause(context)
            RecordingEffect.ResumeTrackingService -> TrackingService.resume(context)
            RecordingEffect.StopTrackingService -> TrackingService.stop(context)
        }
    }

    LaunchedEffect(Unit) {
        if (state.phase == RecordingPhase.IDLE) viewModel.onIntent(RecordingIntent.StartClicked)
    }

    // 뒤로가기로 화면을 못 벗어나게 한다 — 종료는 반드시 "종료하기" 버튼으로.
    BackHandler(enabled = true) {}

    Box(modifier = modifier.fillMaxSize()) {
        if (state.phase != RecordingPhase.IDLE) {
            NewRoadBadge(
                distanceLabel = state.distanceLabel,
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
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                RunvealRunPauseButton(onClick = { viewModel.onIntent(RecordingIntent.PauseClicked) })
            }
            RecordingPhase.PAUSED -> RecordingOverlay(
                state = state,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                RunvealButtonGroupEqual(
                    firstLabel = stringResource(R.string.recording_stop_button),
                    onFirstClick = {
                        viewModel.onIntent(RecordingIntent.StopClicked)
                        onClose()
                    },
                    secondLabel = stringResource(R.string.recording_resume_button),
                    onSecondClick = { viewModel.onIntent(RecordingIntent.ResumeClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 좌상단 "새로 탐험한 길" 배지. 지금은 기존에 가본 길과 구분하지 않고 이번 기록의 누적 거리를 그대로
 * 보여준다 — 실시간으로 "새 길인지" 가리려면 서버 매칭이 필요한데, 러닝 중 매칭(§4.2)은 이번 범위
 * 밖이라 종료 후 최종 매칭(§6)에서만 정확해진다. */
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
private fun RecordingOverlay(state: RecordingState, modifier: Modifier = Modifier, action: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
    ) {
        val mapState = LocalMapState.current
        MapLegendRow(
            onLocateClick = { mapState.recenterToCurrentLocation() },
            modifier = Modifier.padding(horizontal = RunvealTheme.layout.marginScreen),
        )
        RecordingPanel(state = state, action = action)
    }
}

/** 지도 범례(오늘 달린 길/새로 탐험한 길) + 내 위치 버튼. */
@Composable
private fun MapLegendRow(onLocateClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier = Modifier
                .background(colors.bg.scrim, RunvealTheme.shapes.radius8)
                .padding(horizontal = RunvealTheme.spacing.space12, vertical = RunvealTheme.spacing.space8),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        ) {
            LegendItem(swatchColor = colors.map.roadToday, label = stringResource(R.string.recording_legend_today_label))
            LegendItem(
                swatchColor = colors.map.roadNew,
                label = stringResource(R.string.recording_legend_new_label),
                haloColor = colors.map.roadNewHalo,
            )
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

/** 범례 한 칸 — 지도 선 색(바텀시트 드래그 핸들과 같은 모양의 작은 선 스와치) + 라벨. [haloColor]를 주면
 * 실제 지도에 도로가 칠해질 때와 같은 번짐 효과를 작게 얹는다([VisitedRoadLayer]의 glow와 같은 색
 * 페어 — `map.roadNew`/`map.roadNewHalo`). `오늘 달린 길`(roadToday)은 번짐 토큰이 없어 그대로 둔다. */
@Composable
private fun LegendItem(swatchColor: Color, label: String, modifier: Modifier = Modifier, haloColor: Color? = null) {
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
            Box(
                modifier = Modifier
                    .size(width = LEGEND_SWATCH_WIDTH, height = LEGEND_SWATCH_HEIGHT)
                    .clip(RunvealTheme.shapes.full)
                    .background(swatchColor, RunvealTheme.shapes.full),
            )
        }
        Text(label, style = RunvealTheme.typography.captionMRegular, color = RunvealTheme.colors.text.secondary)
    }
}

/** 기록 중/일시정지 공통 패널 — 거리, 경과 시간/평균 페이스, 하단 액션([action])만 상태별로 다르다.
 * 바텀시트처럼 화면 하단에 깔리지만 드래그 핸들은 없다. */
@Composable
private fun RecordingPanel(state: RecordingState, modifier: Modifier = Modifier, action: @Composable () -> Unit) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
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

@Composable
private fun RecordingStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
        Text(value, style = RunvealTheme.typography.numM, color = colors.text.primary)
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
