import SwiftUI

/// "MY" 탭 — Android `MyScreen`과 같이 아직 플레이스홀더다.
struct MyScreen: View {
    var body: some View {
        ZStack {
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()
            Text("MY")
                .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                .foregroundStyle(RunvealTheme.colors.text.primary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    MyScreen()
}
