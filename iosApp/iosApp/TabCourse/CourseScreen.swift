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
    // 저장한 코스를 다 달렸을 때 — "다음에 달려볼 코스" 카드 자리에 새 코스를 권한다.
    static let allRunTitle = "저장한 코스를 모두 달렸어요"
    static let allRunDescription = "새로운 길로 코스를 만들어볼까요?"
    static let allRunAction = "코스 만들기"
    static let notReady = "아직 개발 중입니다"
}

/// "내 코스" 탭 — Android `CourseScreen` + `CourseListViewModel`에 해당한다.
///
/// 탭을 나갔다 들어올 때마다 다시 불러온다 — 그 사이 새로 저장한 코스가 반영돼야 한다.
struct CourseScreen: View {
    let onCreateCourseClick: () -> Void
    /// 줄 전체를 누르면 코스 상세로 — 값은 id만 넘기고 화면이 로컬 DB에서 다시 읽는다.
    /// 줄 오른쪽 아이콘은 그 코스로 **바로 시작**이라 화면 안에서 끝낸다([start]).
    let onCourseClick: (SavedCourseItem) -> Void

    @State private var courses: [SavedCourseItem] = []
    @State private var isLoading = true
    /// 더보기에서 고른 코스 — 이름 수정 시트/삭제 다이얼로그가 어떤 코스인지 들고 있는다.
    @State private var renameTarget: SavedCourseItem?
    @State private var deleteTarget: SavedCourseItem?
    @State private var isNotReadyToastVisible = false
    /// 떠 있는 더보기 메뉴 — 어느 코스의 어느 자리인지. nil이면 안 떠 있다.
    @State private var moreMenu: CourseMoreMenuTarget?

    /**
     "다음에 달려볼 코스" 카드에 띄울 코스 — **아직 안 달린** 코스 중 가장 최근에 저장한 것.

     [courses]가 이미 저장 최신순이라 앞에서부터 처음 걸리는 안 달린 코스가 그 코스다. 전부
     달렸으면 nil — 그때는 카드 자리에 새 코스를 만들라고 권하는 안내를 띄운다.
     */
    private var featuredCourse: SavedCourseItem? { courses.first { !$0.hasRun } }

    /// 더보기 아이콘 자리를 재는 기준 좌표 공간 — 화면 루트에 이름을 붙여 둔다.
    private static let menuSpace = "course-more-menu"
    /// 아이콘과 메뉴 사이 간격. 아이콘을 가리지 않게 **아래로** 띄운다.
    private static let menuGap: CGFloat = 4

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

                    // 카드 조건과 목록 조건을 **나눈다** — 전부 달려서 띄울 코스가 없을 때도
                    // (featuredCourse == nil) 저장한 코스 목록 자체는 그대로 보여야 한다.
                    if !courses.isEmpty {
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
                        } else {
                            // 저장한 코스를 다 달렸다 — 카드 자리에 새 코스를 만들라고 권한다.
                            // TODO: 전용 시안이 나오면 교체한다. 지금은 빈 상태 컴포넌트를 그대로 쓴다.
                            RunvealEmptyState(
                                title: TabCourseStrings.allRunTitle,
                                description: TabCourseStrings.allRunDescription,
                                actionLabel: TabCourseStrings.allRunAction,
                                onActionClick: onCreateCourseClick
                            )
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, RunvealLayout.marginScreen)
                        }

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
                                    onClick: { onCourseClick(course) },
                                    onStartClick: { start(course: course) }
                                )
                            }
                        }
                        .padding(.horizontal, RunvealLayout.marginScreen)
                    }
                }
                .frame(maxWidth: .infinity)
                // 하단 플로팅 탭바에 가리지 않게 — 탭바 높이 + 위아래 여백만큼 띄운다.
                .padding(.bottom, RunvealLayout.sizeTabBar + RunvealSpacing.space24)
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
                // 시안은 토스트를 **하단**에 둔다(`Overlay/Toast`: "하단 GNB(또는 하단 버튼) 위 8").
                // 이 화면은 탭바가 떠 있어서 그 높이만큼 더 띄운다(Android `bottomInset`과 같다).
                VStack {
                    Spacer()
                    RunvealToast(message: TabCourseStrings.notReady)
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.bottom, RunvealLayout.sizeTabBar + RunvealSpacing.space8)
                }
            }

            moreMenuOverlay
        }
        // 더보기 아이콘이 자기 자리를 재는 기준 — 메뉴를 띄우는 자리와 같은 공간이어야 한다.
        .coordinateSpace(name: Self.menuSpace)
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
    /// 자리는 아이콘 **바로 아래**다 — 아이콘을 가리지 않아야 어느 줄의 메뉴인지 보인다.
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
                // 오른쪽 끝을 아이콘에 맞추고, 아이콘 아래로 내린다. 가로는 메뉴 폭만큼 되돌려야
                // 오른쪽 정렬이 된다.
                .alignmentGuide(.leading) { dimension in -(target.anchor.maxX - dimension.width) }
                .alignmentGuide(.top) { _ in -(target.anchor.maxY + Self.menuGap) }
            }
        }
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
        // 저장된 코스라 id가 있다 — 러닝이 끝나 기록이 저장되면 이 코스를 "달림"으로 적는다.
        SelectedCourseHolder.shared.publish(path: course.path, isLoop: course.isLoop, courseId: course.id)
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
    /// 줄 전체를 눌렀을 때 — 코스 상세로 간다.
    let onClick: () -> Void
    /// 오른쪽 재생(▶) 아이콘을 눌렀을 때 — 이 코스로 바로 러닝을 시작한다.
    let onStartClick: () -> Void

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

            Button(action: onStartClick) {
                RunvealIcon(RunvealIcons.play, tint: colors.accent.default, size: 16)
                    .frame(width: 44, height: 44)
                    .background(colors.surface.default, in: RunvealShapes.full)
                    .overlay { RunvealShapes.full.stroke(colors.accent.default, lineWidth: 1) }
                    .contentShape(RunvealShapes.full)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(TabCourseStrings.startNow)
        }
        .padding(.vertical, RunvealSpacing.space12)
        .frame(maxWidth: .infinity)
        // 줄 전체가 눌리되 안쪽 버튼(더보기·바로 시작)이 먼저 받는다 — Button으로 감싸면 그
        // 버튼들이 묻히므로 탭 제스처를 쓴다.
        .contentShape(Rectangle())
        .onTapGesture(perform: onClick)
    }
}
