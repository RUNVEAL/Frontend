import SwiftUI

/// 화면·시트 하단에 고정되는 버튼 묶음 레이아웃(좌우 margin.screen, 아래 8).
/// 안의 버튼 배치는 [RunvealButtonGroupSingle]/[RunvealButtonGroupStacked]/[RunvealButtonGroupPrimaryText]/
/// [RunvealButtonGroupSecondaryPrimary]/[RunvealButtonGroupEqual] 중 하나를 쓴다.
///
/// Android는 OS 내비게이션 바 인셋을 각 화면에서 직접 피해야 하지만, iOS는 SwiftUI가 하단 safe area를
/// 기본으로 지켜 주므로 여기서 인셋을 따로 더하지 않는다 — `.ignoresSafeArea()`로 safe area를 끈
/// 화면(지도 등)만 `.padding(.bottom, safeAreaInsets.bottom)`을 직접 챙기면 된다.
struct RunvealButtonGroup<Content: View>: View {
    @ViewBuilder private let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        VStack(spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, RunvealLayout.marginScreen)
        .padding(.bottom, RunvealSpacing.space8)
    }
}

/// 주 버튼 하나(Gold).
struct RunvealButtonGroupSingle: View {
    private let label: String
    private let onClick: () -> Void

    init(label: String, onClick: @escaping () -> Void) {
        self.label = label
        self.onClick = onClick
    }

    var body: some View {
        RunvealButton(label: label, style: .gold, fillWidth: true, onClick: onClick)
    }
}

/// 같은 무게의 선택지 세로 나열(로그인 수단 등).
struct RunvealButtonGroupStacked: View {
    private let firstLabel: String
    private let onFirstClick: () -> Void
    private let secondLabel: String
    private let onSecondClick: () -> Void

    init(
        firstLabel: String,
        onFirstClick: @escaping () -> Void,
        secondLabel: String,
        onSecondClick: @escaping () -> Void
    ) {
        self.firstLabel = firstLabel
        self.onFirstClick = onFirstClick
        self.secondLabel = secondLabel
        self.onSecondClick = onSecondClick
    }

    var body: some View {
        VStack(spacing: RunvealSpacing.space8) {
            RunvealButton(label: firstLabel, style: .secondary, fillWidth: true, onClick: onFirstClick)
            RunvealButton(label: secondLabel, style: .secondary, fillWidth: true, onClick: onSecondClick)
        }
    }
}

/// 주 버튼(Gold) + 아래 보조 Text 버튼.
struct RunvealButtonGroupPrimaryText: View {
    private let primaryLabel: String
    private let onPrimaryClick: () -> Void
    private let textLabel: String
    private let onTextClick: () -> Void

    init(
        primaryLabel: String,
        onPrimaryClick: @escaping () -> Void,
        textLabel: String,
        onTextClick: @escaping () -> Void
    ) {
        self.primaryLabel = primaryLabel
        self.onPrimaryClick = onPrimaryClick
        self.textLabel = textLabel
        self.onTextClick = onTextClick
    }

    var body: some View {
        VStack(spacing: RunvealSpacing.space4) {
            RunvealButton(label: primaryLabel, style: .gold, fillWidth: true, onClick: onPrimaryClick)
            RunvealButton(label: textLabel, style: .text, fillWidth: true, onClick: onTextClick)
                .frame(height: RunvealLayout.sizeTouchTargetMin)
        }
    }
}

/// 보조 버튼(내용 너비) + 주 버튼(나머지 채움).
struct RunvealButtonGroupSecondaryPrimary: View {
    private let secondaryLabel: String
    private let onSecondaryClick: () -> Void
    private let primaryLabel: String
    private let onPrimaryClick: () -> Void

    init(
        secondaryLabel: String,
        onSecondaryClick: @escaping () -> Void,
        primaryLabel: String,
        onPrimaryClick: @escaping () -> Void
    ) {
        self.secondaryLabel = secondaryLabel
        self.onSecondaryClick = onSecondaryClick
        self.primaryLabel = primaryLabel
        self.onPrimaryClick = onPrimaryClick
    }

    var body: some View {
        HStack(spacing: RunvealSpacing.space8) {
            RunvealButton(label: secondaryLabel, style: .secondary, onClick: onSecondaryClick)
            RunvealButton(label: primaryLabel, style: .gold, fillWidth: true, onClick: onPrimaryClick)
        }
        .frame(maxWidth: .infinity)
    }
}

/// 같은 너비 둘(보조 1/2, 주 1/2).
struct RunvealButtonGroupEqual: View {
    private let firstLabel: String
    private let onFirstClick: () -> Void
    private let secondLabel: String
    private let onSecondClick: () -> Void

    init(
        firstLabel: String,
        onFirstClick: @escaping () -> Void,
        secondLabel: String,
        onSecondClick: @escaping () -> Void
    ) {
        self.firstLabel = firstLabel
        self.onFirstClick = onFirstClick
        self.secondLabel = secondLabel
        self.onSecondClick = onSecondClick
    }

    var body: some View {
        HStack(spacing: RunvealSpacing.space8) {
            RunvealButton(label: firstLabel, style: .secondary, fillWidth: true, onClick: onFirstClick)
            RunvealButton(label: secondLabel, style: .gold, fillWidth: true, onClick: onSecondClick)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    RunvealButtonGroup {
        RunvealButtonGroupSecondaryPrimary(
            secondaryLabel: "취소",
            onSecondaryClick: {},
            primaryLabel: "확인",
            onPrimaryClick: {}
        )
    }
    .background(RunvealTheme.colors.bg.canvas)
}
