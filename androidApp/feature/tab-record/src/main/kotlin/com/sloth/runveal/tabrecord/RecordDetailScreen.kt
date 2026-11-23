package com.sloth.runveal.tabrecord

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import com.sloth.runveal.tabrecord.contract.RecordDetailSplit
import com.sloth.runveal.tabrecord.contract.RecordDetailState
import com.sloth.runveal.ui.mvi.CollectEffect
import com.sloth.runveal.ui.toast.NotReadyToastHost
import com.sloth.runveal.ui.toast.rememberNotReadyToastState

/** 시안의 지도 영역 높이. */
private val MAP_HEIGHT = 291.dp

/** "새 길" 칸 너비 — 거리와 한 줄에 놓이는 작은 수치라 시안 값대로 고정한다. */
private val NEW_ROAD_STAT_WIDTH = 80.dp

/** 구간 기록 한 줄 — "1km" 라벨 폭과 막대 두께(시안 값). */
private val SPLIT_LABEL_WIDTH = 28.dp
private val SPLIT_BAR_HEIGHT = 6.dp

/** 가장 빠른 구간 막대의 번짐 두 겹 — 홈 지도 "가본 길"의 비율(코어:바깥 ≈ 1:5, 안쪽은 그 절반)을
 * 6dp 막대에 맞춰 줄인 값이다. 불투명도도 지도와 같은 0.45. */
private val SPLIT_GLOW_OUTER_HEIGHT = 20.dp
private val SPLIT_GLOW_OUTER_BLUR = 10.dp
private val SPLIT_GLOW_INNER_HEIGHT = 12.dp
private val SPLIT_GLOW_INNER_BLUR = 5.dp
private const val SPLIT_GLOW_ALPHA = 0.45f

/** 범례 선 스와치 — `ResultScreen`/`RecordingScreen`의 같은 모양. */
private val LEGEND_SWATCH_WIDTH = 14.dp
private val LEGEND_SWATCH_HEIGHT = 3.dp
private val LEGEND_SWATCH_HALO_SIZE = 10.dp
private val LEGEND_SWATCH_HALO_PADDING = 3.dp
private val LEGEND_SWATCH_HALO_BLUR = 4.dp
private val LEGEND_SWATCH_LABEL_GAP = 6.dp

/**
 * 러닝 기록 상세(디자인 `D2`). 기록 탭 목록에서 한 건을 눌러 들어온다 — 탭 안에서 열리지만 그 자체로
 * 전체 화면이라 하단 탭바까지 덮는다([com.sloth.runveal.home.HomeScreen]의 `showTabBar`).
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
        // 맨 뒤에 깔리는 배경 글로우 — 상태바 **아래**부터 시작한다(상태바 뒤까지 번지면 시계·아이콘이
        // 빛 위에 겹쳐 읽기 어렵다). design/images/record_detail.png는 가로를 꽉 채우고 넘치는
        // 아래쪽은 잘라 낸다(MyScreen의 mypage_bg와 같은 생각, 크기 계산만 더 단순하다).
        Image(
            painter = painterResource(R.drawable.record_detail),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // 탭바를 덮는 전체 화면이라 위아래 시스템 바만 피하면 된다.
                .windowInsetsPadding(WindowInsets.systemBars),
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
                        .padding(bottom = RunvealTheme.spacing.space24),
                    verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
                ) {
                    RunningSummary(state = readyState)
                    RouteSummaryCard(state = readyState)
                    DongRateSection(dongStats = readyState.dongStats)
                    SplitsSection(splits = readyState.splits)
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
            modifier = Modifier.align(Alignment.BottomStart).padding(RunvealTheme.spacing.space12),
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

/**
 * 이 러닝으로 동네 탐험률이 얼마나 올랐는지 — 결과 화면(`ResultScreen`)의 같은 줄과 같은 숫자다.
 *
 * 빈 경우에도 섹션을 숨기지 않고 이유를 한 줄로 알린다 — 섹션이 통째로 사라지면 "원래 없는 화면"인지
 * "이번 기록만 비는 것"인지 알 수 없다. 적립된 구간이 없거나(아주 짧은 러닝·매칭 실패) 그 지역 도로
 * 구간 인덱스가 아직 안 들어왔으면 빈다.
 */
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
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        ) {
            if (dongStats.isEmpty()) {
                Text(
                    stringResource(R.string.record_detail_dong_rate_empty),
                    style = RunvealTheme.typography.bodyMRegular,
                    color = colors.text.tertiary,
                )
            }
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

/**
 * 1km 구간별 페이스. 막대는 **가장 빠른 구간이 꽉 차는** 길이라, 긴 줄이 더 빨리 달린 구간이다.
 * 1km를 못 채운 러닝은 나눌 구간이 없어 안내 한 줄만 둔다(탐험률 변화와 같은 규칙).
 */
@Composable
private fun SplitsSection(splits: List<RecordDetailSplit>, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
    ) {
        Text(
            stringResource(R.string.record_detail_splits_title),
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
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        ) {
            if (splits.isEmpty()) {
                Text(
                    stringResource(R.string.record_detail_splits_empty),
                    style = RunvealTheme.typography.bodyMRegular,
                    color = colors.text.tertiary,
                )
            }
            splits.forEach { split ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        split.label,
                        style = RunvealTheme.typography.bodyMRegular,
                        color = colors.text.secondary,
                        modifier = Modifier.width(SPLIT_LABEL_WIDTH),
                    )
                    SplitBar(ratio = split.barRatio, isFastest = split.isFastest, modifier = Modifier.weight(1f))
                    Text(split.paceLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                }
            }
        }
    }
}

