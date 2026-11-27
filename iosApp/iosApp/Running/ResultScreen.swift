import SharedLogic
import SwiftUI
import UIKit

/// 러닝 결과 화면 — 기록 종료 직후 들어오지만, 동별 탐험률 변화는 최종 매칭(§6)이 끝나야 나와서
/// 그때까지는 로딩만 보여준다.
struct ResultScreen: View {
    let onClose: () -> Void

    @StateObject private var viewModel = ResultViewModel()
    /// 떠 있는 안내 문구 — nil이면 안 떠 있다. "개발 중"과 이미지 저장 결과가 같은 자리를 쓴다.
    @State private var toastMessage: String?
    /// 저장은 큰 비트맵을 만들고 보관함까지 쓴다 — 끝나기 전에 또 누르면 같은 그림을 두 장 만든다.
    @State private var isSavingImage = false

    private static let glowWidth: CGFloat = 480
    private static let glowHeight: CGFloat = 320
    /// 하단 버튼 그룹 높이 + 여백 — 토스트를 그 **위로** 띄운다. 버튼을 가리면 눌린 건지 알 수 없다.
    private static let toastButtonInset: CGFloat = 72

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack(alignment: .top) {
            colors.bg.canvas.ignoresSafeArea()

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
                    ResultContent(
                        state: ready,
                        onNotReadyClick: { toastMessage = RunningStrings.notReady },
                        onSaveImageClick: { saveImage(state: ready) }
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            // 배경 glow — 상태바 아래까지 그대로 번진다. 이미지 자체에 색이 들어 있다(result_glow.png).
            //
            // **형제가 아니라 배경으로 깐다.** 배경은 레이아웃에 영향을 주지 않는다. 형제로 두면
            // 고정 폭 480이 ZStack을 그만큼 넓혀서, 그 안의 앱바와 좌우 여백까지 화면 밖으로 밀린다
            // (닫기 버튼이 사라지고 글자가 양옆으로 잘리던 원인). Android는 `requiredWidth`로 같은
            // 일을 한다 — 화면 너비로 줄이지 않고 그대로 그리되 부모 크기는 건드리지 않는다.
            .background(alignment: .top) {
                RunvealImages.resultGlow
                    .resizable()
                    .scaledToFill()
                    .frame(width: Self.glowWidth, height: Self.glowHeight)
                    .ignoresSafeArea(edges: .top)
                    .allowsHitTesting(false)
            }

            if let toastMessage {
                // 시안은 토스트를 **하단**에 둔다(`Overlay/Toast`: "하단 GNB(또는 하단 버튼) 위 8").
                // 이 화면은 하단이 버튼 그룹이라 그 높이만큼 더 띄운다(Android도 같다).
                VStack {
                    Spacer()
                    RunvealToast(message: toastMessage)
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.bottom, RunvealSpacing.space8 + Self.toastButtonInset)
                }
            }
        }
        .onDisappear { viewModel.onDisappear() }
        // 토스트는 타이머를 모르는 순수 컴포넌트라 사라지는 건 여기서 재운다(다른 화면과 같은 규칙).
        .task(id: toastMessage) {
            guard toastMessage != nil else { return }
            try? await Task.sleep(for: .seconds(3))
            toastMessage = nil
        }
    }

    /// "이미지 저장하기" — 템플릿을 그려 사진 보관함에 넣고 결과를 토스트로 알린다.
    ///
    /// 그리기(`ImageRenderer`)와 상태 갱신은 메인에서만 돌아야 해서 Task에 `@MainActor`를 박는다.
    private func saveImage(state: ResultReady) {
        guard !isSavingImage else { return }
        isSavingImage = true
        Task { @MainActor in
            // 지도는 **캡처 전에** 다 그려 둔다 — 캡처는 한 프레임에 끝나서, 템플릿 안에서 비동기로
            // 불러오면 지도 자리가 빈 채로 찍힌다(Android에서 실제로 선만 나왔다).
            let mapImage = await Self.renderShareMapImage(request: state.mapSnapshotRequest)
            let rendered = ResultImageSaver.render(
                ResultShareImage(state: state, mapImage: mapImage),
                scale: ResultShareImage.scale
            )
            guard let rendered else {
                toastMessage = RunningStrings.resultSaveImageFailure
                isSavingImage = false
                return
            }
            switch await ResultImageSaver.save(rendered) {
            case .saved: toastMessage = RunningStrings.resultSaveImageSuccess
            case .failed: toastMessage = RunningStrings.resultSaveImageFailure
            case .permissionDenied: toastMessage = RunningStrings.resultSaveImagePermissionDenied
            }
            isSavingImage = false
        }
    }

    /**
     저장 이미지에 깔 지도 그림을 미리 그린다 — 화면 결과 카드와 **같은 스냅샷**이라 경로·새 길 색이
     그대로다.

     크기는 템플릿의 지도 자리를 내보낼 배율로 환산한 값이다. 캐시 파일명에 크기가 들어가서
     (`CourseSnapshotRenderer`) 화면 카드용 캐시와 섞이지 않는다.

     경로 점이 모자라거나(제자리 종료) 타일을 못 받으면 nil — 그때는 템플릿이 선만 그린다.

     `CourseSnapshotRenderer`가 `@MainActor`라 이 함수도 메인에 묶어 둔다 — 격리 밖에서 `shared`를
     건드리면 동시성 검사에 걸린다. 어차피 부르는 쪽도 메인(`saveImage`의 Task)이다.
     */
    @MainActor
    private static func renderShareMapImage(request: SnapshotRequest?) async -> UIImage? {
        guard let request else { return nil }
        let widthPx = Int(ResultShareImage.mapWidth * ResultShareImage.scale)
        let heightPx = Int(ResultShareImage.mapHeight * ResultShareImage.scale)
        guard let url = await CourseSnapshotRenderer.shared.imageURL(
            request: request,
            widthPx: widthPx,
            heightPx: heightPx
        ) else { return nil }
        return UIImage(contentsOfFile: url.path)
    }
}

private struct ResultContent: View {
    let state: ResultReady
    /// 기록 상세가 아직 없어서 누르면 안내만 띄운다(Android와 같다).
    let onNotReadyClick: () -> Void
    let onSaveImageClick: () -> Void

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

            // TODO: 기록 상세 화면으로 이동 — 기록 탭의 상세를 재사용한다.
            //  붙기 전까지는 "개발 중" 안내만 띄운다(Android도 같은 상태다).
            RunvealButtonGroupEqual(
                firstLabel: RunningStrings.resultDetailButton,
                onFirstClick: onNotReadyClick,
                secondLabel: RunningStrings.resultSaveImageButton,
                onSecondClick: onSaveImageClick
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
