import SharedLogic
import SwiftUI

enum TabCourseStrings {
    static let title = "내 코스"
    static func metaLabel(_ count: Int) -> String { "저장한 코스 \(count)개" }
    static let recommendBadge = "다음에 달려볼 코스"
    static let sectionTitle = "저장한 코스"
    static let sortLabel = "최근 저장순"
    static let emptyTitle = "아직 저장한 코스가 없어요"
    static let emptyDescription = "만든 코스를 저장하면\n여기서 바로 달릴 수 있어요"
    static let emptyAction = "코스 만들기"
    static let addCourse = "코스 추가"
    static let more = "더보기"
    static let startWithThisCourse = "이 코스로 러닝 시작"
    static let startNow = "바로 시작"
    static let courseDetail = "코스 상세"
    static let moreRename = "이름 수정"
    static let moreExportGpx = "GPX 내보내기"
    static let moreDelete = "삭제"
    static let deleteDialogTitle = "코스를 삭제할까요?"
    static let deleteDialogDescription = "삭제하면 되돌릴 수 없어요"
    static let deleteDialogConfirm = "삭제"
    static let deleteDialogCancel = "취소"
    static let renameSheetTitle = "코스 이름 수정"
    static let renameInputLabel = "코스 이름"
    static let renameInputPlaceholder = "코스 이름을 입력하세요"
    static let renameCancel = "취소"
    static let renameConfirm = "저장"
    static let notReady = "아직 개발 중입니다"
}

/// "내 코스" 탭 — Android `CourseScreen` + `CourseListViewModel`에 해당한다.
///
/// 탭을 나갔다 들어올 때마다 다시 불러온다 — 그 사이 새로 저장한 코스가 반영돼야 한다.
struct CourseScreen: View {
    let onCreateCourseClick: () -> Void
    /// 줄 오른쪽 아이콘을 누르면 코스 상세로 — 값은 id만 넘기고 화면이 로컬 DB에서 다시 읽는다.
    /// 줄 전체를 누르는 건 상세로 가지 않고 위 큰 카드를 그 코스로 바꾸는 동작이라, 화면 안에서 끝낸다.
    let onCourseClick: (SavedCourseItem) -> Void

    @State private var courses: [SavedCourseItem] = []
    @State private var isLoading = true
    /// 목록에서 고른 코스의 ID — 위 큰 카드가 이걸 띄운다([featuredCourse]).
    @State private var selectedCourseId: String?
    /// 더보기에서 고른 코스 — 이름 수정 시트/삭제 다이얼로그가 어떤 코스인지 들고 있는다.
    @State private var renameTarget: SavedCourseItem?
    @State private var deleteTarget: SavedCourseItem?
    @State private var isNotReadyToastVisible = false
    /// 떠 있는 더보기 메뉴 — 어느 코스의 어느 자리인지. nil이면 안 떠 있다.
    @State private var moreMenu: CourseMoreMenuTarget?
    /// 메뉴를 띄울 좌표 공간의 높이. 아래로 띄우면 탭바에 가리는지 재는 기준이다.
    @State private var menuSpaceHeight: CGFloat = 0
    /// 떠 있는 메뉴의 높이. 항목 수에 따라 달라져서 그릴 때 재 둔다.
    @State private var menuHeight: CGFloat = 0

    /**
     위 큰 카드에 띄울 코스 — 목록에서 고른 것, 아직 아무것도 안 골랐으면 가장 최근에 저장한
     코스(목록 첫 줄, `최근 저장순`)다.

     **ID로** 들고 있는 이유: 목록은 이름을 바꾸거나 코스를 지울 때마다 다시 불러오면서(`reload`)
     항목을 새로 만든다 — 객체를 들고 있으면 이름을 바꾼 뒤에도 카드에 옛 제목이 남는다. 고른
     코스를 지웠으면 ID를 못 찾으니 기본값으로 돌아간다(Android `CourseScreen`과 같은 방식).
     */
    private var featuredCourse: SavedCourseItem? {
        if let id = selectedCourseId, let found = courses.first(where: { $0.id == id }) {
            return found
        }
        return courses.first
    }

