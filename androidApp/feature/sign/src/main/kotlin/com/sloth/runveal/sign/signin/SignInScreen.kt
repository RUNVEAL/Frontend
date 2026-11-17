package com.sloth.runveal.sign.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButton
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonStyle
import com.sloth.runveal.designsystem.component.overlay.RunvealLoading
import com.sloth.runveal.designsystem.component.overlay.RunvealToast
import com.sloth.runveal.designsystem.component.overlay.RunvealToastType
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.sign.R
import com.sloth.runveal.sign.signin.contract.SignInEffect
import com.sloth.runveal.sign.signin.contract.SignInIntent
import com.sloth.runveal.ui.mvi.CollectEffect
import kotlinx.coroutines.delay

private const val ERROR_TOAST_DURATION_MILLIS = 3000L

@Composable
internal fun SignInScreen(
    onSignedIn: () -> Unit,
    onNeedsSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            SignInEffect.NavigateToHome -> onSignedIn()
            SignInEffect.NavigateToSignUp -> onNeedsSignUp()
        }
    }

    // 실패 안내는 3초만 띄운다 — 사라지는 타이머는 띄우는 쪽이 맡는다(RunvealToast 주석, HomeScreen과 같은 방식).
    LaunchedEffect(state.showError) {
        if (!state.showError) return@LaunchedEffect
        delay(ERROR_TOAST_DURATION_MILLIS)
        viewModel.onIntent(SignInIntent.ErrorShown)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
                ) {
                    Text(
                        text = stringResource(R.string.sign_in_logo),
                        style = RunvealTheme.typography.heading1Semibold,
                        color = RunvealTheme.colors.text.primary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.sign_in_headline),
                        style = RunvealTheme.typography.heading1Semibold,
                        color = RunvealTheme.colors.text.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            RunvealButtonGroup(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
                ) {
                    RunvealButton(
                        label = stringResource(R.string.sign_in_kakao_button),
                        onClick = { viewModel.onIntent(SignInIntent.KakaoLoginClicked) },
                        style = RunvealButtonStyle.Secondary,
                        enabled = !state.isLoading,
                        fillWidth = true,
                        leadingIcon = RunvealIcons.Kakao,
                        tintLeadingIcon = false,
                    )
                    RunvealButton(
                        label = stringResource(R.string.sign_in_google_button),
                        onClick = { viewModel.onIntent(SignInIntent.GoogleLoginClicked) },
                        style = RunvealButtonStyle.Secondary,
                        enabled = !state.isLoading,
                        fillWidth = true,
                        leadingIcon = RunvealIcons.Google,
                        tintLeadingIcon = false,
                    )
                }
            }
        }

        if (state.showError) {
            RunvealToast(
                message = stringResource(R.string.sign_in_error),
                type = RunvealToastType.Error,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space16),
            )
        }

        // 소셜 SDK 화면이 닫힌 뒤(= 토큰 저장까지)에도 버튼만 멀쩡히 보이면 아무 반응이 없는 것처럼
        // 보여서 딤 + 로딩을 덮는다. 그 사이 뒤쪽 탭도 같이 막는다.
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                RunvealLoading()
            }
        }
    }
}
