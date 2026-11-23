import CoreLocation
import MapLibre
import SwiftUI
import UIKit

/// 지도 위 현재 위치 마커 — Android `CurrentLocationLayer`가 쓰는 두 벡터
/// (`map_location_dot`, `map_location_bearing`)와 **같은 모양·색·치수**로 맞춘 것.
///
/// MapLibre iOS 기본 마커는 SDK가 정한 파란 점과 큰 반투명 원, 흰 삼각형이라 디자인과 다르다.
/// `mapView(_:viewFor:)`에서 이 뷰를 돌려주면 그 기본 마커를 대신한다.
///
/// 점과 화살표 모두 **벡터를 직접 레이어에 그린다**(이미지로 굽지 않는다) — 화면 배율을 읽어야
/// 하거나 렌더가 실패할 여지를 두지 않으려는 것이다. 화살표 도형은 디자인시스템이 생성한
/// `RunvealIcons.polygon2`(= `design/icons/polygon2.svg`)로, Android 벡터와 같은 원본이다.
@MainActor
final class RunvealUserLocationAnnotationView: MLNUserLocationAnnotationView {
    /// 점 지름 18 — 디자인시스템 `RunvealMapMarker(type: .current)`와 같은 값.
    /// 테두리는 가운데 정렬로 그려져 반지름 8 + 선 2 = 바깥 지름 18이 된다.
    private static let dotDiameter: CGFloat = 18
    private static let dotBorderWidth: CGFloat = 2

    /// 화살표 캔버스 — Android `map_location_bearing.xml`과 같은 56. 회전 중심도 이 한가운데다.
    private static let canvas: CGFloat = 56

    /// 위치 정확도 원의 불투명도 — MapLibre Android `LocationComponentOptions.accuracyAlpha`
    /// 기본값과 같다. Android가 그 기본값을 그대로 쓰고 있어서 숫자를 맞춘다.
    private static let accuracyAlpha: CGFloat = 0.15

    /// 아이콘 안에서 회전 보정의 중심이 되는 점(Android `group`의 pivot).
    private static let arrowPivot = CGPoint(x: 8.91, y: 6.64)
    /// 캔버스 안에서 아이콘을 놓는 자리(Android `group`의 translate).
    private static let arrowOffset = CGPoint(x: 19.09, y: 3.09)
    /// **아이콘이 기울어져 있어서 넣는 보정**(축이 똑바로 위에서 시계방향 6.43°). Android 쪽
    /// TODO대로 똑바로 선 아이콘으로 다시 받으면 양쪽 다 이 값을 지운다.
    private static let arrowSkewCorrection: CGFloat = -6.43 * .pi / 180

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
    private let bearingLayer = CAShapeLayer()
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

        let dotRect = CGRect(x: 0, y: 0, width: Self.dotDiameter, height: Self.dotDiameter)
        // 테두리를 가운데 정렬로 그리려고 선 두께의 절반만큼 안쪽 원을 그린다 — Android 벡터가
        // 반지름 8 원에 2 선을 중앙 정렬로 얹는 것과 같은 결과다.
        let inset = Self.dotBorderWidth / 2
        dotLayer.bounds = dotRect
        dotLayer.path = UIBezierPath(ovalIn: dotRect.insetBy(dx: inset, dy: inset)).cgPath
        dotLayer.fillColor = location.cgColor
        dotLayer.strokeColor = UIColor(colors.icon.primary).cgColor
        dotLayer.lineWidth = Self.dotBorderWidth
        layer.addSublayer(dotLayer)

        setUpBearingLayer()
        layer.addSublayer(bearingLayer)

