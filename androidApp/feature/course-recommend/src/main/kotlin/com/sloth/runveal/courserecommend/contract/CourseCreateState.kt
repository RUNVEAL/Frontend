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
    // 좌표→실제 주소/동 이름 변환은 서버·앱 모두에 아직 없다(API.md §1 — 제3자 지오코딩 API 자체를 안 씀).
    // 거리도 같은 이유로 아직 미구현 고정 문구로 둔다.
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

    // TODO: 좌표 기반 주소(또는 동 이름) 변환 추가되면 좌표 대신 그 결과를 보여주기.
    /** 장소명 대신 탭한 좌표를 그대로 보여준다(§1 — 좌표→주소 변환 자체를 안 씀). */
    val selectedPlaceName: String
        get() = pickedLocation?.let { "%.4f, %.4f".format(it.lat, it.lon) } ?: "위치를 선택하세요"

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
