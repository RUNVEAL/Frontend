import MapLibre
import SharedLogic
import SwiftUI

/// 지도 위에 떠 있는 마커 하나의 화면 좌표 — SwiftUI로 그리기 위해 지도에서 역산한 값.
struct MapMarkerPlacement: Identifiable {
    let id: UUID
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
        private var style: MLNStyle?

        /// 이미 반영한 one-shot 커맨드 번호 — 같은 요청을 두 번 처리하지 않게.
        private var appliedRecenterRequestId = 0
        private var appliedBoundsFocusRequestId = 0
        private var pendingRecenter = false

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

        // MARK: 카메라

        func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
            state.updateZoom(mapView.zoomLevel)
            clampToKoreaBoundsIfNeeded(mapView)
            if pendingRecenter {
                // 한 번만 따라가고 추적을 끈다 — 이후에는 사용자가 자유롭게 둘러볼 수 있게.
                pendingRecenter = false
                mapView.userTrackingMode = .none
            }
            refreshMarkerPositions()
            scheduleGlowRefresh()
        }

        func mapViewRegionIsChanging(_ mapView: MLNMapView) {
            refreshMarkerPositions()
        }

        /// MapLibre iOS에는 Android의 `setLatLngBoundsForCameraTarget`에 해당하는 API가 없어서,
        /// 카메라가 멈출 때 한국 범위를 벗어났으면 되돌린다.
        private func clampToKoreaBoundsIfNeeded(_ mapView: MLNMapView) {
            let northWest = RunvealMapConfig.koreaBoundsNorthWest
            let southEast = RunvealMapConfig.koreaBoundsSouthEast
            let center = mapView.centerCoordinate
            let clampedLat = min(max(center.latitude, southEast.lat), northWest.lat)
            let clampedLon = min(max(center.longitude, northWest.lon), southEast.lon)
            guard clampedLat != center.latitude || clampedLon != center.longitude else { return }
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
                    mapView.userTrackingMode = .follow
                }
            }

            if state.boundsFocusRequestId != appliedBoundsFocusRequestId {
                appliedBoundsFocusRequestId = state.boundsFocusRequestId
                if state.boundsFocusRequestId > 0, let points = state.boundsFocusPoints, points.count >= 2 {
                    focus(mapView: mapView, points: points, extraBottomPadding: state.boundsFocusExtraBottomPadding)
                }
            }
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
