import SwiftUI
import UIKit

/// `.sheet`으로 띄우는 바텀시트의 공통 설정.
///
/// **시트 높이를 내용에 맞춘다.** [RunvealBottomSheet]는 내용만큼만 높이를 갖는데, `.sheet`은
/// detent가 정한 높이(지정이 없으면 화면 전체)를 그대로 잡는다. 그러면 카드 위쪽에 **보이지 않는
/// 시트 영역**이 남아서,
/// - 카드가 바닥에서 떠 보이고,
/// - 그 영역이 시트의 일부라 뒤(지도)를 눌러도 아무 일도 안 일어나며,
/// - 시스템 딤(scrim)이 생기지 않아 바깥을 눌러 닫을 수도 없다.
///
/// 높이를 내용에 맞추면 보이는 카드가 곧 시트가 된다. Android `ModalBottomSheet(dragHandle = null,
/// skipPartiallyExpanded = true)`와 같은 모양이다 — 중간 단계가 없으니 핸들로 크기를 "조절"하는
/// 동작은 양쪽 다 없고, 핸들은 카드가 직접 그리는 표시다.
///
/// **끌어서 움직이는 것도 막는다**([SheetDragDisabler]) — 닫기는 바깥 누르기와 시트 안 버튼이 맡는다.
/// 그래도 들리는 경우를 대비해 바닥에 드러나는 띠를 시트 색으로 채워 둔다([SheetBottomFiller]) —
/// 들린 게 "시트가 길어진 것"처럼 보여야 한다.
extension View {
    func runvealSheetContent() -> some View {
        modifier(RunvealSheetContentModifier())
    }
}

private struct RunvealSheetContentModifier: ViewModifier {
    /// 잰 내용 높이 — 시트 높이를 여기에 맞춘다. 재기 전(0)에는 잠깐 절반 높이로 띄운다.
    @State private var contentHeight: CGFloat = 0

    func body(content: Content) -> some View {
        content
            // 제안 높이와 무관하게 **내용의 원래 높이**를 쓰게 한다 — 그래야 detent를 따라
            // 늘었다 줄었다 하지 않고 한 번에 맞는다.
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity)
            .background(
                GeometryReader { geometry in
                    Color.clear
                        .onAppear { contentHeight = geometry.size.height }
                        .onChange(of: geometry.size.height) { _, height in contentHeight = height }
                }
            )
            .presentationDetents(contentHeight > 0 ? [.height(contentHeight)] : [.medium])
            // 쓸어올림을 "시트 키우기"가 아니라 "내용 스크롤"로 돌린다. 여기엔 커질 detent가 없어
            // 이것만으로 달라지는 건 없지만, 나중에 스크롤되는 내용이 들어와도 시트가 커지지 않게
            // 뜻을 박아 둔다 — Android `skipPartiallyExpanded = true`와 같은 자리다.
            .presentationContentInteraction(.scrolls)
            .presentationBackground(.clear)
            .presentationDragIndicator(.hidden)
            // `.scrolls`는 **detent를 키우는 것**만 막는다 — 시트를 통째로 끄는 제스처는 따로 끈다.
            .background(SheetDragDisabler())
            // 그래도 들리는 경우(제스처를 못 찾았을 때)를 대비해 바닥 띠를 시트 색으로 채워 둔다.
            .background(SheetBottomFiller())
    }
}

/// 시트를 끌어 움직이는 제스처를 끈다.
///
/// `.sheet`의 드래그는 SwiftUI가 아니라 UIKit `UISheetPresentationController`가 presented view에
/// 달아 둔 pan 제스처가 맡는다. detent가 하나뿐이라 "커질 자리"는 없지만 **끌리는 것 자체는 막지
/// 못해서**, 위로 끌면 카드가 들렸다 돌아온다. SwiftUI에는 이걸 끄는 수단이 없어서 그 뷰를 찾아
/// 직접 끈다.
///
/// 끌어내려 닫기도 같이 사라진다 — 대신 **바깥 누르기**(시스템 딤)와 시트 안 취소/저장 버튼이
/// 그대로 닫아 준다. 손잡이는 원래도 카드가 직접 그리는 표시일 뿐이라 하는 일이 줄지 않는다.
private struct SheetDragDisabler: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        // 자리만 차지하지 않으면 되는 빈 뷰다 — 터치는 뒤(시트 내용)로 흘려보낸다.
        view.isUserInteractionEnabled = false
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        // 붙는 순간에는 프레젠테이션이 아직 안 끝나 제스처가 없다 — 한 턴 뒤에 찾는다.
        // 시트가 다시 뜰 때마다 들어오므로 매번 다시 끈다.
        DispatchQueue.main.async {
            uiView.presentedSheetView?
                .gestureRecognizers?
                .compactMap { $0 as? UIPanGestureRecognizer }
                .forEach { $0.isEnabled = false }
        }
    }
}

