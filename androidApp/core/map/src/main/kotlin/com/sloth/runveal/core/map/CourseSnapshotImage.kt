package com.sloth.runveal.core.map

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.sloth.runveal.core.map.internal.snapshot.CourseSnapshotRendererImpl
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.map.snapshot.SnapshotRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 코스 목록/기록 목록 썸네일, 러닝 결과 카드에 쓰는 지도 스냅샷 이미지. 실제로 그려질 픽셀 크기를
 * [Modifier.onSizeChanged]로 직접 재서 [CourseSnapshotRenderer]에 넘긴다 — 코스 목록(72dp)과 기록
 * 목록(80dp)처럼 자리마다 크기가 달라도 호출 쪽 레이아웃을 바꿀 필요가 없다(`docs/running/COURSE_MAP_SNAPSHOT.md`
 * §4 Step 3·4). 캐시가 있으면 바로 보여주고, 없으면 [placeholder]를 보여주며 렌더링한 뒤 전환한다.
 * 실패하면 [placeholder]를 유지한다 — 재시도 버튼은 없고, 다음에 이 컴포저블이 다시 그려질 때 자동으로
 * 재시도된다.
 */
@Composable
fun CourseSnapshotImage(
    request: SnapshotRequest?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = { RunvealMapPlaceholder() },
) {
    val context = LocalContext.current
    val colors = RunvealTheme.colors
    // 출발·도착 점은 디자인시스템 마커(`RunvealMapMarker`)와 같은 색 — 출발은 금색, 도착은 흰색에
    // 둘 다 어두운 테두리(icon/onAccent). ViewModel이 쓰는 [SnapshotColors.DesignTokens]와 같은 값이다.
    val snapshotColors = SnapshotColors(
        route = colors.map.roadExplored.toArgb(),
        start = colors.accent.glow.toArgb(),
        startStroke = colors.icon.onAccent.toArgb(),
        end = colors.icon.primary.toArgb(),
        endStroke = colors.icon.onAccent.toArgb(),
        resultEndpoint = colors.map.location.toArgb(),
        resultEndpointStroke = colors.text.primary.toArgb(),
        newRoad = colors.map.roadNew.toArgb(),
        todayRoad = colors.map.roadToday.toArgb(),
        dongMask = colors.bg.map.toArgb(),
        dongOutline = colors.border.default.toArgb(),
    )
    val renderer = remember(snapshotColors) { CourseSnapshotRendererImpl(context = context, colors = snapshotColors) }

    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    val bitmap by produceState<ImageBitmap?>(initialValue = null, request, sizePx, renderer) {
        value = null
        android.util.Log.d("CourseSnapshotDebug", "produceState request=${request != null} variant=${request?.variant} sizePx=$sizePx")
        if (request == null || sizePx.width <= 0 || sizePx.height <= 0) return@produceState
        val file = renderer.cachedFile(request, sizePx.width, sizePx.height)
            ?: renderer.getOrRender(request, sizePx.width, sizePx.height)
        android.util.Log.d("CourseSnapshotDebug", "file=$file")
        value = file?.let { decodeBitmap(it) }
        android.util.Log.d("CourseSnapshotDebug", "bitmap decoded=${value != null}")
    }

    Box(modifier = modifier.onSizeChanged { sizePx = it }) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            placeholder()
        }
    }
}

@Composable
private fun RunvealMapPlaceholder() {
    Box(modifier = Modifier.fillMaxSize().background(RunvealTheme.colors.bg.map))
}

private suspend fun decodeBitmap(file: File): ImageBitmap? = withContext(Dispatchers.IO) {
    BitmapFactory.decodeFile(file.path)?.asImageBitmap()
}