    /// 더보기 아이콘 자리를 재는 기준 좌표 공간 — 화면 루트에 이름을 붙여 둔다.
    private static let menuSpace = "course-more-menu"
    /// 아이콘과 메뉴 사이 간격. 아이콘을 가리지 않게 띄운다.
    private static let menuGap: CGFloat = 4
    /// 하단 플로팅 탭바가 덮는 높이 — 목록이 탭바를 피할 때 쓰는 값과 같다. 메뉴도 여기까지만 내려간다.
    private static let tabBarInset: CGFloat = RunvealLayout.sizeTabBar + RunvealSpacing.space24

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack(alignment: .top) {
            colors.bg.canvas.ignoresSafeArea()

            // 화면 맨 위부터 깔리는 히어로 이미지 — 원본 비율(1920:544)대로 보여준다.
            //
            // **`.fill`을 쓰면 안 된다.** SwiftUI의 fill은 제안된 공간을 *덮도록* 키우는데, ZStack이
            // 화면 세로 전체를 제안하므로 비율을 맞추려고 가로로 수천 pt까지 커진다(가운데만 보여
            // 화면을 몇 배 확대한 꼴이 된다). `.fit`이면 가로에 맞춰 들어가고 높이가 비율대로
            // 정해진다 — Android의 `fillMaxWidth().aspectRatio(...)`와 같은 결과다.
            RunvealImages.myCourse
                .resizable()
                .aspectRatio(1920.0 / 544.0, contentMode: .fit)
                .frame(maxWidth: .infinity)
                .ignoresSafeArea(edges: .top)
                .allowsHitTesting(false)

            ScrollView {
                VStack(spacing: 0) {
                    RunvealTopAppBarLarge(
                        title: TabCourseStrings.title,
                        metaLabel: TabCourseStrings.metaLabel(courses.count)
                    ) {
                        CourseTopBarActionButton(onClick: onCreateCourseClick)
                    }

                    if let featured = featuredCourse {
                        CourseRecommendationCard(
                            badgeLabel: TabCourseStrings.recommendBadge,
                            course: featured,
                            menuSpace: Self.menuSpace,
                            onMoreTap: { anchor in
                                moreMenu = CourseMoreMenuTarget(course: featured, anchor: anchor)
                            },
                            onStartClick: { start(course: featured) }
                        )
                        .padding(.horizontal, RunvealLayout.marginScreen)

                        SavedCourseSectionHeader(
                            title: TabCourseStrings.sectionTitle,
                            sortLabel: TabCourseStrings.sortLabel,
                            onSortClick: {}
                        )
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.top, RunvealSpacing.space24)

                        VStack(spacing: 0) {
                            ForEach(courses, id: \.id) { course in
                                SavedCourseListItem(
                                    course: course,
                                    menuSpace: Self.menuSpace,
                                    onMoreTap: { anchor in
                                        moreMenu = CourseMoreMenuTarget(course: course, anchor: anchor)
                                    },
                                    onClick: { selectedCourseId = course.id },
                                    onDetailClick: { onCourseClick(course) }
                                )
                            }
                        }
                        .padding(.horizontal, RunvealLayout.marginScreen)
                    }
                }
                .frame(maxWidth: .infinity)
                // 하단 플로팅 탭바에 가리지 않게 — 탭바 높이 + 위아래 여백만큼 띄운다.
                .padding(.bottom, Self.tabBarInset)
            }

            if courses.isEmpty && !isLoading {
                RunvealEmptyState(
                    title: TabCourseStrings.emptyTitle,
                    description: TabCourseStrings.emptyDescription,
                    actionLabel: TabCourseStrings.emptyAction,
                    onActionClick: onCreateCourseClick
                ) {
                    RunvealImages.emptyCourse
                        .resizable()
                        .scaledToFit()
                }
                .padding(.horizontal, RunvealLayout.marginScreen)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }

