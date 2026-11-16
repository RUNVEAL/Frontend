import SharedLogic
import SwiftUI

/// 러닝 결과 화면 — 기록 종료 직후 들어오지만, 동별 탐험률 변화는 최종 매칭(§6)이 끝나야 나와서
/// 그때까지는 로딩만 보여준다.
struct ResultScreen: View {
    let onClose: () -> Void

    @StateObject private var viewModel = ResultViewModel()

    private static let glowWidth: CGFloat = 480
    private static let glowHeight: CGFloat = 320

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack(alignment: .top) {
            colors.bg.canvas.ignoresSafeArea()

            // 제일 뒤에 깔리는 배경 glow — 상태바 아래까지 그대로 번진다. 이미지 자체에 색이 이미
            // 들어 있다(result_glow.png). 화면 너비보다 넓은 480을 그대로 유지해야 세로로 쏠리지 않는다.
            RunvealImages.resultGlow
                .resizable()
                .scaledToFill()
                .frame(width: Self.glowWidth, height: Self.glowHeight)
                .frame(maxWidth: .infinity, alignment: .center)
                .ignoresSafeArea(edges: .top)
                .allowsHitTesting(false)

            VStack(spacing: 0) {
                // 앱바는 X 아이콘 하나만 보이지만 영역(56)은 그대로 차지한다 — 아래 날짜/타이틀은 그
                // 영역 밑에서 시작한다. Default 스타일 — Map 스타일의 원형 글래스 배경은 지도 위에
                // 얹는 화면용이라 불투명 배경인 이 화면엔 안 맞는다.
                RunvealTopAppBar(
                    leading: .close,
                    leadingAccessibilityLabel: RunningStrings.resultCloseAccessibilityLabel,
                    onLeadingClick: onClose
                )

                switch viewModel.state {
                case .loading:
                    ProgressView()
                        .tint(colors.accent.default)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                case .ready(let ready):
                    ResultContent(state: ready)
                }
            }
        }
        .onDisappear { viewModel.onDisappear() }
    }
}

private struct ResultContent: View {
    let state: ResultReady

    private static let mapCardHeight: CGFloat = 268
    /// 디자인 값 40 — spacing 스케일에 없어 그대로 하드코딩.
    private static let titleToExploreGap: CGFloat = 40

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 0) {
                    VStack(spacing: RunvealSpacing.space8) {
                        // 밝은 glow 위에 얹히는 글자라 onAccent(어두운 색)를 쓴다.
                        Text(state.dateLabel)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(colors.text.onAccent)
                        Text(RunningStrings.resultTitle)
                            .runvealTextStyle(RunvealTheme.typography.heading1)
                            .foregroundStyle(colors.text.onAccent)
                    }

                    Spacer().frame(height: Self.titleToExploreGap)

                    // "새로 탐험한 길" + 동네 탐험률 리스트 — 화면 기본 16에 더해 섹션 자체 마진 24(합계 40).
                    ExploreResultCard(state: state)
                        .padding(.horizontal, RunvealSpacing.space24)

                    Spacer().frame(height: RunvealSpacing.space32)

                    // 지도 카드 — 화면 기본 여백(16)만 적용, 별도 섹션 마진 없음.
                    RouteMapCard(request: state.mapSnapshotRequest)
                        .frame(height: Self.mapCardHeight)

                    Spacer().frame(height: RunvealSpacing.space20)

                    ResultStatsRow(state: state)
                        .padding(.horizontal, RunvealSpacing.space24)
                }
                .padding(.top, RunvealSpacing.space32)
                .padding(.horizontal, RunvealLayout.marginScreen)
                .frame(maxWidth: .infinity)
            }

            RunvealButtonGroupEqual(
                firstLabel: RunningStrings.resultDetailButton,
                onFirstClick: {},
                secondLabel: RunningStrings.resultSaveImageButton,
                onSecondClick: {}
            )
            .padding(.horizontal, RunvealLayout.marginScreen)
            .padding(.vertical, RunvealSpacing.space8)
        }
    }
}

/// "탐험 결과" — 새로 탐험한 길 + 이번에 지나간 동들의 탐험률 전/후.
private struct ExploreResultCard: View {
    let state: ResultReady

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: RunvealSpacing.space12) {
            HStack {
                Text(RunningStrings.recordingLegendNewLabel)
                    .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                    .foregroundStyle(colors.text.accent)
                Spacer()
                HStack(alignment: .lastTextBaseline, spacing: RunvealSpacing.space2) {
                    Text(RunningStrings.resultNewRoadValue(state.newRoadLabel))
                        .runvealTextStyle(RunvealTheme.typography.numL)
                        .foregroundStyle(colors.text.accent)
                    Text(RunningStrings.recordingDistanceUnit)
                        .runvealTextStyle(RunvealTheme.typography.unitL)
                        .foregroundStyle(colors.text.accent)
                }
            }
            .frame(maxWidth: .infinity)

            VStack(spacing: RunvealSpacing.space8) {
                ForEach(state.dongStats) { stat in
                    DongRateRow(stat: stat)
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
}

private struct DongRateRow: View {
    let stat: ResultDongStat

    var body: some View {
        let colors = RunvealTheme.colors
        HStack {
            Text(RunningStrings.resultDongRateLabel(stat.name))
                .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                .foregroundStyle(colors.text.secondary)
            Spacer()
            HStack(spacing: RunvealSpacing.space4) {
                Text(RunningStrings.resultRatePercent(stat.beforePercent))
                    .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                    .foregroundStyle(colors.text.tertiary)
                Text(RunningStrings.resultRateArrow)
                    .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                    .foregroundStyle(colors.text.tertiary)
                Text(RunningStrings.resultRatePercent(stat.afterPercent))
                    .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                    .foregroundStyle(colors.text.accent)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

/// 경로 지도 카드 — 요청이 있으면 실제 경로가 그려진 지도 스냅샷, 없으면(경로 점이 2개 미만인 아주
/// 짧은 기록) 플레이스홀더만 보여준다.
private struct RouteMapCard: View {
    let request: SnapshotRequest?

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack(alignment: .bottomLeading) {
            CourseSnapshotImage(
                request: request,
                accessibilityLabel: RunningStrings.resultMapAccessibilityLabel
            )

            HStack(spacing: RunvealSpacing.space12) {
                LegendItem(
                    swatchColor: colors.map.roadNew,
                    label: RunningStrings.recordingLegendNewLabel,
                    haloColor: colors.map.roadNewHalo
                )
                LegendItem(swatchColor: colors.map.roadToday, label: RunningStrings.recordingLegendTodayLabel)
            }
            .padding(RunvealSpacing.space16)
        }
        .frame(maxWidth: .infinity)
        .background(colors.bg.canvas, in: RunvealShapes.radius16)
        .overlay { RunvealShapes.radius16.stroke(colors.border.subtle, lineWidth: 1) }
        .clipShape(RunvealShapes.radius16)
    }
}

private struct ResultStatsRow: View {
    let state: ResultReady

    var body: some View {
        HStack {
            ResultStat(
                label: RunningStrings.resultDistanceLabel,
                value: state.distanceLabel,
                unit: RunningStrings.recordingDistanceUnit
            )
            Spacer()
            ResultStat(label: RunningStrings.resultElapsedLabel, value: state.elapsedLabel)
            Spacer()
            ResultStat(label: RunningStrings.recordingPaceLabel, value: state.paceLabel)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct ResultStat: View {
    let label: String
    let value: String
    var unit: String?

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
    }
}
