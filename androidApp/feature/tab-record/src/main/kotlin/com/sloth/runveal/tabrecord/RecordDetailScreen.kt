package com.sloth.runveal.tabrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.core.map.CourseSnapshotImage
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.overlay.RunvealDialog
import com.sloth.runveal.designsystem.component.overlay.RunvealDialogType
import com.sloth.runveal.designsystem.component.stat.RunvealStat
import com.sloth.runveal.designsystem.component.stat.RunvealStatType
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabrecord.R
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.tabrecord.component.RecordMoreMenuIcon
import com.sloth.runveal.tabrecord.component.RecordRenameSheet
import com.sloth.runveal.tabrecord.contract.RecordDetailDongStat
import com.sloth.runveal.tabrecord.contract.RecordDetailEffect
import com.sloth.runveal.tabrecord.contract.RecordDetailIntent
import com.sloth.runveal.tabrecord.contract.RecordDetailState
import com.sloth.runveal.ui.mvi.CollectEffect
import com.sloth.runveal.ui.toast.NotReadyToastHost
import com.sloth.runveal.ui.toast.rememberNotReadyToastState

/** 시안의 지도 영역 높이. */
private val MAP_HEIGHT = 291.dp

/** "새 길" 칸 너비 — 거리와 한 줄에 놓이는 작은 수치라 시안 값대로 고정한다. */
private val NEW_ROAD_STAT_WIDTH = 80.dp

/** 범례 선 스와치 — `ResultScreen`/`RecordingScreen`의 같은 모양. */
private val LEGEND_SWATCH_WIDTH = 16.dp
private val LEGEND_SWATCH_HEIGHT = 3.dp
private val LEGEND_SWATCH_HALO_SIZE = 10.dp
private val LEGEND_SWATCH_HALO_PADDING = 3.dp
private val LEGEND_SWATCH_HALO_BLUR = 4.dp
private val LEGEND_SWATCH_LABEL_GAP = 6.dp

/**
 * 러닝 기록 상세(디자인 `D2`). 기록 탭 목록에서 한 건을 눌러 들어온다 — 탭 안의 화면이라 하단 탭바는
 * 그대로 보이고([com.sloth.runveal.home.HomeScreen]이 그린다), 그만큼 아래 여백을 둔다.
 *
 * 칼로리는 아직 저장하는 값이 없어 "-"로 둔다. 시안의 "구간 기록"(km별 페이스)은 이번 범위 밖 —
 * 필터된 GPX의 시각을 읽어 계산해야 한다.
 */
@Composable
fun RecordDetailScreen(
    trackId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(trackId) { viewModel.onIntent(RecordDetailIntent.Load(trackId)) }

    // 목록에서 지운 기록을 백스택으로 되돌아와 다시 열면 보여줄 게 없다 — 빈 화면을 띄우는 대신
    // 목록으로 돌려보낸다.
    LaunchedEffect(state) { if (state is RecordDetailState.NotFound) onBack() }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            RecordDetailEffect.NavigateBack -> onBack()
        }
    }

    val readyState = state as? RecordDetailState.Ready
    val notReadyToast = rememberNotReadyToastState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = onBack,
                title = readyState?.title.orEmpty(),
                // 읽을 기록이 있을 때만 더보기를 단다 — 빈 화면에 메뉴만 떠 있을 이유가 없다.
                action = if (readyState != null) {
                    {
                        RecordMoreMenuIcon(
                            onRenameClick = { viewModel.onIntent(RecordDetailIntent.RenameClicked) },
                            // TODO: GPX 파일은 이미 저장돼 있다(track.gpx_filtered_path) — 공유 시트로
                            //  내보내는 길만 붙이면 된다. 그때까지는 "개발 중" 안내만 띄운다.
                            onExportGpxClick = notReadyToast::show,
                            onDeleteClick = { viewModel.onIntent(RecordDetailIntent.DeleteClicked) },
                        )
                    }
                } else {
                    null
                },
            )

            if (readyState != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = RunvealTheme.layout.marginScreen)
                        // 하단 탭바는 이 화면 위에 떠 있다 — 마지막 카드가 그 뒤로 들어가지 않게 띄운다.
                        .padding(bottom = RunvealTheme.layout.sizeTabBar + RunvealTheme.spacing.space24),
                    verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
                ) {
                    RunningSummary(state = readyState)
                    RouteSummaryCard(state = readyState)
                    if (readyState.dongStats.isNotEmpty()) {
                        DongRateSection(dongStats = readyState.dongStats)
                    }
                }
            }
        }

        NotReadyToastHost(notReadyToast)

        if (readyState?.isRenameSheetVisible == true) {
            RecordRenameModalSheet(
                recordName = readyState.title,
                onConfirm = { viewModel.onIntent(RecordDetailIntent.RenameConfirmed(it)) },
                onDismiss = { viewModel.onIntent(RecordDetailIntent.RenameDismissed) },
            )
        }

        // RunvealDialog는 내용만 그리는 컴포넌트라(그 KDoc 참고) Compose의 Dialog로 감싼다 —
        // 바깥을 누르거나 뒤로가기로 닫히는 동작을 거기서 얻는다(기록 탭에 있던 그대로다).
        if (readyState?.isDeleteDialogVisible == true) {
            Dialog(onDismissRequest = { viewModel.onIntent(RecordDetailIntent.DeleteDismissed) }) {
                RunvealDialog(
                    title = stringResource(R.string.record_delete_dialog_title),
                    description = stringResource(R.string.record_delete_dialog_description),
                    confirmLabel = stringResource(R.string.record_delete_dialog_confirm),
                    cancelLabel = stringResource(R.string.record_delete_dialog_cancel),
                    onConfirm = { viewModel.onIntent(RecordDetailIntent.DeleteConfirmed) },
                    onCancel = { viewModel.onIntent(RecordDetailIntent.DeleteDismissed) },
                    // 되돌릴 수 없는 동작이라 확인 버튼을 빨갛게 둔다.
                    type = RunvealDialogType.Destructive,
                )
            }
        }
    }
}

