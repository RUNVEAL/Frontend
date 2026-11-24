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
    /// 떠 있는 더보기 메뉴가 차지할 자리. nil이면 안 떠 있다.
    @State private var moreMenuAnchor: CGRect?

    /// 더보기 아이콘 자리를 재는 기준 좌표 공간 — 화면 루트에 이름을 붙여 둔다.
    private static let menuSpace = "record-more-menu"
    /// 아이콘과 메뉴 사이 간격. 아이콘을 가리지 않게 **아래로** 띄운다.
    private static let menuGap: CGFloat = 4

    var body: some View {
        ZStack {
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()

            // 맨 뒤에 깔리는 배경 글로우 — 안전 영역을 무시해 상태 바 밑까지 그대로 번진다.
            //
            // 원본이 화면보다 **세로로 길어서** 가로를 꽉 채우고 넘치는 아래쪽을 잘라 낸다. 가로를
            // 직접 재서 고정하지 않으면(`.fill`만 쓰면) 세로를 덮으려고 가로가 화면 밖까지 커진다.
            GeometryReader { geometry in
                RunvealImages.recordDetail
                    .resizable()
                    .scaledToFill()
                    .frame(width: geometry.size.width, alignment: .top)
                    .clipped()
            }
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
                        RecordMoreMenuButton(coordinateSpace: Self.menuSpace) { anchor in
                            moreMenuAnchor = anchor
                        }
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

            moreMenuOverlay
        }
        // 더보기 아이콘이 자기 자리를 재는 기준 — 메뉴를 띄우는 자리와 같은 공간이어야 한다.
        .coordinateSpace(name: Self.menuSpace)
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
            .runvealSheetContent()
        }
        .runvealDialog(
            isPresented: viewModel.state.ready?.isDeleteDialogVisible == true,
            onDismiss: { viewModel.onIntent(.deleteDismissed) }
        ) {
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
        .onDisappear { viewModel.cancelRunningTasks() }
    }

    /// 더보기 메뉴 — **화면 루트에서** 그린다.
    ///
    /// 아이콘 안에서 그리면 바깥을 눌러 닫을 자리가 없고, 조상 뷰가 자르거나 나중에 그려지는
    /// 요소가 덮을 수 있다(내 코스 탭과 같은 구조).
    ///
    /// 자리는 아이콘 **바로 아래**다 — 아이콘을 가리지 않아야 한다.
    @ViewBuilder
    private var moreMenuOverlay: some View {
        if let anchor = moreMenuAnchor {
            ZStack(alignment: .topLeading) {
                // 바깥 어디를 눌러도 닫힌다. 보이지는 않지만 터치는 받아야 해서 `contentShape`를 준다.
                //
                // **`ignoresSafeArea`를 주지 않는다.** 그러면 이 ZStack이 화면 밖까지 커져서
                // 기준점(top-leading)이 위로 밀리고, 아이콘 자리를 잰 좌표와 어긋난다.
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture { moreMenuAnchor = nil }

                RecordMoreMenuContent(
                    onRenameClick: {
                        moreMenuAnchor = nil
                        viewModel.onIntent(.renameClicked)
                    },
                    // TODO: GPX 파일은 이미 저장돼 있다(`IosRecordDetail.gpx`) — 공유 시트로
                    //  내보내는 길만 붙이면 된다. 그때까지는 "개발 중" 안내만 띄운다.
                    onExportGpxClick: {
                        moreMenuAnchor = nil
                        isNotReadyToastVisible = true
                    },
                    onDeleteClick: {
                        moreMenuAnchor = nil
                        viewModel.onIntent(.deleteClicked)
                    }
                )
                // 오른쪽 끝을 아이콘에 맞추고, 아이콘 아래로 내린다.
                .alignmentGuide(.leading) { dimension in -(anchor.maxX - dimension.width) }
                .alignmentGuide(.top) { _ in -(anchor.maxY + Self.menuGap) }
            }
        }
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
            // 지도 범례는 디자인시스템 컴포넌트를 쓴다 — swatch 14x3, 항목 간격 12, "새로 탐험한 길"만
            // glow-gold가 붙는 규칙이 거기 들어 있다. 가장자리에서 12 띄우는 것도 컴포넌트 가이드다.
            RunvealLegend(items: [.new, .today])
                .padding(RunvealSpacing.space12)
        }
        .frame(maxWidth: .infinity)
        .frame(height: Self.mapHeight)
        .clipped()
    }

    /**
     이 러닝으로 동네 탐험률이 얼마나 올랐는지 — 결과 화면의 같은 줄과 같은 숫자다.

     빈 경우에도 섹션을 숨기지 않고 이유를 한 줄로 알린다 — 섹션이 통째로 사라지면 "원래 없는 화면"인지
     "이번 기록만 비는 것"인지 알 수 없다.
     */
    private func dongRateSection(_ dongStats: [RecordDetailDongRow]) -> some View {
        section(title: TabRecordStrings.dongRateTitle, spacing: RunvealSpacing.space4) {
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
            RunvealSectionHeader(title: title)
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
 트랙 위에 [ratio]만큼 채운 막대. 채움은 `progress/filled`, **가장 빠른 구간만** `progress/gained`로
 그리고 `effect/glow-gold`를 얹는다(시안 `구간 기록`).

 Android는 같은 자리에 색을 두 겹 번지게 깔아 두께감을 냈는데, Compose의 blur가 6dp 막대에서는
 거의 안 보였기 때문이다. iOS는 그림자가 그대로 먹으므로 시안대로 디자인시스템 효과 토큰을 쓴다.
 */
private struct SplitBar: View {
    let ratio: Double
    let isFastest: Bool

    private static let height: CGFloat = 6

    var body: some View {
        let colors = RunvealTheme.colors
        let fillColor = isFastest ? colors.progress.gained : colors.progress.filled
        GeometryReader { geometry in
            let width = geometry.size.width * CGFloat(max(0, min(ratio, 1)))
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(colors.progress.track)
                Capsule()
                    .fill(fillColor)
                    .frame(width: width)
                    .runvealDropShadow(isFastest ? RunvealEffects.glowGold : [])
            }
            .frame(height: Self.height)
            .frame(height: geometry.size.height, alignment: .center)
        }
        .frame(height: Self.height)
    }
}
