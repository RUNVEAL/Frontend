import SwiftUI

/// 위치 권한을 거부해 시스템 권한 다이얼로그를 더 띄울 수 없을 때 지도 위에 띄우는 바텀시트 —
/// 시스템 다이얼로그 대신 앱 설정 화면으로 보낸다.
struct RunvealLocationPermissionSettingsSheet: View {
    private let onSettingsClick: () -> Void
    private let onCloseClick: () -> Void

    init(onSettingsClick: @escaping () -> Void, onCloseClick: @escaping () -> Void) {
        self.onSettingsClick = onSettingsClick
        self.onCloseClick = onCloseClick
    }

    var body: some View {
        RunvealBottomSheet(
            header: {
                RunvealBottomSheetHeader(
                    type: .hero,
                    title: RunvealDesignSystemStrings.locationPermissionSettingsSheetTitle,
                    description: RunvealDesignSystemStrings.locationPermissionSettingsSheetDescription,
                    heroIcon: RunvealIcons.locationOff
                )
            },
            content: {
                EmptyView()
            },
            footer: {
                RunvealButtonGroup {
                    RunvealButtonGroupPrimaryText(
                        primaryLabel: RunvealDesignSystemStrings.locationPermissionSettingsSheetPrimary,
                        onPrimaryClick: onSettingsClick,
                        textLabel: RunvealDesignSystemStrings.locationPermissionSettingsSheetSecondary,
                        onTextClick: onCloseClick
                    )
                }
            }
        )
    }
}

#Preview {
    RunvealLocationPermissionSettingsSheet(onSettingsClick: {}, onCloseClick: {})
        .background(RunvealTheme.colors.bg.canvas)
}