/** 이름 수정 시트 — 모양은 [RecordRenameSheet]가 그리고 ModalBottomSheet는 scrim·드래그 닫기만
 * 맡는다(그래서 배경은 투명, 드래그 핸들은 시트가 직접 그린다). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordRenameModalSheet(recordName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        RecordRenameSheet(initialName = recordName, onConfirm = onConfirm, onCancel = onDismiss)
    }
}

/** 날짜·제목 + 거리와 새 길. */
@Composable
private fun RunningSummary(state: RecordDetailState.Ready, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
            Text(state.dateTimeLabel, style = RunvealTheme.typography.captionMRegular, color = colors.text.secondary)
            Text(state.title, style = RunvealTheme.typography.heading2, color = colors.text.primary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
            verticalAlignment = Alignment.Bottom,
        ) {
            RunvealStat(
                value = state.distanceValue,
                unit = stringResource(R.string.record_detail_distance_unit),
                type = RunvealStatType.Display,
            )
            RunvealStat(
                label = stringResource(R.string.record_detail_new_road_label),
                value = state.newRoadValue,
                unit = stringResource(R.string.record_detail_distance_unit),
                accent = true,
                modifier = Modifier.width(NEW_ROAD_STAT_WIDTH),
            )
        }
    }
}

/** 경로 지도 + 러닝 시간·평균 페이스·칼로리를 한 카드에 담는다. */
@Composable
private fun RouteSummaryCard(state: RecordDetailState.Ready, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius24
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.default, shape)
            .border(1.dp, colors.border.default, shape),
    ) {
        RouteMap(request = state.mapSnapshotRequest)
        Row(
            modifier = Modifier.fillMaxWidth().padding(RunvealTheme.spacing.space20),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            RunvealStat(
                label = stringResource(R.string.record_detail_elapsed_label),
                value = state.elapsedLabel,
                modifier = Modifier.weight(1f),
            )
            RunvealStat(
                label = stringResource(R.string.record_detail_pace_label),
                value = state.paceLabel,
                modifier = Modifier.weight(1f),
            )
            RunvealStat(
                label = stringResource(R.string.record_detail_calories_label),
                value = state.caloriesLabel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** [request]가 없으면(매칭 실패·점 부족) 플레이스홀더만 — `ResultScreen`의 지도 카드와 같은 규칙. */
@Composable
private fun RouteMap(request: SnapshotRequest?, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MAP_HEIGHT)
            .background(colors.bg.map),
    ) {
        CourseSnapshotImage(
            request = request,
            contentDescription = stringResource(R.string.record_detail_map_content_description),
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(RunvealTheme.spacing.space16),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        ) {
            LegendItem(
                swatchColor = colors.map.roadNew,
                label = stringResource(R.string.record_detail_legend_new),
                haloColor = colors.map.roadNewHalo,
            )
            LegendItem(swatchColor = colors.map.roadToday, label = stringResource(R.string.record_detail_legend_today))
        }
    }
}

/** 이 러닝으로 동네 탐험률이 얼마나 올랐는지 — 결과 화면(`ResultScreen`)의 같은 줄과 같은 숫자다. */
@Composable
private fun DongRateSection(dongStats: List<RecordDetailDongStat>, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
    ) {
        Text(
            stringResource(R.string.record_detail_dong_rate_title),
            style = RunvealTheme.typography.bodyLSemibold,
            color = colors.text.primary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface.default, shape)
                .border(1.dp, colors.border.default, shape)
                .padding(RunvealTheme.spacing.space16),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            dongStats.forEach { dongStat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(dongStat.name, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LEGEND_SWATCH_LABEL_GAP),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.record_detail_percent_format, dongStat.beforePercent),
                            style = RunvealTheme.typography.bodyMRegular,
                            color = colors.text.tertiary,
                        )
                        Text(
                            stringResource(R.string.record_detail_rate_arrow),
                            style = RunvealTheme.typography.bodyMRegular,
                            color = colors.text.tertiary,
                        )
                        Text(
                            stringResource(R.string.record_detail_percent_format, dongStat.afterPercent),
                            style = RunvealTheme.typography.bodyMRegular,
                            color = colors.text.primary,
                        )
                    }
                }
            }
        }
    }
}

/** 범례 한 칸 — `ResultScreen`·`RecordingScreen`에 같은 모양이 있다. 세 번째 쓰임이라 디자인시스템으로
 * 올릴 때가 됐지만, 그건 세 화면을 같이 고치는 일이라 이 화면 범위와 분리한다. */
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

@Preview
@Composable
private fun RecordDetailContentPreview() {
    RunvealTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RunvealTheme.colors.bg.canvas)
                .padding(RunvealTheme.layout.marginScreen),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
        ) {
            val state = RecordDetailState.Ready(
                title = "저녁 러닝",
                dateTimeLabel = "2026.10.31 18:30",
                distanceValue = "5.21",
                newRoadValue = "+1.3",
                elapsedLabel = "31:40",
                paceLabel = "6'05″",
                caloriesLabel = "-",
                dongStats = listOf(RecordDetailDongStat("성수동", 37, 39)),
                mapSnapshotRequest = null,
            )
            RunningSummary(state = state)
            RouteSummaryCard(state = state)
            DongRateSection(dongStats = state.dongStats)
        }
    }
}
