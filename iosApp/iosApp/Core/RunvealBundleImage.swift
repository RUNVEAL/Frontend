import SwiftUI
import UIKit

/// 번들에 들어 있는 래스터 이미지(`design/images/*`)를 SwiftUI `Image`로 만든다 —
/// 자동 생성된 `RunvealImages`가 이 함수만 부른다.
///
/// **`Image("splash")`로는 안 뜬다.** SwiftUI의 `Image(_:bundle:)`은 **에셋 카탈로그만** 들여다봐서,
/// 카탈로그 밖에 낱개 파일로 들어 있는 PNG는 못 찾고 아무것도 그리지 않는다 — 오류도, 경고도 없이
/// 그 자리가 빈 칸으로 남는다. 이 이미지들은 Gradle이 design/images에서 번들로 복사해 둔 낱개
/// 파일이라(gradle/runveal-images-swift.gradle.kts) UIKit 쪽 조회를 거쳐야 한다.
///
/// 아이콘(`RunvealIcons`)은 SVG를 Swift 코드(`RunvealIconSpec`)로 바꿔 두므로 이 경로를 타지 않는다.
enum RunvealBundleImage {

    /// `UIImage(named:)`는 에셋 카탈로그를 먼저 보고, 없으면 번들의 낱개 파일을 찾는다 — SwiftUI
    /// `Image(_:)`와 달리 둘 다 된다. 결과를 UIKit이 캐시하므로 뷰 body가 다시 평가될 때마다
    /// 디코딩하지 않는다.
    static func image(_ name: String, _ fileExtension: String) -> Image {
        if let loaded = UIImage(named: name) {
            return Image(uiImage: loaded)
        }
        // Xcode가 번들 루트에 평평하게 복사하지 않은 경우(하위 폴더로 들어간 경우)의 대비책.
        if let url = Bundle.main.url(forResource: name, withExtension: fileExtension),
           let loaded = UIImage(contentsOfFile: url.path) {
            return Image(uiImage: loaded)
        }
        // 빈 이미지를 돌려준다 — 여기서 멈추면 이미지 하나 때문에 화면 전체가 안 뜬다.
        print("[RunvealBundleImage] 번들에 \(name).\(fileExtension)이 없다 — Gradle generateRunvealIosResources가 돌았는지 확인한다.")
        return Image(uiImage: UIImage())
    }
}
