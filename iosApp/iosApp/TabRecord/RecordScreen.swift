import SharedLogic
import SwiftUI

enum TabRecordStrings {
    static let title = "기록"
    /// 요약 줄 앞머리 — 보고 있는 달이 늘 이번 달은 아니라서 그 달을 가리키는 말을 끼운다.
    static func runCountPrefix(_ periodLabel: String) -> String { "\(periodLabel) 러닝" }
    static let previousYear = "이전 해"
    static let nextYear = "다음 해"
    static let monthPicker = "달 선택"
    static func yearLabel(_ year: Int) -> String { "\(year)년" }
    static func monthLabel(_ month: Int) -> String { "\(month)월" }
    static let newRoadLabel = "새로 탐험한 길"
    static let distanceUnit = "km"
    static let emptyTitle = "아직 달린 기록이 없어요"
    static let emptyDescription = "코스를 만들고 첫 길을 밝혀보세요"
    static let emptyAction = "러닝 시작하기"

    // MARK: 기록 상세

    /// 요약의 "새로 탐험한 길"과 달리 상세는 수치 옆 짧은 라벨이라 따로 둔다.
    static let detailNewRoadLabel = "새 길"
    static let elapsedLabel = "러닝 시간"
    static let paceLabel = "평균 페이스"
    static let caloriesLabel = "칼로리"
    static let mapLabel = "이 러닝의 경로 지도"
    static let legendNew = "새로 탐험한 길"
    static let legendToday = "오늘 달린 길"
    static let dongRateTitle = "탐험률 변화"
    static let dongRateEmpty = "이 러닝으로 집계된 동네가 없어요"
    static let splitsTitle = "구간 기록"
    static let splitsEmpty = "1km를 넘겨야 구간 기록이 생겨요"
    static let rateArrow = "→"
    static func percent(_ value: Int) -> String { "\(value)%" }

    static let moreLabel = "더보기"
    static let moreRename = "이름 수정"
    static let moreExportGpx = "GPX 내보내기"
    static let moreDelete = "삭제"

    static let renameSheetTitle = "기록 이름 수정"
    static let renameInputLabel = "기록 이름"
    static let renameInputPlaceholder = "기록 이름을 입력하세요"
    static let renameCancel = "취소"
    static let renameConfirm = "저장"

    static let deleteDialogTitle = "러닝을 기록을 삭제할까요?"
    static let deleteDialogDescription = "삭제하면 해당 러닝이 목록에서 사라져요."
    static let deleteDialogConfirm = "삭제"
    static let deleteDialogCancel = "취소"

    /// 아직 기능이 안 붙은 버튼을 눌렀을 때 — 붙는 대로 해당 호출부에서 제거한다.
    static let notReady = "아직 개발 중입니다"
}

/// "기록" 탭 — Android `RecordScreen` + `RecordViewModel`에 해당한다.
///
/// 러닝을 끝내면 최종 매칭 후 결과가 `TrackResultHolder`에 publish된다 — 그걸 구독해 그때마다 DB를
/// 다시 읽어, 방금 끝낸 러닝이 목록과 월간 요약에 바로 올라오게 한다.
struct RecordScreen: View {
    /// 목록에서 기록 하나를 눌렀다 — 상세로 올리는 건 홈 셸이 맡는다(탭 화면은 내비게이터를 모른다).
    let onRecordClick: (String) -> Void

    @State private var summary: MonthlyRecordSummary?
    @State private var isLoading = true
    @State private var subscription: FlowSubscription?

    /// 보고 있는 달. 0이면 아직 안 골랐다는 뜻이라 이번 달을 읽는다.
    @State private var shownYear = 0
    @State private var shownMonth = 0
    @State private var isMonthPickerOpen = false
    /// 월 선택 패널을 앱바 **바로 아래**에 놓으려고 재는 값 — 앱바 높이는 글꼴·안전 영역에 따라 달라진다.
    @State private var appBarHeight: CGFloat = 0

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack(alignment: .top) {
            colors.bg.canvas.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    RunvealTopAppBarLarge(
                        title: TabRecordStrings.title,
                        metaLabel: summary?.monthLabel ?? "",
                        titleStyle: RunvealTheme.typography.heading1,
                        onMetaLabelClick: { isMonthPickerOpen = true }
                    )
                    .background {
                        GeometryReader { geometry in
                            Color.clear
                                .onAppear { appBarHeight = geometry.size.height }
                                .onChange(of: geometry.size.height) { _, height in appBarHeight = height }
                        }
                    }

                    MonthlyRunningSummary(
                        periodLabel: summary?.periodLabel ?? "이번 달",
                        runCountLabel: summary?.runCountLabel ?? "0회",
                        totalDistanceValue: summary?.totalDistanceValue ?? "0.00",
                        newRoadValue: summary?.newRoadValue ?? "+0.00"
                    )
                    .padding(.horizontal, RunvealLayout.marginScreen)

                    RunvealImages.record
                        .resizable()
                        .aspectRatio(1816.0 / 405.0, contentMode: .fill)
                        .frame(maxWidth: .infinity)
                        .clipShape(RunvealShapes.radius16)
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.vertical, RunvealSpacing.space12)

