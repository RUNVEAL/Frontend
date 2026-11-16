import SharedLogic
import SwiftUI

/// 지도 위에 떠 있는 마커 하나(출발/도착 등 위치 + 종류) — Android `MapPin`.
struct MapPin: Identifiable, Equatable {
    let id = UUID()
    let location: LatLon
    let type: RunvealMapMarkerType

    static func == (lhs: MapPin, rhs: MapPin) -> Bool {
        lhs.id == rhs.id
    }
}

/// 지도의 초기 카메라(중심 좌표 + 줌), 현재 줌, 하위 화면이 선언한 레이어를 들고 있다 —
/// Android `MapState`에 해당한다.
///
/// Map Shell(`RunvealHomeShell`)이 하나만 만들어 환경에 넣고, 하위 화면은 `@EnvironmentObject`로 읽는다.
@MainActor
final class RunvealMapState: ObservableObject {
    let initialCenter: LatLon
    let initialZoom: Double

    @Published private(set) var currentZoom: Double

    /// 지도 위에 그릴 핀. 하위 화면(예: 출발/도착 선택)이 쓰고 지운다.
    @Published var pin: MapPin?

    /// 지도 위에 늘 떠 있는 점 마커들(추천 코스의 출발·도착 등) — 선택 중 임시 핀([pin])과 별개로,
    /// 설정한 화면이 떠 있는 동안 계속 보인다. 직접 쓰지 말고 `.runvealMapDotMarkers(_:)`를 쓴다.
    @Published var dotMarkers: [MapPin] = []

    /// [dotMarkers]를 지금 띄워 둔 화면을 가리키는 표식 — "내가 올린 것만 치우기"를 판단하는 데 쓴다.
    /// 화면이 사라질 때 읽는 값이라 발행(@Published)하지 않는다.
    var dotMarkersOwner: UUID?

    /// 지도 위에 늘 떠 있는 핀 마커들(코스 만들기에서 확정한 출발·도착 등) — [dotMarkers]와 같은
    /// 용도지만 Pin 스타일로 그린다. [pin](선택 중 임시 핀)과도 별개.
    @Published var pinMarkers: [MapPin] = []

    /// 홈 줌인 뷰에서 "안 가본 길" 보기를 고른 동안만 true — 켜면 베이스 지도의 도로를
    /// `map.roadUnexploredFocus` 색으로 덮어 아직 안 가본 길이 어디로 이어지는지 보이게 한다.
    @Published var unexploredRoadFocus: Bool = false

    /// 지도를 탭한 마지막 좌표 — 지도 뷰가 탭마다 채우고, 소비한 화면이 바로 nil로 되돌려
    /// 한 번만 처리되는 이벤트처럼 쓴다.
    @Published var lastMapClick: LatLon?

    /// 0부터 시작 — 이 값이 바뀔 때마다(첫 조립 제외) 현재 위치로 카메라를 다시 옮긴다.
    /// 값 자체는 의미가 없고 변경 이벤트만 쓴다(일종의 one-shot 커맨드).
    @Published private(set) var recenterRequestId: Int = 0

    /// [focusOnBounds]가 마지막으로 넘긴 좌표 — 이 좌표가 전부 보이게 카메라를 맞춘다.
    @Published private(set) var boundsFocusPoints: [LatLon]?

    /// [focusOnBounds]의 extraBottomPadding — 하단을 덮는 바텀시트 등 때문에 화면 아래쪽은 안 보일 때,
    /// 그 높이만큼 아래 여백을 더 줘서 "보이는 영역(위쪽)"에 좌표가 맞춰지게 한다.
    @Published private(set) var boundsFocusExtraBottomPadding: CGFloat = 0

    /// [recenterRequestId]와 같은 one-shot 커맨드 패턴.
    @Published private(set) var boundsFocusRequestId: Int = 0

    /// 하위 화면이 선언한 선 레이어 — 소스 ID → GeoJSON.
    @Published private(set) var layers: [String: String] = [:]

    init(
        initialCenter: LatLon = RunvealMapConfig.seoulCityHall,
        initialZoom: Double = RunvealMapConfig.initialZoom
    ) {
        self.initialCenter = initialCenter
        self.initialZoom = initialZoom
        self.currentZoom = initialZoom
    }