            if isNotReadyToastVisible {
                VStack {
                    RunvealToast(message: TabCourseStrings.notReady)
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.vertical, RunvealSpacing.space16)
                    Spacer()
                }
            }

            moreMenuOverlay
        }
        // 더보기 아이콘이 자기 자리를 재는 기준 — 메뉴를 띄우는 자리와 같은 공간이어야 한다.
        .coordinateSpace(name: Self.menuSpace)
        // 그 공간이 얼마나 높은지도 같이 재 둔다 — 메뉴를 위로 뒤집을지 여기서 갈린다.
        .background(
            GeometryReader { geometry in
                Color.clear
                    .onAppear { menuSpaceHeight = geometry.size.height }
                    .onChange(of: geometry.size.height) { _, next in menuSpaceHeight = next }
            }
        )
        .task { await reload() }
        // 토스트 자체는 타이머를 모르는 순수 컴포넌트라 사라지는 건 여기서 재운다.
        .task(id: isNotReadyToastVisible) {
            guard isNotReadyToastVisible else { return }
            try? await Task.sleep(for: Self.toastDuration)
            isNotReadyToastVisible = false
        }
        // `SavedCourseItem`은 Kotlin에서 건너온 타입이라 Identifiable이 아니다 — 값을 따로 들고
        // 표시 여부만 Binding으로 만든다(홈 셸의 위치 안내 시트와 같은 방식).
        .sheet(isPresented: Binding(get: { renameTarget != nil },
                                    set: { if !$0 { renameTarget = nil } })) {
            if let course = renameTarget {
                CourseRenameSheet(
                    initialName: course.title,
                    onConfirm: { name in
                        renameTarget = nil
                        Task { await rename(course: course, to: name) }
                    },
                    onCancel: { renameTarget = nil }
                )
                .runvealSheetContent()
            }
        }
        .runvealDialog(
            isPresented: deleteTarget != nil,
            onDismiss: { deleteTarget = nil }
        ) {
            RunvealDialog(
                title: TabCourseStrings.deleteDialogTitle,
                confirmLabel: TabCourseStrings.deleteDialogConfirm,
                cancelLabel: TabCourseStrings.deleteDialogCancel,
                description: TabCourseStrings.deleteDialogDescription,
                // 되돌릴 수 없는 동작이라 확인 버튼을 빨갛게 둔다.
                type: .destructive,
                onConfirm: {
                    guard let course = deleteTarget else { return }
                    deleteTarget = nil
                    Task { await delete(course: course) }
                },
                onCancel: { deleteTarget = nil }
            )
        }
    }

    /// 토스트가 떠 있는 시간 — 디자인시스템 토스트 주석의 "3초"를 그대로 따른다.
    private static let toastDuration: Duration = .seconds(3)

    /// 더보기 메뉴 — **화면 루트에서** 그린다.
    ///
    /// 카드 안에서 그리면 `clipShape`에 잘리고 러닝 시작 버튼이 위를 덮는다. 여기서 그리면 둘 다
    /// 없어지고, 바깥을 눌러 닫는 것도 같이 붙는다(Android `DropdownMenu`와 같은 동작).
    ///
    /// 자리는 아이콘 **바로 아래**다 — 아이콘을 가리지 않아야 어느 줄의 메뉴인지 보인다. 다만
    /// 목록 끝 줄처럼 아래로 띄우면 하단 플로팅 탭바에 덮이는 자리에서는 위로 뒤집는다([menuTop]).
    @ViewBuilder
    private var moreMenuOverlay: some View {
        if let target = moreMenu {
            ZStack(alignment: .topLeading) {
                // 바깥 어디를 눌러도 닫힌다. 보이지는 않지만 터치는 받아야 해서 `contentShape`를 준다.
                //
                // **`ignoresSafeArea`를 주지 않는다.** 그러면 이 ZStack이 화면 밖까지 커져서
                // 기준점(top-leading)이 위로 밀리고, 아이콘 자리를 잰 좌표와 어긋난다.
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture { moreMenu = nil }

                CourseMoreMenuContent(
                    onRenameClick: {
                        moreMenu = nil
                        renameTarget = target.course
                    },
                    // TODO: GPX 파일로 내보내기는 아직이다 — 그때까지는 "개발 중" 안내만 띄운다
                    //  (Android도 같은 상태다).
                    onExportGpxClick: {
                        moreMenu = nil
                        isNotReadyToastVisible = true
                    },
                    onDeleteClick: {
                        moreMenu = nil
                        deleteTarget = target.course
                    }
                )
                // 항목 수에 따라 높이가 달라져서 그릴 때 재 둔다 — 위로 뒤집을지 이 높이로 가린다.
                .background(
                    GeometryReader { geometry in
                        Color.clear
                            .onAppear { menuHeight = geometry.size.height }
                            .onChange(of: geometry.size.height) { _, next in menuHeight = next }
                    }
                )
                // 재기 전 한 프레임은 자리를 모른다 — 아래에 잠깐 떴다가 올라가는 게 보이지 않게 숨긴다.
                .opacity(menuHeight > 0 ? 1 : 0)
                // 오른쪽 끝을 아이콘에 맞춘다. 가로는 메뉴 폭만큼 되돌려야 오른쪽 정렬이 된다.
                .alignmentGuide(.leading) { dimension in -(target.anchor.maxX - dimension.width) }
                .alignmentGuide(.top) { _ in -menuTop(anchor: target.anchor) }
            }
        }
    }

    /// 메뉴 위쪽 모서리가 놓일 자리(좌표 공간 기준).
    ///
    /// 기본은 아이콘 **바로 아래**다. 거기로 내리면 하단 플로팅 탭바에 덮이는 줄 — 목록 끝 쪽 —
    /// 에서만 아이콘 **위로** 뒤집는다(Android `MenuBelowAnchorPositionProvider`와 같은 규칙).
    private func menuTop(anchor: CGRect) -> CGFloat {
        let below = anchor.maxY + Self.menuGap
        // 아직 못 잰 동안은 기본 자리 — 어차피 `menuHeight`가 0이면 메뉴를 숨겨 둔다.
        guard menuHeight > 0, menuSpaceHeight > 0 else { return below }
        guard below + menuHeight > menuSpaceHeight - Self.tabBarInset else { return below }
        let above = anchor.minY - Self.menuGap - menuHeight
        // 위로도 자리가 모자라면(화면이 아주 짧은 경우) 아래 그대로 둔다 — 잘려 나가는 것보단 낫다.
        return above >= 0 ? above : below
    }

    private func reload() async {
        let loaded = await Task.detached(priority: .userInitiated) {
            IosCourseList.shared.load()
        }.value
        courses = loaded
        isLoading = false
    }

    /// 이름만 바꾸고 목록을 다시 읽는다 — 썸네일은 경로로 캐시되니 이름이 바뀌어도 그대로 쓴다.
    private func rename(course: SavedCourseItem, to name: String) async {
        await Task.detached(priority: .userInitiated) {
            IosCourseList.shared.rename(courseId: course.id, name: name)
        }.value
        await reload()
    }

    /// 코스 행과 그 썸네일 캐시를 지우고 목록을 다시 읽는다(Android도 둘을 같이 지운다).
    private func delete(course: SavedCourseItem) async {
        await Task.detached(priority: .userInitiated) {
            IosCourseList.shared.delete(courseId: course.id)
        }.value
        CourseSnapshotRenderer.shared.delete(ownerId: course.id)
        await reload()
    }

    /// 코스 상세의 "이 코스로 러닝 시작"과 같은 패턴 — 기록 화면이 점선 안내선과 출발·도착 핀을
    /// 그리는 데 쓴다. 순환 코스는 도착 핀을 생략해야 해서 isLoop도 같이 넘긴다.
    private func start(course: SavedCourseItem) {
        RunvealAnalyticsLogger.logCourseStart(fromRecommend: false)
        SelectedCourseHolder.shared.publish(path: course.path, isLoop: course.isLoop)
        // 탭 화면에서는 기록 화면으로 직접 못 가므로, 홈 셸이 보는 홀더에 담아 두고 탭을 홈으로
        // 돌린 뒤 기록 흐름으로 들어간다 — Android는 탭 NavHost가 같은 그래프라 바로 navigate한다.
        RunvealPendingCourseRun.shared.request()
    }
}

