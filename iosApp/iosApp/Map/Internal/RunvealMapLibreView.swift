import MapLibre
import UIKit

/// 레이아웃이 끝나 **실제 크기가 잡힌 시점**을 알려 주는 `MLNMapView`.
///
/// `UIViewRepresentable`에는 "SwiftUI가 이 뷰를 배치하고 나서"를 받을 훅이 없다. `makeUIView`는
/// 배치 전이고, 첫 `updateUIView`도 보통 배치 전에 온다. 그런데 MapLibre의 카메라 API는 뷰 크기와
/// `contentInset`으로 화면 중심을 역산하므로, 크기가 안전 영역 패딩보다 작은 동안 부르면 계산이
/// 성립하지 않는다. 그래서 `layoutSubviews`를 직접 받아 그 시점을 알린다.
final class RunvealMapLibreView: MLNMapView {
    /// 배치가 끝날 때마다 불린다 — 받는 쪽에서 "한 번만" 처리해야 한다.
    var onLayout: (@MainActor (MLNMapView) -> Void)?

    override func layoutSubviews() {
        super.layoutSubviews()
        onLayout?(self)
    }
}
