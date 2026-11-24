package com.sloth.runveal.courserecommend.contract

import com.sloth.runveal.map.GeoMath
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.ui.mvi.UiState

internal val DistanceOptions = listOf("3km", "5km", "7km", "10km")
internal const val DefaultDistance = "5km"
internal val FilterOptions = listOf("신호/횡단보도 적은 길", "완만한 길", "공원 지나가기", "하천길 따라가기")

/**
 * 필터 칩 라벨 → `POST /api/v1/routes`의 `filters` 코드(`docs/api/API.md` §3.3.1).
 *
 * "신호/횡단보도 적은 길"은 **서버에 대응 코드가 없어 빠져 있다** — 고르면 화면에는 켜진 것으로
 * 남지만 추천 결과는 달라지지 않는다. 서버가 코드를 추가하면 여기에 한 줄 넣으면 된다.
 */
internal val FilterServerCodes = mapOf(
    "완만한 길" to "GENTLE_SLOPE",
    "공원 지나가기" to "PARK",
    "하천길 따라가기" to "RIVERSIDE",
)

/** 1km ~ 42km, 0.5km 단위. 인덱스 22 = "12" (휠 피커 기본값). */
internal val CustomDistanceValues = (2..84).map { step ->
    val km = step / 2.0
    if (km % 1.0 == 0.0) km.toInt().toString() else km.toString()
}
internal const val DefaultCustomDistanceIndex = 22

/** 거리를 아직 잴 수 없을 때 — 단위를 남겨 둬서 그 자리가 거리라는 건 보이게 한다. */
private const val DISTANCE_PLACEHOLDER = "-km"

/** 상단 앱바 제목을 바꾸는 출발/도착 선택 모드. 선택 중이 아니면 [None]. */
enum class LocationSelectionTarget { None, Start, End }

data class CourseCreateState(
    val startLocation: String = "현위치",
    val endLocation: String = "출발지로 돌아오기",
    val endLocationIsDefault: Boolean = true,
    val locationSelectionTarget: LocationSelectionTarget = LocationSelectionTarget.None,
    /** 지도를 탭해 고른 실제 좌표(확정 전) — [startLatLon]/[endLatLon]은 [ConfirmLocationSelection]에서
     * 여기서 복사된다. */
    val pickedLocation: LatLon? = null,
    val startLatLon: LatLon? = null,
    val endLatLon: LatLon? = null,
    /** [pickedLocation]이 속한 행정동 이름(`GET /api/v1/dong`) — 아직 못 받았거나 조회가 실패하면
     * null이고, 그때는 [selectedPlaceName]이 좌표를 그대로 보여준다. */
    val pickedPlaceName: String? = null,
    /** 화면이 받아 둔 현재 위치 — [selectedPlaceDistance]를 재는 기준점이다. 권한이 없거나 위치가
     * 꺼져 있으면 null이고, 그때는 거리를 안 보여준다. */
    val currentLocation: LatLon? = null,
    val distanceOptions: List<String> = DistanceOptions,
    val selectedDistance: String = DefaultDistance,
    val isCustomDistanceSelected: Boolean = false,
    val customDistanceValues: List<String> = CustomDistanceValues,
    val customDistanceIndex: Int = DefaultCustomDistanceIndex,
    val avoidVisitedPreferred: Boolean = true,
    val filterOptions: List<String> = FilterOptions,
    val selectedFilters: Set<String> = emptySet(),
    val showFilterSheet: Boolean = false,
) : UiState {
    val distanceLabel: String
        get() = if (isCustomDistanceSelected) "${customDistanceValues[customDistanceIndex]}km" else selectedDistance

    /**
     * 탭한 자리의 행정동 이름([pickedPlaceName], 예: "중구 명동"). 조회 중이거나 실패했으면 좌표를
     * 그대로 보여준다 — 이름이 안 떠도 어디를 찍었는지는 알 수 있어야 한다.
     *
     * 확정하면 이 값이 그대로 출발지/도착지 라벨로 들어간다.
     */
    val selectedPlaceName: String
        get() = pickedPlaceName
            ?: pickedLocation?.let { "%.4f, %.4f".format(it.lat, it.lon) }
            ?: "위치를 선택하세요"

    /**
     * 현재 위치에서 고른 자리까지의 거리 — 선택 패널에서 동 이름 아래 줄에 쓴다.
     *
     * 아직 잴 수 없을 때도(현재 위치를 못 받았거나 자리를 안 골랐을 때) 줄을 비우지 않고
     * [DISTANCE_PLACEHOLDER]를 둔다 — 줄이 사라지면 사용자가 그 자리가 거리인지 알 수 없다.
     *
     * **직선거리**다(`GeoMath`) — 실제로 달릴 경로 길이가 아니다. 길을 따라가는 거리는 서버에
     * 물어야 하는데, 여기서 필요한 건 "고른 자리가 지금 얼마나 먼지" 감을 주는 것뿐이다.
     */
    val selectedPlaceDistance: String
        get() {
            val from = currentLocation ?: return DISTANCE_PLACEHOLDER
            val to = pickedLocation ?: return DISTANCE_PLACEHOLDER
            return "%.1fkm".format(GeoMath.distanceMeters(from, to) / 1000.0)
        }

    val topBarTitle: String
        get() = when (locationSelectionTarget) {
            LocationSelectionTarget.None -> "코스 만들기"
            LocationSelectionTarget.Start -> "출발지 선택"
            LocationSelectionTarget.End -> "도착지 선택"
        }

    val locationConfirmLabel: String
        get() = when (locationSelectionTarget) {
            LocationSelectionTarget.None -> ""
            LocationSelectionTarget.Start -> "이 위치를 출발지로 설정하기"
            LocationSelectionTarget.End -> "이 위치를 도착지로 설정하기"
        }

    val locationHintMessage: String
        get() = when (locationSelectionTarget) {
            LocationSelectionTarget.None -> ""
            LocationSelectionTarget.Start -> "지도를 탭해 출발지를 선택하세요"
            LocationSelectionTarget.End -> "지도를 탭해 도착지를 선택하세요"
        }
}
