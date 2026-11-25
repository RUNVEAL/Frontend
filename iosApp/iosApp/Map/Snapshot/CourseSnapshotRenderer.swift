import MapLibre
import SharedLogic
import UIKit

/// 코스/기록 썸네일, 러닝 결과 카드용 지도 스냅샷 이미지(`docs/running/COURSE_MAP_SNAPSHOT.md`) —
/// Android `CourseSnapshotRenderer`에 해당한다.
///
/// **Android와 구현이 다른 지점**: Android는 `MapSnapshotter.Options.withStyleBuilder(...)`로 스냅샷
/// 스타일에 경로 레이어를 끼워 넣지만, iOS `MLNMapSnapshotter`에는 스타일에 소스·레이어를 주입하는
/// API가 없다. 그래서 베이스 지도만 스냅샷으로 받고, 경로선과 시작·끝 점은 `MLNMapSnapshot.point(for:)`로
/// 좌표를 화면 점으로 바꿔 Core Graphics로 직접 그린다. 선 두께·색은 Android `SnapshotStyleBuilder`와
/// 같은 값을 쓴다.
@MainActor
final class CourseSnapshotRenderer {
    static let shared = CourseSnapshotRenderer()

    private static let routeWidth: CGFloat = 3
    /// 러닝 결과 카드의 출발·도착 점 — 카드 크기가 고정이라 상수로 둔다.
    private static let endpointRadius: CGFloat = 5
    private static let endpointStrokeWidth: CGFloat = 2

    /// 코스 썸네일의 출발·도착 점 반지름 = 그림 높이 x 이 비율. 디자인시스템 마커(지름 18) 크기로
    /// 그려 봤더니 썸네일 안에서는 너무 작아 보여서 **지름 기준 2.5배**로 키웠다 — 160 카드에서
    /// 반지름 17.5(지름 35). Android `ENDPOINT_RADIUS_RATIO`와 같은 값이다.
    private static let thumbnailEndpointRadiusRatio: CGFloat = 17.5 / 160
    /// 아주 작은 썸네일에서도 점이 보이게, 큰 그림에서 과하게 커지지 않게 두는 한계.
    private static let thumbnailEndpointRadiusRange: ClosedRange<CGFloat> = 11.25...20
    /// 테두리는 반지름에 비례한다 — 점을 키워도 실처럼 얇아지지 않게(Android `START_STROKE_RATIO`).
    private static let thumbnailEndpointStrokeRatio: CGFloat = 2.0 / 7.0
    private static let timeout: TimeInterval = 15

    /// 스냅샷 하단에 합성돼 오는 **출처 표기 띠의 높이**(pt). 넉넉하게 잡는다.
    ///
    /// MapLibre iOS `MLNMapSnapshotter`에는 Android의 `withLogo(false)`·`withAttribution(false)`에
    /// 해당하는 옵션이 없다. 그래서 이 높이만큼 **위아래로 더 찍고 잘라 낸다** — 같은 양을 위아래에서
    /// 버려야 경로가 가운데에 그대로 남는다. 출처 표기 의무는 앱의 지도 화면에 상시 떠 있는 표기로
    /// 지킨다(Android도 썸네일에서는 끈다).
    private static let attributionInset: CGFloat = 22

    /// 같은 캐시 키에 요청이 겹치면 하나만 렌더링하고 나머지는 그 결과를 같이 기다린다.
    private var inFlight: [String: Task<URL?, Never>] = [:]

    private init() {}

    /// 캐시가 있으면 바로 반환하고, 없으면 렌더링해서 저장한 뒤 파일 URL을 반환한다. 실패하면 nil.
    func imageURL(request: SnapshotRequest, widthPx: Int, heightPx: Int) async -> URL? {
        if let cached = cachedURL(request: request, widthPx: widthPx, heightPx: heightPx) { return cached }

        let key = CourseSnapshotDiskCache.fileName(
            request: request,
            widthPx: widthPx,
            heightPx: heightPx,
            pixelRatio: Self.pixelRatio
        )
        if let running = inFlight[key] { return await running.value }

        let task = Task<URL?, Never> { [weak self] in
            guard let self else { return nil }
            defer { self.inFlight.removeValue(forKey: key) }
            guard let image = await self.snapshot(request: request, widthPx: widthPx, heightPx: heightPx) else {
                return nil
            }
            return CourseSnapshotDiskCache.write(
                request: request,
                image: image,
                widthPx: widthPx,
                heightPx: heightPx,
                pixelRatio: Self.pixelRatio
            )
        }
        inFlight[key] = task
        return await task.value
    }

    /// 렌더링 없이 캐시만 조회한다.
    func cachedURL(request: SnapshotRequest, widthPx: Int, heightPx: Int) -> URL? {
        CourseSnapshotDiskCache.existingFile(
            request: request,
            widthPx: widthPx,
            heightPx: heightPx,
            pixelRatio: Self.pixelRatio
        )
    }

