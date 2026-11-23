import SwiftUI

/// 더보기 메뉴를 **어느 코스에 대해, 화면 어디에** 띄울지.
///
/// 메뉴를 아이콘 안에서(overlay로) 그리면 두 가지가 깨진다 — 카드의 `clipShape`에 잘리고, 같은
/// 카드에서 나중에 그려지는 요소(러닝 시작 버튼)가 메뉴 위를 덮는다. 그래서 아이콘은 **자기 자리만
/// 알려 주고**, 메뉴는 화면 루트에서 그린다(Android `DropdownMenu`가 Popup으로 떠서 잘리지 않는
/// 것과 같은 결과).
struct CourseMoreMenuTarget: Equatable {
    let course: SavedCourseItem
    /// 아이콘이 차지한 자리 — 화면 루트의 좌표 공간 기준.
    let anchor: CGRect

    static func == (lhs: CourseMoreMenuTarget, rhs: CourseMoreMenuTarget) -> Bool {
        lhs.course.id == rhs.course.id && lhs.anchor == rhs.anchor
    }
}

/// 저장한 코스 카드·목록 줄의 더보기(⋯) 아이콘.
///
/// 누르면 자기 자리를 [onTap]으로 알려 주기만 한다 — 메뉴를 그리는 건 화면 루트의 일이다.
struct CourseMoreMenuButton: View {
    let coordinateSpace: String
    let onTap: (CGRect) -> Void

    /// 아이콘이 차지한 자리. 스크롤하면 바뀌므로 계속 따라간다.
    @State private var frame: CGRect = .zero

    var body: some View {
        Button {
            onTap(frame)
        } label: {
            RunvealIcon(RunvealIcons.more, tint: RunvealTheme.colors.icon.tertiary)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(TabCourseStrings.more)
        .background(
            GeometryReader { geometry in
                Color.clear
                    .onAppear { frame = geometry.frame(in: .named(coordinateSpace)) }
                    .onChange(of: geometry.frame(in: .named(coordinateSpace))) { _, next in
                        frame = next
                    }
            }
        )
    }
}

/// 더보기 메뉴 내용(이름 수정·GPX 내보내기·삭제). Android `CourseMoreMenuActions`와 같은 구성.
///
/// 모양은 디자인시스템([RunvealMenu])이 그리고, 띄울 자리와 닫기는 화면 루트가 맡는다.
struct CourseMoreMenuContent: View {
    let onRenameClick: () -> Void
    let onExportGpxClick: () -> Void
    let onDeleteClick: () -> Void

    var body: some View {
        RunvealMenu {
            RunvealMenuItem(label: TabCourseStrings.moreRename, icon: RunvealIcons.edit, onClick: onRenameClick)
            RunvealMenuItem(
                label: TabCourseStrings.moreExportGpx,
                icon: RunvealIcons.download,
                onClick: onExportGpxClick
            )
            RunvealMenuItem(
                label: TabCourseStrings.moreDelete,
                icon: RunvealIcons.delete,
                type: .danger,
                onClick: onDeleteClick
            )
        }
        .fixedSize()
    }
}

/// 코스 이름을 바꾸는 바텀시트. Android `CourseRenameSheet`.
struct CourseRenameSheet: View {
    let initialName: String
    let onConfirm: (String) -> Void
    let onCancel: () -> Void

    @State private var name: String

    init(initialName: String, onConfirm: @escaping (String) -> Void, onCancel: @escaping () -> Void) {
        self.initialName = initialName
        self.onConfirm = onConfirm
        self.onCancel = onCancel
        _name = State(initialValue: initialName)
    }

    /// 공백만 남으면 저장할 수 없다 — 데이터 쪽(`IosCourseList.rename`)도 같은 조건으로 무시한다.
    private var isNameBlank: Bool {
        name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        RunvealBottomSheet {
            RunvealBottomSheetHeader(type: .title, title: TabCourseStrings.renameSheetTitle)
        } content: {
            RunvealTextInput(
                text: $name,
                label: TabCourseStrings.renameInputLabel,
                placeholder: TabCourseStrings.renameInputPlaceholder
            )
        } footer: {
            RunvealButtonGroup {
                RunvealButtonGroupEqual(
                    firstLabel: TabCourseStrings.renameCancel,
                    onFirstClick: onCancel,
                    secondLabel: TabCourseStrings.renameConfirm,
                    onSecondClick: { onConfirm(name) },
                    enabled: !isNameBlank
                )
            }
        }
    }
}
