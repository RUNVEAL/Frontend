import SwiftUI

/// `.sheet`으로 띄우는 바텀시트 내용을 **아래에 붙인다**.
///
/// [RunvealBottomSheet]는 내용만큼만 높이를 갖는데, `.sheet`의 컨테이너는 detent가 정한 높이(또는
/// 화면 전체)를 그대로 제안한다. SwiftUI는 제안보다 작은 뷰를 **가운데**에 놓으므로, 그대로 두면
/// 시트 카드가 바닥에서 떠 보인다 — detent 높이를 내용에 딱 맞추지 않는 한 늘 조금씩 뜬다.
///
/// 배경을 `.presentationBackground(.clear)`로 비운 구성이라 위쪽 남는 자리는 그대로 비쳐 보이고,
/// 카드만 바닥에 붙는다(Android `ModalBottomSheet`와 같은 모양).
extension View {
    func runvealSheetContent() -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            self
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
