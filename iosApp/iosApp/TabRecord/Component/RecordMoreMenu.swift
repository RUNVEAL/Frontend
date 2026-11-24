import SwiftUI

/// 기록 상세 상단의 더보기(⋯) 아이콘.
///
/// 누르면 자기 자리를 [onTap]으로 알려 주기만 한다 — 메뉴는 화면 루트가 그린다. 아이콘 안에서
/// 그리면 바깥을 눌러 닫을 자리가 없고, 조상 뷰가 자르거나 나중에 그려지는 요소가 덮을 수 있다
/// (내 코스 탭의 [CourseMoreMenuButton]과 같은 구조).
struct RecordMoreMenuButton: View {
    let coordinateSpace: String
    let onTap: (CGRect) -> Void

    /// 상단 앱바 액션 자리에 들어가는 크기 — 보이는 아이콘 24, 터치 영역 48.
    private static let iconSize: CGFloat = 24
    private static let touchSize: CGFloat = 48

    /// 아이콘이 차지한 자리. 누르는 순간 읽어서 넘긴다.
    @State private var frame: CGRect = .zero

    var body: some View {
        Button {
            onTap(frame)
        } label: {
            RunvealIcon(RunvealIcons.more, tint: RunvealTheme.colors.icon.primary, size: Self.iconSize)
                .frame(width: Self.touchSize, height: Self.touchSize)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(TabRecordStrings.moreLabel)
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

/// 더보기 메뉴 내용(이름 수정·GPX 내보내기·삭제).
///
/// 모양은 디자인시스템([RunvealMenu])이 그리고, 띄울 자리와 닫기는 화면 루트가 맡는다.
struct RecordMoreMenuContent: View {
    let onRenameClick: () -> Void
    let onExportGpxClick: () -> Void
    let onDeleteClick: () -> Void

    var body: some View {
        RunvealMenu {
            RunvealMenuItem(label: TabRecordStrings.moreRename, icon: RunvealIcons.edit, onClick: onRenameClick)
            RunvealMenuItem(
                label: TabRecordStrings.moreExportGpx,
                icon: RunvealIcons.download,
                onClick: onExportGpxClick
            )
            RunvealMenuItem(
                label: TabRecordStrings.moreDelete,
                icon: RunvealIcons.delete,
                type: .danger,
                onClick: onDeleteClick
            )
        }
        .fixedSize()
    }
}
