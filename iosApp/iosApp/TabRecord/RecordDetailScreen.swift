import SharedLogic
import SwiftUI

/**
 기록 상세 — 탭바까지 덮는 전체 화면. Android `RecordDetailScreen`.

 목록에서 지운 기록을 뒤로가기로 되돌아와 다시 열면 보여줄 게 없다 — 빈 화면을 띄우는 대신 목록으로
 돌려보낸다(`notFound`도 `navigateBack` 이펙트를 낸다).
 */
struct RecordDetailScreen: View {
    let trackId: String
    let onBack: () -> Void

    /// 경로 지도 높이 — 시안 값.
    private static let mapHeight: CGFloat = 291
    /// "새 길" 수치가 거리 옆에서 밀리지 않게 잡아 두는 폭.
    private static let newRoadStatWidth: CGFloat = 80

    @StateObject private var viewModel = RecordDetailViewModel()
    @State private var isNotReadyToastVisible = false

    var body: some View {
        ZStack {
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()

            // 맨 뒤에 깔리는 배경 글로우 — 안전 영역을 무시해 상태 바 밑까지 그대로 번진다.
            // 원본이 화면보다 길어서 가로를 꽉 채우고 넘치는 아래쪽은 잘라 낸다(MyScreen의 배경과 같은 생각).
            RunvealImages.recordDetail
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(maxWidth: .infinity, alignment: .top)
                .ignoresSafeArea()
                .allowsHitTesting(false)

            VStack(spacing: 0) {
                // `RunvealTopAppBar`는 액션 자리에 **텍스트**만 받는다(Android는 Composable을 받는다)
                // — 더보기 아이콘을 넣으려고 디자인시스템을 제네릭으로 바꾸는 대신, 앱바 위에 겹쳐
                // 올린다. ZStack이 앱바 높이를 따르므로 세로 가운데에 놓인다.
                ZStack(alignment: .trailing) {
                    RunvealTopAppBar(
                        leading: .back,
                        title: viewModel.state.ready?.data.title,
                        onLeadingClick: onBack
                    )
                    // 읽을 기록이 있을 때만 더보기를 단다 — 빈 화면에 메뉴만 떠 있을 이유가 없다.
                    if viewModel.state.ready != nil {
                        RecordMoreMenuIcon(
                            onRenameClick: { viewModel.onIntent(.renameClicked) },
                            // TODO: GPX 파일은 이미 저장돼 있다(`IosRecordDetail.gpx`) — 공유 시트로
                            //  내보내는 길만 붙이면 된다. 그때까지는 "개발 중" 안내만 띄운다.
                            onExportGpxClick: { isNotReadyToastVisible = true },
                            onDeleteClick: { viewModel.onIntent(.deleteClicked) }
                        )
                        .padding(.trailing, RunvealSpacing.space4)
                    }
                }

                if let ready = viewModel.state.ready {
                    ScrollView {
                        VStack(spacing: RunvealSpacing.space24) {
                            runningSummary(ready.data)
                            routeSummaryCard(ready.data)
                            dongRateSection(ready.data.dongStats)
                            splitsSection(ready.data.splits)
                        }
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.bottom, RunvealSpacing.space24)
                    }
                } else {
                    Spacer()
                }
            }

            if isNotReadyToastVisible {
                VStack {
                    RunvealToast(message: TabRecordStrings.notReady)
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.vertical, RunvealSpacing.space16)
                    Spacer()
                }
            }
        }
        .task(id: trackId) { viewModel.onIntent(.load(trackId: trackId)) }
        .collectEffect(viewModel.effects) { effect in
            switch effect {
            case .navigateBack: onBack()
            }
        }
        // 토스트는 타이머를 모르는 순수 컴포넌트라 사라지는 건 여기서 재운다(홈 셸과 같은 규칙).
        .task(id: isNotReadyToastVisible) {
            guard isNotReadyToastVisible else { return }
            try? await Task.sleep(for: .seconds(3))
            isNotReadyToastVisible = false
        }
        .sheet(isPresented: Binding(
            get: { viewModel.state.ready?.isRenameSheetVisible == true },
            set: { if !$0 { viewModel.onIntent(.renameDismissed) } }
        )) {
            RecordRenameSheet(
                initialName: viewModel.state.ready?.data.title ?? "",
                onConfirm: { viewModel.onIntent(.renameConfirmed(name: $0)) },
                onCancel: { viewModel.onIntent(.renameDismissed) }
            )
            .presentationBackground(Color.clear)
            .presentationDragIndicator(.hidden)
            .presentationDetents([.height(320)])
        }
        .overlay {
            if viewModel.state.ready?.isDeleteDialogVisible == true {
                RunvealDialog(
                    title: TabRecordStrings.deleteDialogTitle,
                    confirmLabel: TabRecordStrings.deleteDialogConfirm,
                    cancelLabel: TabRecordStrings.deleteDialogCancel,
                    description: TabRecordStrings.deleteDialogDescription,
                    // 되돌릴 수 없는 동작이라 확인 버튼을 빨갛게 둔다.
                    type: .destructive,
                    onConfirm: { viewModel.onIntent(.deleteConfirmed) },
                    onCancel: { viewModel.onIntent(.deleteDismissed) }
                )
            }
        }
        .onDisappear { viewModel.cancelRunningTasks() }
    }

    // MARK: - 섹션

    private func runningSummary(_ data: RecordDetailData) -> some View {
        VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
            VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                Text(data.dateTimeLabel)
                    .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                    .foregroundStyle(RunvealTheme.colors.text.secondary)
                Text(data.title)
                    .runvealTextStyle(RunvealTheme.typography.heading2)
                    .foregroundStyle(RunvealTheme.colors.text.primary)
            }
            HStack(alignment: .bottom, spacing: RunvealSpacing.space24) {
                RunvealStat(
                    value: data.distanceValue,
                    unit: TabRecordStrings.distanceUnit,
                    type: .display
                )
                RunvealStat(
                    value: data.newRoadValue,
                    label: TabRecordStrings.detailNewRoadLabel,
                    unit: TabRecordStrings.distanceUnit,
                    accent: true
                )
                .frame(width: Self.newRoadStatWidth)
                Spacer(minLength: 0)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// 경로 지도 + 러닝 시간·평균 페이스·칼로리를 한 카드에 담는다.
    private func routeSummaryCard(_ data: RecordDetailData) -> some View {
        VStack(spacing: 0) {
            routeMap(data.mapSnapshotRequest)
            HStack(spacing: RunvealSpacing.space8) {
                RunvealStat(value: data.elapsedLabel, label: TabRecordStrings.elapsedLabel)
                    .frame(maxWidth: .infinity, alignment: .leading)
                RunvealStat(value: data.paceLabel, label: TabRecordStrings.paceLabel)
                    .frame(maxWidth: .infinity, alignment: .leading)
                RunvealStat(value: data.caloriesLabel, label: TabRecordStrings.caloriesLabel)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(RunvealSpacing.space20)
        }
        .frame(maxWidth: .infinity)
        .background(RunvealTheme.colors.surface.default)
        .clipShape(RunvealShapes.radius24)
        .overlay {
            RunvealShapes.radius24.stroke(RunvealTheme.colors.border.default, lineWidth: 1)
        }
    }

    /// 요청이 없으면(매칭 실패·점 부족) 플레이스홀더만 — 결과 화면의 지도 카드와 같은 규칙.
    private func routeMap(_ request: SnapshotRequest?) -> some View {
        ZStack(alignment: .bottomLeading) {
            RunvealTheme.colors.bg.map
            CourseSnapshotImage(request: request, accessibilityLabel: TabRecordStrings.mapLabel)
            HStack(spacing: RunvealSpacing.space12) {
                legendItem(color: RunvealTheme.colors.map.roadNew,
                           label: TabRecordStrings.legendNew,
                           halo: RunvealTheme.colors.map.roadNewHalo)
                legendItem(color: RunvealTheme.colors.map.roadToday,
                           label: TabRecordStrings.legendToday,
                           halo: nil)
            }
            .padding(RunvealSpacing.space16)
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.mapHeight)
        .clipped()
    }

    private func legendItem(color: Color, label: String, halo: Color?) -> some View {
        HStack(spacing: 6) {
            ZStack {
                if let halo {
                    Capsule().fill(halo).frame(width: 16, height: 10).blur(radius: 4)
                }
                Capsule().fill(color).frame(width: 16, height: 3)
            }
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                .foregroundStyle(RunvealTheme.colors.text.secondary)
        }
    }

    /**
     이 러닝으로 동네 탐험률이 얼마나 올랐는지 — 결과 화면의 같은 줄과 같은 숫자다.

     빈 경우에도 섹션을 숨기지 않고 이유를 한 줄로 알린다 — 섹션이 통째로 사라지면 "원래 없는 화면"인지
     "이번 기록만 비는 것"인지 알 수 없다.
     */
    private func dongRateSection(_ dongStats: [RecordDetailDongRow]) -> some View {
        section(title: TabRecordStrings.dongRateTitle, spacing: RunvealSpacing.space8) {
            if dongStats.isEmpty {
                emptyLine(TabRecordStrings.dongRateEmpty)
            }
            ForEach(dongStats, id: \.name) { stat in
                HStack {
                    Text(stat.name)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(RunvealTheme.colors.text.secondary)
                    Spacer()
                    HStack(spacing: 6) {
                        Text(TabRecordStrings.percent(Int(stat.beforePercent)))
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(RunvealTheme.colors.text.tertiary)
                        Text(TabRecordStrings.rateArrow)
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(RunvealTheme.colors.text.tertiary)
                        Text(TabRecordStrings.percent(Int(stat.afterPercent)))
                            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                            .foregroundStyle(RunvealTheme.colors.text.primary)
                    }
                }
            }
        }
    }

    /**
     1km 구간별 페이스. 막대는 **가장 빠른 구간이 꽉 차는** 길이라, 긴 줄이 더 빨리 달린 구간이다.
     1km를 못 채운 러닝은 나눌 구간이 없어 안내 한 줄만 둔다(탐험률 변화와 같은 규칙).
     */
    private func splitsSection(_ splits: [RecordDetailSplitRow]) -> some View {
        section(title: TabRecordStrings.splitsTitle, spacing: RunvealSpacing.space4) {
            if splits.isEmpty {
                emptyLine(TabRecordStrings.splitsEmpty)
            }
            ForEach(splits, id: \.label) { split in
                HStack(spacing: RunvealSpacing.space8) {
                    Text(split.label)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(RunvealTheme.colors.text.secondary)
                        .frame(width: 28, alignment: .leading)
                    SplitBar(ratio: split.barRatio, isFastest: split.isFastest)
                    Text(split.paceLabel)
                        .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                        .foregroundStyle(RunvealTheme.colors.text.secondary)
                }
            }
        }
    }

    /// 제목 + 테두리 카드 — 탐험률 변화와 구간 기록이 같은 모양이다.
    private func section<Content: View>(
        title: String,
        spacing: CGFloat,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: RunvealSpacing.space12) {
            Text(title)
                .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                .foregroundStyle(RunvealTheme.colors.text.primary)
            VStack(alignment: .leading, spacing: spacing) {
                content()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(RunvealSpacing.space16)
            .background(RunvealTheme.colors.surface.default)
            .clipShape(RunvealShapes.radius16)
            .overlay {
                RunvealShapes.radius16.stroke(RunvealTheme.colors.border.default, lineWidth: 1)
            }
        }
    }

    private func emptyLine(_ text: String) -> some View {
        Text(text)
            .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
            .foregroundStyle(RunvealTheme.colors.text.tertiary)
    }
}

/**
 트랙 위에 [ratio]만큼 채운 막대. 채움은 진행 바 색, **가장 빠른 구간만** 강조 색으로 그리고 글로우를 얹는다.

 글로우는 홈 지도의 "가본 길"과 같은 방식이다 — 그림자 대신 **같은 색을 두 겹 번지게 깔고 그 위에
 막대를 얹는다**(바깥은 넓고 연하게, 안쪽은 좁고 진하게). 6pt짜리 가는 막대에 그림자만 주면 번짐이
 거의 안 보여서, 지도가 굵은 선으로 내는 두께감을 같은 구조로 흉내 낸다.
 */
private struct SplitBar: View {
    let ratio: Double
    let isFastest: Bool

    private static let height: CGFloat = 6
    private static let glowOuterHeight: CGFloat = 20
    private static let glowOuterBlur: CGFloat = 10
    private static let glowInnerHeight: CGFloat = 12
    private static let glowInnerBlur: CGFloat = 5
    private static let glowAlpha: Double = 0.45

    var body: some View {
        let colors = RunvealTheme.colors
        let fillColor = isFastest ? colors.progress.gained : colors.progress.filled
        GeometryReader { geometry in
            let width = geometry.size.width * CGFloat(max(0, min(ratio, 1)))
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(colors.progress.track)
                    .frame(height: Self.height)
                if isFastest {
                    glow(width: width, height: Self.glowOuterHeight, blur: Self.glowOuterBlur, color: fillColor)
                    glow(width: width, height: Self.glowInnerHeight, blur: Self.glowInnerBlur, color: fillColor)
                }
                Capsule()
                    .fill(fillColor)
                    .frame(width: width, height: Self.height)
            }
            .frame(height: geometry.size.height, alignment: .center)
        }
        .frame(height: Self.glowOuterHeight)
    }

    private func glow(width: CGFloat, height: CGFloat, blur: CGFloat, color: Color) -> some View {
        Capsule()
            .fill(color.opacity(Self.glowAlpha))
            .frame(width: width, height: height)
            .blur(radius: blur)
    }
}
