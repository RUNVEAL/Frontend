import SharedLogic
import SwiftUI

/// 현재 위치 추적 명령 하나 — Android `FollowCurrentLocationCommand`.
///
/// [id]는 같은 내용으로 다시 불렀을 때도("내 위치" 버튼 재탭) 새 명령으로 보이게 하는 구분자다 —
/// 값이 같으면 이미 반영한 명령으로 보고 넘어가 카메라가 복귀하지 않는다.
struct FollowCurrentLocationCommand: Equatable {
    let id: Int
    let isFollowing: Bool
    /// 켜면서 맞출 줌. nil이면 지금 줌을 그대로 둔다.
    let zoom: Double?
    /// 끌 때 카메라 여백까지 되돌릴지.
    let resetAnchor: Bool
}

/// 지도 위에 떠 있는 마커 하나(출발/도착 등 위치 + 종류) — Android `MapPin`.
struct MapPin: Identifiable, Equatable {
    let location: LatLon
    let type: RunvealMapMarkerType

    /// 같은 자리·같은 종류면 **같은 마커**다.
    ///
    /// 만들 때마다 달라지는 값(UUID 등)을 쓰면 안 된다 — 마커 목록은 화면이 다시 그려질 때마다
    /// 새로 만들어지는데, 그걸 `onChange`가 "바뀌었다"로 보고 지도 상태를 또 갱신한다. 그 갱신이
    /// 다시 화면을 그리게 해서 끝없이 돌고, 그동안 그 화면은 탭도 스와이프도 받지 못한다.
    var id: String { "\(type)-\(location.lat)-\(location.lon)" }

