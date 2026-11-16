package com.swyp.runveal.sign.signup

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.designsystem.component.button.RunvealButton
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonStyle
import com.swyp.runveal.designsystem.component.input.RunvealTextInput
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.overlay.RunvealToast
import com.swyp.runveal.designsystem.component.overlay.RunvealToastType
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.sign.R
import com.swyp.runveal.sign.signup.contract.NicknameEffect
import com.swyp.runveal.sign.signup.contract.NicknameIntent
import com.swyp.runveal.ui.mvi.CollectEffect
import kotlinx.coroutines.delay

private const val ERROR_TOAST_DURATION_MILLIS = 3000L

@Composable
internal fun NicknameScreen(
    onBackClick: () -> Unit,
    onSignedUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NicknameViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            NicknameEffect.NavigateToHome -> onSignedUp()
        }
    }

    LaunchedEffect(state.showError) {
        if (!state.showError) return@LaunchedEffect
        delay(ERROR_TOAST_DURATION_MILLIS)
        viewModel.onIntent(NicknameIntent.ErrorShown)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = onBackClick,
                title = stringResource(R.string.sign_up_title),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )
        },
        bottomBar = {
            RunvealButtonGroup(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                RunvealButton(
                    label = stringResource(R.string.nickname_complete),
                    onClick = { viewModel.onIntent(NicknameIntent.CompleteClicked) },
                    style = RunvealButtonStyle.Gold,
                    fillWidth = true,
                    enabled = state.canSubmit,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RunvealTheme.layout.marginScreen),
            ) {
                Text(
                    text = stringResource(R.string.nickname_headline),
                    style = RunvealTheme.typography.heading2,
                    color = RunvealTheme.colors.text.primary,
                    modifier = Modifier.padding(top = RunvealTheme.spacing.space24),
                )
                RunvealTextInput(
                    value = state.nickname,
                    onValueChange = { viewModel.onIntent(NicknameIntent.NicknameChanged(it)) },
                    label = stringResource(R.string.nickname_label),
                    placeholder = stringResource(R.string.nickname_placeholder),
                    helper = if (state.hasInvalidChar) stringResource(R.string.nickname_error_invalid_char) else null,
                    isError = state.hasInvalidChar,
                    modifier = Modifier.padding(top = RunvealTheme.spacing.space32),
                )
            }

            if (state.showError) {
                RunvealToast(
                    message = stringResource(R.string.sign_up_error),
                    type = RunvealToastType.Error,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space16),
                )
            }
        }
    }
}
