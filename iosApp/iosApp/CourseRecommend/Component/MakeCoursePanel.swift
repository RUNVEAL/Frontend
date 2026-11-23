import SwiftUI

/// 홈 하단에 뜨는 코스 만들기 패널 — 출발/도착 설정, 거리·조건 칩, 경로 추천 받기 버튼.
struct MakeCoursePanel: View {
    let startLocation: String
    let endLocation: String
    let distanceLabel: String
    let avoidVisitedPreferred: Bool
    var endLocationIsDefault = true
    var selectedFilterCount = 0
    var recommendEnabled = true
    let onStartLocationClick: () -> Void
    let onEndLocationClick: () -> Void
    let onDistanceClick: () -> Void
    let onAvoidVisitedPreferredChange: (Bool) -> Void
    let onFilterClick: () -> Void
    let onRecommendClick: () -> Void
    /// 주면 패널 우측 상단 바로 위에 "내 위치" 아이콘 버튼이 떠 있는 형태로 추가된다(Android와 같다).
    /// 기본값이 있는 프로퍼티는 **맨 뒤**에 둔다 — memberwise init 인자 순서가 선언 순서라서다.
    var onMyLocationClick: (() -> Void)?

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: RunvealSpacing.space16) {
            VStack(spacing: 0) {
                RouteSettingRow(
                    label: CourseRecommendStrings.startLabel,
                    location: startLocation,
                    locationColor: colors.text.primary,
                    onClick: onStartLocationClick
                )
                Rectangle()
                    .fill(colors.border.default)
                    .frame(height: 1)
                RouteSettingRow(
                    label: CourseRecommendStrings.endLabel,
                    location: endLocation,
                    locationColor: endLocationIsDefault ? colors.text.secondary : colors.text.primary,
                    onClick: onEndLocationClick
                )
            }
            // 10에 맞는 spacing 토큰이 없어 그대로 하드코딩(Android와 같다).
            .padding(.horizontal, 10)
            .frame(maxWidth: .infinity)

            HStack(spacing: RunvealSpacing.space8) {
                RunvealChipDropdown(label: distanceLabel, onClick: onDistanceClick)
                RunvealChipCondition(
                    checked: avoidVisitedPreferred,
                    label: CourseRecommendStrings.avoidVisitedPreferred,
                    onCheckedChange: onAvoidVisitedPreferredChange
                )
                RunvealChipCondition(
                    checked: selectedFilterCount > 0,
                    label: selectedFilterCount > 0 ? "\(selectedFilterCount)" : nil,
                    icon: .filter
                ) { _ in
                    onFilterClick()
                }
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 10)
            .frame(maxWidth: .infinity)

            RunvealButton(
                label: CourseRecommendStrings.recommendAction,
                style: .gold,
                size: .l,
                enabled: recommendEnabled,
                fillWidth: true,
                onClick: onRecommendClick
            )
        }
        .padding(.leading, 6)
        .padding(.top, RunvealSpacing.space16)
        .padding(.trailing, 6)
        .padding(.bottom, 6)
        .frame(maxWidth: .infinity)
        .background(colors.surface.default, in: RunvealShapes.radius32)
        .overlay { RunvealShapes.radius32.stroke(colors.border.default, lineWidth: 1) }
        .clipShape(RunvealShapes.radius32)
        .overlay(alignment: .topTrailing) {
            if let onMyLocationClick {
                RunvealIconButton(
                    icon: RunvealIcons.locate,
                    accessibilityLabel: CourseRecommendStrings.myLocation,
                    onClick: onMyLocationClick
                )
                .offset(y: -(RunvealLayout.sizeIconButtonL + RunvealSpacing.space12))
            }
        }
    }
}

private struct RouteSettingRow: View {
    let label: String
    let location: String
    let locationColor: Color
    let onClick: () -> Void

    var body: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            HStack(spacing: RunvealSpacing.space8) {
                Text(label)
                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                    .foregroundStyle(colors.text.tertiary)
                    .lineLimit(1)
                Text(location)
                    .runvealTextStyle(RunvealTheme.typography.labelButton)
                    .foregroundStyle(locationColor)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
                RunvealIcon(RunvealIcons.chevron, tint: colors.icon.tertiary, size: 16)
            }
            .padding(.horizontal, RunvealSpacing.space4)
            .padding(.vertical, RunvealSpacing.space16)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// 출발/도착 위치 선택 모드일 때 지도 위에 뜨는 하단 시트 — 선택한 지점 정보 + 확정 버튼.
///
/// [onMyLocationClick]을 주면 시트 우측 상단 바로 위에 "내 위치" 아이콘 버튼이 떠 있는 형태로
/// 추가된다(Android와 같은 자리 — 시트는 좌우 여백 없이 가장자리에 붙으므로 화면 여백만큼 띄운다).
struct LocationConfirmPanel: View {
    let placeName: String
    let distanceLabel: String
    let confirmLabel: String
    let onConfirmClick: () -> Void
    var onMyLocationClick: (() -> Void)?

    var body: some View {
        sheet.overlay(alignment: .topTrailing) {
            if let onMyLocationClick {
                RunvealIconButton(
                    icon: RunvealIcons.locate,
                    accessibilityLabel: CourseRecommendStrings.myLocation,
                    onClick: onMyLocationClick
                )
                .padding(.trailing, RunvealLayout.marginScreen)
                .offset(y: -(RunvealLayout.sizeIconButtonL + RunvealSpacing.space12))
            }
        }
    }

    private var sheet: some View {
        RunvealBottomSheet(
            header: {
                RunvealBottomSheetHeader(type: .title, title: placeName, subtitle: distanceLabel)
            },
            content: {
                EmptyView()
            },
            footer: {
                RunvealButtonGroup {
                    RunvealButtonGroupSingle(label: confirmLabel, onClick: onConfirmClick)
                }
            }
        )
    }
}

/// 지도 위에 뜨는 짧은 안내 배너 (출발/도착 선택 중 "지도를 탭해 … 선택하세요" 등).
struct MapHintBanner: View {
    let message: String

    var body: some View {
        let colors = RunvealTheme.colors
        Text(message)
            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
            .foregroundStyle(colors.text.primary)
            .multilineTextAlignment(.center)
            .padding(.horizontal, RunvealSpacing.space16)
            .padding(.vertical, RunvealSpacing.space12)
            .background(colors.bg.scrim, in: RunvealShapes.radius16)
            .overlay { RunvealShapes.radius16.stroke(colors.border.subtle, lineWidth: 1) }
    }
}
