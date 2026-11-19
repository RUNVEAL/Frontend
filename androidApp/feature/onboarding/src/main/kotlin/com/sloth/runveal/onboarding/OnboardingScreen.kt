package com.sloth.runveal.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupSingle
import com.sloth.runveal.designsystem.component.navigation.RunvealPageIndicator
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.onboarding.R
import com.sloth.runveal.onboarding.contract.OnboardingEffect
import com.sloth.runveal.onboarding.contract.OnboardingIntent
import com.sloth.runveal.ui.mvi.CollectEffect

/** 온보딩 한 장 — 제목과 그래픽만 다르고 나머지 구성은 같다. */
private enum class OnboardingStep(@StringRes val titleRes: Int) {
    EXPLORE_MAP(R.string.onboarding_step1_title),
    COURSE_RECOMMEND(R.string.onboarding_step2_title),
    DONG_RATE(R.string.onboarding_step3_title),
}

/**
 * 첫 실행에 한 번 보여주는 온보딩(디자인 `L1_온보딩_Step1~3`). 세 장을 스와이프하거나 "다음"으로
 * 넘기고, 어느 장에서든 "건너뛰기"로 끝낼 수 있다 — 둘 다 "봤다"로 기록하고 홈으로 간다
 * ([OnboardingStore][com.sloth.runveal.onboarding.OnboardingStore]).
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val steps = OnboardingStep.entries
    // 화면은 한 장이고 단계에 따라 제목·그래픽만 갈아 끼운다 — 인디케이터와 버튼은 제자리에 있는다.
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            OnboardingEffect.NavigateToHome -> onFinished()
        }
    }

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
                leading = RunvealTopAppBarLeading.None,
                onLeadingClick = {},
                actionLabel = stringResource(R.string.onboarding_skip),
                onActionClick = { viewModel.onIntent(OnboardingIntent.Finished) },
            )

            OnboardingContent(
                step = steps[stepIndex],
                stepCount = steps.size,
                currentStep = stepIndex,
                modifier = Modifier.weight(1f),
            )

            RunvealButtonGroup(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                RunvealButtonGroupSingle(
                    label = stringResource(R.string.onboarding_next),
                    onClick = {
                        if (stepIndex == steps.lastIndex) {
                            viewModel.onIntent(OnboardingIntent.Finished)
                        } else {
                            stepIndex++
                        }
                    },
                )
            }
        }
    }
}

/** 제목 → 그래픽 → 페이지 인디케이터를 32 간격으로 쌓는다(시안 `Onboarding / Content`).
 * 단계가 바뀌면 인디케이터 위쪽(제목·그래픽)만 갈린다. */
@Composable
private fun OnboardingContent(step: OnboardingStep, stepCount: Int, currentStep: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RunvealTheme.layout.marginScreen),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(step.titleRes),
            style = RunvealTheme.typography.heading1Semibold,
            color = RunvealTheme.colors.text.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        OnboardingGraphicPlaceholder()
        RunvealPageIndicator(count = stepCount, current = currentStep)
    }
}

/**
 * 그래픽이 들어갈 자리(시안 `Graphic/Onboarding/1~3`, 좌우 여백을 뺀 정사각형).
 *
 * TODO: 단계별 그래픽이 나오면 이 자리에 넣는다. 지금은 자리만 알아볼 수 있게 테두리만 둔다 —
 *  시안의 배경색은 화면 배경과 같은 색이라 그대로 두면 아무것도 안 보인다.
 */
@Composable
private fun OnboardingGraphicPlaceholder(modifier: Modifier = Modifier) {
    val shape = RunvealTheme.shapes.radius24
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .border(1.dp, RunvealTheme.colors.border.subtle, shape),
    )
}

@Preview
@Composable
private fun OnboardingPagePreview() {
    RunvealTheme {
        Box(modifier = Modifier.fillMaxSize().background(RunvealTheme.colors.bg.canvas)) {
            OnboardingContent(step = OnboardingStep.EXPLORE_MAP, stepCount = 3, currentStep = 0)
        }
    }
}
