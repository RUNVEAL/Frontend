import SharedLogic
import SwiftUI

/**
 달린 경로를 **지도 타일 없이 선으로만** 그린다. 저장 이미지([ResultShareImage])가 쓴다.

 타일을 깔지 않는 이유: 저장은 네트워크가 없을 때도 돼야 하고, 타일을 쓰면 저작권 표기가 따라붙는다.
 어차피 이 자리에서 알아야 하는 건 "어떤 모양으로 달렸나"지 주변에 뭐가 있나가 아니다.

 화면에 보이는 결과 카드(`ResultScreen`의 `RouteMapCard`)는 그대로 지도 스냅샷을 쓴다 — 거기서는
 주변 지형이 보이는 편이 낫다.

 [newRoadPaths]는 [routePaths] 위에 덧그린다. 러닝 중 화면·결과 카드와 같은 순서라 "오늘 달린 길
 위에 처음 간 구간이 금색으로 얹히는" 모양이 같다.

 Android `RouteVectorMap`과 같은 규칙이다.
 */
struct RouteVectorMap: View {
    let routePaths: [[LatLon]]
    let newRoadPaths: [[LatLon]]

    /// 선 굵기·점 크기는 pt다 — 내보낼 때 `ImageRenderer`가 배율을 곱하므로 여기서 px를 셀 필요가 없다.
    private static let routeStroke: CGFloat = 4
    private static let endpointRadius: CGFloat = 6
    private static let endpointStroke: CGFloat = 2

    var body: some View {
        let colors = RunvealTheme.colors
        let todayColor = colors.map.roadToday
        let newColor = colors.map.roadNew
        let startColor = colors.map.location
        // 도착 점은 gold200 — 시맨틱 이름이 `roadExplored`지만 값이 시안의 도착 점 색과 같다.
        let endColor = colors.map.roadExplored
        let endpointStrokeColor = colors.text.primary

        Canvas { context, size in
            guard let projection = RouteProjection(paths: routePaths + newRoadPaths, size: size) else { return }

            for path in routePaths {
                Self.drawRoute(path, projection: projection, color: todayColor, in: &context)
            }
            for path in newRoadPaths {
                Self.drawRoute(path, projection: projection, color: newColor, in: &context)
            }

            // 출발은 파랑, 도착은 금색 — 둘 다 흰 테두리를 둘러 경로 선 위에서 구분된다.
            if let start = routePaths.first?.first {
                Self.drawEndpoint(
                    at: projection.point(start), fill: startColor, stroke: endpointStrokeColor, in: &context
                )
            }
            if let end = routePaths.last?.last {
                Self.drawEndpoint(
                    at: projection.point(end), fill: endColor, stroke: endpointStrokeColor, in: &context
                )
            }
        }
    }

    private static func drawRoute(
        _ points: [LatLon],
        projection: RouteProjection,
        color: Color,
        in context: inout GraphicsContext
    ) {
        guard !points.isEmpty else { return }
        // 점이 하나뿐인 조각은 선이 안 되지만 그 자리에 점은 찍어 준다 — 아예 빠지면 경로가 끊겨 보인다.
        if points.count == 1 {
            let center = projection.point(points[0])
            context.fill(circle(at: center, radius: routeStroke / 2), with: .color(color))
            return
        }
        var path = Path()
        for (index, point) in points.enumerated() {
            let offset = projection.point(point)
            if index == 0 { path.move(to: offset) } else { path.addLine(to: offset) }
        }
        context.stroke(
            path,
            with: .color(color),
            style: StrokeStyle(lineWidth: routeStroke, lineCap: .round, lineJoin: .round)
        )
    }

    private static func drawEndpoint(
        at center: CGPoint,
        fill: Color,
        stroke: Color,
        in context: inout GraphicsContext
    ) {
        context.fill(circle(at: center, radius: endpointRadius + endpointStroke), with: .color(stroke))
        context.fill(circle(at: center, radius: endpointRadius), with: .color(fill))
    }

    private static func circle(at center: CGPoint, radius: CGFloat) -> Path {
        Path(ellipseIn: CGRect(
            x: center.x - radius,
            y: center.y - radius,
            width: radius * 2,
            height: radius * 2
        ))
    }
}

/**
 위경도를 그림 안의 좌표로 옮기는 셈. 경로 전체가 들어가도록 **가로세로 같은 배율**로 줄이고
 가운데에 놓는다 — 배율을 따로 주면 경로 모양이 찌그러진다.

 경도는 위도에 따라 실제 거리가 달라지므로(고위도일수록 좁다) `lonScale`로 보정한다. 한 번의 러닝이
 걸치는 범위에서는 이 정도면 충분하고, 제대로 된 메르카토르 투영까지 갈 필요가 없다.
 */
private struct RouteProjection {
    private let minLat: Double
    private let minLon: Double
    private let lonScale: Double
    private let scale: Double
    private let offsetX: Double
    private let offsetY: Double
    private let height: Double

    /// 그림 짧은 변의 이 비율만큼 사방을 비운다 — 경로가 가장자리에 붙어 잘려 보이지 않게.
    private static let paddingRatio: Double = 0.08
    /// 경도 보정 하한 — 극지방에서 0으로 나누는 걸 막는다(실제로 그 위도에서 달릴 일은 없다).
    private static let minLonScale: Double = 0.01

    /// 점이 하나도 없으면 nil — 그릴 게 없다는 뜻이라 호출부가 캔버스를 비워 둔다.
    init?(paths: [[LatLon]], size: CGSize) {
        let points = paths.flatMap { $0 }
        guard !points.isEmpty, size.width > 0, size.height > 0 else { return nil }

        let lats = points.map(\.lat)
        let lons = points.map(\.lon)
        let minLat = lats.min() ?? 0
        let maxLat = lats.max() ?? 0
        let minLon = lons.min() ?? 0
        let maxLon = lons.max() ?? 0
        let lonScale = max(cos((minLat + maxLat) / 2 * .pi / 180), Self.minLonScale)

        // 선 굵기와 출발·도착 점이 가장자리에서 잘리지 않게 안쪽으로 물린다.
        let padding = Self.paddingRatio * Double(min(size.width, size.height))
        let usableWidth = max(Double(size.width) - padding * 2, 1)
        let usableHeight = max(Double(size.height) - padding * 2, 1)

        let spanX = (maxLon - minLon) * lonScale
        let spanY = maxLat - minLat
        // 제자리에서 끝난 러닝처럼 범위가 0이면 배율을 못 구한다 — 가운데 한 점으로 그린다.
        let scale: Double
        if spanX <= 0, spanY <= 0 {
            scale = 0
        } else {
            scale = min(
                spanX > 0 ? usableWidth / spanX : .greatestFiniteMagnitude,
                spanY > 0 ? usableHeight / spanY : .greatestFiniteMagnitude
            )
        }

        self.minLat = minLat
        self.minLon = minLon
        self.lonScale = lonScale
        self.scale = scale
        self.offsetX = padding + (usableWidth - spanX * scale) / 2
        self.offsetY = padding + (usableHeight - spanY * scale) / 2
        self.height = Double(size.height)
    }

    func point(_ coordinate: LatLon) -> CGPoint {
        let x = (coordinate.lon - minLon) * lonScale * scale + offsetX
        // 위도는 위로 갈수록 커지는데 화면 y는 아래로 갈수록 커진다 — 뒤집는다.
        let y = height - ((coordinate.lat - minLat) * scale + offsetY)
        return CGPoint(x: x, y: y)
    }
}
