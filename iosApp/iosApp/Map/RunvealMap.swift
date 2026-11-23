import MapLibre
import SharedLogic
import SwiftUI

/// 지도 위에 떠 있는 마커 하나의 화면 좌표 — SwiftUI로 그리기 위해 지도에서 역산한 값.
struct MapMarkerPlacement: Identifiable, Equatable {
    /// [MapPin.id]를 그대로 쓴다 — 자리·종류로 정해지는 값이라 다시 그려도 같다.
    let id: String
    let type: RunvealMapMarkerType
    let style: RunvealMapMarkerStyle
    let point: CGPoint
}

/// 마커 화면 좌표를 지도 뷰(UIKit) → SwiftUI로 건네는 통로.
@MainActor
final class MapMarkerPositions: ObservableObject {
    @Published var placements: [MapMarkerPlacement] = []
}

/// feature가 아는 것은 이것뿐 — MapLibre SDK 타입은 전부 `Internal`에 숨긴다.
/// Map Shell에서 단 하나만 그리고, 하위 화면은 [RunvealMapState]로 상태를 읽거나
/// `.runvealMapLayers(...)`로 레이어를 선언한다. Android `RunvealMap`에 해당한다.
struct RunvealMap: View {
    @ObservedObject var state: RunvealMapState
    /// 동/시군구/시도 오버레이. nil이면 지역 오버레이를 안 그린다(러닝 중, 코스 추천 중).
    var overlay: RunvealDongOverlayBundle?

    @StateObject private var markerPositions = MapMarkerPositions()

    var body: some View {
        ZStack(alignment: .topLeading) {
            MapLibreMapViewRepresentable(
                state: state,
                overlay: overlay,
                markerPositions: markerPositions
            )

            ForEach(markerPositions.placements) { placement in
                RunvealMapMarker(type: placement.type, style: placement.style)
                    .offset(markerOffset(for: placement))
            }
        }
    }

    /// Pin 디자인의 앵커(바닥 점의 중심)는 맨 아래 8pt 점의 중앙이라 가로는 절반, 세로는
    /// (전체 높이 - 점 반지름)만큼 끌어올려야 탭한 좌표와 점이 겹친다. Dot는 원 하나라 정가운데다.
    private func markerOffset(for placement: MapMarkerPlacement) -> CGSize {
        switch placement.style {
        case .pin:
            // 라벨(약 24) + 선 10 + 점 8 = 42, 폭은 라벨 너비에 따라 달라지지만 중앙 정렬이라
            // 대략값으로 충분하다(Android는 실제 측정 크기를 쓴다).
            let height: CGFloat = 42
            let dotRadius: CGFloat = 4
            return CGSize(width: placement.point.x - 24, height: placement.point.y - height + dotRadius)
        case .dot:
            let half: CGFloat = 9
            return CGSize(width: placement.point.x - half, height: placement.point.y - half)
        }
    }
}

/// MapLibre `MLNMapView`를 SwiftUI에 끼우고, [RunvealMapState]의 변화를 스타일 레이어에 반영한다 —
/// Android `MapLibreMapView`(Composable + AndroidView)에 해당한다.
private struct MapLibreMapViewRepresentable: UIViewRepresentable {
    @ObservedObject var state: RunvealMapState
    var overlay: RunvealDongOverlayBundle?
    var markerPositions: MapMarkerPositions

    func makeCoordinator() -> Coordinator {
        Coordinator(state: state, markerPositions: markerPositions)
    }

    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView(frame: .zero, styleURL: RunvealMapConfig.styleURL)
        mapView.delegate = context.coordinator
        mapView.logoView.isHidden = true
        mapView.attributionButton.isHidden = true
        mapView.compassView.isHidden = true
        mapView.tintColor = RunvealMapColors.default.currentLocation
        mapView.minimumZoomLevel = RunvealMapConfig.minZoomPreference
        mapView.setCenter(state.initialCenter.coordinate, zoomLevel: state.initialZoom, animated: false)