                    let items = summary?.items ?? []
                    if !items.isEmpty {
                        // Android는 아이템 사이 간격을 -12로 줘서 타임라인 선이 이어 붙는다 — 여기서는
                        // 아이템 자체가 아래 여백을 갖지 않으므로 음수 간격으로 같은 모양을 만든다.
                        VStack(spacing: -12) {
                            ForEach(items, id: \.trackId) { item in
                                RecordListItemRow(item: item)
                                    .contentShape(Rectangle())
                                    .onTapGesture { onRecordClick(item.trackId) }
                            }
                        }
                        .padding(.horizontal, RunvealLayout.marginScreen)
                    } else if !isLoading {
                        RunvealEmptyState(
                            title: TabRecordStrings.emptyTitle,
                            description: TabRecordStrings.emptyDescription,
                            actionLabel: TabRecordStrings.emptyAction,
                            onActionClick: {}
                        ) {
                            RunvealImages.emptyCourse
                                .resizable()
                                .scaledToFit()
                        }
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        // 60에 맞는 spacing 토큰이 없어 그대로 하드코딩(Android와 같다).
                        .padding(.top, 60)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.bottom, RunvealLayout.sizeTabBar + RunvealSpacing.space24)
            }

            if isMonthPickerOpen {
                // 바깥을 탭하면 닫힌다(Android Popup의 focusable과 같은 역할). 투명해도 터치를
                // 받으려면 모양을 줘야 한다.
                Color.clear
                    .contentShape(Rectangle())
                    .ignoresSafeArea()
                    .onTapGesture { isMonthPickerOpen = false }

                MonthPicker(
                    selectedYear: Int(summary?.selectedYear ?? 0),
                    selectedMonth: Int(summary?.selectedMonth ?? 0),
                    onSelect: { year, month in
                        isMonthPickerOpen = false
                        shownYear = year
                        shownMonth = month
                        Task { await reload() }
                    }
                )
                .accessibilityLabel(TabRecordStrings.monthPicker)
                // 앱바 바로 아래, 화면 좌우 여백에 맞춰 왼쪽에 붙인다.
                .padding(.leading, RunvealLayout.marginScreen)
                .padding(.top, appBarHeight + RunvealSpacing.space4)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
        }
        .onAppear { observe() }
        .onDisappear {
            subscription?.cancel()
            subscription = nil
        }
    }

    private func observe() {
        guard subscription == nil else { return }
        subscription = RunvealFlowWatchers.shared.watchTrackResult { _ in
            Task { await reload() }
        }
    }

    private func reload() async {
        let year = shownYear
        let month = shownMonth
        let loaded = await Task.detached(priority: .userInitiated) {
            // Kotlin `Int`는 Swift에 `Int32`로 넘어간다.
            if year > 0 {
                return IosRecordList.shared.load(year: Int32(year), month: Int32(month))
            }
            return IosRecordList.shared.loadThisMonth()
        }.value
        summary = loaded
        isLoading = false
    }
}

/// 기록 탭 앱바 바로 아래, 이번 달 러닝 총 거리 + 새로 탐험한 길 요약(가운데 정렬).
private struct MonthlyRunningSummary: View {
    /// "이번 달"처럼 어느 달인지 가리키는 앞머리 — 달을 고를 수 있어서 늘 이번 달은 아니다.
    let periodLabel: String
    let runCountLabel: String
    let totalDistanceValue: String
    let newRoadValue: String

