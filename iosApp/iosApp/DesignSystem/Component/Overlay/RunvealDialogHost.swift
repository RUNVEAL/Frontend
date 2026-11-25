import SwiftUI

/// 다이얼로그를 띄우는 자리 — 뒤를 덮는 스크림과 "바깥을 눌러 닫기"를 맡는다.
///
/// [RunvealDialog]는 카드 모양만 그리는 순수 컴포넌트라 스크림이 없다. Android는 Compose의
/// `Dialog`가 그 둘(어두운 뒤 배경, 바깥·뒤로가기로 닫기)을 공짜로 주는데, SwiftUI의 `.overlay`
/// 에는 그게 없어서 여기서 같은 동작을 맞춰 준다.
struct RunvealDialogHostModifier<Dialog: View>: ViewModifier {
    let isPresented: Bool
    /// 바깥을 눌렀을 때 — Android `onDismissRequest`에 해당한다.
    let onDismiss: () -> Void
    let dialog: () -> Dialog

    func body(content: Content) -> some View {
        content.overlay {
            if isPresented {
                ZStack {
                    RunvealTheme.colors.bg.scrim
                        .ignoresSafeArea()
                        .contentShape(Rectangle())
                        .onTapGesture(perform: onDismiss)
                    dialog()
                }
            }
        }
    }
}

extension View {
    /// [RunvealDialog]를 스크림과 함께 띄운다.
    func runvealDialog<Dialog: View>(
        isPresented: Bool,
        onDismiss: @escaping () -> Void,
        @ViewBuilder dialog: @escaping () -> Dialog
    ) -> some View {
        modifier(RunvealDialogHostModifier(isPresented: isPresented, onDismiss: onDismiss, dialog: dialog))
    }
}
