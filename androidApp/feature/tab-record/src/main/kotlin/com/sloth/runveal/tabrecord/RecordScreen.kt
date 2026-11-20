package com.sloth.runveal.tabrecord

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.designsystem.component.list.RunvealEmptyState
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabrecord.R
import com.sloth.runveal.tabrecord.component.MonthlyRunningSummary
import com.sloth.runveal.tabrecord.component.RecordListItem
import com.sloth.runveal.tabrecord.contract.RecordIntent
import com.sloth.runveal.tabrecord.contract.RecordItem
import com.sloth.runveal.tabrecord.contract.RecordState
import com.sloth.runveal.ui.toast.NotReadyToastHost
import com.sloth.runveal.ui.toast.rememberNotReadyToastState

@Composable
fun RecordScreen(
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 상세에서 기록을 지우고 돌아오면 ViewModel은 그대로 살아 있다 — 화면에 들어올 때마다 다시 읽는다.
    LaunchedEffect(Unit) { viewModel.onIntent(RecordIntent.Load) }

    RecordScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onRecordClick = onRecordClick,
        modifier = modifier,
    )
}

@Composable
private fun RecordScreen(
    state: RecordState,
    onIntent: (RecordIntent) -> Unit,
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors

    val notReadyToast = rememberNotReadyToastState()

    Box(modifier = modifier.fillMaxSize().background(colors.bg.canvas)) {
        NotReadyToastHost(notReadyToast)

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState()),
        ) {
            RunvealTopAppBarLarge(
                title = "기록",
                metaLabel = state.monthLabel,
                titleStyle = RunvealTheme.typography.heading1,
                // TODO: 월 선택 — 고를 수 있게 되면 람다를 주면 화살표가 돌아온다. 지금은 이번 달만
                //  조회해서 바꿀 게 없다(RecordViewModel.loadThisMonth).
                onMetaLabelClick = null,
                modifier = Modifier.fillMaxWidth(),
            )

            MonthlyRunningSummary(
                runCountLabel = state.runCountLabel,
                totalDistanceValue = state.totalDistanceValue,
                totalDistanceUnit = "km",
                newRoadLabel = "새로 탐험한 길",
                newRoadValue = state.newRoadValue,
                newRoadUnit = "km",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RunvealTheme.layout.marginScreen),
            )

            Image(
                painter = painterResource(R.drawable.record),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space12)
                    .clip(RunvealTheme.shapes.radius16)
                    .aspectRatio(1816f / 405f),
            )

            if (state.records.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = RunvealTheme.layout.marginScreen),
                    verticalArrangement = Arrangement.spacedBy((-12).dp),
                ) {
                    state.records.forEach { record ->
                        RecordListItem(
                            dateLabel = record.dateLabel,
                            courseTitle = record.courseTitle,
                            distanceLabel = record.distanceLabel,
                            durationLabel = record.durationLabel,
                            paceLabel = record.paceLabel,
                            thumbnailRequest = record.thumbnailRequest,
                            // 삭제는 상세 화면의 더보기 메뉴로 옮겼다 — 길게 누르기로는 지울 수 없다.
                            onClick = { onRecordClick(record.trackId) },
                        )
                    }
                }
            } else if (!state.isLoading) {
                // 60dp에 맞는 spacing 토큰이 없어 그대로 하드코딩.
                // TODO: 홈 탭으로 전환하고 러닝 기록 화면을 연다 — 탭을 넘나드는 이동이라
                //  HomeScreen이 콜백을 내려줘야 한다. 그때까지는 "개발 중" 안내만 띄운다.
                RunvealEmptyState(
                    title = "아직 달린 기록이 없어요",
                    description = "코스를 만들고 첫 길을 밝혀보세요",
                    actionLabel = "러닝 시작하기",
                    onActionClick = notReadyToast::show,
                    graphic = { Image(painter = painterResource(R.drawable.empty_course), contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = RunvealTheme.layout.marginScreen,
                            end = RunvealTheme.layout.marginScreen,
                            top = 60.dp,
                        ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun RecordScreenPreview() {
    RunvealTheme {
        RecordScreen(
            onIntent = {},
            onRecordClick = {},
            state = RecordState(
                monthLabel = "2026년 11월",
                runCountLabel = "8회",
                totalDistanceValue = "32.40",
                newRoadValue = "+6.20",
                records = listOf(
                    RecordItem(
                        trackId = "preview-1",
                        dateLabel = "11월 3일 화요일",
                        courseTitle = "저녁 러닝",
                        distanceLabel = "5.21km",
                        durationLabel = "31:40",
                        paceLabel = "6′05″",
                    ),
                ),
                isLoading = false,
            ),
        )
    }
}
