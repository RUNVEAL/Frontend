package com.swyp.runveal.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Button/IconButton/RunStartButton처럼 누르면 위에 덮는 오버레이 색을 그린다
 * (design/runveal-components.json의 `states.pressed.overlay`). 색 자체를 바꾸지 않고
 * 위에 반투명 색을 한 겹 더 얹는 방식 — conventions.pressed 그대로.
 */
internal fun Modifier.runvealPressOverlay(shape: Shape, overlay: Color, pressed: Boolean): Modifier =
    if (pressed) background(overlay, shape) else this