/// 시트 아래 바닥을 시트와 같은 색으로 채운다 — [SheetDragDisabler]가 막지 못하고 들렸을 때의 대비다.
///
/// 손잡이를 위로 끌면 시트가 들리면서 **시트와 화면 바닥 사이**가 드러난다. 거기는 시트 프레임
/// 바깥이라 시트가 직접 그릴 수 없고(둥근 모서리 마스크에 잘린다), 뒤 화면이 그려 봐야 시스템
/// 딤에 덮여 색이 안 맞는다. 그래서 **시트를 담고 있는 컨테이너**에 같은 색 띠를 깔고 시트보다
/// 뒤(z)에 둔다 — 쉴 때는 시트에 완전히 가려지고, 끌어올린 만큼만 드러난다.
private struct SheetBottomFiller: UIViewRepresentable {
    /// 끌어올릴 수 있는 거리(고무줄)보다 넉넉히 — 더 길어도 평소엔 시트에 가려 보이지 않는다.
    private static let height: CGFloat = 400
    /// 같은 시트에 띠를 두 번 깔지 않으려고 붙여 두는 표식.
    private static let tag = 0x52_56_42_46

    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        // 자리만 차지하지 않으면 되는 빈 뷰다 — 터치는 뒤(시트 내용)로 흘려보낸다.
        view.isUserInteractionEnabled = false
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        // 붙는 순간에는 프레젠테이션이 아직 안 끝나 컨테이너가 없다 — 한 턴 뒤에 찾는다.
        DispatchQueue.main.async {
            guard let sheet = uiView.presentedSheetView,
                  let container = sheet.superview,
                  container.viewWithTag(Self.tag) == nil else { return }

            let filler = UIView()
            filler.tag = Self.tag
            filler.backgroundColor = UIColor(RunvealTheme.colors.surface.raised)
            filler.isUserInteractionEnabled = false
            filler.translatesAutoresizingMaskIntoConstraints = false
            // 시트 **바로 뒤**에 넣는다 — 딤보다는 앞이라 색이 흐려지지 않고, 시트보다는 뒤라
            // 쉴 때는 통째로 가려진다.
            container.insertSubview(filler, belowSubview: sheet)
            NSLayoutConstraint.activate([
                filler.leadingAnchor.constraint(equalTo: container.leadingAnchor),
                filler.trailingAnchor.constraint(equalTo: container.trailingAnchor),
                filler.bottomAnchor.constraint(equalTo: container.bottomAnchor),
                filler.heightAnchor.constraint(equalToConstant: Self.height),
            ])
        }
    }
}

private extension UIView {
    /// 이 뷰가 올라타 있는 시트의 presented view — 끌어올리면 이 뷰가 통째로 움직인다.
    ///
    /// 응답 체인을 타고 자기를 들고 있는 뷰 컨트롤러를 찾고(SwiftUI는 `UIHostingController`),
    /// 프레젠테이션의 주인은 그 컨트롤러의 맨 위 부모라 거기까지 올라간 뒤 물어본다.
    /// 못 찾으면 nil — 띠를 못 깔 뿐 시트는 그대로 뜬다.
    var presentedSheetView: UIView? {
        let owner = sequence(first: self as UIResponder) { $0.next }
            .compactMap { $0 as? UIViewController }
            .first
        guard var root = owner else { return nil }
        while let parent = root.parent { root = parent }
        return root.presentationController?.presentedView
    }
}