/// 탭 화면에서 "바로 시작"을 눌렀을 때 홈 셸이 기록 흐름으로 넘어가게 하는 신호.
///
/// Android는 탭 화면과 기록 화면이 같은 NavHost에 있어 바로 이동하지만, iOS는 탭 콘텐츠가
/// 홈 셸 아래에 있어서 셸에 올라가 있는 내비게이터를 탭 화면이 직접 못 만진다.
@MainActor
final class RunvealPendingCourseRun: ObservableObject {
    static let shared = RunvealPendingCourseRun()

    @Published private(set) var requestId = 0

    private init() {}

    func request() {
        requestId += 1
    }
}

/// 앱바 오른쪽의 코스 추가 버튼. 지도 위 글래스 버튼과 달리 불투명 카드 배경이라 여기서 직접 그린다.
private struct CourseTopBarActionButton: View {
    let onClick: () -> Void

    var body: some View {
        let colors = RunvealTheme.colors
        return Button(action: onClick) {
            RunvealIcon(RunvealIcons.plus, tint: colors.icon.primary, size: RunvealLayout.sizeIcon)
                .frame(width: 44, height: 44)
                .background(colors.surface.default, in: RunvealShapes.full)
                .overlay { RunvealShapes.full.stroke(colors.border.default, lineWidth: 1) }
                .contentShape(RunvealShapes.full)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(TabCourseStrings.addCourse)
    }
}

/// 다음에 달려볼 코스 추천 카드 (내 코스 탭, 앱바 바로 아래). 저장한 코스가 있을 때 보여준다.
private struct CourseRecommendationCard: View {
    let badgeLabel: String
    let course: SavedCourseItem
    let menuSpace: String
    let onMoreTap: (CGRect) -> Void
    let onStartClick: () -> Void