    /// 기록이나 코스가 삭제됐을 때 호출 — 그 소유자의 모든 variant/크기 캐시를 지운다.
    func delete(ownerId: String) {
        CourseSnapshotDiskCache.deleteAll(ownerId: ownerId)
    }

    private static var pixelRatio: CGFloat { UIScreen.main.scale }

    // MARK: - 렌더링

    private func snapshot(request: SnapshotRequest, widthPx: Int, heightPx: Int) async -> UIImage? {
        guard widthPx > 0, heightPx > 0, let styleURL = RunvealMapConfig.styleURL else { return nil }
        let scale = Self.pixelRatio
        let size = CGSize(width: CGFloat(widthPx) / scale, height: CGFloat(heightPx) / scale)
        // 출처 표기를 잘라 내려고 위아래로 더 찍는다. 지도 내용이 들어갈 자리는 그만큼 세로로 넓어진다.
        let renderSize = CGSize(width: size.width, height: size.height + Self.attributionInset * 2)
        let renderHeightPx = Int(renderSize.height * scale)

        let bounds = MLNCoordinateBounds(
            sw: CLLocationCoordinate2D(latitude: request.bounds.south, longitude: request.bounds.west),
            ne: CLLocationCoordinate2D(latitude: request.bounds.north, longitude: request.bounds.east)
        )
        let options = MLNMapSnapshotOptions(styleURL: styleURL, camera: MLNMapCamera(), size: renderSize)
        options.scale = scale

        // `coordinateBounds`에는 최대 줌 제한이 없다 — 계산된 줌이 MAX_ZOOM을 넘으면 중심점 + 고정 줌으로
        // 바꿔 지정한다(Android `applyRegion`과 같은 판단).
        let fitZoom = IosSnapshot.shared.zoomToFit(
            bounds: request.bounds,
            widthPx: Int32(widthPx),
            heightPx: Int32(renderHeightPx)
        )
        if fitZoom <= SnapshotConfig.shared.MAX_ZOOM {
            options.coordinateBounds = bounds
        } else {
            let center = CLLocationCoordinate2D(
                latitude: (request.bounds.south + request.bounds.north) / 2,
                longitude: (request.bounds.west + request.bounds.east) / 2
            )
            options.camera = MLNMapCamera(lookingAtCenter: center, altitude: 0, pitch: 0, heading: 0)
            options.zoomLevel = SnapshotConfig.shared.MAX_ZOOM
        }

        let snapshotter = MLNMapSnapshotter(options: options)
        let snapshot: MLNMapSnapshot? = await withCheckedContinuation { continuation in
            var finished = false
            snapshotter.start { snapshot, _ in
                guard !finished else { return }
                finished = true
                continuation.resume(returning: snapshot)
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + Self.timeout) {
                guard !finished else { return }
                finished = true
                snapshotter.cancel()
                continuation.resume(returning: nil)
            }
        }
        guard let snapshot else { return nil }
        return draw(request: request, on: snapshot, size: size, renderSize: renderSize, scale: scale)
    }