        let tap = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleTap(_:))
        )
        mapView.addGestureRecognizer(tap)
        context.coordinator.mapView = mapView
        return mapView
    }

    func updateUIView(_ mapView: MLNMapView, context: Context) {
        context.coordinator.apply(state: state, overlay: overlay)
    }

    // MARK: - Coordinator

    @MainActor
    final class Coordinator: NSObject, MLNMapViewDelegate {
        private let state: RunvealMapState
        private let markerPositions: MapMarkerPositions
        private let colors = RunvealMapColors.default
        private let roadFocus = BaseStyleRoadFocus()

        private lazy var glowProvider = DongGlowProvider { [weak self] in
            // 빛 이미지가 새로 만들어질 때마다 다시 얹는다 — 캐시가 빈 첫 실행에서 "지도를 한 번
            // 움직여야 빛이 뜨는" 현상을 막는다.
            self?.scheduleGlowRefresh()
        }
        private lazy var glowLayer = DongGlowImageLayer(provider: glowProvider)

        weak var mapView: MLNMapView?
        /// 지금 화면에 떠 있는 현재 위치 마커 — 방향 화살표를 켜고 끄려고 들고 있는다.
        /// 소유자는 지도(annotation view를 재사용·폐기한다)라 약한 참조로 둔다.
        private weak var userLocationView: RunvealUserLocationAnnotationView?
        private var style: MLNStyle?

        /// 이미 반영한 one-shot 커맨드 번호 — 같은 요청을 두 번 처리하지 않게.
        private var appliedRecenterRequestId = 0
        private var appliedBoundsFocusRequestId = 0
        private var pendingRecenter = false
        /// [pendingRecenter] 요청이 같이 맞춰야 할 줌 — nil이면 배율은 그대로 둔다.
        private var pendingRecenterZoom: Double?

        private var appliedFollowCommandId = 0
        /// 우리가 추적을 켠 상태인가 — "한 번 옮기기"([pendingRecenter])와 구분하는 데 쓴다.
        private var isFollowRequested = false
        /// 추적을 켜면서 맞춰야 할 줌 — [pendingRecenterZoom]과 같은 이유로 전환이 끝난 뒤에 건다.
        private var pendingFollowZoom: Double?
        private var appliedFollowBottomInset: CGFloat = 0

        private var lastOverlayBoundarySourceKey: String?
        private var glowRefreshTask: Task<Void, Never>?
        private var currentOverlay: RunvealDongOverlayBundle?
        private var appliedUnexploredRoadFocus = false

        init(state: RunvealMapState, markerPositions: MapMarkerPositions) {
            self.state = state
            self.markerPositions = markerPositions
        }

        deinit {
            glowRefreshTask?.cancel()
        }

        // MARK: 스타일 로드

        func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
            self.style = style
            // 베이스 스타일 레이어 스냅샷이 필요해서 다른 레이어를 추가하기 전에 가장 먼저 호출한다.
            BaseMapDimmer.applyAtStyleLoad(style: style)
            BaseStyleDarkCorrection.applyAtStyleLoad(style: style)
            // 도로 색을 바꾸기 전의 원래 색을 기억해 둬야 "안 가본 길" 토글을 끌 때 되돌릴 수 있다.
            roadFocus.rememberOriginalColors(style: style)
            BaseStyleLabels.hide(style: style)
            mapView.showsUserLocation = true

            apply(state: state, overlay: currentOverlay)
        }

        // MARK: 현재 위치 마커

        /// 현재 위치만 우리 마커로 바꾼다 — 나머지 annotation은 쓰지 않으므로 nil을 돌려줘
        /// SDK 기본값에 맡긴다.
        func mapView(_ mapView: MLNMapView, viewFor annotation: MLNAnnotation) -> MLNAnnotationView? {
            guard annotation is MLNUserLocation else { return nil }
            let view = RunvealUserLocationAnnotationView()
            view.showsBearing = state.showsHeadingIndicator
            userLocationView = view
            return view
        }

        // MARK: 카메라

        func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
            state.updateZoom(mapView.zoomLevel)
            clampToKoreaBoundsIfNeeded(mapView)
            if let zoom = pendingFollowZoom {
                // 추적을 켜는 시점에 같이 주면 전환 애니메이션에 묻힌다 — 아래 한 번 옮기기와 같은 이유로
                // 전환이 끝난 이 시점에 배율만 따로 건다. 중심은 추적이 계속 맞춰 준다.
                pendingFollowZoom = nil
                mapView.setZoomLevel(zoom, animated: true)
            }
            if pendingRecenter {
                // 한 번만 따라가고 추적을 끈다 — 이후에는 사용자가 자유롭게 둘러볼 수 있게.
                // 러닝처럼 계속 따라가야 하는 중이면 끄지 않는다.
                pendingRecenter = false
                if !isFollowRequested { mapView.userTrackingMode = .none }
                // 줌까지 맞춰야 하는 요청이면(기록 탭 → 홈 줌인 뷰) 추적이 현위치로 옮겨 놓은
                // 중심을 그대로 두고 배율만 바꾼다. 추적을 켜는 시점에 같이 주지 않는 이유는
                // Android(`setCameraMode`에 zoom 인자)와 달리 MapLibre iOS에는 그런 묶음 API가
                // 없어서다 — 따로 걸면 전환 애니메이션과 겹쳐 중간 배율에서 멈춘다.
                if let zoom = pendingRecenterZoom {
                    pendingRecenterZoom = nil
                    mapView.setZoomLevel(zoom, animated: true)
                }
            }
            refreshMarkerPositions()
            scheduleGlowRefresh()
        }

        func mapViewRegionIsChanging(_ mapView: MLNMapView) {
            refreshMarkerPositions()
        }

        /// [clampToKoreaBoundsIfNeeded]가 부른 `setCenter` 안인가 — 재진입을 막는다.
        private var isClampingToKorea = false

        /// MapLibre iOS에는 Android의 `setLatLngBoundsForCameraTarget`에 해당하는 API가 없어서,
        /// 카메라가 멈출 때 한국 범위를 벗어났으면 되돌린다.
        ///
        /// **자기가 부른 `setCenter` 안에서는 다시 들어오지 않는다.** MapLibre가 카메라를 옮길 수 없는
        /// 상태(뷰 크기가 아직 0×0, 또는 줌이 낮아 세계 전체가 화면에 들어가 위도가 0에 고정)에서는
        /// `setCenter`가 위치를 못 바꾼 채 `regionDidChangeAnimated`를 **동기로** 다시 부른다. 그러면
        /// 여기서 또 옮기라고 하고, 끝없이 반복돼 스택이 넘쳐 홈 진입 때 앱이 죽었다.
        /// 비교에 오차를 두는 것도 같은 이유다 — 되돌아온 경도가 124.49999999999999처럼 미세하게 달라
        /// 정확히 비교하면 "아직 밖"으로 보고 계속 옮긴다.
        private func clampToKoreaBoundsIfNeeded(_ mapView: MLNMapView) {
            guard !isClampingToKorea else { return }
            let northWest = RunvealMapConfig.koreaBoundsNorthWest
            let southEast = RunvealMapConfig.koreaBoundsSouthEast
            let center = mapView.centerCoordinate
            let clampedLat = min(max(center.latitude, southEast.lat), northWest.lat)
            let clampedLon = min(max(center.longitude, northWest.lon), southEast.lon)
            let epsilon = 1e-6
            guard abs(clampedLat - center.latitude) > epsilon || abs(clampedLon - center.longitude) > epsilon else {
                return
            }
            isClampingToKorea = true
            defer { isClampingToKorea = false }
            mapView.setCenter(
                CLLocationCoordinate2D(latitude: clampedLat, longitude: clampedLon),
                animated: true
            )
        }

        @objc func handleTap(_ recognizer: UITapGestureRecognizer) {
            guard let mapView else { return }
            let point = recognizer.location(in: mapView)
            state.lastMapClick = mapView.convert(point, toCoordinateFrom: mapView).latLon
        }

        // MARK: 상태 반영

        func apply(state: RunvealMapState, overlay: RunvealDongOverlayBundle?) {
            currentOverlay = overlay
            // 방향 화살표는 스타일 레이어가 아니라 지도 뷰의 사용자 위치 표시 옵션이다 — 스타일이
            // 아직 안 올라왔어도 걸어 둘 수 있으므로 아래 guard 앞에서 반영한다.
            // SDK 옵션은 켜 둬야 MapLibre가 나침반 갱신을 시작한다. 실제로 화살표를 그릴지는
            // 우리 마커가 정한다([RunvealUserLocationAnnotationView.showsBearing]).
            mapView?.showsUserHeadingIndicator = state.showsHeadingIndicator
            userLocationView?.showsBearing = state.showsHeadingIndicator
            guard let style else { return }

            applyRoadFocus(style: style, enabled: state.unexploredRoadFocus)
            applyLineLayers(style: style, layers: state.layers)
            applyRegionOverlays(style: style, overlay: overlay)
            applyCommands(state: state)
            refreshMarkerPositions()
            scheduleGlowRefresh()
        }

        private func applyRoadFocus(style: MLNStyle, enabled: Bool) {
            guard enabled != appliedUnexploredRoadFocus else { return }
            appliedUnexploredRoadFocus = enabled
            roadFocus.apply(style: style, focusColor: enabled ? colors.roadUnexploredFocus : nil)
        }

        private func applyLineLayers(style: MLNStyle, layers: [String: String]) {
            VisitedRoadLayer.apply(
                style: style,
                sourceId: RunvealMapConfig.sourceVisitedRoad,
                geoJson: layers[RunvealMapConfig.sourceVisitedRoad],
                coreColor: colors.visitedRoadCore,
                glowColor: colors.visitedRoadGlow
            )
            VisitedRoadLayer.apply(
                style: style,
                sourceId: RunvealMapConfig.sourceLiveRunOld,
                geoJson: layers[RunvealMapConfig.sourceLiveRunOld],
                coreColor: colors.liveRunOld
            )
            VisitedRoadLayer.apply(
                style: style,
                sourceId: RunvealMapConfig.sourceLiveRunNew,
                geoJson: layers[RunvealMapConfig.sourceLiveRunNew],
                coreColor: colors.liveRunNew,
                glowColor: colors.liveRunNewGlow
            )
            RecommendedRouteLayer.apply(
                style: style,
                sourceId: RunvealMapConfig.sourceRecommendedRoute,
                geoJson: layers[RunvealMapConfig.sourceRecommendedRoute],
                colors: colors
            )
            RecommendedRouteLayer.apply(
                style: style,
                sourceId: RunvealMapConfig.sourceCourseDetailRoute,
                geoJson: layers[RunvealMapConfig.sourceCourseDetailRoute],
                colors: colors
            )
            CourseGuideLayer.apply(
                style: style,
                sourceId: RunvealMapConfig.sourceRecordingCourseGuide,
                geoJson: layers[RunvealMapConfig.sourceRecordingCourseGuide],
                colors: colors
            )
        }

        private func applyRegionOverlays(style: MLNStyle, overlay: RunvealDongOverlayBundle?) {
            DongOverlayLayer.apply(style: style, overlay: overlay?.dong, colors: colors)
            SigunguOverlayLayer.apply(style: style, overlay: overlay?.sigungu, colors: colors)
            SidoOverlayLayer.apply(style: style, overlay: overlay?.sido, colors: colors)
        }

        private func applyCommands(state: RunvealMapState) {
            guard let mapView else { return }

            // 0은 초기값(= 아직 요청 안 함) — 값이 바뀔 때만 "내 위치" 요청으로 본다.
            if state.recenterRequestId != appliedRecenterRequestId {
                appliedRecenterRequestId = state.recenterRequestId
                if state.recenterRequestId > 0 {
                    pendingRecenter = true
                    pendingRecenterZoom = state.recenterZoom
                    mapView.userTrackingMode = .follow
                }
            }

            if let command = state.followCommand, command.id != appliedFollowCommandId {
                appliedFollowCommandId = command.id
                applyFollow(command, mapView: mapView)
            }

            // 추적 중에 패널 높이가 바뀌면 카메라 여백만 다시 반영한다.
            if isFollowRequested, state.followBottomInset != appliedFollowBottomInset {
                applyFollowBottomInset(state.followBottomInset, mapView: mapView)
            }

            if state.boundsFocusRequestId != appliedBoundsFocusRequestId {
                appliedBoundsFocusRequestId = state.boundsFocusRequestId
                if state.boundsFocusRequestId > 0, let points = state.boundsFocusPoints, points.count >= 2 {
                    focus(mapView: mapView, points: points, extraBottomPadding: state.boundsFocusExtraBottomPadding)
                }
            }
        }

        /// 현재 위치 추적을 켜고 끈다 — Android `CurrentLocationLayer.startFollowing`/`stopFollowing`.
        ///
        /// `.follow`는 **북쪽 고정**으로 따라간다(`.followWithHeading`은 지도가 같이 돌아서 안 쓴다).
        private func applyFollow(_ command: FollowCurrentLocationCommand, mapView: MLNMapView) {
            isFollowRequested = command.isFollowing
            if command.isFollowing {
                // 한 번 옮기기 요청이 걸려 있었다면 버린다 — 계속 따라가는 쪽이 이긴다.
                pendingRecenter = false
                pendingRecenterZoom = nil
                pendingFollowZoom = command.zoom
                mapView.userTrackingMode = .follow
                applyFollowBottomInset(state.followBottomInset, mapView: mapView)
            } else {
                pendingFollowZoom = nil
                mapView.userTrackingMode = .none
                if command.resetAnchor {
                    applyFollowBottomInset(0, mapView: mapView)
                }
            }
        }

        /// 하단 패널에 가리는 만큼 카메라 아래 여백을 줘서, 현재 위치가 보이는 영역의 가운데에 오게 한다.
        private func applyFollowBottomInset(_ inset: CGFloat, mapView: MLNMapView) {
            appliedFollowBottomInset = inset
            mapView.contentInset = UIEdgeInsets(top: 0, left: 0, bottom: inset, right: 0)
        }

        /// 코스 선이 화면 가장자리에 딱 붙지 않게 주는 여백.
        private static let boundsFocusPadding: CGFloat = 48

        private func focus(mapView: MLNMapView, points: [LatLon], extraBottomPadding: CGFloat) {
            var minLat = points[0].lat
            var maxLat = points[0].lat
            var minLon = points[0].lon
            var maxLon = points[0].lon
            for point in points {
                minLat = min(minLat, point.lat)
                maxLat = max(maxLat, point.lat)
                minLon = min(minLon, point.lon)
                maxLon = max(maxLon, point.lon)
            }
            let bounds = MLNCoordinateBounds(
                sw: CLLocationCoordinate2D(latitude: minLat, longitude: minLon),
                ne: CLLocationCoordinate2D(latitude: maxLat, longitude: maxLon)
            )
            let padding = Self.boundsFocusPadding
            mapView.setVisibleCoordinateBounds(
                bounds,
                edgePadding: UIEdgeInsets(
                    top: padding,
                    left: padding,
                    bottom: padding + extraBottomPadding,
                    right: padding
                ),
                animated: true,
                completionHandler: nil
            )
        }

        // MARK: 마커

        /// 선택 중 임시 핀(Pin), 항상 떠 있는 점 마커(Dot), 항상 떠 있는 핀 마커(Pin)를 하나의 목록으로
        /// 합쳐서 화면 좌표를 계산한다 — 셋 다 결국 좌표 + 종류일 뿐이라 계산이 같다.
        private func refreshMarkerPositions() {
            guard let mapView else { return }
            var placements: [MapMarkerPlacement] = []

            func append(_ pin: MapPin, _ markerStyle: RunvealMapMarkerStyle) {
                let point = mapView.convert(pin.location.coordinate, toPointTo: mapView)
                placements.append(
                    MapMarkerPlacement(id: pin.id, type: pin.type, style: markerStyle, point: point)
                )
            }

            state.dotMarkers.forEach { append($0, .dot) }
            state.pinMarkers.forEach { append($0, .pin) }
            if let pin = state.pin { append(pin, .pin) }

            // **그대로면 발행하지 않는다.** 이 함수는 `apply()` 끝에서 불리는데, `apply()`는
            // `updateUIView`에서 불린다 — 값이 같아도 발행하면 뷰가 다시 그려지고 그게 다시
            // `updateUIView`를 불러서, 아무것도 안 바뀌어도 지도 상태 반영이 끝없이 돈다.
            guard markerPositions.placements != placements else { return }
            markerPositions.placements = placements
        }

        // MARK: 동 빛 이미지

        /// 오버레이(단계 변경)·카메라 멈춤(화면 범위 변경)·생성 완료 중 하나라도 바뀌면 다시 계산한다.
        private func scheduleGlowRefresh() {
            glowRefreshTask?.cancel()
            glowRefreshTask = Task { [weak self] in
                guard let self, let style = self.style, let mapView = self.mapView else { return }
                let overlay = self.currentOverlay
                if self.lastOverlayBoundarySourceKey != overlay?.boundarySourceKey {
                    self.lastOverlayBoundarySourceKey = overlay?.boundarySourceKey
                }
                await self.glowLayer.refresh(
                    style: style,
                    mapView: mapView,
                    query: overlay?.query,
                    boundarySourceKey: overlay?.boundarySourceKey ?? ""
                )
            }
        }
    }
}
