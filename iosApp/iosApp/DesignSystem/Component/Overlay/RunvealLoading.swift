import Lottie
import SwiftUI

/// 디자이너가 전달한 공용 로딩 모션(`runveal_loading_common.json`, 80x80 기준). 계속 반복
/// 재생만 한다 — 배경 딤 처리나 탭 막기는 호출하는 쪽이 맡는다.
///
/// 원본 JSON은 Android `res/raw/runveal_loading_common.json`이고, 빌드할 때 iOS 번들로 복사된다
/// (gradle/runveal-bundle-assets-ios.gradle.kts).
struct RunvealLoading: View {
    /// JSON 원본 크기(80x80)와 맞춘 기본 크기 — 호출하는 쪽이 따로 안 주면 이 크기로 보인다.
    private static let defaultSize: CGFloat = 80

    private let size: CGFloat

    init(size: CGFloat = RunvealLoading.defaultSize) {
        self.size = size
    }

    var body: some View {
        LottieView(animation: .named(RunvealBundleAsset.loadingMotion.resource))
            .playing(loopMode: .loop)
            .frame(width: size, height: size)
    }
}

#Preview {
    RunvealLoading()
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
