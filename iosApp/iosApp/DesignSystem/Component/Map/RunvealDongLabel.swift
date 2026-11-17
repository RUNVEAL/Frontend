import SwiftUI

/// 지도 동 라벨. 동 면 중앙에 배치. 터치는 라벨이 아닌 동 면(path) 전체가 받는다.
enum RunvealDongLabelType { case mine, other }

struct RunvealDongLabel: View {
    private let name: String
    private let ratePercent: Int?
    private let type: RunvealDongLabelType

    init(name: String, ratePercent: Int? = nil, type: RunvealDongLabelType = .other) {
        self.name = name
        self.ratePercent = ratePercent
        self.type = type
    }

    var body: some View {
        let colors = RunvealTheme.colors
        switch type {
        case .mine:
            VStack(spacing: RunvealSpacing.space2) {
                HStack(spacing: RunvealSpacing.space4) {
                    Text(name)
                        .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                        .foregroundStyle(colors.text.primary)
                    Text(RunvealDesignSystemStrings.dongMineTag)
                        .runvealTextStyle(RunvealTheme.typography.captionSRegular)
                        .foregroundStyle(colors.text.tertiary)
                }
                if let ratePercent {
                    StatValue(
                        value: "\(ratePercent)",
                        unit: RunvealDesignSystemStrings.percentUnit,
                        numberStyle: RunvealTheme.typography.numL,
                        unitStyle: RunvealTheme.typography.unitL,
                        numberColor: colors.text.primary,
                        unitColor: colors.text.secondary,
                        gap: 0
                    )
                }
            }

        case .other:
            VStack(spacing: 0) {
                Text(name)
                    .runvealTextStyle(RunvealTheme.typography.captionSRegular)
                    .foregroundStyle(colors.text.secondary)
                if let ratePercent {
                    StatValue(
                        value: "\(ratePercent)",
                        unit: RunvealDesignSystemStrings.percentUnit,
                        numberStyle: RunvealTheme.typography.numS,
                        unitStyle: RunvealTheme.typography.unitS,
                        numberColor: colors.text.primary,
                        unitColor: colors.text.secondary,
                        gap: 0
                    )
                }
            }
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        RunvealDongLabel(name: "성수동", ratePercent: 37, type: .mine)
        RunvealDongLabel(name: "역삼동")
    }
    .padding()
    .background(RunvealTheme.colors.bg.canvas)
}
