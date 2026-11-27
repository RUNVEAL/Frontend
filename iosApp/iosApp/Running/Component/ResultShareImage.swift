import SharedLogic
import SwiftUI
import UIKit

/**
 러닝 결과를 **한 장의 이미지로 저장**할 때 쓰는 템플릿. 화면에 보여주는 결과 카드와 달리 크기가
 고정이다 — 9:16 비율의 360x640pt 아트보드이고, 저장할 때 3배(1080x1920px)로 그린다.

 **화면용 `ResultScreen`과 따로 두는 이유**: 저장 이미지는 기기 화면 크기·밀도와 무관하게 항상 같은
 그림이어야 한다. 화면 뷰를 그대로 캡처하면 기기마다 줄바꿈·잘림이 달라지고, 스크롤로 가려진
 부분은 아예 안 찍힌다.

 경로는 지도 타일 없이 **벡터로 직접** 그린다([RouteVectorMap]) — 타일을 깔면 저장 시점의 네트워크
 상태에 따라 그림이 달라지고, 지도 저작권 표기까지 따라붙는다.

 치수는 디자인 시안 값을 그대로 쓴다. 고정 아트보드라 spacing 토큰으로 바꾸면 오히려 시안과
 어긋난다 — 색과 타이포만 토큰을 쓴다. Android `ResultShareImage`와 같은 구성이다.
 */
struct ResultShareImage: View {
    let state: ResultReady
    /**
     미리 그려 둔 지도 그림(경로·새 길까지 들어 있다). 저장 직전에 렌더링해 넘긴다 — 캡처는 한
     프레임에 끝나서, 여기서 비동기로 불러오면 빈 자리로 찍힌다.

     nil이면 지도 없이 [RouteVectorMap]으로 선만 그린다. 타일을 못 받았을 때(오프라인 등) 네모만
     비는 것보다는 경로 모양이라도 남는 편이 낫다.
     */
    var mapImage: UIImage?

    /// 아트보드 크기(9:16) — 저장할 때 [scale]배로 그린다.
    static let imageWidth: CGFloat = 360
    static let imageHeight: CGFloat = 640
    /// 저장 배율. 360x640pt를 3배로 그려 1080x1920px로 내보낸다.
    static let scale: CGFloat = 3

    /// 저장 이미지에 깔 지도 그림의 크기(pt) — 시안의 경로 자리 값이다.
    ///
    /// 템플릿의 실제 자리는 남는 높이를 받아 가변이라 이 값과 딱 맞지는 않는다. 그래서 그릴 때
    /// 채우기(fill)로 깐다 — 스냅샷은 경로 둘레에 여백을 두고 그려서, 조금 잘려도 경로 자체는 남는다.
    static let mapWidth: CGFloat = 312
    static let mapHeight: CGFloat = 255

    /// 로고 원본이 366x60px이라 그 비율을 유지한다. 높이만 정하고 너비는 비율에서 따라오게 둬서,
    /// 원본이 바뀌어도 찌그러지지 않는다.
    private static let logoHeight: CGFloat = 15
    private static let logoAspectRatio: CGFloat = 366.0 / 60.0

    private static let heroWidth: CGFloat = 480
    private static let heroHeight: CGFloat = 136

    /// 좌우 여백 24 — 본문 너비가 312가 되어 시안과 맞는다(360 - 24*2).
    private static let sideMargin: CGFloat = 24
    private static let contentWidth: CGFloat = 312

    var body: some View {
        let colors = RunvealTheme.colors

        ZStack(alignment: .top) {
            colors.bg.canvas

            // 기록 탭 상단과 같은 히어로 이미지. 아트보드(360)보다 넓은 480이라 좌우가 잘린다 —
            // 시안 그대로다.
            RunvealImages.myCourse
                .resizable()
                .scaledToFill()
                .frame(width: Self.heroWidth, height: Self.heroHeight)
                .clipped()

            // 맨 윗줄 — 왼쪽 로고, 오른쪽 날짜. 따로 배치하지 않고 한 줄에 넣는 이유: 글자와 이미지는
            // 높이가 달라서 각자 top 여백으로 맞추면 눈높이가 어긋난다.
            HStack(alignment: .center, spacing: 0) {
                RunvealImages.runvealLogo
                    .resizable()
                    .scaledToFit()
                    .frame(width: Self.logoHeight * Self.logoAspectRatio, height: Self.logoHeight)

                Spacer(minLength: 0)

                // 저장한 날이 아니라 **달린 날**이다([ResultReady.shareDateLabel]).
                Text(state.shareDateLabel)
                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                    .foregroundStyle(colors.text.secondary)
            }
            .padding(.top, Self.sideMargin)
            .padding(.horizontal, Self.sideMargin)

            VStack(alignment: .leading, spacing: Self.sideMargin) {
                // **경로 그림이 남는 높이를 흡수한다.** 시안 값으로 고정하면 글자 쪽이 조금만 커져도
                // (글꼴 메트릭, 동 이름 줄바꿈) 아래 통계가 아트보드 밖으로 밀려 잘린다 — Android에서
                // 실제로 잘렸다. 글자는 필요한 만큼 가져가고 남는 걸 그림이 받는 쪽이 안전하다.
                mapArea
                    .frame(width: Self.contentWidth, maxHeight: .infinity)

                ExplorationSummary(newRoadLabel: state.newRoadLabel)

                Information(state: state)
            }
            .padding(.horizontal, Self.sideMargin)
            .padding(.top, Self.heroHeight)
            .padding(.bottom, Self.sideMargin)
            .frame(width: Self.imageWidth, height: Self.imageHeight, alignment: .top)
        }
        .frame(width: Self.imageWidth, height: Self.imageHeight)
        .background(colors.bg.canvas)
        // 히어로가 아트보드보다 넓어서 그대로 두면 밖으로 삐져나온다.
        .clipped()
    }