    var body: some View {
        let colors = RunvealTheme.colors
        // 9/36에 맞는 spacing 토큰이 없어 그대로 하드코딩(Android와 같다).
        VStack(spacing: 9) {
            VStack(spacing: RunvealSpacing.space4) {
                HStack(spacing: RunvealSpacing.space4) {
                    Text(TabRecordStrings.runCountPrefix(periodLabel))
                        .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                        .foregroundStyle(colors.text.secondary)
                    Text(runCountLabel)
                        .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                        .foregroundStyle(colors.text.secondary)
                }
                HStack(alignment: .lastTextBaseline, spacing: RunvealSpacing.space4) {
                    Text(totalDistanceValue)
                        .runvealTextStyle(RunvealTheme.typography.numDisplay)
                        .foregroundStyle(colors.text.primary)
                    Text(TabRecordStrings.distanceUnit)
                        .runvealTextStyle(RunvealTheme.typography.unitDisplay)
                        .foregroundStyle(colors.text.secondary)
                }
            }
            .frame(maxWidth: .infinity)

            HStack(alignment: .bottom, spacing: 36) {
                Text(TabRecordStrings.newRoadLabel)
                    .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                    .foregroundStyle(colors.text.accent)
                HStack(alignment: .lastTextBaseline, spacing: RunvealSpacing.space2) {
                    Text(newRoadValue)
                        .runvealTextStyle(RunvealTheme.typography.numM)
                        .foregroundStyle(colors.text.accent)
                    Text(TabRecordStrings.distanceUnit)
                        .runvealTextStyle(RunvealTheme.typography.unitM)
                        .foregroundStyle(colors.text.accent)
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
}

/// 기록 목록의 한 줄 — 날짜 + 타임라인 마커(점/세로선) + 러닝 카드(지도/코스/거리·시간·페이스).
private struct RecordListItemRow: View {
    let item: MonthlyRecordItem

    var body: some View {
        let colors = RunvealTheme.colors
        // 타임라인 세로선 그라디언트 — 0% 색은 gold/100 토큰, 나머지는 이 선 전용이라 하드코딩.
        let lineGradient = LinearGradient(
            stops: [
                .init(color: colors.accent.default.opacity(0.5), location: 0),
                .init(color: Color(red: 0.8745, green: 0.8314, blue: 0.6549, opacity: 0.363), location: 0.5),
                .init(color: Color(red: 0.5843, green: 0.5569, blue: 0.4392, opacity: 0), location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )

        HStack(alignment: .top, spacing: RunvealSpacing.space8) {
            ZStack(alignment: .top) {
                Rectangle()
                    .fill(lineGradient)
                    .frame(width: 1, height: 148)
                // 타임라인 점 — 색은 의미(semantic) 토큰이 아니라 원시 gold 토큰을 직접 쓴다.
                RunvealShapes.full
                    .fill(RunvealPalette.gold100)
                    .overlay { RunvealShapes.full.stroke(RunvealPalette.gold500, lineWidth: 2) }
                    .frame(width: 8, height: 8)
            }
            .frame(width: 8, height: 154, alignment: .top)

            VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
                Text(item.dateLabel)
                    .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                    .foregroundStyle(colors.text.tertiary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                HStack(spacing: RunvealSpacing.space12) {
                    // 달린 경로 지도 스냅샷 — 캐시가 없으면 렌더링이 끝날 때까지 빈 지도색으로 보인다.
                    CourseSnapshotImage(request: item.thumbnailRequest)
                        .frame(width: 80, height: 80)
                        .background(colors.bg.map, in: RunvealShapes.radius8)
                        .overlay { RunvealShapes.radius8.stroke(colors.border.subtle, lineWidth: 1) }
                        .clipShape(RunvealShapes.radius8)

                    VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                        Text(item.courseTitle)
                            .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                            .foregroundStyle(colors.text.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        HStack(alignment: .top, spacing: RunvealSpacing.space8) {
                            Text(item.distanceLabel)
                                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                                .foregroundStyle(colors.text.secondary)
                            Text(item.durationLabel)
                                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                                .foregroundStyle(colors.text.secondary)
                            Text(item.paceLabel)
                                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                                .foregroundStyle(colors.text.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(RunvealSpacing.space12)
                .frame(maxWidth: .infinity)
                .frame(height: 106)
                .background(colors.surface.default, in: RunvealShapes.radius16)
                .overlay { RunvealShapes.radius16.stroke(colors.border.default, lineWidth: 1) }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(height: 154, alignment: .top)
    }
}