    static func == (lhs: MapPin, rhs: MapPin) -> Bool {
        lhs.type == rhs.type
            && lhs.location.lat == rhs.location.lat
            && lhs.location.lon == rhs.location.lon
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

    /// 카메라가 **멈춘** 자리의 중심 좌표. 움직이는 동안에는 갱신하지 않는다 — 매 프레임 바뀌면
    /// 이 값을 보고 서버를 부르는 쪽(홈의 지도 중심 행정동)이 요청을 쏟아낸다.
    @Published private(set) var currentCenter: LatLon

    /// 지금 지도 가운데 보이는 "메인" 행정동 코드 — 그 동 라벨만 크고 굵게 그린다.
    /// nil이면 모든 동이 같은 크기다(아직 못 받았거나 서비스 지역 밖).
    @Published var mainDongCode: String?

    /// [moveTo]가 마지막으로 넘긴 목표 자리와 줌.
    @Published private(set) var moveToCenter: LatLon?
    @Published private(set) var moveToZoom: Double?
    @Published private(set) var moveToRequestId: Int = 0

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

    /// [recenterToCurrentLocation]이 같이 맞출 줌 — nil이면 지금 줌을 그대로 둔다.
    @Published private(set) var recenterZoom: Double?

    /// [focusOnBounds]가 마지막으로 넘긴 좌표 — 이 좌표가 전부 보이게 카메라를 맞춘다.
    @Published private(set) var boundsFocusPoints: [LatLon]?

    /// `focusOnBounds`가 쓸 네 방향 여백. 경로가 가장자리에 딱 붙지 않게 하는 기본 여백에 더해,
    /// **화면 위아래를 덮는 UI(앱바·칩 줄·하단 카드)만큼 더 비워 두는** 데 쓴다 — 안 비우면 경로가
    /// 그 UI 뒤로 숨어서 일부만 보인다.
    @Published private(set) var boundsFocusHorizontalPadding: CGFloat = RunvealMapState.boundsFocusPadding
    @Published private(set) var boundsFocusTopPadding: CGFloat = RunvealMapState.boundsFocusPadding
    @Published private(set) var boundsFocusBottomPadding: CGFloat = RunvealMapState.boundsFocusPadding

    /// [recenterRequestId]와 같은 one-shot 커맨드 패턴.
    @Published private(set) var boundsFocusRequestId: Int = 0

    /// 하위 화면이 선언한 선 레이어 — 소스 ID → GeoJSON.
    @Published private(set) var layers: [String: String] = [:]

    /// 현재 위치 마커에 "지금 바라보는 방향" 화살표를 띄울지(러닝 중). 기기 나침반(지자기 센서)을
    /// 따라 도는 부채꼴이 마커 둘레에 붙는다 — 달리는 방향(GPS 진행 방향)이 아니라 **기기가 향한
    /// 쪽**이다. 홈 지도처럼 둘러보기만 하는 화면에서는 꺼 둔다(가만히 서 있어도 계속 떨린다).
    /// Android `MapState.showsHeadingIndicator`.
    @Published var showsHeadingIndicator = false

    /// 현재 위치 추적 명령 — `startFollowingCurrentLocation`/`stopFollowingCurrentLocation` 호출
    /// 한 번이 하나를 만들고 `RunvealMap`이 적용한다.
    @Published private(set) var followCommand: FollowCurrentLocationCommand?
    private var followCommandCount = 0

    /// 추적 중 지도 아래쪽이 가려지는 높이(러닝 패널 등) — 그만큼 카메라 여백을 줘서 현재 위치가
    /// **가려지지 않고 보이는 영역의 가운데**에 오게 한다([focusOnBounds]의 `bottomPadding`과
    /// 같은 뜻). 추적 on/off와 별개인 상태다.
    @Published var followBottomInset: CGFloat = 0

    init(
        initialCenter: LatLon = RunvealMapConfig.seoulCityHall,
        initialZoom: Double = RunvealMapConfig.initialZoom
    ) {
        self.initialCenter = initialCenter
        self.initialZoom = initialZoom
        self.currentZoom = initialZoom
        self.currentCenter = initialCenter
    }

    /// 줌인(러닝 포커스) 상태인지.
    ///
    /// **지금은 아무도 쓰지 않는다**(v2) — 패널은 줌과 무관하게 줌인 모양 하나로 통일됐고, 지도에
    /// 길이 보이냐 동네가 보이냐는 [homeMapScale]이 정한다. 지우지 않고 두는 건 줌으로 갈리는
    /// 화면이 되살아날 여지가 남아서다.
    var isZoomedIn: Bool { currentZoom >= RunvealMapConfig.zoomInThreshold }

    /**
     홈 지도가 무엇을 보여줄지 — 홈 상단 토글("길 단위" / "동네 단위")이 정한다.

     **줌 레벨과 완전히 무관하다**(v2). 예전에는 줌아웃이면 동·시군구·시도 경계, 줌인이면 가본 길
     선이었는데, 이제 토글만이 그걸 가른다 — 길 단위면 줌아웃에서도 가본 길이 보이고, 동네 단위면
     줌인에서도 동 경계가 보인다.
     */
    @Published var homeMapScale: HomeMapScale = .dong

    func setLayer(id: String, geoJson: String) {
        layers[id] = geoJson
    }

    func clearLayer(id: String) {
        layers.removeValue(forKey: id)
    }

    /// 지도 뷰가 카메라 이동이 멈출 때마다 알려준다.
    /// 지도 뷰가 카메라 이동이 멈출 때마다 알려준다 — 중심이 실제로 달라졌을 때만 발행한다.
    func updateCenter(_ center: LatLon) {
        guard currentCenter.lat != center.lat || currentCenter.lon != center.lon else { return }
        currentCenter = center
    }

    /**
     지정한 자리로 카메라를 옮긴다. `focusOnBounds`와 달리 **줌을 그대로 두거나 직접 정한다** —
     범위를 꽉 채우지 않아서 "그 동이 가운데 오되 줌아웃 상태 유지"가 된다(앱 시작 포커싱).

     - Parameter zoom: nil이면 지금 줌을 유지한다.
     */
    func moveTo(center: LatLon, zoom: Double? = nil) {
        moveToCenter = center
        moveToZoom = zoom
        moveToRequestId += 1
    }

    func updateZoom(_ zoom: Double) {
        guard abs(zoom - currentZoom) > 0.0001 else { return }
        currentZoom = zoom
    }

    /// "내 위치" 버튼 등에서 호출 — 위치 권한/서비스가 꺼져 있으면 지도 뷰가 조용히 무시한다.
    ///
    /// [zoom]을 주면 옮기면서 그 배율로 맞춘다(기록 탭에서 홈 줌인 뷰로 보낼 때 등). nil이면 지금
    /// 줌을 그대로 둔다 — "내 위치" 버튼은 보고 있던 배율을 유지해야 한다.
    func recenterToCurrentLocation(zoom: Double? = nil) {
        recenterZoom = zoom
        recenterRequestId += 1
    }

    /**
     현재 위치를 계속 따라가는 카메라 추적을 켠다(러닝 중 지도). 방위는 북쪽으로 고정이다 —
     달리는 방향으로 지도가 돌면 길을 알아보기 어렵다(Android `CameraMode.TRACKING_GPS_NORTH`).

     지도를 드래그·핀치하면 추적이 자동으로 꺼진다. 지도는 화면보다 오래 살아 있으니, 켠 화면은
     나갈 때 [stopFollowingCurrentLocation]으로 꼭 꺼 준다.

     현재 위치를 둘 자리는 [followBottomInset]이 정한다.

     - Parameter zoom: 켜면서 맞출 줌. nil이면 지금 줌을 그대로 둔다("내 위치" 버튼·러닝 재개).
     */
    func startFollowingCurrentLocation(zoom: Double? = nil) {
        followCommandCount += 1
        followCommand = FollowCurrentLocationCommand(
            id: followCommandCount, isFollowing: true, zoom: zoom, resetAnchor: false
        )
    }

    /**
     추적을 끈다. 카메라는 지금 보이는 자리에 그대로 둔다 — 러닝 일시정지처럼 "따라가기만 멈추는"
     경우다.

     - Parameter resetAnchor: [followBottomInset]으로 준 카메라 여백까지 되돌릴지 — 러닝 화면을
       나갈 때처럼 이 지도를 다른 화면에 돌려줄 때만 true로 둔다(그대로 두면 다음 화면에서도 지도가
       계속 치우쳐 보인다).
     */
    func stopFollowingCurrentLocation(resetAnchor: Bool = false) {
        followCommandCount += 1
        followCommand = FollowCurrentLocationCommand(
            id: followCommandCount, isFollowing: false, zoom: nil, resetAnchor: resetAnchor
        )
    }

    /// 아무것도 안 덮을 때 가장자리에 딱 붙지 않을 만큼의 기본 여백.
    static let boundsFocusPadding: CGFloat = 48

    /// [points]가 전부 화면에 보이게 카메라를 옮긴다. 2개 미만이면 무시한다.
    ///
    /// 여백은 **화면 가장자리에서 얼마나 떨어뜨릴지**다. 지도 위에 앱바·칩 줄·하단 카드가 얹히는
    /// 화면은 그 높이를 더해서 넘겨야 경로가 그 UI 뒤로 숨지 않는다.
    func focusOnBounds(
        points: [LatLon],
        horizontalPadding: CGFloat = RunvealMapState.boundsFocusPadding,
        topPadding: CGFloat = RunvealMapState.boundsFocusPadding,
        bottomPadding: CGFloat = RunvealMapState.boundsFocusPadding
    ) {
        guard points.count >= 2 else { return }
        boundsFocusPoints = points
        boundsFocusHorizontalPadding = horizontalPadding
        boundsFocusTopPadding = topPadding
        boundsFocusBottomPadding = bottomPadding
        boundsFocusRequestId += 1
    }
}

/// 홈 지도가 보여줄 단위 — 홈 상단 토글이 고른다([RunvealMapState.homeMapScale]).
enum HomeMapScale {
    /// 가본 길 선을 보여주고 동·시군구·시도 경계는 숨긴다.
    case road
    /// 동·시군구·시도 경계를 보여주고 가본 길 선은 숨긴다.
    case dong
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
