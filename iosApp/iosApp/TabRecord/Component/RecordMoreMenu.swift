import SwiftUI

/**
 기록 상세 상단의 더보기(⋯) 아이콘 + 눌렀을 때 그 아래에 뜨는 메뉴(이름 수정·GPX 내보내기·삭제).
 Android `RecordMoreMenuIcon`.

 메뉴 모양은 디자인시스템([RunvealMenu])이 그리고 여기서는 띄울 자리와 닫기만 맡는다. 항목을 고르면
 메뉴를 먼저 닫고 콜백을 부른다 — 부르는 쪽이 매번 닫아 주지 않아도 되게.
 */
struct RecordMoreMenuIcon: View {
    let onRenameClick: () -> Void
    let onExportGpxClick: () -> Void
    let onDeleteClick: () -> Void

    /// 상단 앱바 액션 자리에 들어가는 크기 — 보이는 아이콘 24, 터치 영역 48.
    private static let iconSize: CGFloat = 24
    private static let touchSize: CGFloat = 48
    /// 아이콘과 메뉴 사이 간격.
    private static let menuGap: CGFloat = 4

    @State private var isMenuOpen = false

    var body: some View {
        Button {
            isMenuOpen = true
        } label: {
            RunvealIcon(RunvealIcons.more, tint: RunvealTheme.colors.icon.primary, size: Self.iconSize)
                .frame(width: Self.touchSize, height: Self.touchSize)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(TabRecordStrings.moreLabel)
        // 아이콘 바로 아래, 오른쪽 끝을 맞춰 띄운다 — 앱바 오른쪽 끝에 있는 버튼이라 왼쪽으로 펼친다.
        .overlay(alignment: .topTrailing) {
            if isMenuOpen {
                menu
                    .alignmentGuide(.top) { $0[.bottom] + Self.menuGap }
            }
        }
    }

    private var menu: some View {
        RunvealMenu {
            RunvealMenuItem(label: TabRecordStrings.moreRename, icon: RunvealIcons.edit) {
                isMenuOpen = false
                onRenameClick()
            }
            RunvealMenuItem(label: TabRecordStrings.moreExportGpx, icon: RunvealIcons.download) {
                isMenuOpen = false
                onExportGpxClick()
            }
            RunvealMenuItem(label: TabRecordStrings.moreDelete, icon: RunvealIcons.delete, type: .danger) {
                isMenuOpen = false
                onDeleteClick()
            }
        }
        .fixedSize()
    }
}
