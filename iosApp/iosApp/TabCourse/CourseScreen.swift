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
}

/// "내 코스" 탭 — Android `CourseScreen` + `CourseListViewModel`에 해당한다.
///
/// 탭을 나갔다 들어올 때마다 다시 불러온다 — 그 사이 새로 저장한 코스가 반영돼야 한다.
struct CourseScreen: View {
    let onCreateCourseClick: () -> Void

    @State private var courses: [SavedCourseItem] = []
    @State private var isLoading = true

    var body: some View {
        let colors = RunvealTheme.colors
        ZStack(alignment: .top) {
            colors.bg.canvas.ignoresSafeArea()

            // 화면 맨 위부터 깔리는 히어로 이미지 — 원본 비율(1920:544)대로 보여준다.
            RunvealImages.myCourse
                .resizable()
                .aspectRatio(1920.0 / 544.0, contentMode: .fill)
                .frame(maxWidth: .infinity)
                .clipped()
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

                    if let featured = courses.first {
                        CourseRecommendationCard(
                            badgeLabel: TabCourseStrings.recommendBadge,
                            course: featured,
                            onMoreClick: {},
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
                                    onMoreClick: {},
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
        }
        .task { await reload() }
    }

    private func reload() async {
        let loaded = await Task.detached(priority: .userInitiated) {
            IosCourseList.shared.load()
        }.value
        courses = loaded
        isLoading = false
    }

    /// 코스 상세의 "이 코스로 러닝 시작"과 같은 패턴 — 기록 화면이 점선 안내선과 출발·도착 핀을
    /// 그리는 데 쓴다. 순환 코스는 도착 핀을 생략해야 해서 isLoop도 같이 넘긴다.
    private func start(course: SavedCourseItem) {
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
    let onMoreClick: () -> Void
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

                    Button(action: onMoreClick) {
                        RunvealIcon(RunvealIcons.more, tint: colors.icon.tertiary)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(TabCourseStrings.more)
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
    let onMoreClick: () -> Void
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

            Button(action: onMoreClick) {
                RunvealIcon(RunvealIcons.more, tint: colors.icon.tertiary)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(TabCourseStrings.more)

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
    }
}
