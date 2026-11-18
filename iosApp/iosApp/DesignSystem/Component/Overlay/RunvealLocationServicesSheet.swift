import SwiftUI

/**
 기기의 위치 서비스가 꺼져 있을 때 지도 위에 띄우는 바텀시트 — Android `RunvealLocationServicesSheet`.

 **권한 시트와 다른 상태다.** 앱 권한은 허용돼 있어도 기기 위치 서비스가 꺼져 있으면 좌표를 못 받는다.
 그 경우 앱이 할 수 있는 건 없고 기기 설정으로 보내는 것뿐이라, 문구도 "권한 허용"이 아니라
 "위치 서비스를 켜 주세요"다.
 */
struct RunvealLocationServicesSheet: View {
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
                    title: RunvealDesignSystemStrings.locationServicesSheetTitle,
                    description: RunvealDesignSystemStrings.locationServicesSheetDescription,
                    heroIcon: RunvealIcons.locationOff
                )
            },
            content: {
                EmptyView()
            },
            footer: {
                RunvealButtonGroup {
                    RunvealButtonGroupPrimaryText(
                        primaryLabel: RunvealDesignSystemStrings.locationServicesSheetPrimary,
                        onPrimaryClick: onSettingsClick,
                        textLabel: RunvealDesignSystemStrings.locationServicesSheetSecondary,
                        onTextClick: onCloseClick
                    )
                }
            }
        )
    }
}

#Preview {
    RunvealLocationServicesSheet(onSettingsClick: {}, onCloseClick: {})
        .background(RunvealTheme.colors.bg.canvas)
}
