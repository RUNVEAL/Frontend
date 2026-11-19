package com.sloth.runveal.onboarding

import com.sloth.runveal.data.local.RunvealDatabaseProvider
import com.sloth.runveal.onboarding.contract.OnboardingEffect
import com.sloth.runveal.onboarding.contract.OnboardingIntent
import com.sloth.runveal.onboarding.contract.OnboardingState
import com.sloth.runveal.ui.mvi.MviViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnboardingViewModel : MviViewModel<OnboardingIntent, OnboardingState, OnboardingEffect>(
    initialState = OnboardingState,
) {
    private val onboardingStore = OnboardingStore(RunvealDatabaseProvider.database)

    override suspend fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            // "봤다"를 먼저 적고 넘어간다 — 적기 전에 화면을 떠나면 다음 실행에 또 보여주게 된다.
            OnboardingIntent.Finished -> {
                withContext(Dispatchers.IO) { onboardingStore.markCompleted() }
                sendEffect(OnboardingEffect.NavigateToHome)
            }
        }
    }
}
