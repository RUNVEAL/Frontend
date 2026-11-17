package com.swyp.runveal.tabrecord

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.designsystem.component.list.RunvealEmptyState
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.swyp.runveal.designsystem.component.overlay.RunvealDialog
import com.swyp.runveal.designsystem.component.overlay.RunvealDialogType
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabrecord.R
import com.swyp.runveal.tabrecord.component.MonthlyRunningSummary
import com.swyp.runveal.tabrecord.component.RecordListItem
import com.swyp.runveal.tabrecord.contract.RecordIntent
import com.swyp.runveal.tabrecord.contract.RecordItem
import com.swyp.runveal.tabrecord.contract.RecordState

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RecordScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
private fun RecordScreen(
    state: RecordState,
    onIntent: (RecordIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg.canvas)) {
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
                onMetaLabelClick = {},
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
                            onLongClick = { onIntent(RecordIntent.DeleteRequested(record.trackId)) },
                        )
                    }
                }
            } else if (!state.isLoading) {
                // 60dp에 맞는 spacing 토큰이 없어 그대로 하드코딩.
                RunvealEmptyState(
                    title = "아직 달린 기록이 없어요",
                    description = "코스를 만들고 첫 길을 밝혀보세요",
                    actionLabel = "러닝 시작하기",
                    onActionClick = {},
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

        // RunvealDialog는 내용만 그리는 컴포넌트라(그 KDoc 참고) Compose의 Dialog로 감싼다 —
        // 바깥을 누르거나 뒤로가기로 닫히는 동작을 거기서 얻는다(마이페이지 로그아웃과 같은 방식).
        if (state.deleteTargetTrackId != null) {
            Dialog(onDismissRequest = { onIntent(RecordIntent.DeleteDismissed) }) {
                RunvealDialog(
                    title = stringResource(R.string.record_delete_dialog_title),
                    description = stringResource(R.string.record_delete_dialog_description),
                    confirmLabel = stringResource(R.string.record_delete_dialog_confirm),
                    cancelLabel = stringResource(R.string.record_delete_dialog_cancel),
                    onConfirm = { onIntent(RecordIntent.DeleteConfirmed) },
                    onCancel = { onIntent(RecordIntent.DeleteDismissed) },
                    // 되돌릴 수 없는 동작이라 확인 버튼을 빨갛게 둔다.
                    type = RunvealDialogType.Destructive,
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
