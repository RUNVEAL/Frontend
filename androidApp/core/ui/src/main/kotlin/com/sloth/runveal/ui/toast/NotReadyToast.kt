package com.sloth.runveal.ui.toast

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sloth.runveal.core.ui.R
import com.sloth.runveal.designsystem.component.overlay.RunvealToast
import com.sloth.runveal.designsystem.theme.RunvealTheme
import kotlinx.coroutines.delay

/** 토스트 노출 시간 — 앱 안 다른 토스트와 같은 값. */
private const val TOAST_DURATION_MILLIS = 3000L

/**
 * **아직 안 붙은 기능**을 눌렀을 때 "개발 중" 안내를 띄우는 상태.
 *
 * 누를 수는 있는데 아무 일도 안 일어나면 고장으로 보인다. 기능이 붙기 전까지 그 자리를 메운다 —
 * 버튼을 지우거나 비활성화하지 않는 건, 시안에 있는 자리를 그대로 두고 동작만 나중에 채우기
 * 때문이다.
 *
 * 쓰는 쪽은 [rememberNotReadyToastState]로 만들어 `onClick`에 [show]를 걸고, 화면 Box 안에
 * [NotReadyToastHost]를 둔다.
 */
@Stable
class NotReadyToastState internal constructor() {
    internal var isVisible by mutableStateOf(false)

    fun show() {
        isVisible = true
    }

    internal fun dismiss() {
        isVisible = false
    }
}

@Composable
fun rememberNotReadyToastState(): NotReadyToastState = remember { NotReadyToastState() }

/**
 * [NotReadyToastState]가 켜지면 화면 위쪽에 토스트를 띄우고 3초 뒤 스스로 내린다.
 * 사라지는 타이머를 띄우는 쪽이 맡는 건 `RunvealToast`가 순수 컴포넌트이기 때문이다(그 주석 참고).
 */
@Composable
fun BoxScope.NotReadyToastHost(state: NotReadyToastState) {
    LaunchedEffect(state.isVisible) {
        if (!state.isVisible) return@LaunchedEffect
        delay(TOAST_DURATION_MILLIS)
        state.dismiss()
    }

    if (state.isVisible) {
        RunvealToast(
            message = stringResource(R.string.not_ready_toast),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    horizontal = RunvealTheme.layout.marginScreen,
                    vertical = RunvealTheme.spacing.space16,
                ),
        )
    }
}
