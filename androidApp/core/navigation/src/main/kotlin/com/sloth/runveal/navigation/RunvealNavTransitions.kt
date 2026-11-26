package com.sloth.runveal.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * 두 NavHost(앱 최상위 [com.sloth.runveal.App], 홈 탭)가 **같이** 쓰는 화면 전환.
 *
 * **기본 전환을 그대로 쓰지 않는 이유**: Navigation의 기본 pop 전환은 나가는 화면을 축소시킨다
 * (`scaleOut`). 이 앱은 지도를 깔고 그 위에 화면을 얹는 구조라, 뒤로 갈 때 창이 쪼그라들었다
 * 펴지는 것처럼 보여 산만하다. 크기를 건드리지 않는 페이드만 쓴다.
 *
 * **제스처 뒤로가기도 같은 전환을 타게 해야 한다** — `predictivePopEnterTransition`/
 * `predictivePopExitTransition`을 따로 안 주면 버튼 뒤로가기만 바뀌고 제스처에는 축소가 남는다.
 *
 * 길이는 기본값(700ms)보다 짧게 잡는다. 전환이 길수록 지도 덮개가 걷히기 전의 어중간한 구간이
 * 오래 보인다 — 숫자 하나만 바꾸면 되도록 여기 모아 둔다.
 *
 * 두 NavHost가 같은 값을 쓰는 게 중요하다. 한쪽만 바꿔 두면 같은 뒤로가기인데 앱 최상위(온보딩·
 * 가입 흐름)와 탭 안에서 움직임이 달라진다.
 */
object RunvealNavTransitions {

    const val DURATION_MILLIS = 220

    val enter: EnterTransition = fadeIn(tween(DURATION_MILLIS))
    val exit: ExitTransition = fadeOut(tween(DURATION_MILLIS))
}