    /// 줌인(러닝 포커스) 상태인지 — 탭바·오버레이 전환 기준.
    var isZoomedIn: Bool { currentZoom >= RunvealMapConfig.zoomInThreshold }

    func setLayer(id: String, geoJson: String) {
        layers[id] = geoJson
    }

    func clearLayer(id: String) {
        layers.removeValue(forKey: id)
    }

    /// 지도 뷰가 카메라 이동이 멈출 때마다 알려준다.
    func updateZoom(_ zoom: Double) {
        guard abs(zoom - currentZoom) > 0.0001 else { return }
        currentZoom = zoom
    }

    /// "내 위치" 버튼 등에서 호출 — 위치 권한/서비스가 꺼져 있으면 지도 뷰가 조용히 무시한다.
    func recenterToCurrentLocation() {
        recenterRequestId += 1
    }

    /// [points]가 전부 화면에 보이게 카메라를 옮긴다. 2개 미만이면 무시한다.
    /// [extraBottomPadding]을 주면 그만큼 아래 여백을 더 둬서, 화면 하단을 덮는 바텀시트 같은
    /// 오버레이가 있어도 코스가 그 위쪽(보이는 영역)에 맞춰지게 한다.
    func focusOnBounds(points: [LatLon], extraBottomPadding: CGFloat = 0) {
        guard points.count >= 2 else { return }
        boundsFocusPoints = points
        boundsFocusExtraBottomPadding = extraBottomPadding
        boundsFocusRequestId += 1
    }
}

/// Map Shell이 지도 위에 떠 있는 하단 패널(탭바 등)에 얹은 높이 — 줌인 오버레이의 하단 액션 줄처럼
/// 그 패널과 겹치지 않게 띄워야 할 때 쓴다. Map Shell 밖에서는 항상 0.
private struct MapOverlayBottomInsetKey: EnvironmentKey {
    static let defaultValue: CGFloat = 0
}

extension EnvironmentValues {
    var runvealMapOverlayBottomInset: CGFloat {
        get { self[MapOverlayBottomInsetKey.self] }
        set { self[MapOverlayBottomInsetKey.self] = newValue }
    }
}

extension View {
    /// 이 화면이 보이는 동안만 [layers]를 지도 상태에 등록하고, 사라지면 해제한다 —
    /// Android `MapLayers(vararg MapLayerSpec)`에 해당한다.
    func runvealMapLayers(_ state: RunvealMapState, _ layers: [String: String]) -> some View {
        onAppear {
            layers.forEach { state.setLayer(id: $0.key, geoJson: $0.value) }
        }
        .onChange(of: layers) { _, newLayers in
            newLayers.forEach { state.setLayer(id: $0.key, geoJson: $0.value) }
        }
        .onDisappear {
            layers.keys.forEach { state.clearLayer(id: $0) }
        }
    }

    /// 이 화면이 떠 있는 동안 [markers]를 지도에 띄우고, 화면을 벗어나면 **자기가 올린 마커일 때만**
    /// 치운다 — Android `MapDotMarkers`에 해당한다.
    ///
    /// 화면 전환은 "다음 화면이 먼저 뜨고, 전환이 끝나면 이전 화면이 사라지는" 순서라, 떠나는 화면이
    /// 무조건 비우면 다음 화면이 올려 둔 마커까지 지워진다. 빈 목록은 "아직 띄울 게 없다"는 뜻으로
    /// 보고 지도를 건드리지 않는다.
    func runvealMapDotMarkers(_ state: RunvealMapState, _ markers: [MapPin], owner: UUID) -> some View {
        onAppear {
            guard !markers.isEmpty else { return }
            state.dotMarkersOwner = owner
            state.dotMarkers = markers
        }
        .onChange(of: markers) { _, newMarkers in
            guard !newMarkers.isEmpty else { return }
            state.dotMarkersOwner = owner
            state.dotMarkers = newMarkers
        }
        .onDisappear {
            guard state.dotMarkersOwner == owner else { return }
            state.dotMarkersOwner = nil
            state.dotMarkers = []
        }
    }
}
