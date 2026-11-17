package com.sloth.runveal.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * 이 화면이 떠 있는 동안 [markers]를 지도에 띄우고, 화면을 벗어나면 **자기가 올린 마커일 때만** 치운다.
 *
 * [MapState.dotMarkers]는 화면들이 같이 쓰는 값이라, 떠나는 화면이 무조건 비우면 문제가 생긴다 —
 * 화면 전환은 "다음 화면이 먼저 뜨고, 전환 애니메이션이 끝나면 이전 화면이 dispose"되는 순서라
 * 다음 화면이 올려 둔 마커까지 지워진다(코스 상세에서 러닝을 시작하면 출발·도착 핀이 사라지던
 * 원인). 그래서 [MapState.dotMarkersOwner] 표식으로 주인을 확인하고 치운다.
 *
 * 빈 목록은 "아직 띄울 게 없다"는 뜻으로 보고 지도를 건드리지 않는다 — 화면 전환 중 상태가 잠깐
 * 비는 동안(ViewModel이 정리되며 Loading으로 돌아가는 등) 다음 화면의 마커를 지우면 안 된다.
 */
@Composable
fun MapDotMarkers(markers: List<MapPin>) {
    val mapState = LocalMapState.current
    val owner = remember { Any() }
    DisposableEffect(mapState, markers, owner) {
        if (markers.isNotEmpty()) {
            mapState.dotMarkersOwner = owner
            mapState.dotMarkers = markers
        }
        onDispose {
            if (mapState.dotMarkersOwner === owner) {
                mapState.dotMarkersOwner = null
                mapState.dotMarkers = emptyList()
            }
        }
    }
}
