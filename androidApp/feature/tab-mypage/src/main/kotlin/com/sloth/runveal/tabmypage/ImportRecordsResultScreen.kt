package com.sloth.runveal.tabmypage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.core.map.CourseSnapshotImage
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupEqual
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupPrimaryText
import com.sloth.runveal.designsystem.component.list.RunvealEmptyState
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.overlay.RunvealImportRecordsLoading
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabmypage.R
import com.sloth.runveal.map.snapshot.SnapshotRequest
import com.sloth.runveal.tabmypage.contract.ImportDongStat
import com.sloth.runveal.tabmypage.contract.ImportRecordsResultState

/** 시안(`A4-3`)이 지정한 지도 카드 크기 — 화면 너비를 채우지 않고 고정 크기로 가운데 놓인다. */
private val MAP_CARD_WIDTH = 240.dp
private val MAP_CARD_HEIGHT = 180.dp

/** 지도 카드 ↔ 탐험 결과 묶음 사이. 시안이 두 묶음 사이 간격을 안 잡아 둬서 섹션 간격(32dp)을 썼다. */
private val SECTION_GAP = 32.dp

/** 탐험률 전/후 숫자 칸. 자릿수가 달라도("8%" vs "100%") 화살표 위치가 흔들리지 않게 폭을 고정한다. */
private val RATE_COLUMN_WIDTH = 30.dp

/**
 * 기록 불러오기 결과. 가져온 게 있으면 탐험 결과(`A4-3`), 한 건도 없으면 빈 상태(`A4-4`)다.
 *
 * 두 경우가 화면 구성이 꽤 다르다 — 결과 쪽은 상단 앱바 없이 지도·집계를 가운데 놓고 버튼이 좌우
 * 두 개, 빈 상태 쪽은 뒤로가기가 있는 앱바에 버튼이 위아래 두 개다. 그래도 **흐름에서 같은 한
 * 단계**라 라우트를 쪼개지 않고 상태로 가른다.
 */
@Composable
internal fun ImportRecordsResultScreen(
    onBackClick: () -> Unit,
    onCreateCourseClick: () -> Unit,
    onGoHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportRecordsResultViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // 빈 상태에만 뒤로가기가 있다(시안 기준) — 가져온 게 있을 때는 아래 두 버튼으로 나간다.
        if (state is ImportRecordsResultState.Empty) {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = onBackClick,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = RunvealTheme.layout.marginScreen),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = state) {
                // 진행 화면에서 넘어오면 결과가 이미 채워져 있어서 거의 보이지 않는 상태 — 프로세스가
                // 죽었다 이 화면으로 복구된 경우에만 남는다.
                ImportRecordsResultState.Loading -> RunvealImportRecordsLoading()
                ImportRecordsResultState.Empty -> EmptyContent()
                is ImportRecordsResultState.Ready -> ResultContent(state = current)
            }
        }

        BottomButtons(
            isEmpty = state is ImportRecordsResultState.Empty,
            onCreateCourseClick = onCreateCourseClick,
            onGoHomeClick = onGoHomeClick,
        )
    }
}

/**
 * 빈 상태는 "코스 만들기"를 주 버튼으로 세우고 나가는 길을 아래 텍스트 버튼으로 둔다(`A4-4`).
 * 가져온 게 있으면 둘 다 같은 무게다(`A4-3`).
 */
@Composable
private fun BottomButtons(
    isEmpty: Boolean,
    onCreateCourseClick: () -> Unit,
    onGoHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val insetModifier = modifier
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))

    if (isEmpty) {
        RunvealButtonGroup(modifier = insetModifier) {
            RunvealButtonGroupPrimaryText(
                primaryLabel = stringResource(R.string.import_records_result_create_course),
                onPrimaryClick = onCreateCourseClick,
                textLabel = stringResource(R.string.import_records_empty_go_home),
                onTextClick = onGoHomeClick,
            )
        }
    } else {
        RunvealButtonGroupEqual(
            firstLabel = stringResource(R.string.import_records_result_create_course),
            onFirstClick = onCreateCourseClick,
            secondLabel = stringResource(R.string.import_records_result_view_map),
            onSecondClick = onGoHomeClick,
            modifier = insetModifier
                .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space8),
        )
    }
}

