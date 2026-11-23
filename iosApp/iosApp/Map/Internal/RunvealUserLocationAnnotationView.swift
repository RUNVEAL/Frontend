import CoreLocation
import MapLibre
import SwiftUI
import UIKit

/// 지도 위 현재 위치 마커 — Android `CurrentLocationLayer`가 쓰는 두 벡터
/// (`map_location_dot`, `map_location_bearing`)와 **같은 모양·색·치수**로 맞춘 것.
///
/// MapLibre iOS 기본 마커는 SDK가 정한 파란 점과 큰 반투명 원, 흰 삼각형이라 디자인과 다르다.
/// `mapView(_:viewFor:)`에서 이 뷰를 돌려주면 그 기본 마커를 대신한다.
@MainActor
final class RunvealUserLocationAnnotationView: MLNUserLocationAnnotationView {
    /// 점 지름 18 — 디자인시스템 `RunvealMapMarker(type: .current)`와 같은 값.
    /// 테두리는 가운데 정렬로 그려져 반지름 8 + 선 2 = 바깥 지름 18이 된다.
    private static let dotDiameter: CGFloat = 18
    private static let dotBorderWidth: CGFloat = 2

    /// 방향 화살표 캔버스 — Android `map_location_bearing.xml`과 같은 56.
    private static let canvas: CGFloat = 56

    /// 위치 정확도 원의 불투명도 — MapLibre Android `LocationComponentOptions.accuracyAlpha`
    /// 기본값과 같다. Android가 그 기본값을 그대로 쓰고 있어서 숫자를 맞춘다.
    private static let accuracyAlpha: CGFloat = 0.15

    /// 화살표 이미지는 매번 그릴 필요가 없어 한 번 만들어 둔다(해상도 배율별로).
    private static var bearingImages: [CGFloat: UIImage] = [:]

    /// 방향 화살표를 보일지 — `RunvealMapState.showsHeadingIndicator`가 정한다(러닝 중에만 켠다).
    ///
    /// SDK의 `showsUserHeadingIndicator`는 **기본 마커**가 쓰는 옵션이라, 마커를 우리 것으로
    /// 바꾼 뒤로는 그 값에 기대지 않고 여기서 직접 정한다. 다만 그 옵션은 계속 켜 둔다 —
    /// 그래야 MapLibre가 나침반 갱신을 시작해서 `userLocation.heading`이 들어온다.
    var showsBearing = false {
        didSet {
            guard showsBearing != oldValue, let userLocation else { return }
            updateBearing(userLocation)
        }
    }

    private let accuracyLayer = CAShapeLayer()
    private let dotLayer = CAShapeLayer()
    private let bearingView = UIImageView()
    private var didSetUpLayers = false

    override func update() {
        // 처음 한 번은 크기만 잡고 돌아간다 — 레이아웃 뒤에 다시 불린다(SDK 예제와 같은 순서).
        if frame.isNull {
            frame = CGRect(x: 0, y: 0, width: Self.canvas, height: Self.canvas)
            return setNeedsLayout()
        }
        guard let userLocation, CLLocationCoordinate2DIsValid(userLocation.coordinate) else { return }

        setUpLayersIfNeeded()
        updateAccuracy(userLocation)
        updateBearing(userLocation)
    }

    private func setUpLayersIfNeeded() {
        guard !didSetUpLayers else { return }
        didSetUpLayers = true

        // 정확도 원은 점보다 훨씬 커질 수 있다 — 뷰 밖으로 그려져야 하므로 자르지 않는다.
        clipsToBounds = false
        isUserInteractionEnabled = false

        let colors = RunvealTheme.colors
        let location = UIColor(colors.map.location)

        accuracyLayer.fillColor = location.withAlphaComponent(Self.accuracyAlpha).cgColor
        accuracyLayer.strokeColor = UIColor.clear.cgColor
        layer.addSublayer(accuracyLayer)

        let dotRect = CGRect(
            x: (Self.canvas - Self.dotDiameter) / 2,
            y: (Self.canvas - Self.dotDiameter) / 2,
            width: Self.dotDiameter,
            height: Self.dotDiameter
        )
        // 테두리를 가운데 정렬로 그리려고 선 두께의 절반만큼 안쪽 원을 그린다 — Android 벡터가
        // 반지름 8 원에 2 선을 중앙 정렬로 얹는 것과 같은 결과다.
        let inset = Self.dotBorderWidth / 2
        dotLayer.path = UIBezierPath(ovalIn: dotRect.insetBy(dx: inset, dy: inset)).cgPath
        dotLayer.fillColor = location.cgColor
        dotLayer.strokeColor = UIColor(colors.icon.primary).cgColor
        dotLayer.lineWidth = Self.dotBorderWidth
        layer.addSublayer(dotLayer)

        bearingView.frame = bounds
        addSubview(bearingView)
    }

