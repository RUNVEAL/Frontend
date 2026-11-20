package com.sloth.runveal.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.main.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 시안(L0-1 스플래시)의 금빛 번짐 — 오른쪽 위에서 왼쪽 아래로 흐르는 대각선 그라데이션이다.
 * CSS `linear-gradient(212.83deg, …)` 값을 그대로 옮겼다: 각도는 "위쪽"을 0도로 시계방향이라,
 * 방향 벡터는 (sin θ, -cos θ)다(화면 y축이 아래로 향해서 부호가 이렇게 된다).
 */
private const val SPLASH_GLOW_ANGLE_DEGREES = 212.82791553699082
private val SPLASH_GLOW_START_COLOR = Color(0xFFFBEFBC).copy(alpha = 0.3f)
private val SPLASH_GLOW_END_COLOR = Color(0xFF958E70).copy(alpha = 0f)
private const val SPLASH_GLOW_START_STOP = 0.034881f
private const val SPLASH_GLOW_END_STOP = 0.62583f

/** 로고(Lottie 자리)와 태그라인 사이 간격 — 시안 기준 로고 아래 16dp. */
@Composable
internal fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas)
            .splashGlow()
            .padding(horizontal = RunvealTheme.layout.marginScreen),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
        ) {
            Image(
                painter = painterResource(R.drawable.splash),
                contentDescription = null,
                modifier = Modifier.size(width = 210.dp, height = 140.dp),
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                style = RunvealTheme.typography.heading2,
                color = RunvealTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 화면 전체에 까는 금빛 대각선 그라데이션. CSS와 같은 방식으로 **실제 크기에서** 그라데이션 선을
 * 계산한다 — 360x800 시안 좌표를 그대로 박으면 화면 비율이 다른 기기에서 빛이 엉뚱한 데로 간다.
 */
private fun Modifier.splashGlow(): Modifier = drawBehind {
    val radians = Math.toRadians(SPLASH_GLOW_ANGLE_DEGREES)
    val direction = Offset(sin(radians).toFloat(), -cos(radians).toFloat())
    // CSS 그라데이션 선의 길이 — 그 각도로 사각형을 완전히 덮는 길이다.
    val lineLength = abs(size.width * direction.x) + abs(size.height * direction.y)
    val center = Offset(size.width / 2f, size.height / 2f)
    val half = direction * (lineLength / 2f)
    drawRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                SPLASH_GLOW_START_STOP to SPLASH_GLOW_START_COLOR,
                SPLASH_GLOW_END_STOP to SPLASH_GLOW_END_COLOR,
            ),
            start = center - half,
            end = center + half,
        ),
    )
}
