package com.swyp.runveal.sign.signup

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.designsystem.component.button.RunvealButton
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonStyle
import com.swyp.runveal.designsystem.component.input.RunvealCheckboxState
import com.swyp.runveal.designsystem.component.list.RunvealListItem
import com.swyp.runveal.designsystem.component.list.RunvealListItemSize
import com.swyp.runveal.designsystem.component.list.RunvealListItemTrailing
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.sign.R
import com.swyp.runveal.sign.signup.contract.RequiredTerm
import com.swyp.runveal.sign.signup.contract.TermsAgreementIntent

@Composable
internal fun TermsAgreementScreen(
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TermsAgreementViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                    label = stringResource(R.string.terms_agreement_next),
                    onClick = onNextClick,
                    style = RunvealButtonStyle.Gold,
                    fillWidth = true,
                    enabled = state.allAgreed,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(horizontal = RunvealTheme.layout.marginScreen),
        ) {
            Text(
                text = stringResource(R.string.terms_agreement_headline),
                style = RunvealTheme.typography.heading2,
                color = RunvealTheme.colors.text.primary,
                modifier = Modifier.padding(top = RunvealTheme.spacing.space24),
            )
            Column(
                modifier = Modifier.padding(top = RunvealTheme.spacing.space32),
                verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RunvealTheme.shapes.radius16)
                        .background(RunvealTheme.colors.surface.raised),
                ) {
                    RunvealListItem(
                        title = stringResource(R.string.terms_agreement_all),
                        onClick = { viewModel.onIntent(TermsAgreementIntent.ToggleAll) },
                        leadingCheckbox = checkboxState(state.allAgreed),
                        size = RunvealListItemSize.L,
                    )
                }
                RequiredTerm.entries.forEach { term ->
                    RunvealListItem(
                        title = stringResource(term.labelRes),
                        onClick = { viewModel.onIntent(TermsAgreementIntent.Toggle(term)) },
                        leadingCheckbox = checkboxState(state.isAgreed(term)),
                        trailing = RunvealListItemTrailing.TextButton,
                        trailingButtonLabel = stringResource(R.string.terms_agreement_view_button),
                        // TODO: 약관 본문 보기 — 약관 목록 API(API.md §3.8)가 붙으면 받아 온 URL을 연다.
                        onTrailingButtonClick = {},
                    )
                }
            }
        }
    }
}

private fun checkboxState(checked: Boolean): RunvealCheckboxState =
    if (checked) RunvealCheckboxState.Checked else RunvealCheckboxState.Unchecked

private val RequiredTerm.labelRes: Int
    get() = when (this) {
        RequiredTerm.Service -> R.string.terms_agreement_service_required
        RequiredTerm.Privacy -> R.string.terms_agreement_privacy_required
        RequiredTerm.Location -> R.string.terms_agreement_location_required
    }
