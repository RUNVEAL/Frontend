package com.sloth.runveal.running

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.core.map.CourseSnapshotImage
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupEqual
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.overlay.RunvealLoading
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.running.R
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.running.contract.ResultDongStat
import com.sloth.runveal.running.contract.ResultState
import com.sloth.runveal.ui.toast.NotReadyToastHost
import com.sloth.runveal.ui.toast.rememberNotReadyToastState

/** 공용 로딩 모션 크기(시안 Z0의 80). */
private val LOADING_SIZE = 80.dp

private val MAP_CARD_HEIGHT = 268.dp
private val RESULT_GLOW_WIDTH = 480.dp
private val RESULT_GLOW_HEIGHT = 320.dp
private val RESULT_TITLE_TO_EXPLORE_GAP = 40.dp
private val LEGEND_SWATCH_WIDTH = 14.dp
private val LEGEND_SWATCH_HEIGHT = 3.dp
private val LEGEND_SWATCH_HALO_SIZE = 10.dp
private val LEGEND_SWATCH_HALO_PADDING = 3.dp
private val LEGEND_SWATCH_HALO_BLUR = 4.dp
private val LEGEND_SWATCH_LABEL_GAP = 6.dp

/** 러닝 결과 화면 — 기록 종료 직후 들어오지만, 동별 완주율 변화는 최종 매칭(§6)이 끝나야 나와서
 * [ResultState.Loading] 동안은 로딩만 보여준다. */
@Composable
fun ResultScreen(onClose: () -> Unit, modifier: Modifier = Modifier, viewModel: ResultViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = RunvealTheme.colors

    val notReadyToast = rememberNotReadyToastState()

    Box(modifier = modifier.fillMaxSize().background(colors.bg.canvas)) {
        // 제일 뒤에 깔리는 배경 glow — statusBar 인셋을 안 받아서 그 밑까지 그대로 번진다. 이미지 자체에
        // 색이 이미 들어 있다(result_glow.png). RESULT_GLOW_WIDTH(480dp)가 화면 너비보다 넓은데, width()를
        // 쓰면 화면 너비로 coerce되면서 ContentScale.Fit이 세로로 센터 정렬돼 상단에 빈 여백이 생기고
        // statusBar까지 안 닿는다 — requiredWidth/requiredHeight로 강제해서 그대로 렌더링한다.
        Image(
            painter = painterResource(R.drawable.result_glow),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).requiredWidth(RESULT_GLOW_WIDTH).requiredHeight(RESULT_GLOW_HEIGHT),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar는 X 아이콘 하나만 보이지만, Row 영역 자체(56dp)는 그대로 차지한다 — 아래 날짜/
            // 타이틀은 그 영역 밑에서 시작한다(겹치지 않음). Default 스타일 — Map 스타일의 원형 글래스
            // 배경은 지도 위에 얹는 화면용이라 불투명 배경인 이 화면엔 안 맞는다(아이콘만 보이게).
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Close,
                onLeadingClick = onClose,
                leadingContentDescription = stringResource(R.string.result_close_content_description),
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars),
            )

            when (val current = state) {
                is ResultState.Loading -> Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    RunvealLoading(modifier = Modifier.align(Alignment.Center).size(LOADING_SIZE))
                }
                is ResultState.Ready -> ResultContent(
                    state = current,
                    onNotReadyClick = notReadyToast::show,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }

        NotReadyToastHost(notReadyToast)
    }
}

@Composable
private fun ResultContent(
    state: ResultState.Ready,
    onNotReadyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // 시안(C1)의 타이틀 y=120 — 상태바 52 + 앱바 56 바로 아래 12다.
                .padding(top = RunvealTheme.spacing.space12)
                // 화면 기본 좌우 여백 16dp — 모든 하위 콘텐츠에 공통으로 적용된다.
                .padding(horizontal = RunvealTheme.layout.marginScreen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
            ) {
                // 밝은 glow 위에 얹히는 글자라 onAccent(어두운 색)를 쓴다 — 일반 화면의 text.secondary/primary와 다르다.
                Text(state.dateLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.onAccent)
                Text(stringResource(R.string.result_title), style = RunvealTheme.typography.heading1, color = colors.text.onAccent)
            }

            // 디자인 값 40dp — spacing 스케일에 없어 그대로 하드코딩.
            Spacer(modifier = Modifier.height(RESULT_TITLE_TO_EXPLORE_GAP))

            // "새로 탐험한 길" + 동네 탐험률 리스트 — 화면 기본 16dp에 더해 섹션 자체 마진 24dp(합계 40dp).
            ExploreResultCard(
                state = state,
                modifier = Modifier.padding(horizontal = RunvealTheme.spacing.space24).fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(RunvealTheme.spacing.space20))

            // 지도 카드 — 화면 기본 여백(16dp)만 적용, 별도 섹션 마진 없음.
            RouteMapCard(request = state.mapSnapshotRequest, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(RunvealTheme.spacing.space20))

            // 거리/시간/평균 페이스 — 화면 기본 16dp에 더해 섹션 자체 마진 24dp(합계 40dp).
            ResultStatsRow(
                state = state,
                modifier = Modifier.padding(horizontal = RunvealTheme.spacing.space24).fillMaxWidth(),
            )
        }

        // TODO: 기록 상세 화면으로 이동 — 기록 탭의 상세가 생기면 그 화면을 재사용한다.
        // TODO: 결과 카드를 이미지로 저장(공유) — 지도 스냅샷과 통계를 한 장으로 렌더링해야 한다.
        //  둘 다 붙기 전까지는 "개발 중" 안내만 띄운다.
        RunvealButtonGroupEqual(
            firstLabel = stringResource(R.string.result_detail_button),
            onFirstClick = onNotReadyClick,
            secondLabel = stringResource(R.string.result_save_image_button),
            onSecondClick = onNotReadyClick,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space8),
        )
    }
}