/** 불러올 기록이 없을 때(`A4-4`). 레이아웃은 [RunvealEmptyState]가 맡는다 — 시안의 80dp 그래픽,
 * 간격 24/8, 제목·설명 스타일이 그대로 맞는다. 그래픽은 코스·기록 탭의 빈 상태와 같은 그림이다. */
@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    RunvealEmptyState(
        title = stringResource(R.string.import_records_empty_title),
        description = stringResource(R.string.import_records_empty_description),
        graphic = { Image(painter = painterResource(R.drawable.empty_course), contentDescription = null) },
        modifier = modifier,
    )
}

@Composable
private fun ResultContent(state: ImportRecordsResultState.Ready, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RouteMapCard(request = state.mapSnapshotRequest)

            Text(
                text = stringResource(R.string.import_records_result_title, state.importedCount),
                style = RunvealTheme.typography.heading2,
                color = RunvealTheme.colors.text.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(SECTION_GAP))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExploreResultCard(state = state)

            // 경로가 없어 지도에 못 올린 기록이 없으면 꺼낼 말이 아니다.
            if (state.skippedCount > 0) {
                Text(
                    text = stringResource(R.string.import_records_result_skipped, state.skippedCount),
                    style = RunvealTheme.typography.captionMRegular,
                    color = RunvealTheme.colors.text.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 가져온 경로를 모두 그린 지도. [request]가 없으면(경로가 2점 미만) 빈 카드만 남는다. */
@Composable
private fun RouteMapCard(request: SnapshotRequest?, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius24
    Box(
        modifier = modifier
            .size(width = MAP_CARD_WIDTH, height = MAP_CARD_HEIGHT)
            .clip(shape)
            .background(colors.bg.map, shape)
            .border(1.dp, colors.border.default, shape),
    ) {
        CourseSnapshotImage(
            request = request,
            contentDescription = stringResource(R.string.import_records_result_map_content_description),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** "새로 탐험한 길" + 탐험률이 오른 동 목록. 러닝 결과 화면의 같은 카드보다 글자가 작다(시안 기준). */
@Composable
private fun ExploreResultCard(state: ImportRecordsResultState.Ready, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.default, RunvealTheme.shapes.radius16)
            // 시안(A4-3)의 이 카드만 안쪽 여백이 16이다 — 다른 카드(20)보다 촘촘하다.
            .padding(RunvealTheme.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.import_records_result_new_road),
                style = RunvealTheme.typography.bodyMSemibold,
                color = colors.text.primary,
            )
            Text(
                text = stringResource(R.string.import_records_result_new_road_value, state.newRoadLabel),
                style = RunvealTheme.typography.bodyMRegular,
                color = colors.text.primary,
            )
        }

        // 탐험률이 오른 동이 하나도 없으면(구간 인덱스를 못 들여온 지역만 달린 경우 등) 제목만
        // 남아 빈 줄처럼 보이므로 섹션을 통째로 뺀다.
        if (state.dongStats.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            ) {
                Text(
                    text = stringResource(R.string.import_records_result_explore_rate),
                    style = RunvealTheme.typography.bodyMSemibold,
                    color = colors.text.primary,
                )
                state.dongStats.forEach { dongStat -> DongRateRow(dongStat) }
            }
        }
    }
}

@Composable
private fun DongRateRow(dongStat: ImportDongStat, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dongStat.name,
            style = RunvealTheme.typography.bodyMRegular,
            color = colors.text.secondary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.import_records_result_rate_percent, dongStat.beforePercent),
                style = RunvealTheme.typography.bodyMRegular,
                color = colors.text.tertiary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(RATE_COLUMN_WIDTH),
            )
            Text(
                text = stringResource(R.string.import_records_result_rate_arrow),
                style = RunvealTheme.typography.bodyMRegular,
                color = colors.text.tertiary,
            )
            Text(
                text = stringResource(R.string.import_records_result_rate_percent, dongStat.afterPercent),
                style = RunvealTheme.typography.bodyMRegular,
                color = colors.text.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(RATE_COLUMN_WIDTH),
            )
        }
    }
}
