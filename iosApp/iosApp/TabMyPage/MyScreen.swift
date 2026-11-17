import SwiftUI

/**
 "MY" 탭 — Android `MyScreen`의 **약관 및 정책 섹션까지만** 옮겼다.

 프로필 카드·기록 불러오기(Health)·위치 권한 토글·계정 섹션은 아직 없다. 이 섹션을 먼저 옮긴 건
 **오픈소스 라이선스 고지가 법적 의무**이기 때문이다 — 지도 데이터가 OpenStreetMap(ODbL)이라
 저작자 표시를 해야 하는데 지도 화면에서는 MapLibre 기본 표시를 꺼 두었고(`RunvealMap`),
 그 고지가 앱 안 어딘가에는 반드시 있어야 한다(`docs/map-stack/README.md` §4).
 */
struct MyScreen: View {
    /// 약관 한 건을 열 때 — 전체 화면이라 탭바를 숨겨야 해서 호출부(`RunvealHomeShell`)가 띄운다.
    let onTermsItemClick: (TermsPolicyItem) -> Void

    var body: some View {
        ZStack(alignment: .top) {
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    RunvealTopAppBarLarge(title: MyStrings.title, metaLabel: "")

                    termsPolicySection
                        .padding(.horizontal, RunvealLayout.marginScreen)
                }
                // 하단 탭바는 RunvealHomeShell이 이 화면 위에 띄우므로, 목록이 그 뒤로 들어가지
                // 않도록 높이만큼 아래 여백을 둔다.
                .padding(.bottom, RunvealLayout.sizeTabBar + RunvealSpacing.space24)
            }
        }
    }

    private var termsPolicySection: some View {
        VStack(spacing: 0) {
            RunvealSectionHeader(title: MyStrings.termsPolicyTitle)

            ForEach(TermsPolicyItem.allCases, id: \.self) { item in
                RunvealListItem(
                    title: item.label,
                    trailing: .chevron,
                    onClick: { onTermsItemClick(item) }
                )
            }

            // 앱 버전은 눌러서 여는 줄이 아니라 값을 보여주는 줄이다 — chevron 대신 값을 오른쪽에 둔다.
            RunvealListItem(
                title: MyStrings.termsPolicyAppVersion,
                trailing: .textButton,
                trailingButtonLabel: Self.appVersionName,
                onClick: {}
            )
        }
        .frame(maxWidth: .infinity)
    }

    /// `Config.xcconfig`의 `MARKETING_VERSION` — Android `versionName`에 해당한다.
    /// 빌드 번호(`CURRENT_PROJECT_VERSION`)는 CI가 매 실행 올리는 값이라 보여주지 않는다.
    private static var appVersionName: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
    }
}

#Preview {
    MyScreen(onTermsItemClick: { _ in })
}
