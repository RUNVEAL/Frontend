import SwiftUI

/**
 MY 탭 맨 위의 누적 수치 카드 — Android `ProfileSummaryCard`와 같다.

 TODO(로그인 도입): 로그인 없이 나가는 1차 출시라 보여 줄 닉네임이 없어 줄을 통째로 비웠다.
  로그인을 켤 때 `nickname`을 받아 제목 줄을 되살린다(Android도 같은 자리에 TODO가 있다).
 */
struct ProfileSummaryCard: View {
    let exploredDongCount: Int
    let totalDistanceLabel: String

    var body: some View {
        HStack(alignment: .center, spacing: RunvealSpacing.space16) {
            RunvealStat(
                value: "\(exploredDongCount)",
                label: MyStrings.statExploredDongLabel,
                unit: MyStrings.statExploredDongUnit,
                type: .large,
                accent: true
            )
            .frame(maxWidth: .infinity)

            RunvealStat(
                value: totalDistanceLabel,
                label: MyStrings.statTotalDistanceLabel,
                unit: MyStrings.statTotalDistanceUnit,
                type: .large
            )
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity)
        .padding(RunvealLayout.marginCardPadding)
        .background(Self.cardBackground, in: RunvealShapes.radius16)
    }

    /// 카드 배경(시안 `#FFFAE8` 4%). 디자인 토큰에 이 값에 해당하는 semantic 색이 없어 Android와
    /// 같은 리터럴을 쓴다 — 토큰이 생기면 양쪽을 같이 바꾼다.
    private static let cardBackground = Color(
        red: 1.0,
        green: 250.0 / 255.0,
        blue: 232.0 / 255.0,
        opacity: 0.04
    )
}

#Preview {
    ProfileSummaryCard(exploredDongCount: 3, totalDistanceLabel: "35.10")
        .padding()
        .background(RunvealTheme.colors.bg.canvas)
}