/** "탐험 결과" 글래스 카드 — 새로 탐험한 길 + 이번에 지나간 동들의 완주율 전/후. */
@Composable
private fun ExploreResultCard(state: ResultState.Ready, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.recording_legend_new_label),
                style = RunvealTheme.typography.bodyLSemibold,
                color = colors.text.accent,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2)) {
                Text(
                    stringResource(R.string.result_new_road_value_format, state.newRoadLabel),
                    style = RunvealTheme.typography.numL,
                    color = colors.text.accent,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    stringResource(R.string.recording_distance_unit),
                    style = RunvealTheme.typography.unitL,
                    color = colors.text.accent,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
            state.dongStats.forEach { dongStat -> DongRateRow(dongStat) }
        }
    }
}

@Composable
private fun DongRateRow(dongStat: ResultDongStat, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.result_dong_rate_label_format, dongStat.name),
            style = RunvealTheme.typography.bodyLRegular,
            color = colors.text.secondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.result_rate_percent_format, dongStat.beforePercent),
                style = RunvealTheme.typography.bodyLRegular,
                color = colors.text.tertiary,
            )
            Text(stringResource(R.string.result_rate_arrow), style = RunvealTheme.typography.bodyLRegular, color = colors.text.tertiary)
            Text(
                stringResource(R.string.result_rate_percent_format, dongStat.afterPercent),
                style = RunvealTheme.typography.bodyLRegular,
                color = colors.text.accent,
            )
        }
    }
}

/** 경로 지도 카드 — [request]가 있으면 실제 경로가 그려진 지도 스냅샷([CourseSnapshotImage]), 없으면
 * (경로 점이 2개 미만인 아주 짧은 기록) 플레이스홀더만 보여준다. */
@Composable
private fun RouteMapCard(request: SnapshotRequest?, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MAP_CARD_HEIGHT)
            .clip(shape)
            .background(colors.bg.canvas, shape)
            .border(1.dp, colors.border.subtle, shape),
    ) {
        CourseSnapshotImage(
            request = request,
            contentDescription = stringResource(R.string.result_map_content_description),
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(RunvealTheme.spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        ) {
            LegendItem(
                swatchColor = colors.map.roadNew,
                label = stringResource(R.string.recording_legend_new_label),
                haloColor = colors.map.roadNewHalo,
            )
            LegendItem(swatchColor = colors.map.roadToday, label = stringResource(R.string.recording_legend_today_label))
        }
    }
}

/** 범례 한 칸 — 지도 선 색(바텀시트 드래그 핸들과 같은 모양의 작은 선 스와치) + 라벨. `feature:tab-record`의
 * `RecordingScreen`에 있는 `LegendItem`과 같은 모양 — [haloColor]를 주면 실제 지도에 도로가 칠해질 때와
 * 같은 번짐 효과를 작게 얹는다. */
@Composable
private fun LegendItem(swatchColor: Color, label: String, modifier: Modifier = Modifier, haloColor: Color? = null) {
    Row(
        modifier = modifier,
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

@Composable
private fun ResultStatsRow(state: ResultState.Ready, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ResultStat(
            label = stringResource(R.string.result_distance_label),
            value = state.distanceLabel,
            unit = stringResource(R.string.recording_distance_unit),
        )
        ResultStat(label = stringResource(R.string.recording_elapsed_label), value = state.elapsedLabel)
        ResultStat(label = stringResource(R.string.recording_pace_label), value = state.paceLabel)
    }
}

@Composable
private fun ResultStat(label: String, value: String, unit: String? = null, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
        Text(label, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
        if (unit != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space2)) {
                Text(value, style = RunvealTheme.typography.numM, color = colors.text.primary, modifier = Modifier.alignByBaseline())
                Text(unit, style = RunvealTheme.typography.unitM, color = colors.text.secondary, modifier = Modifier.alignByBaseline())
            }
        } else {
            Text(value, style = RunvealTheme.typography.numM, color = colors.text.primary)
        }
    }
}

@Preview
@Composable
private fun ResultScreenPreview() {
    RunvealTheme {
        ResultContent(
            onNotReadyClick = {},
            state = ResultState.Ready(
                dateLabel = "10월 25일 일요일 저녁 러닝",
                newRoadLabel = "1.3",
                dongStats = listOf(
                    ResultDongStat("성수동", 37, 39),
                    ResultDongStat("송정동", 25, 28),
                    ResultDongStat("화양동", 18, 20),
                ),
                distanceLabel = "5.21",
                elapsedLabel = "31:40",
                paceLabel = "6'05″",
                mapSnapshotRequest = null,
                routePaths = emptyList(),
                newRoadPaths = emptyList(),
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
