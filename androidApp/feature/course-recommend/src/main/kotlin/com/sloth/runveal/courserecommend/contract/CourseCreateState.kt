package com.sloth.runveal.courserecommend.contract

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
    // 거리는 아직 미구현 고정 문구다 — 좌표 사이 거리를 재 주는 API가 따로 없다.
    val selectedPlaceDistance: String = "거리 표시 미구현",
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
