import SwiftUI

/// 위치 권한 미승인 시 지도 위에 띄우는 바텀시트 — 권한이 왜 필요한지 안내하고
/// '허용하기'를 누르면 시스템 권한 다이얼로그를 띄우는 흐름으로 이어진다.
/// 시트를 띄우고 닫는 건 호출하는 쪽이 맡는다.
struct RunvealLocationPermissionSheet: View {
    private let onAllowClick: () -> Void
    private let onLaterClick: () -> Void

    init(onAllowClick: @escaping () -> Void, onLaterClick: @escaping () -> Void) {
        self.onAllowClick = onAllowClick
        self.onLaterClick = onLaterClick
    }

    var body: some View {
        RunvealBottomSheet(
            header: {
                RunvealBottomSheetHeader(
                    type: .hero,
                    title: RunvealDesignSystemStrings.locationPermissionSheetTitle,
                    description: RunvealDesignSystemStrings.locationPermissionSheetDescription,
                    heroIcon: RunvealIcons.pin
                )
            },
            content: {
                LocationPermissionBenefits(
                    benefits: [
                        RunvealDesignSystemStrings.locationPermissionBenefitExploreRate,
                        RunvealDesignSystemStrings.locationPermissionBenefitRealtimeRoute,
                        RunvealDesignSystemStrings.locationPermissionBenefitBackgroundRecord,
                    ]
                )
            },
            footer: {
                RunvealButtonGroup {
                    RunvealButtonGroupPrimaryText(
                        primaryLabel: RunvealDesignSystemStrings.locationPermissionSheetAllow,
                        onPrimaryClick: onAllowClick,
                        textLabel: RunvealDesignSystemStrings.locationPermissionSheetLater,
                        onTextClick: onLaterClick
                    )
                }
            }
        )
    }
}

private struct LocationPermissionBenefits: View {
    let benefits: [String]

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
            ForEach(benefits, id: \.self) { benefit in
                HStack(spacing: RunvealSpacing.space12) {
                    RunvealShapes.full
                        .fill(colors.icon.tertiary)
                        .frame(width: 8, height: 8)
                    Text(benefit)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(colors.text.secondary)
                }
            }
        }
        .padding(RunvealSpacing.space20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.bg.canvas, in: RunvealShapes.radius16)
    }
}

#Preview {
    RunvealLocationPermissionSheet(onAllowClick: {}, onLaterClick: {})
        .background(RunvealTheme.colors.bg.canvas)
}