    /// 정확도 원의 반지름은 "미터 → 화면 pt"로 환산해 정한다 — 줌이 바뀌면 같이 커지고 작아진다.
    private func updateAccuracy(_ userLocation: MLNUserLocation) {
        guard let mapView,
              let accuracyMeters = userLocation.location?.horizontalAccuracy,
              accuracyMeters > 0 else {
            accuracyLayer.isHidden = true
            return
        }
        let metersPerPoint = mapView.metersPerPoint(atLatitude: userLocation.coordinate.latitude)
        guard metersPerPoint > 0 else {
            accuracyLayer.isHidden = true
            return
        }
        let radius = CGFloat(accuracyMeters / metersPerPoint)
        let center = CGPoint(x: Self.canvas / 2, y: Self.canvas / 2)
        accuracyLayer.isHidden = false
        // 위치가 자주 들어와서 애니메이션이 붙으면 원이 계속 울렁인다 — 즉시 반영한다.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        accuracyLayer.path = UIBezierPath(
            arcCenter: center,
            radius: radius,
            startAngle: 0,
            endAngle: .pi * 2,
            clockwise: true
        ).cgPath
        CATransaction.commit()
    }

    /// 화살표는 **기기가 향한 방향**을 가리킨다(Android `RenderMode.COMPASS`와 같다) — 지도를
    /// 돌리면 화면 기준 각도가 달라지므로 지도 방위를 빼서 그린다.
    private func updateBearing(_ userLocation: MLNUserLocation) {
        guard showsBearing, let mapView,
              let heading = userLocation.heading,
              heading.headingAccuracy >= 0 else {
            bearingView.isHidden = true
            return
        }
        // 이미지는 여기서 챙긴다 — 뷰가 window에 붙기 전에는 화면 배율을 못 읽어 만들기에
        // 실패할 수 있고, 한 번 실패한 채로 두면 화살표가 영영 안 뜬다.
        if bearingView.image == nil {
            bearingView.image = Self.bearingImage(scale: displayScale)
        }
        let trueHeading = heading.trueHeading >= 0 ? heading.trueHeading : heading.magneticHeading
        bearingView.isHidden = false
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        let radians = (mapView.direction - trueHeading) * .pi / 180
        bearingView.transform = CGAffineTransform(rotationAngle: -CGFloat(radians))
        CATransaction.commit()
    }

    /// 화면 배율. 뷰가 아직 화면에 붙기 전이면 `traitCollection`이 0을 주는데, 그 값으로 그리면
    /// 빈 이미지가 나온다 — 그럴 땐 현재 trait, 그것도 없으면 3배를 쓴다(가장 촘촘한 쪽으로 그려
    /// 두면 어느 기기에서도 뭉개지지 않는다).
    private var displayScale: CGFloat {
        for candidate in [traitCollection.displayScale, UITraitCollection.current.displayScale] where candidate > 0 {
            return candidate
        }
        return 3
    }

    private static func bearingImage(scale: CGFloat) -> UIImage? {
        if let cached = bearingImages[scale] { return cached }
        let renderer = ImageRenderer(content: BearingArrow())
        renderer.scale = scale
        guard let image = renderer.uiImage else { return nil }
        bearingImages[scale] = image
        return image
    }
}

/// Android `map_location_bearing.xml`을 그대로 옮긴 것 — 56 캔버스 위 같은 자리에 같은 각도로
/// 놓인 `polygon2` 아이콘.
///
/// 회전·이동 값은 **아이콘이 기울어져 있어서 넣은 보정값**이다(축이 똑바로 위에서 시계방향 6.43°).
/// Android 쪽 TODO대로 똑바로 선 아이콘으로 다시 받으면 양쪽 다 보정을 지운다.
private struct BearingArrow: View {
    /// 아이콘 안에서 회전의 중심이 되는 점(Android `group`의 pivot).
    private static let pivot = UnitPoint(x: 8.91 / 24, y: 6.64 / 24)

    var body: some View {
        RunvealIcon(RunvealIcons.polygon2, size: RunvealIconSpec.viewBox)
            .rotationEffect(.degrees(-6.43), anchor: Self.pivot)
            .frame(width: RunvealIconSpec.viewBox, height: RunvealIconSpec.viewBox, alignment: .topLeading)
            .offset(x: 19.09, y: 3.09)
            .frame(width: 56, height: 56, alignment: .topLeading)
    }
}
