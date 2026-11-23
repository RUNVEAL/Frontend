import SwiftUI

/**
 저장한 코스 카드·목록 줄의 더보기(⋯) 아이콘 + 눌렀을 때 그 아래에 뜨는 메뉴
 (이름 수정·GPX 내보내기·삭제). Android `CourseMoreMenuActions`가 하는 일과 같다.

 메뉴 모양은 디자인시스템([RunvealMenu])이 그리고 여기서는 띄울 자리와 닫기만 맡는다. 항목을 고르면
 메뉴를 먼저 닫고 콜백을 부른다 — 부르는 쪽이 매번 닫아 주지 않아도 되게(기록 탭의
 [RecordMoreMenuIcon]과 같은 구조).
 */
struct CourseMoreMenuIcon: View {
    let onRenameClick: () -> Void
    let onExportGpxClick: () -> Void
    let onDeleteClick: () -> Void

    /// 아이콘과 메뉴 사이 간격.
    private static let menuGap: CGFloat = 4

    @State private var isMenuOpen = false

    var body: some View {
        Button {
            isMenuOpen = true
        } label: {
            RunvealIcon(RunvealIcons.more, tint: RunvealTheme.colors.icon.tertiary)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(TabCourseStrings.more)
        // 아이콘 바로 아래, 오른쪽 끝을 맞춰 띄운다 — 줄 오른쪽에 있는 버튼이라 왼쪽으로 펼친다.
        .overlay(alignment: .topTrailing) {
            if isMenuOpen {
                menu
                    .alignmentGuide(.top) { $0[.bottom] + Self.menuGap }
            }
        }
    }

    private var menu: some View {
        RunvealMenu {
            RunvealMenuItem(label: TabCourseStrings.moreRename, icon: RunvealIcons.edit) {
                isMenuOpen = false
                onRenameClick()
            }
            RunvealMenuItem(label: TabCourseStrings.moreExportGpx, icon: RunvealIcons.download) {
                isMenuOpen = false
                onExportGpxClick()
            }
            RunvealMenuItem(label: TabCourseStrings.moreDelete, icon: RunvealIcons.delete, type: .danger) {
                isMenuOpen = false
                onDeleteClick()
            }
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

    /// 공백만 남으면 저장할 수 없다 — 데이터 쪽([IosCourseList.rename])도 같은 조건으로 무시한다.
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
