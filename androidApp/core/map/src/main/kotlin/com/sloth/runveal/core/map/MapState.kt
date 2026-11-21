package com.sloth.runveal.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerType
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.map.MapConfig

/** 지도 위에 떠 있는 마커 하나(출발/도착 등 위치 + 종류). */
data class MapPin(val location: LatLon, val type: RunvealMapMarkerType)

/**
 * 현재 위치 추적 명령 하나 — [MapState.startFollowingCurrentLocation]/
 * [MapState.stopFollowingCurrentLocation] 호출 한 번이 하나를 만들고 [MapLibreMapView]가 적용한다.
 *
 * [id]는 같은 내용으로 다시 불렀을 때도("내 위치" 버튼 재탭) 새 명령으로 보이게 하는 구분자다 —
 * 값이 같으면 LaunchedEffect가 다시 돌지 않아 카메라가 복귀하지 않는다.
 */
internal sealed interface FollowCurrentLocationCommand {
    val id: Int

    data class Start(override val id: Int, val zoom: Double?) : FollowCurrentLocationCommand

    data class Stop(override val id: Int, val resetAnchor: Boolean) : FollowCurrentLocationCommand
}

/** 지도의 초기 카메라(중심 좌표 + 줌), 현재 줌, 하위 화면이 선언한 레이어([MapLayers])를 들고 있다. */
@Stable
class MapState(
    val initialCenter: LatLon,
    val initialZoom: Double,
) {
    var currentZoom: Double by mutableDoubleStateOf(initialZoom)
        internal set

    /** 지도 위에 그릴 핀. 하위 화면(예: 출발/도착 선택)이 쓰고 지운다. */
    var pin: MapPin? by mutableStateOf(null)

    /** 지도 위에 늘 떠 있는 점 마커들(추천 코스의 출발·도착 등) — 선택 중 임시 핀([pin])과 별개로,
     * 설정한 화면이 떠 있는 동안 계속 보인다. 직접 쓰지 말고 [MapDotMarkers]를 쓴다. */
    var dotMarkers: List<MapPin> by mutableStateOf(emptyList())

    /** [dotMarkers]를 지금 띄워 둔 화면을 가리키는 표식 — [MapDotMarkers]가 "내가 올린 것만 치우기"를
     * 판단하는 데 쓴다. Compose 상태가 아니라 일반 필드다: 화면이 사라질 때(onDispose) 읽는 값이
     * 스냅샷 격리 때문에 옛 값으로 보이면 다음 화면이 올린 마커를 지워 버린다. */
    internal var dotMarkersOwner: Any? = null

    /** 지도 위에 늘 떠 있는 핀 마커들(코스 만들기에서 확정한 출발·도착 등) — [dotMarkers]와 같은
     * 용도지만 Pin 스타일로 그린다. [pin](선택 중 임시 핀)과도 별개. */
    var pinMarkers: List<MapPin> by mutableStateOf(emptyList())

    /** 홈 줌인 뷰에서 "안 가본 길" 보기를 고른 동안만 true — 켜면 베이스 지도의 도로를
     * `map.roadUnexploredFocus` 색으로 덮어 아직 안 가본 길이 어디로 이어지는지 보이게 한다
     * ([BaseStyleRoadFocus]). 가본 길(금색 선)은 그대로 둔다. */
    var unexploredRoadFocus: Boolean by mutableStateOf(false)

    /**
     * 현재 위치 마커에 "지금 바라보는 방향" 화살표를 띄울지(러닝 중). 기기 나침반(지자기 센서)을
     * 따라 도는 삼각형이 마커 둘레에 붙는다 — 달리는 방향(GPS 진행 방향)이 아니라 **기기가 향한
     * 쪽**이다. 홈 지도처럼 둘러보기만 하는 화면에서는 꺼 둔다(가만히 서 있어도 계속 떨린다).
     */
    var showsHeadingIndicator: Boolean by mutableStateOf(false)

    /** 지도를 탭한 마지막 좌표 — [MapLibreMapView]가 탭마다 채우고, 소비한 화면이 바로 null로 되돌려
     * 한 번만 처리되는 이벤트처럼 쓴다. */
    var lastMapClick: LatLon? by mutableStateOf(null)

    /** 0부터 시작 — [MapLibreMapView]가 이 값이 바뀔 때마다(첫 조립 제외) 현재 위치로 카메라를 다시
     * 옮긴다. 값 자체는 의미가 없고 변경 이벤트만 쓴다(일종의 one-shot 커맨드). */
    var recenterRequestId: Int by mutableIntStateOf(0)
        internal set

    /** [focusOnBounds]가 마지막으로 넘긴 좌표 — [MapLibreMapView]가 이 좌표가 전부 보이게 카메라를
     * 맞춘다. */
    var boundsFocusPoints: List<LatLon>? by mutableStateOf(null)
        internal set

    /** [focusOnBounds]의 [extraBottomPadding] — 하단을 덮는 바텀시트 등 때문에 화면 아래쪽은 안 보일 때,
     * 그 높이만큼 아래 여백을 더 줘서 "보이는 영역(위쪽)"에 좌표가 맞춰지게 한다. */
    var boundsFocusExtraBottomPadding: Dp by mutableStateOf(0.dp)
        internal set

    /** 0부터 시작 — [recenterRequestId]와 같은 one-shot 커맨드 패턴. */
    var boundsFocusRequestId: Int by mutableIntStateOf(0)
        internal set

    private val _layers = mutableStateMapOf<String, String>()
    internal val layers: Map<String, String> get() = _layers

    internal fun setLayer(id: String, geoJson: String) {
        _layers[id] = geoJson
    }

    internal fun clearLayer(id: String) {
        _layers.remove(id)
    }

    /** 마지막 현재 위치 추적 명령 — null이면 아직 아무도 추적을 요청하지 않았다. */
    internal var followCommand: FollowCurrentLocationCommand? by mutableStateOf(null)
        private set

    private var followCommandCount = 0

    /**
     * 추적 중 지도 아래쪽이 가려지는 높이(러닝 패널 등) — 그만큼 카메라 여백을 줘서 현재 위치가
     * **가려지지 않고 보이는 영역의 가운데**에 오게 한다([focusOnBounds]의 `extraBottomPadding`과 같은 뜻).
     *
     * 추적 on/off와 별개인 상태다 — 패널 높이가 바뀌면 추적 중일 때만 다시 반영되고, 지도를 드래그해
     * 추적이 풀린 동안에는 이 값이 바뀌어도 추적이 다시 켜지지 않는다.
     */
    var followBottomInset: Dp by mutableStateOf(0.dp)

    /** "내 위치" 버튼 등에서 호출 — 위치 권한/서비스가 꺼져 있으면 [CurrentLocationLayer]가 조용히
     * 무시한다. 현재 위치로 한 번 옮기기만 하고 계속 따라가지는 않는다 —
     * 러닝 중처럼 따라가야 할 땐 [startFollowingCurrentLocation]을 쓴다. */
    fun recenterToCurrentLocation() {
        recenterRequestId++
    }

    /**
     * 현재 위치를 계속 따라가는 카메라 추적을 켠다(러닝 중 지도). 방위는 북쪽으로 고정이다 —
     * 달리는 방향으로 지도가 돌면 길을 알아보기 어렵다.
     *
     * 지도를 드래그·핀치하면 추적이 자동으로 꺼진다([MapLibreMapView]) — 그 뒤엔 "내 위치"
     * 버튼에서 이 함수를 다시 불러 복귀한다. 지도는 화면보다 오래 살아 있으니, 켠 화면은 나갈 때
     * [stopFollowingCurrentLocation]으로 꼭 꺼 준다.
     *
     * 현재 위치를 둘 자리는 [followBottomInset]이 정한다.
     *
     * @param zoom 켜면서 맞출 줌. null이면 지금 줌을 그대로 둔다("내 위치" 버튼·러닝 재개).
     */
    fun startFollowingCurrentLocation(zoom: Double? = null) {
        followCommandCount++
        followCommand = FollowCurrentLocationCommand.Start(id = followCommandCount, zoom = zoom)
    }

    /**
     * 추적을 끈다. 카메라는 지금 보이는 자리에 그대로 둔다 — 러닝 일시정지나 지도 드래그처럼
     * "따라가기만 멈추는" 경우다. 특히 드래그로 풀 때 [resetAnchor]까지 하면 손가락을 대는 순간
     * 지도가 여백만큼 튀어 오른다.
     *
     * @param resetAnchor [followBottomInset]으로 준 카메라 여백까지 되돌릴지 — 러닝 화면을 나갈 때처럼
     *  이 지도를 다른 화면에 돌려줄 때만 true로 둔다(그대로 두면 다음 화면에서도 지도가 계속 치우쳐 보인다).
     */
    fun stopFollowingCurrentLocation(resetAnchor: Boolean = false) {
        followCommandCount++
        followCommand = FollowCurrentLocationCommand.Stop(id = followCommandCount, resetAnchor = resetAnchor)
    }

    /** [points]가 전부 화면에 보이게 카메라를 옮긴다(추천 코스 전체가 보이게 포커싱 등). 2개 미만이면
     * 무시한다. [extraBottomPadding]을 주면 그만큼 아래 여백을 더 둬서, 화면 하단을 덮는 바텀시트
     * 같은 오버레이가 있어도 코스가 그 위쪽(보이는 영역)에 맞춰지게 한다. */
    fun focusOnBounds(points: List<LatLon>, extraBottomPadding: Dp = 0.dp) {
        if (points.size < 2) return
        boundsFocusPoints = points
        boundsFocusExtraBottomPadding = extraBottomPadding
        boundsFocusRequestId++
    }
}

@Composable
fun rememberMapState(
    initialCenter: LatLon = MapConfig.SEOUL_CITY_HALL,
    initialZoom: Double = MapConfig.INITIAL_ZOOM,
): MapState = remember(initialCenter, initialZoom) { MapState(initialCenter, initialZoom) }

/** Map Shell(지도를 그리는 화면) 안에서만 제공된다. 지도 상태를 읽거나 [MapLayers]로 레이어를 선언할 때 쓴다. */
val LocalMapState = staticCompositionLocalOf<MapState> {
    error("MapState가 제공되지 않았다. Map Shell 안에서만 사용할 수 있다.")
}

/**
 * Map Shell이 지도 위에 떠 있는 하단 패널(탭바 등)에 얹은 높이 — 줌인 오버레이의 하단 액션 줄처럼
 * NavHost 안 화면이 그 패널과 겹치지 않게 띄워야 할 때 쓴다. Map Shell 밖에서는 항상 0.dp.
 */
val LocalMapOverlayBottomInset = compositionLocalOf { 0.dp }