    private static let mapHeight: CGFloat = 160

    var body: some View {
        let colors = RunvealTheme.colors
        VStack(spacing: 0) {
            ZStack(alignment: .topLeading) {
                CourseSnapshotImage(request: course.snapshotRequest)
                    .frame(maxWidth: .infinity)
                    .frame(height: Self.mapHeight)
                    .background(colors.bg.map)

                Text(badgeLabel)
                    .runvealTextStyle(RunvealTheme.typography.captionSSemibold)
                    .foregroundStyle(colors.text.accent)
                    // 10/6에 맞는 spacing 토큰이 없어 그대로 하드코딩.
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(colors.surface.raised, in: RunvealShapes.full)
                    .padding(RunvealSpacing.space12)

                RunvealShapes.full
                    .fill(colors.accent.glow)
                    .overlay { RunvealShapes.full.stroke(colors.bg.map, lineWidth: 2) }
                    .frame(width: 18, height: 18)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            }
            .frame(height: Self.mapHeight)

            VStack(spacing: RunvealSpacing.space16) {
                HStack(alignment: .top, spacing: RunvealSpacing.space12) {
                    VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                        Text(course.title)
                            .runvealTextStyle(RunvealTheme.typography.heading3)
                            .foregroundStyle(colors.text.primary)

                        VStack(alignment: .leading, spacing: RunvealSpacing.space4) {
                            HStack(spacing: RunvealSpacing.space8) {
                                Text(course.distanceLabel)
                                    .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                                    .foregroundStyle(colors.text.secondary)
                                Text(course.durationLabel)
                                    .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                                    .foregroundStyle(colors.text.secondary)
                                RunvealTag(label: course.tagLabel)
                            }
                            HStack(alignment: .top, spacing: RunvealSpacing.space8) {
                                Text(course.newRoadLabel)
                                    .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                                    .foregroundStyle(colors.text.accent)
                                Text(course.notRunLabel)
                                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                                    .foregroundStyle(colors.text.accent)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    CourseMoreMenuButton(coordinateSpace: menuSpace, onTap: onMoreTap)
                }
                .frame(maxWidth: .infinity)

                RunvealButton(
                    label: TabCourseStrings.startWithThisCourse,
                    style: .gold,
                    size: .l,
                    fillWidth: true,
                    leadingIcon: RunvealIcons.play,
                    onClick: onStartClick
                )
            }
            .padding(RunvealSpacing.space16)
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity)
        .background(colors.surface.default, in: RunvealShapes.radius24)
        .overlay { RunvealShapes.radius24.stroke(colors.border.default, lineWidth: 1) }
        .clipShape(RunvealShapes.radius24)
    }
}

/// "저장한 코스" 목록 섹션 머리 — 제목 + 정렬 기준(탭하면 정렬 메뉴).
private struct SavedCourseSectionHeader: View {
    let title: String
    let sortLabel: String
    let onSortClick: () -> Void

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(alignment: .bottom) {
            Text(title)
                .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                .foregroundStyle(colors.text.primary)
            Spacer()
            Button(action: onSortClick) {
                HStack(spacing: RunvealSpacing.space4) {
                    Text(sortLabel)
                        .runvealTextStyle(RunvealTheme.typography.bodyMSemibold)
                        .foregroundStyle(colors.text.primary)
                    RunvealIcon(RunvealIcons.expand, tint: colors.icon.primary, size: 16)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 24)
    }
}

/// 저장한 코스 목록의 한 줄 — 썸네일 + 정보 + 더보기 + 바로 시작.
private struct SavedCourseListItem: View {
    let course: SavedCourseItem
    let menuSpace: String
    let onMoreTap: (CGRect) -> Void
    /// 줄 전체를 눌렀을 때 — 위 큰 카드를 이 코스로 바꾼다.
    let onClick: () -> Void
    /// 오른쪽 원형 아이콘을 눌렀을 때 — 코스 상세로 간다.
    ///
    /// 아이콘은 재생(▶) 그대로다. 줄에서 바로 달리기 시작하던 자리였는데, 줄 탭이 "위 카드 바꾸기"가
    /// 되면서 상세로 들어갈 자리가 필요해져 이쪽으로 옮겼다 — 달리기 시작은 위 큰 카드의 같은
    /// 아이콘(`CourseRecommendationCard`)이 맡는다.
    let onDetailClick: () -> Void

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space16) {
            CourseSnapshotImage(request: course.snapshotRequest)
                .frame(width: 72, height: 72)
                .background(colors.bg.map, in: RunvealShapes.radius8)
                .overlay { RunvealShapes.radius8.stroke(colors.border.subtle, lineWidth: 1) }
                .clipShape(RunvealShapes.radius8)

            VStack(alignment: .leading, spacing: RunvealSpacing.space2) {
                Text(course.title)
                    .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                    .foregroundStyle(colors.text.primary)
                    .lineLimit(1)
                    .truncationMode(.tail)
                HStack(spacing: RunvealSpacing.space8) {
                    Text(course.distanceLabel)
                        .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                        .foregroundStyle(colors.text.primary)
                    Text(course.durationLabel)
                        .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                        .foregroundStyle(colors.text.primary)
                    RunvealTag(label: course.tagLabel)
                }
                Text(course.notRunLabel)
                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                    .foregroundStyle(colors.text.tertiary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            CourseMoreMenuButton(coordinateSpace: menuSpace, onTap: onMoreTap)

            Button(action: onDetailClick) {
                RunvealIcon(RunvealIcons.play, tint: colors.accent.default, size: 16)
                    .frame(width: 44, height: 44)
                    .background(colors.surface.default, in: RunvealShapes.full)
                    .overlay { RunvealShapes.full.stroke(colors.accent.default, lineWidth: 1) }
                    .contentShape(RunvealShapes.full)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(TabCourseStrings.courseDetail)
        }
        .padding(.vertical, RunvealSpacing.space12)
        .frame(maxWidth: .infinity)
        // 줄 전체가 눌리되 안쪽 버튼(더보기·코스 상세)이 먼저 받는다 — Button으로 감싸면 그
        // 버튼들이 묻히므로 탭 제스처를 쓴다.
        .contentShape(Rectangle())
        .onTapGesture(perform: onClick)
    }
}