    /// 베이스 스냅샷 위에 경로선 → (처음 간 길) 하이라이트 → (결과 카드면) 시작·끝 점을 그린다.
    ///
    /// 러닝 기록은 "오늘 달린 길"(흰색)로 깔고 그 위에 "새로 탐험한 길"(금색)을 덧그려 둘을 구분한다 —
    /// 결과 화면 범례가 그 두 색으로 설명하고 있어서 기본선도 범례와 같아야 한다. 아직 달리지 않은
    /// 코스는 그 구분이 없어 경로색(금색) 한 가지로 그린다.
    private func draw(
        request: SnapshotRequest,
        on snapshot: MLNMapSnapshot,
        size: CGSize,
        renderSize: CGSize,
        scale: CGFloat
    ) -> UIImage {
        let colors = RunvealTheme.colors
        let baseColor = UIColor(request.isRunRoute ? colors.map.roadToday : colors.map.roadExplored)
        let highlightColor = UIColor(colors.map.roadNew)
        let endpointColor = UIColor(colors.map.location)
        let endpointStrokeColor = UIColor(colors.text.primary)

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = scale
        let renderer = UIGraphicsImageRenderer(size: size, format: format)

        return renderer.image { context in
            let cgContext = context.cgContext
            // 위로 밀어 올려서 위아래 띠를 같이 버린다 — 아래 띠에 출처 표기가 들어 있다.
            // 좌표 변환(`snapshot.point(for:)`)은 찍은 크기 기준이라, 이 이동을 먼저 걸어 두면
            // 배경과 경로·점이 한꺼번에 같은 만큼 따라간다.
            cgContext.translateBy(x: 0, y: -Self.attributionInset)
            snapshot.image.draw(in: CGRect(origin: .zero, size: renderSize))

            func stroke(_ lines: [[LatLon]], color: UIColor) {
                cgContext.setStrokeColor(color.cgColor)
                cgContext.setLineWidth(Self.routeWidth)
                cgContext.setLineCap(.round)
                cgContext.setLineJoin(.round)
                for line in lines where line.count >= 2 {
                    cgContext.beginPath()
                    for (index, point) in line.enumerated() {
                        let screenPoint = snapshot.point(for: point.coordinate)
                        if index == 0 {
                            cgContext.move(to: screenPoint)
                        } else {
                            cgContext.addLine(to: screenPoint)
                        }
                    }
                    cgContext.strokePath()
                }
            }

            stroke(SnapshotGeoJson.lines(from: request.routeGeoJson), color: baseColor)
            if let highlight = request.highlightGeoJson {
                stroke(SnapshotGeoJson.lines(from: highlight), color: highlightColor)
            }

            func fillCircle(at coordinate: LatLon, radius: CGFloat, strokeWidth: CGFloat,
                            color: UIColor, strokeColor: UIColor) {
                let center = snapshot.point(for: coordinate.coordinate)
                let rect = CGRect(
                    x: center.x - radius,
                    y: center.y - radius,
                    width: radius * 2,
                    height: radius * 2
                )
                cgContext.setFillColor(color.cgColor)
                cgContext.setStrokeColor(strokeColor.cgColor)
                cgContext.setLineWidth(strokeWidth)
                cgContext.fillEllipse(in: rect)
                cgContext.strokeEllipse(in: rect)
            }

            if request.variant == .resultCard {
                // 결과 카드는 두 점이 같은 색이다 — 코스 썸네일과 색 체계가 다르다.
                for coordinate in [request.start, request.end].compactMap({ $0 }) {
                    fillCircle(
                        at: coordinate,
                        radius: Self.endpointRadius,
                        strokeWidth: Self.endpointStrokeWidth,
                        color: endpointColor,
                        strokeColor: endpointStrokeColor
                    )
                }
            } else {
                // 코스 썸네일은 디자인시스템 마커와 같은 색 — 출발은 금색, 도착은 흰색(둘 다 어두운
                // 테두리). 순환 코스면 도착 점이 아예 안 실려 온다(`toSnapshotRequest`).
                let scaled = size.height * Self.thumbnailEndpointRadiusRatio
                let radius = min(
                    max(scaled, Self.thumbnailEndpointRadiusRange.lowerBound),
                    Self.thumbnailEndpointRadiusRange.upperBound
                )
                let strokeWidth = radius * Self.thumbnailEndpointStrokeRatio
                let markerStrokeColor = UIColor(colors.icon.onAccent)

                // 도착을 먼저 깔아 두 점이 가까울 때 출발이 위에 오게 한다 — 출발이 더 중요하다.
                if let end = request.end {
                    fillCircle(
                        at: end,
                        radius: radius,
                        strokeWidth: strokeWidth,
                        color: UIColor(colors.icon.primary),
                        strokeColor: markerStrokeColor
                    )
                }
                if let start = request.start {
                    fillCircle(
                        at: start,
                        radius: radius,
                        strokeWidth: strokeWidth,
                        color: UIColor(colors.accent.glow),
                        strokeColor: markerStrokeColor
                    )
                }
            }
        }
    }
}

/// 스냅샷 요청의 경로 GeoJSON(`LineString`/`MultiLineString`/`FeatureCollection`)에서 좌표 줄만 뽑는다.
///
/// Android는 GeoJSON을 지도 소스에 그대로 넘기지만, iOS는 직접 그려야 해서 좌표가 필요하다 —
/// `SnapshotRequest`는 두 플랫폼이 같이 쓰는 타입이라 좌표 배열을 따로 담지 않는다.
enum SnapshotGeoJson {
    static func lines(from geoJson: String) -> [[LatLon]] {
        guard let data = geoJson.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        return lines(fromGeoJsonObject: root)
    }

    private static func lines(fromGeoJsonObject object: [String: Any]) -> [[LatLon]] {
        switch object["type"] as? String {
        case "FeatureCollection":
            let features = object["features"] as? [[String: Any]] ?? []
            return features.flatMap { lines(fromGeoJsonObject: $0) }
        case "Feature":
            guard let geometry = object["geometry"] as? [String: Any] else { return [] }
            return lines(fromGeoJsonObject: geometry)
        case "LineString":
            guard let coordinates = object["coordinates"] as? [[Double]] else { return [] }
            return [points(coordinates)]
        case "MultiLineString":
            guard let coordinates = object["coordinates"] as? [[[Double]]] else { return [] }
            return coordinates.map(points)
        default:
            return []
        }
    }

    private static func points(_ coordinates: [[Double]]) -> [LatLon] {
        coordinates.compactMap { pair in
            guard pair.count >= 2 else { return nil }
            return LatLon(lat: pair[1], lon: pair[0])
        }
    }
}
