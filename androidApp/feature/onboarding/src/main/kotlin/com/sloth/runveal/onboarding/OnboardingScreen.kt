package com.sloth.runveal.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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

/** 단계가 바뀔 때 제목·그래픽이 겹쳐 바뀌는 시간. */
private const val STEP_TRANSITION_MILLIS = 300

/** 이만큼 옆으로 끌면 단계가 넘어간다 — 살짝 스친 것까지 넘기면 글을 읽다가도 장이 바뀐다. */
private val STEP_SWIPE_THRESHOLD = 56.dp

/** 밀려 들어오고 나가는 거리 — 콘텐츠 너비의 1/8만 움직인다(화면을 가로지르면 산만하다). */
private const val STEP_SLIDE_DIVIDER = 8

/** 온보딩 한 장 — 제목과 그래픽만 다르고 나머지 구성은 같다.
 * 그래픽은 design/images/onboarding{1,2,3}.png — 1312x1312px(= 328dp, xxxhdpi 4배)로, 시안의
 * 그래픽 자리(좌우 여백을 뺀 정사각형)와 같은 크기다. */
private enum class OnboardingStep(@StringRes val titleRes: Int, @DrawableRes val imageRes: Int) {
    EXPLORE_MAP(R.string.onboarding_step1_title, R.drawable.onboarding1),
    COURSE_RECOMMEND(R.string.onboarding_step2_title, R.drawable.onboarding2),
    DONG_RATE(R.string.onboarding_step3_title, R.drawable.onboarding3),
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

    // 좌우로 끌어서도 넘긴다. 페이저처럼 손가락을 따라 콘텐츠가 움직이지는 않고(인디케이터를 시안
    // 자리에 고정하려고 한 장짜리로 뒀다), 끌어 놓은 방향만 보고 단계를 바꾼다 — 바뀌는 모습은
    // "다음"을 눌렀을 때와 같은 전환이다.
    val swipeThresholdPx = with(LocalDensity.current) { STEP_SWIPE_THRESHOLD.toPx() }
    var swipeAmount by remember { mutableFloatStateOf(0f) }
    val swipeState = rememberDraggableState { delta -> swipeAmount += delta }

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
                modifier = Modifier
                    .weight(1f)
                    .draggable(
                        state = swipeState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = { swipeAmount = 0f },
                        onDragStopped = {
                            // 왼쪽으로 끌면 다음, 오른쪽으로 끌면 이전. 양 끝에서 더 끌어도 아무 일
                            // 없다 — 마지막 장을 넘겨서 온보딩이 끝나 버리면 실수로 지나치게 된다.
                            stepIndex = when {
                                swipeAmount <= -swipeThresholdPx -> (stepIndex + 1).coerceAtMost(steps.lastIndex)
                                swipeAmount >= swipeThresholdPx -> (stepIndex - 1).coerceAtLeast(0)
                                else -> stepIndex
                            }
                            swipeAmount = 0f
                        },
                    ),
            )

            val isLastStep = stepIndex == steps.lastIndex
            RunvealButtonGroup(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                RunvealButtonGroupSingle(
                    // 마지막 장에서는 "다음"이 아니라 "시작하기" — 다음 장이 없고 홈으로 나간다.
                    label = stringResource(if (isLastStep) R.string.onboarding_start else R.string.onboarding_next),
                    onClick = {
                        if (isLastStep) {
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

/**
 * 제목 → 그래픽 → 페이지 인디케이터를 32 간격으로 쌓고(시안 `Onboarding / Content`), 셋을 한 덩어리로
 * 상단 앱바와 버튼 사이 가운데에 둔다.
 *
 * 단계가 바뀌면 인디케이터 위쪽(제목·그래픽)만 갈린다 — 다음 장이 오른쪽에서 밀려 들어오며 겹쳐
 * 바뀐다. 인디케이터는 제자리에서 활성 점만 움직인다([RunvealPageIndicator]가 직접 애니메이션한다).
 */
@Composable
private fun OnboardingContent(step: OnboardingStep, stepCount: Int, currentStep: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RunvealTheme.layout.marginScreen),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space32, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val spec = tween<Float>(STEP_TRANSITION_MILLIS)
                val slideSpec = tween<IntOffset>(STEP_TRANSITION_MILLIS)
                // 뒤로 돌아갈 땐(오른쪽으로 스와이프) 반대 방향으로 밀린다.
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (fadeIn(spec) + slideInHorizontally(slideSpec) { width -> direction * width / STEP_SLIDE_DIVIDER })
                    .togetherWith(
                        fadeOut(spec) +
                            slideOutHorizontally(slideSpec) { width -> -direction * width / STEP_SLIDE_DIVIDER },
                    )
            },
            label = "onboarding-step",
        ) { animatedStep ->
            Column(
                verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space32),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(animatedStep.titleRes),
                    style = RunvealTheme.typography.heading1Semibold,
                    color = RunvealTheme.colors.text.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                OnboardingGraphic(step = animatedStep)
            }
        }
        RunvealPageIndicator(count = stepCount, current = currentStep)
    }
}

/** 단계별 그래픽(시안 `Graphic/Onboarding/1~3`) — 좌우 여백을 뺀 정사각형 자리에 원본 비율 그대로 넣는다. */
@Composable
private fun OnboardingGraphic(step: OnboardingStep, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(step.imageRes),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RunvealTheme.shapes.radius24),
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