    @ViewBuilder
    private var mapArea: some View {
        if let mapImage {
            Image(uiImage: mapImage)
                .resizable()
                .scaledToFill()
                .clipped()
        } else {
            RouteVectorMap(routePaths: state.routePaths, newRoadPaths: state.newRoadPaths)
        }
    }
}

/// "새로 탐험한 길 +1.3km" — 이 이미지에서 가장 크게 보여줄 값이다.
private struct ExplorationSummary: View {
    let newRoadLabel: String

    /// 시안은 Geist Light(300)인데 num/display 토큰은 ExtraLight(200)다 — 글꼴 크기·자간은 토큰을
    /// 그대로 두고 굵기만 시안에 맞춘다(Android도 같은 처리다).
    private static let numberStyle = RunvealTextStyle(
        font: .custom("Geist-Light", size: 48),
        lineHeight: 48,
        tracking: -0.96
    )

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
            Text(RunningStrings.recordingLegendNewLabel)
                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                .foregroundStyle(colors.text.accent)
            HStack(alignment: .lastTextBaseline, spacing: RunvealSpacing.space2) {
                Text(RunningStrings.resultNewRoadValue(newRoadLabel))
                    .runvealTextStyle(Self.numberStyle)
                    .foregroundStyle(colors.text.accent)
                Text(RunningStrings.recordingDistanceUnit)
                    .runvealTextStyle(RunvealTheme.typography.unitDisplay)
                    .foregroundStyle(colors.text.accent)
            }
        }
    }
}

/// 동 이름 한 줄 + 거리·시간·평균 페이스 세 칸.
private struct Information: View {
    let state: ResultReady

    /// 통계 세 칸 — 96 * 3 + 12 * 2 = 312로 본문 너비에 꼭 맞는다.
    private static let statWidth: CGFloat = 96
    private static let statGap: CGFloat = 12

    var body: some View {
        let colors = RunvealTheme.colors
        // 지나간 동이 여럿이면 첫 동만 적고 나머지는 "외 N개 동"으로 줄인다 — 한 줄짜리 자리라
        // 다 적으면 넘친다. 동을 하나도 못 받았으면 줄 자체를 그리지 않는다.
        let dongNames = state.dongStats.map(\.name)

        VStack(alignment: .leading, spacing: RunvealSpacing.space20) {
            if let first = dongNames.first {
                HStack(alignment: .lastTextBaseline, spacing: RunvealSpacing.space8) {
                    Text(first)
                        .runvealTextStyle(RunvealTheme.typography.heading3)
                        .foregroundStyle(colors.text.primary)
                    if dongNames.count > 1 {
                        Text(RunningStrings.resultShareMoreDong(dongNames.count - 1))
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.secondary)
                    }
                }
            }

            HStack(alignment: .top, spacing: Self.statGap) {
                ShareStat(
                    label: RunningStrings.resultDistanceLabel,
                    value: state.distanceLabel,
                    unit: RunningStrings.recordingDistanceUnit,
                    width: Self.statWidth
                )
                ShareStat(
                    label: RunningStrings.resultElapsedLabel,
                    value: state.elapsedLabel,
                    width: Self.statWidth
                )
                ShareStat(
                    label: RunningStrings.recordingPaceLabel,
                    value: state.paceLabel,
                    width: Self.statWidth
                )
            }
        }
    }
}

/// 통계 한 칸 — 화면용 `ResultStat`과 같은 모양이지만 칸 너비가 고정이다(세 칸이 312를 나눠 쓴다).
private struct ShareStat: View {
    let label: String
    let value: String
    var unit: String?
    let width: CGFloat

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                .foregroundStyle(colors.text.secondary)
            if let unit {
                HStack(alignment: .lastTextBaseline, spacing: RunvealSpacing.space2) {
                    Text(value)
                        .runvealTextStyle(RunvealTheme.typography.numM)
                        .foregroundStyle(colors.text.primary)
                    Text(unit)
                        .runvealTextStyle(RunvealTheme.typography.unitM)
                        .foregroundStyle(colors.text.secondary)
                }
            } else {
                Text(value)
                    .runvealTextStyle(RunvealTheme.typography.numM)
                    .foregroundStyle(colors.text.primary)
            }
        }
        .frame(width: width, alignment: .leading)
    }
}