        setNeedsLayout()
    }

    /// 점·화살표·정확도 원을 **뷰의 실제 한가운데**에 놓는다.
    ///
    /// 지도는 이 뷰의 중심을 현재 위치에 맞춘다. 그래서 좌표를 56 캔버스 기준으로 박아 두면
    /// 지도가 뷰를 다른 크기로 잡는 순간 마커가 실제 위치에서 밀린다 — 크기를 가정하지 않고
    /// 그때그때 `bounds`에서 중심을 읽는다.
    override func layoutSubviews() {
        super.layoutSubviews()
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        withoutAnimation {
            accuracyLayer.position = center
            dotLayer.position = center
            bearingLayer.position = center
        }
    }

    /// 화살표 도형을 캔버스 좌표로 한 번 만들어 둔다. 회전(방위)은 레이어 변환으로 따로 준다.
    private func setUpBearingLayer() {
        bearingLayer.bounds = CGRect(x: 0, y: 0, width: Self.canvas, height: Self.canvas)
        bearingLayer.isHidden = true

        guard let spec = RunvealIcons.polygon2.layers.first,
              case .path(let d) = spec.geometry else { return }

        // Android `group`의 변환 순서를 그대로 옮긴다: pivot을 원점으로 → 기울기 보정 회전 →
        // pivot 복귀 + translate(둘 다 평행이동이라 한 번에 더한다).
        var transform = CGAffineTransform(translationX: -Self.arrowPivot.x, y: -Self.arrowPivot.y)
            .concatenating(CGAffineTransform(rotationAngle: Self.arrowSkewCorrection))
            .concatenating(
                CGAffineTransform(
                    translationX: Self.arrowPivot.x + Self.arrowOffset.x,
                    y: Self.arrowPivot.y + Self.arrowOffset.y
                )
            )
        guard let placed = RunvealSvgPath.path(from: d).cgPath.copy(using: &transform) else { return }

        bearingLayer.path = placed
        bearingLayer.fillColor = spec.fill.map { UIColor($0).cgColor }
        bearingLayer.strokeColor = spec.stroke.map { UIColor($0).cgColor }
        bearingLayer.lineWidth = spec.strokeWidth
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
        accuracyLayer.isHidden = false
        // 위치가 자주 들어와서 애니메이션이 붙으면 원이 계속 울렁인다 — 즉시 반영한다.
        // 원점 기준으로 그리고 자리는 [layoutSubviews]가 정한다.
        withoutAnimation {
            accuracyLayer.path = UIBezierPath(
                arcCenter: .zero,
                radius: radius,
                startAngle: 0,
                endAngle: .pi * 2,
                clockwise: true
            ).cgPath
        }
    }

    /// 화살표는 **기기가 향한 방향**을 가리킨다(Android `RenderMode.COMPASS`와 같다) — 지도를
    /// 돌리면 화면 기준 각도가 달라지므로 지도 방위를 빼서 그린다.
    ///
    /// 나침반 값이 없으면 **이동 방향**(`course`)으로 떨어진다. 나침반 정확도가 "유효하지 않음"일
    /// 때도 각도 자체는 쓸 만한 경우가 많아서, 그 이유만으로 화살표를 지우지는 않는다.
    private func updateBearing(_ userLocation: MLNUserLocation) {
        guard showsBearing, let mapView, let degrees = headingDegrees(userLocation) else {
            bearingLayer.isHidden = true
            return
        }
        bearingLayer.isHidden = false
        withoutAnimation {
            let radians = (mapView.direction - degrees) * .pi / 180
            bearingLayer.setAffineTransform(CGAffineTransform(rotationAngle: -CGFloat(radians)))
        }
    }

    /// 진북 → 자북 → 이동 방향 순으로 쓸 수 있는 값을 고른다. 셋 다 없으면 nil(화살표를 숨긴다).
    private func headingDegrees(_ userLocation: MLNUserLocation) -> CLLocationDirection? {
        if let heading = userLocation.heading {
            if heading.trueHeading >= 0 { return heading.trueHeading }
            if heading.magneticHeading >= 0 { return heading.magneticHeading }
        }
        if let course = userLocation.location?.course, course >= 0 { return course }
        return nil
    }

    /// 레이어 기본 애니메이션을 끄고 바로 반영한다 — 위치·방위가 자주 들어와서 애니메이션이
    /// 겹치면 마커가 계속 흔들린다.
    private func withoutAnimation(_ body: () -> Void) {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        body()
        CATransaction.commit()
    }
}
