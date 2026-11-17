package com.sloth.runveal.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/** 화면이 보이는 동안 [effect]를 구독해 한 번만 소비되는 부수효과를 처리한다. */
@Composable
fun <Effect : UiEffect> CollectEffect(effect: Flow<Effect>, onEffect: suspend (Effect) -> Unit) {
    LaunchedEffect(effect) {
        effect.collect { onEffect(it) }
    }
}
