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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.designsystem.component.list.RunvealEmptyState
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabrecord.R
import com.sloth.runveal.tabrecord.component.MonthPicker
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

/** 앱바와 요약 사이 간격 — spacing 스케일에 13이 없어 시안 값 그대로 쓴다. */
private val SUMMARY_TOP_GAP = 13.dp

@Composable
private fun RecordScreen(
    state: RecordState,
    onIntent: (RecordIntent) -> Unit,
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors

    val notReadyToast = rememberNotReadyToastState()
    var isMonthPickerOpen by remember { mutableStateOf(false) }
    val density = LocalDensity.current

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
                onMetaLabelClick = { isMonthPickerOpen = true },
                modifier = Modifier.fillMaxWidth(),
            )

            // 월 선택 패널을 앱바 **바로 아래**에 띄우기 위한 높이 0짜리 자리 — Popup은 부모 영역을
            // 기준으로 놓이므로 여기 두면 따로 좌표를 계산하지 않아도 된다.
            Box {
                if (isMonthPickerOpen) {
                    val closePicker = { isMonthPickerOpen = false }
                    val pickerOffset = with(density) {
                        IntOffset(
                            RunvealTheme.layout.marginScreen.roundToPx(),
                            RunvealTheme.spacing.space4.roundToPx(),
                        )
                    }
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = pickerOffset,
                        onDismissRequest = closePicker,
                        // focusable이어야 바깥 탭·뒤로가기로 닫힌다.
                        properties = PopupProperties(focusable = true),
                    ) {
                        MonthPicker(
                            selectedYear = state.selectedYear,
                            selectedMonth = state.selectedMonth,
                            onSelect = { year, month ->
                                closePicker()
                                onIntent(RecordIntent.SelectMonth(year, month))
                            },
                        )
                    }
                }
            }

            MonthlyRunningSummary(
                periodLabel = state.periodLabel,
                runCountLabel = state.runCountLabel,
                totalDistanceValue = state.totalDistanceValue,
                totalDistanceUnit = "km",
                newRoadLabel = "새로 탐험한 길",
                newRoadValue = state.newRoadValue,
                newRoadUnit = "km",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SUMMARY_TOP_GAP)
                    .padding(horizontal = RunvealTheme.layout.marginScreen),
            )

            Image(
                painter = painterResource(R.drawable.record),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    // 위로는 12dp, 목록과는 24dp를 띄운다(시안).
                    .padding(
                        start = RunvealTheme.layout.marginScreen,
                        end = RunvealTheme.layout.marginScreen,
                        top = RunvealTheme.spacing.space12,
                        bottom = RunvealTheme.spacing.space24,
                    )
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
                            newRoadLabel = record.newRoadLabel,
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
                selectedYear = 2026,
                selectedMonth = 11,
                periodLabel = "이번 달",
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
