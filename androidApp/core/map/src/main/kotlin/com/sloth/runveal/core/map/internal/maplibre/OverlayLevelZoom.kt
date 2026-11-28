package com.sloth.runveal.core.map.internal.maplibre

import com.sloth.runveal.core.map.MapOverlayLevel
import com.sloth.runveal.map.MapConfig

/**
 * 보기 단위([MapOverlayLevel])를 레이어 줌 범위로 옮긴다.
 *
 * 숨길 때 가시성 프로퍼티가 아니라 줌 범위를 쓰는 이유: 이 레이어들은 원래부터 minZoom/maxZoom으로
 * 보이는 구간을 정하고 있다. 한 가지 방식으로 두면 "지금 왜 안 보이나"를 한 곳만 보고 알 수 있다.
 */

/** MapLibre가 쓰는 최대 줌보다 큰 값 — minZoom에 주면 영구히 숨고, maxZoom에 주면 상한이 없어진다. */
internal const val ZOOM_ABOVE_MAX = 30f

/** minZoom에 주면 하한이 없어지고(항상 보임), maxZoom에 주면 영구히 숨는다. */
internal const val ZOOM_BELOW_MIN = 0f

/**
 * 길 단위 전용 레이어(가본 길 선, 베이스 POI 라벨)의 minZoom.
 *
 * 길 단위면 줌아웃에서도 보여야 하므로 하한을 없애고, 동네 단위면 줌인에서도 숨어야 하므로 줌
 * 범위 밖으로 민다. null(홈 밖 화면)이면 예전처럼 줌인 구간에서만 보인다.
 */
internal fun roadLayerMinZoom(level: MapOverlayLevel?): Float = when (level) {
    MapOverlayLevel.Road -> ZOOM_BELOW_MIN
    MapOverlayLevel.Dong -> ZOOM_ABOVE_MAX
    null -> MapConfig.ZOOM_IN_THRESHOLD.toFloat()
}

/**
 * 동네 단위 전용 레이어(행정동 경계·채움·빛)의 maxZoom.
 *
 * 동네 단위면 줌인에서도 보여야 하므로 상한을 없앤다 — minZoom([MapConfig.SIGUNGU_SWITCH_ZOOM])은
 * 그대로 둬서 더 축소하면 시군구/시도로 넘어가는 계층은 유지한다. 길 단위면 숨긴다.
 */
internal fun dongLayerMaxZoom(level: MapOverlayLevel?): Float = when (level) {
    MapOverlayLevel.Dong -> ZOOM_ABOVE_MAX
    MapOverlayLevel.Road -> ZOOM_BELOW_MIN
    null -> MapConfig.ZOOM_IN_THRESHOLD.toFloat()
}
