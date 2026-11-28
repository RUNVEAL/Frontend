package com.sloth.runveal.running.image

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.view.doOnPreDraw
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 컴포저블을 **화면 크기·밀도와 무관하게** 정해진 픽셀 크기로 그려 비트맵으로 돌려준다.
 *
 * 화면에 보이는 뷰를 캡처하지 않고 따로 그리는 이유:
 *  - 기기 밀도가 제각각이라 화면을 그대로 찍으면 결과물 크기가 기기마다 다르다. 여기서는 밀도를
 *    [LocalDensity]로 강제해 360x640dp 아트보드가 항상 같은 픽셀 수로 나온다.
 *  - 스크롤로 가려진 부분은 화면 캡처로는 안 찍힌다.
 *
 * **보이지 않게 붙였다 떼는 이유**: `ComposeView`는 윈도에 붙어야 컴포지션이 돌아간다. 붙이지 않고
 * measure/layout만 하면 내용이 비어 있는 비트맵이 나온다. 그래서 액티비티 콘텐츠에 [View.INVISIBLE]로
 * 잠깐 붙여 한 프레임 그리게 한 뒤 떼어 낸다 — 사용자에게는 보이지 않는다.
 */
suspend fun captureComposable(
    context: Context,
    width: Dp,
    height: Dp,
    /** 1dp를 몇 px로 그릴지. 3이면 360x640dp가 1080x1920px로 나온다. */
    scale: Int,
    content: @Composable () -> Unit,
): Bitmap? {
    val activity = context.findActivity() ?: return null
    val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return null

    val widthPx = (width.value * scale).toInt()
    val heightPx = (height.value * scale).toInt()
    if (widthPx <= 0 || heightPx <= 0) return null

    val composeView = ComposeView(activity).apply {
        visibility = View.INVISIBLE
        layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
        setContent {
            // 기기 밀도를 무시하고 내보낼 배율로 고정한다 — 폰트 배율도 같이 고정해야 사용자가
            // 글씨 크기를 키워 둔 기기에서 글자만 커져 레이아웃이 깨지지 않는다.
            CompositionLocalProvider(
                LocalDensity provides Density(density = scale.toFloat(), fontScale = 1f),
            ) {
                content()
            }
        }
    }

    root.addView(composeView)
    return try {
        composeView.awaitPreDraw()
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, widthPx, heightPx)

        Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { bitmap ->
            composeView.draw(Canvas(bitmap))
        }
    } catch (e: Exception) {
        // 메모리 부족(큰 비트맵)처럼 저장만 실패하고 앱은 계속 써야 하는 경우다 — 던지지 않고
        // null로 알려서 호출부가 "저장하지 못했어요"만 띄우게 한다.
        null
    } finally {
        root.removeView(composeView)
    }
}

/** 컴포지션이 한 번 돌아 그릴 내용이 생길 때까지 기다린다. */
private suspend fun View.awaitPreDraw() = suspendCancellableCoroutine { continuation ->
    doOnPreDraw { continuation.resume(Unit) }
}

/** Compose의 `LocalContext`는 보통 액티비티를 감싼 ContextWrapper라 한 겹씩 벗겨 찾는다. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
