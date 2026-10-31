package com.swyp.runveal.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** Runveal은 다크 모드 하나라 색은 하나의 인스턴스를 공유한다. */
internal val DefaultRunvealColors = RunvealColors()

val LocalRunvealColors = staticCompositionLocalOf { DefaultRunvealColors }

val LocalRunvealTypography = staticCompositionLocalOf<RunvealTypography> {
    error("RunvealTypography not provided. Wrap your content with RunvealTheme { }.")
}

/**
 * Runveal 디자인 시스템 테마 (다크 모드 하나).
 *
 * 토큰 값은 design/runveal-tokens.json에서 빌드 때 자동 생성된다 (gradle/runveal-tokens.gradle.kts).
 *
 * 사용: `RunvealTheme.colors.text.primary`, `RunvealTheme.typography.heading2`,
 * `RunvealTheme.spacing.space16`, `RunvealTheme.shapes.radius16`, `RunvealTheme.layout.marginScreen`
 *
 * Material3 컴포넌트도 어울리도록 MaterialTheme의 colorScheme / typography를 토큰에 맞춰 함께 제공한다.
 */
@Composable
fun RunvealTheme(content: @Composable () -> Unit) {
    val colors = DefaultRunvealColors
    val typography = runvealTypography

    CompositionLocalProvider(
        LocalRunvealColors provides colors,
        LocalRunvealTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = runvealMaterialColorScheme(colors),
            typography = typography.toMaterialTypography(),
            content = content,
        )
    }
}

object RunvealTheme {
    val colors: RunvealColors
        @Composable @ReadOnlyComposable get() = LocalRunvealColors.current

    val typography: RunvealTypography
        @Composable @ReadOnlyComposable get() = LocalRunvealTypography.current

    val palette get() = RunvealPalette
    val spacing get() = RunvealSpacing
    val radius get() = RunvealRadius
    val shapes get() = RunvealShapes
    val layout get() = RunvealLayout
    val effects get() = RunvealEffects
    val mapStroke get() = RunvealMapStroke
    val dong get() = RunvealDong
}

private fun runvealMaterialColorScheme(c: RunvealColors) = darkColorScheme(
    primary = c.accent.default,
    onPrimary = c.text.onAccent,
    primaryContainer = c.accent.default,
    onPrimaryContainer = c.text.onAccent,
    secondary = c.accent.glow,
    onSecondary = c.text.onAccent,
    background = c.bg.canvas,
    onBackground = c.text.primary,
    surface = c.surface.raised,
    onSurface = c.text.primary,
    surfaceVariant = c.surface.raised,
    onSurfaceVariant = c.text.secondary,
    surfaceContainer = c.surface.raised,
    surfaceContainerHigh = c.surface.raised,
    surfaceContainerHighest = c.surface.raised,
    surfaceContainerLow = c.surface.raised,
    surfaceContainerLowest = c.bg.canvas,
    outline = c.border.default,
    outlineVariant = c.border.default,
    error = c.status.error,
    onError = c.text.onAccent,
    scrim = c.bg.scrim,
)

private fun RunvealTypography.toMaterialTypography() = Typography(
    headlineLarge = heading1Semibold,
    headlineMedium = heading2,
    headlineSmall = heading3,
    titleLarge = heading2,
    titleMedium = heading3,
    titleSmall = bodyMSemibold,
    bodyLarge = bodyLRegular,
    bodyMedium = bodyMRegular,
    bodySmall = captionMRegular,
    labelLarge = labelButton,
    labelMedium = captionMRegular,
    labelSmall = captionSRegular,
)
