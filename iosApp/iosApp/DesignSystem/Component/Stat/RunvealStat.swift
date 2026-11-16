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

    init(
        value: String,
        label: String? = nil,
        unit: String? = nil,
        type: RunvealStatType = .large,
        accent: Bool = false
    ) {
        self.value = value
        self.label = label
        self.unit = unit
        self.type = type
        self.accent = accent
    }

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
                }
                StatValue(
                    value: value,
                    unit: unit,
                    numberStyle: RunvealTheme.typography.numS,
                    unitStyle: RunvealTheme.typography.unitS,
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
            Text(value).runvealTextStyle(numberStyle).foregroundStyle(numberColor)
            if let unit {
                Text(unit).runvealTextStyle(unitStyle).foregroundStyle(unitColor)
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
