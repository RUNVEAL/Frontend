package com.swyp.runveal.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI 뼈대. 화면은 [onIntent]로 의도를 보내고, 그 처리는 [handleIntent]에서 [setState]로 상태를
 * 갱신하거나 [sendEffect]로 한 번만 소비되는 부수효과를 내보낸다. 효과가 필요 없는 화면은
 * Effect 타입 인자로 [Nothing]을 쓰면 된다.
 */
abstract class MviViewModel<Intent : UiIntent, State : UiState, Effect : UiEffect>(
    initialState: State,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()
    protected val currentState: State get() = _state.value

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    fun onIntent(intent: Intent) {
        viewModelScope.launch { handleIntent(intent) }
    }

    protected abstract suspend fun handleIntent(intent: Intent)

    protected fun setState(reduce: State.() -> State) {
        _state.update(reduce)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
