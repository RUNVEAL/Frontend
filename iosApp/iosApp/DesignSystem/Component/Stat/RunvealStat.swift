import SwiftUI

/// 러닝 결과·코스 수치. Display=기록 상세 맨 위 큰 거리, Large=결과 화면, Small=코스 카드 안 작은 수치,
/// Highlight=결과 카드의 새로 탐험한 길 한 줄.
enum RunvealStatType { case display, large, small, highlight }

/// 시간처럼 단위가 없는 값은 [unit]을 생략, 라벨이 없으면 [label]을 생략(Display는 라벨이 아예 없다).
struct RunvealStat: View {
    private let value: String
    private let label: String?
    private let unit: String?
    private let type: RunvealStatType
    /// true면 Small/Large에서도 숫자+단위를 text/accent로 강조한다(코스 카드의 새 길 수치 등).
    private let accent: Bool
    /// true면 Small에서 숫자+단위만 한 단계 작게 쓴다 — 라벨은 그대로다. 코스 카드가 수치 네 개를
    /// 다 넣기에 좁을 때 카드가 쓴다.
    private let compactValue: Bool

    init(
        value: String,
        label: String? = nil,
        unit: String? = nil,
        type: RunvealStatType = .large,
        accent: Bool = false,
        compactValue: Bool = false
    ) {
        self.value = value
        self.label = label
        self.unit = unit
        self.type = type
        self.accent = accent
        self.compactValue = compactValue
    }

    /// num/s를 한 단계 줄인 값 스타일 — 12/14, 자간 -1%. 디자인 가이드의 "축소 폰트"로, 토큰에는
    /// 대응하는 스타일이 없어서(num/s가 가장 작다) 여기서 만든다.
    private static let compactNum = RunvealTextStyle(
        font: .custom("Geist-Light", size: 12),
        lineHeight: 14,
        tracking: -0.12
    )
    /// unit/s를 한 단계 줄인 단위 스타일 — 10/12. `compactNum`과 짝이다.
    private static let compactUnit = RunvealTextStyle(
        font: .custom("Geist-Light", size: 10),
        lineHeight: 12,
        tracking: 0
    )

    var body: some View {
        let colors = RunvealTheme.colors
        switch type {
        case .display:
            StatValue(
                value: value,
                unit: unit,
                numberStyle: RunvealTheme.typography.numDisplay,
                unitStyle: RunvealTheme.typography.unitDisplay,
                numberColor: colors.text.primary,
                unitColor: colors.text.secondary,
                gap: RunvealSpacing.space4
            )

        case .large:
            VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                if let label {
                    Text(label)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(colors.text.secondary)
                }
                StatValue(
                    value: value,
                    unit: unit,
                    numberStyle: RunvealTheme.typography.numM,
                    unitStyle: RunvealTheme.typography.unitM,
                    numberColor: accent ? colors.text.accent : colors.text.primary,
                    unitColor: accent ? colors.text.accent : colors.text.secondary,
                    gap: RunvealSpacing.space2
                )
            }

        case .small:
            VStack(alignment: .leading, spacing: RunvealSpacing.space2) {
                if let label {
                    Text(label)
                        .runvealTextStyle(RunvealTheme.typography.captionSRegular)
                        .foregroundStyle(colors.text.secondary)
                        .lineLimit(1)
                        .fixedSize(horizontal: true, vertical: false)
                }
                StatValue(
                    value: value,
                    unit: unit,
                    numberStyle: compactValue ? Self.compactNum : RunvealTheme.typography.numS,
                    unitStyle: compactValue ? Self.compactUnit : RunvealTheme.typography.unitS,
                    numberColor: accent ? colors.text.accent : colors.text.primary,
                    unitColor: accent ? colors.text.accent : colors.text.secondary,
                    gap: 0
                )
            }

        case .highlight:
            HStack {
                if let label {
                    Text(label)
                        .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                        .foregroundStyle(colors.text.accent)
                }
                Spacer()
                StatValue(
                    value: value,
                    unit: unit,
                    numberStyle: RunvealTheme.typography.numL,
                    unitStyle: RunvealTheme.typography.unitL,
                    numberColor: colors.text.accent,
                    unitColor: colors.text.accent,
                    gap: RunvealSpacing.space2
                )
            }
            .frame(maxWidth: .infinity)
        }
    }
}

/// 숫자 + 단위. 단위는 숫자와 **베이스라인**을 맞춘다(Android `alignByBaseline`).
struct StatValue: View {
    let value: String
    let unit: String?
    let numberStyle: RunvealTextStyle
    let unitStyle: RunvealTextStyle
    let numberColor: Color
    let unitColor: Color
    let gap: CGFloat

    var body: some View {
        HStack(alignment: .lastTextBaseline, spacing: gap) {
            Text(value)
                .runvealTextStyle(numberStyle)
                .foregroundStyle(numberColor)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
            if let unit {
                Text(unit)
                    .runvealTextStyle(unitStyle)
                    .foregroundStyle(unitColor)
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
            }
        }
    }
}

#Preview {
    VStack(alignment: .leading, spacing: 16) {
        RunvealStat(value: "5.21", unit: "km", type: .display)
        RunvealStat(value: "5.21", label: "거리", unit: "km")
        RunvealStat(value: "5.21", unit: "km", type: .small)
        RunvealStat(value: "+1.3", label: "새로 탐험한 길", unit: "km", type: .highlight)
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
