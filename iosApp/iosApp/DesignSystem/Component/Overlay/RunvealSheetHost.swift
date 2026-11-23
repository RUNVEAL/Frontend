import SwiftUI

/// 지도 위에 띄우는 바텀시트 — UIKit `.sheet` 대신 **같은 화면 위에 얹는다**.
///
/// `.sheet`은 별도 프레젠테이션이라 시트가 화면을 크게 차지하면 iOS가 뒤 화면을 그리기를 멈춘다.
/// 지도는 Metal로 그려서 그 순간 **검게** 보인다 — 코스 조건 설정처럼 시트가 높은 화면에서 특히
/// 두드러진다. Android `ModalBottomSheet`는 같은 화면 위에 그려서 뒤 지도가 계속 보이는데,
/// 여기서 그 모양을 맞춘다.
///
/// 입력(키보드)이 있는 시트에는 쓰지 않는다 — 키보드 회피는 UIKit `.sheet` 쪽이 맡는 게 낫다.
extension View {
    func runvealBottomSheet<SheetContent: View>(
        isPresented: Bool,
        onDismiss: @escaping () -> Void,
        @ViewBuilder content: @escaping () -> SheetContent
    ) -> some View {
        modifier(RunvealSheetHostModifier(isPresented: isPresented, onDismiss: onDismiss, sheetContent: content))
    }
}

private struct RunvealSheetHostModifier<SheetContent: View>: ViewModifier {
    let isPresented: Bool
    let onDismiss: () -> Void
    @ViewBuilder let sheetContent: () -> SheetContent

    /// 끌어내린 거리 — 손을 떼면 0으로 돌아가거나, 충분히 내렸으면 닫는다.
    @State private var dragOffset: CGFloat = 0

    /// 이만큼 끌어내리면 닫는다.
    private static var dismissDistance: CGFloat { 120 }

    func body(content: Content) -> some View {
        content.overlay {
            if isPresented {
                ZStack(alignment: .bottom) {
                    RunvealTheme.colors.bg.scrim
                        .ignoresSafeArea()
                        .contentShape(Rectangle())
                        .onTapGesture { dismiss() }

                    sheetContent()
                        // 아래로만 따라 내려간다 — 위로는 끌어올릴 자리가 없다.
                        .offset(y: max(dragOffset, 0))
                        .gesture(dragGesture)
                        .transition(.move(edge: .bottom))
                }
                // 안전 영역은 그대로 둔다 — 시트 내용(버튼)이 홈 인디케이터 위에 남아야 한다.
                // 그 아래 띠는 `RunvealBottomSheet`의 배경이 알아서 채운다.
            }
        }
        .animation(.easeOut(duration: 0.25), value: isPresented)
        // 닫혔다가 다시 열 때 이전에 끌어내린 거리가 남아 있지 않게 한다.
        .onChange(of: isPresented) { _, presented in
            if !presented { dragOffset = 0 }
        }
    }

    private var dragGesture: some Gesture {
        DragGesture()
            .onChanged { gesture in
                dragOffset = gesture.translation.height
            }
            .onEnded { gesture in
                if gesture.translation.height > Self.dismissDistance {
                    dismiss()
                } else {
                    withAnimation(.easeOut(duration: 0.2)) { dragOffset = 0 }
                }
            }
    }

    private func dismiss() {
        dragOffset = 0
        onDismiss()
    }
}
