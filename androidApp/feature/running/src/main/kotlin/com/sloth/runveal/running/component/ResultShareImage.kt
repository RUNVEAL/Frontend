package com.sloth.runveal.running.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.running.R
import com.sloth.runveal.running.contract.ResultState

/**
 * 러닝 결과를 **한 장의 이미지로 저장**할 때 쓰는 템플릿. 화면에 보여주는 결과 카드와 달리 크기가
 * 고정이다 — 9:16 비율의 360x640dp 아트보드이고, 저장할 때 3배(1080x1920px)로 그린다.
 *
 * **화면용 `ResultScreen`과 따로 두는 이유**: 저장 이미지는 기기 화면 크기·밀도와 무관하게 항상 같은
 * 그림이어야 한다. 화면 컴포저블을 그대로 캡처하면 기기마다 줄바꿈·잘림이 달라지고, 스크롤로 가려진
 * 부분은 아예 안 찍힌다.
 *
 * 경로는 지도 타일 없이 **벡터로 직접** 그린다([RouteVectorMap]) — 타일을 깔면 저장 시점의 네트워크
 * 상태에 따라 그림이 달라지고, 지도 저작권 표기까지 따라붙는다.
 *
 * 치수는 디자인 시안 값을 그대로 쓴다. 고정 아트보드라 spacing 토큰으로 바꾸면 오히려 시안과
 * 어긋난다 — 색과 타이포만 토큰을 쓴다.
 */
@Composable
fun ResultShareImage(
    state: ResultState.Ready,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors

    Box(
        modifier = modifier
            .width(IMAGE_WIDTH)
            .height(IMAGE_HEIGHT)
            .background(colors.bg.canvas),
    ) {
        // 기록 탭 상단과 같은 히어로 이미지(Graphic/My Course). 아트보드(360)보다 넓은 480이라
        // 좌우가 잘린다 — 시안 그대로다.
        Image(
            painter = painterResource(R.drawable.my_course),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(HERO_WIDTH)
                .height(HERO_HEIGHT),
        )

        // TODO: 우측 상단 아이콘 — 시안 확정되면 추가한다.
        // TODO: 우측 상단 이미지 — 시안 확정되면 추가한다.

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SIDE_MARGIN, end = SIDE_MARGIN, top = HERO_HEIGHT, bottom = SIDE_MARGIN),
            verticalArrangement = Arrangement.spacedBy(SIDE_MARGIN),
        ) {
            RouteVectorMap(
                routePaths = state.routePaths,
                newRoadPaths = state.newRoadPaths,
                modifier = Modifier.width(CONTENT_WIDTH).height(ROUTE_MAP_HEIGHT),
            )

            ExplorationSummary(newRoadLabel = state.newRoadLabel)

            Information(state = state)
        }
    }
}

/** "새로 탐험한 길 +1.3km" — 이 이미지에서 가장 크게 보여줄 값이다. */
@Composable
private fun ExplorationSummary(newRoadLabel: String, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.recording_legend_new_label),
            style = RunvealTheme.typography.bodyMRegular,
            color = colors.text.accent,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.result_new_road_value_format, newRoadLabel),
                // 시안은 Geist Light(300)인데 num/display 토큰은 200이다 — 글꼴·크기는 토큰을 그대로
                // 두고 굵기만 시안에 맞춘다.
                style = RunvealTheme.typography.numDisplay.copy(fontWeight = FontWeight.Light),
                color = colors.text.accent,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = stringResource(R.string.recording_distance_unit),
                style = RunvealTheme.typography.unitDisplay,
                color = colors.text.accent,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

/** 동 이름 한 줄 + 거리·시간·평균 페이스 세 칸. */
@Composable
private fun Information(state: ResultState.Ready, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier.width(CONTENT_WIDTH),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // 지나간 동이 여럿이면 첫 동만 적고 나머지는 "외 N개 동"으로 줄인다 — 한 줄짜리 자리라
        // 다 적으면 넘친다. 동을 하나도 못 받았으면 줄 자체를 그리지 않는다.
        val dongNames = state.dongStats.map { it.name }
        if (dongNames.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
                Text(
                    text = dongNames.first(),
                    style = RunvealTheme.typography.heading3,
                    color = colors.text.primary,
                    modifier = Modifier.alignByBaseline(),
                )
                if (dongNames.size > 1) {
                    Text(
                        text = stringResource(R.string.result_share_more_dong_format, dongNames.size - 1),
                        style = RunvealTheme.typography.bodyMRegular,
                        color = colors.text.secondary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(STAT_GAP),
            verticalAlignment = Alignment.Top,
        ) {
            ShareStat(
                label = stringResource(R.string.result_distance_label),
                value = state.distanceLabel,
                unit = stringResource(R.string.recording_distance_unit),
            )
            ShareStat(label = stringResource(R.string.recording_elapsed_label), value = state.elapsedLabel)
            ShareStat(label = stringResource(R.string.recording_pace_label), value = state.paceLabel)
        }
    }
}

/** 통계 한 칸 — 화면용 `ResultStat`과 같은 모양이지만 칸 너비가 고정이다(세 칸이 312를 나눠 쓴다). */
@Composable
private fun ShareStat(label: String, value: String, unit: String? = null, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier.width(STAT_WIDTH),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(label, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
        if (unit != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
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

/** 아트보드 크기(9:16) — 저장할 때 [RESULT_IMAGE_SCALE]배로 그린다. */
internal val IMAGE_WIDTH = 360.dp
internal val IMAGE_HEIGHT = 640.dp

/** 저장 배율. 360x640dp를 3배로 그려 1080x1920px로 내보낸다. */
internal const val RESULT_IMAGE_SCALE = 3

private val HERO_WIDTH = 480.dp
private val HERO_HEIGHT = 136.dp

/** 좌우 여백 24 — 본문 너비가 312가 되어 시안과 맞는다(360 - 24*2). */
private val SIDE_MARGIN = 24.dp
private val CONTENT_WIDTH = 312.dp

/** 시안 값 그대로. 경로 그림은 아트보드에서 가장 큰 덩어리라 소수점까지 맞춘다. */
private val ROUTE_MAP_HEIGHT = 254.93.dp

/** 통계 세 칸 — 96 * 3 + 12 * 2 = 312로 본문 너비에 꼭 맞는다. */
private val STAT_WIDTH = 96.dp
private val STAT_GAP = 12.dp

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun ResultShareImagePreview() {
    RunvealTheme {
        ResultShareImage(
            state = ResultState.Ready(
                dateLabel = "화요일 저녁",
                newRoadLabel = "1.3",
                dongStats = emptyList(),
                distanceLabel = "5.21",
                elapsedLabel = "31:40",
                paceLabel = "6'05\"",
                mapSnapshotRequest = null,
                routePaths = emptyList(),
                newRoadPaths = emptyList(),
            ),
        )
    }
}