/**
 * 트랙 위에 [ratio]만큼 채운 막대. 채움은 `progress/filled`, **가장 빠른 구간만** `progress/gained`로
 * 그리고 글로우를 얹는다.
 *
 * 글로우는 홈 지도의 "가본 길"과 같은 방식이다([VisitedRoadLayer]) — drop shadow 대신 **같은 색을
 * 두 겹 번지게 깔고 그 위에 막대를 얹는다**(바깥은 넓고 연하게, 안쪽은 좁고 진하게). 6dp짜리 가는
 * 막대에 그림자만 주면 번짐이 거의 안 보여서, 지도가 굵은 선으로 내는 두께감을 같은 구조로 흉내 낸다.
 *
 * [RunvealProgressBar]를 쓰지 않는 건 그쪽이 "기존 + 이번에 늘어난" 두 토막을 겹쳐 그리는 컴포넌트라
 * 한 토막만 필요한 여기와 모양이 다르기 때문이다.
 */
@Composable
private fun SplitBar(ratio: Float, isFastest: Boolean, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full
    val fillRatio = ratio.coerceIn(0f, 1f)

    // 글로우가 막대 위아래로 번져 나가야 해서 바깥 Box에는 clip을 주지 않는다 — 트랙만 따로 자른다.
    Box(modifier = modifier.height(SPLIT_BAR_HEIGHT), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxSize().clip(shape).background(colors.progress.track, shape))

        if (isFastest) {
            SplitBarGlow(widthRatio = fillRatio, height = SPLIT_GLOW_OUTER_HEIGHT, blur = SPLIT_GLOW_OUTER_BLUR)
            SplitBarGlow(widthRatio = fillRatio, height = SPLIT_GLOW_INNER_HEIGHT, blur = SPLIT_GLOW_INNER_BLUR)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(fillRatio)
                .height(SPLIT_BAR_HEIGHT)
                .clip(shape)
                .background(if (isFastest) colors.progress.gained else colors.progress.filled, shape),
        )
    }
}

/**
 * 막대와 같은 폭으로 깔리는 번짐 한 겹. 높이·블러만 바꿔 두 번 겹친다.
 *
 * [BlurredEdgeTreatment.Unbounded]가 핵심이다 — 기본값(Rectangle)은 번진 결과를 **자기 영역 안으로
 * 잘라내서**, 빛이 막대 밖으로 못 나가고 안에서만 흐려진다(= 글로우가 거의 안 보인다).
 */
@Composable
private fun BoxScope.SplitBarGlow(widthRatio: Float, height: Dp, blur: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthRatio)
            .height(height)
            .align(Alignment.CenterStart)
            .blur(blur, BlurredEdgeTreatment.Unbounded)
            .background(
                RunvealTheme.colors.accent.glow.copy(alpha = SPLIT_GLOW_ALPHA),
                RunvealTheme.shapes.full,
            ),
    )
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
                splits = listOf(
                    RecordDetailSplit("1km", "6'12″", 0.95f, isFastest = false),
                    RecordDetailSplit("2km", "6'01″", 1f, isFastest = true),
                    RecordDetailSplit("3km", "6'40″", 0.8f, isFastest = false),
                ),
                mapSnapshotRequest = null,
            )
            RunningSummary(state = state)
            RouteSummaryCard(state = state)
            DongRateSection(dongStats = state.dongStats)
            SplitsSection(splits = state.splits)
        }
    }
}
