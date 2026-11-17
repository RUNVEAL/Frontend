package com.sloth.runveal.tabmypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.designsystem.component.button.RunvealButton
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonStyle
import com.sloth.runveal.designsystem.component.overlay.RunvealImportRecordsLoading
import com.sloth.runveal.designsystem.component.stat.RunvealProgressBar
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.external.ImportProgress
import com.sloth.runveal.feature.tabmypage.R
import com.sloth.runveal.tabmypage.contract.ImportRecordsProgressEffect
import com.sloth.runveal.ui.mvi.CollectEffect

/** 시안(`A4-2`)이 지정한 진행 바 두께. 탐험률 진행 바 토큰(6dp)과 달라서 직접 넘긴다. */
private val PROGRESS_BAR_HEIGHT = 10.dp

/**
 * Health Connect에서 기록을 가져오는 동안 보이는 화면(디자인 `A4-2`). 다 가져오면
 * [onFinished]로 결과 화면에 집계를 넘긴다.
 *
 * "중단하기"는 [onClose]를 부른다 — 이 화면이 백스택에서 빠지면 ViewModel이 정리되면서
 * 가져오기 코루틴도 함께 취소되므로, 중단에 따로 할 일이 없다. OS 뒤로가기도 같은 결과다.
 * 거기까지 들여온 기록은 남고 다음 실행이 이어 간다([ImportRecordsProgressViewModel] 주석).
 */
@Composable
internal fun ImportRecordsProgressScreen(
    onFinished: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // HealthConnectImporter가 Context를 받아서 ViewModel에 넘겨야 한다 — 화면보다 오래 사는
    // 객체라 Activity가 아니라 application context를 준다.
    val viewModel: ImportRecordsProgressViewModel = viewModel {
        ImportRecordsProgressViewModel(context.applicationContext)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            ImportRecordsProgressEffect.Finished -> onFinished()
            ImportRecordsProgressEffect.Unavailable -> onClose()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = RunvealTheme.layout.marginScreen),
            contentAlignment = Alignment.Center,
        ) {
            ImportingContent(progress = state.progress)
        }

        RunvealButtonGroup(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        ) {
            RunvealButton(
                label = stringResource(R.string.import_records_progress_stop),
                onClick = onClose,
                style = RunvealButtonStyle.Text,
                fillWidth = true,
            )
        }
    }
}

/**
 * 화면 중앙에 모이는 묶음 — 모션, 제목·설명, 진행 바·건수. 시안이 묶음 안 간격을 24(모션↔글)와
 * 8(제목↔설명)로만 잡아 둬서, 글↔진행 바는 24를, 진행 바↔건수는 8을 그대로 따랐다.
 */
@Composable
private fun ImportingContent(progress: ImportProgress, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 크기를 안 주면 JSON 원본 크기(100dp)로 그려진다 — 시안도 같은 100dp다.
        RunvealImportRecordsLoading()

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.import_records_progress_title),
                style = RunvealTheme.typography.heading2,
                color = colors.text.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.import_records_progress_description),
                style = RunvealTheme.typography.bodyMRegular,
                color = colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 가져온 만큼만 채운다 — "이번에 새로 늘어난 만큼"을 따로 보여 주는 탐험률 진행 바와
            // 달리 여기선 한 가지 진행도뿐이라 gained는 쓰지 않는다.
            RunvealProgressBar(
                filledPercent = progress.donePercent(),
                gainedPercent = 0f,
                height = PROGRESS_BAR_HEIGHT,
            )

            // 목록을 훑는 동안은 전체 건수를 몰라서(`Scanning`) 건수를 비워 둔다 — 진행 바는 0으로
            // 자리를 지키고, 총 건수가 정해지는 순간 숫자가 들어온다.
            if (progress is ImportProgress.Running) {
                Text(
                    text = stringResource(
                        R.string.import_records_progress_count,
                        progress.done,
                        progress.total,
                    ),
                    style = RunvealTheme.typography.captionMRegular,
                    color = colors.text.tertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 진행률(0~100). 아직 훑는 중이거나 가져올 게 없으면 0이다. */
private fun ImportProgress.donePercent(): Float = when (this) {
    is ImportProgress.Running -> if (total > 0) done * 100f / total else 0f
    ImportProgress.Idle, ImportProgress.Scanning -> 0f
    is ImportProgress.Finished, is ImportProgress.Unavailable -> 0f
}
