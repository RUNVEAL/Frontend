import SwiftUI

/**
 약관 한 건의 본문 — Android `TermsDetailScreen`과 같다. 상단 제목은 목록에서 누른 약관 이름을
 그대로 쓴다.

 본문은 두 가지다:
 - **웹에 올려 둔 약관**(``TermsPolicyItem/url``) — 웹뷰로 띄운다. 문구가 바뀌어도 앱을 다시
   배포하지 않아도 되고, 두 플랫폼이 같은 원문을 본다(`TermsDocument`).
 - **오픈소스 라이선스** — 앱이 직접 그린다. 서버에서 받아올 성질이 아니라 빌드에 들어간
   의존성에서 나오는 값이기 때문이다.
 */
struct TermsDetailScreen: View {
    let item: TermsPolicyItem
    let onBack: () -> Void

    var body: some View {
        ZStack(alignment: .top) {
            // 배경을 불투명하게 깔아야 뒤의 지도가 비치지 않는다 — 탭 안에서 들어가지만 그 자체로
            // 전체 화면이다.
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()

            VStack(spacing: 0) {
                RunvealTopAppBar(leading: .back, title: item.label, onLeadingClick: onBack)

                if let url = item.url {
                    // 웹뷰는 스스로 스크롤하므로 ScrollView로 감싸지 않는다 — 감싸면 높이 제약이
                    // 없어져 페이지 전체가 펼쳐지고 스크롤이 두 겹이 된다.
                    RunvealWebView(url: url)
                } else {
                    ScrollView {
                        OpenSourceLicenseContent()
                            .padding(.horizontal, RunvealLayout.marginScreen)
                            .padding(.top, RunvealSpacing.space24)
                    }
                }
            }
        }
    }
}

/**
 오픈소스 고지. 지도를 맨 위에 따로 둔 건 **표시 의무가 있기 때문**이다 — 지도 데이터가
 OpenStreetMap(ODbL)이라 저작자 표시를 해야 하는데, 지도 화면에서는 MapLibre 기본 표시를 꺼 두었다
 (`RunvealMap`의 `attributionButton.isHidden = true`). 그 고지가 사는 자리가 여기다.

 자세한 근거는 `docs/map-stack/README.md` §4.
 */
private struct OpenSourceLicenseContent: View {
    var body: some View {
        VStack(alignment: .leading, spacing: RunvealSpacing.space24) {
            LicenseSection(title: MyStrings.ossMapSection) {
                LicenseRow(name: MyStrings.ossMapData, license: MyStrings.ossMapDataLicense)
                LicenseRow(name: MyStrings.ossMapTiles, license: MyStrings.ossMapTilesLicense)
            }

            LicenseSection(title: MyStrings.ossLibrarySection) {
                ForEach(openSourceLicenses, id: \.self) { entry in
                    LicenseRow(name: entry.name, license: entry.license)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, RunvealSpacing.space32)
    }
}

private struct LicenseSection<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: RunvealSpacing.space12) {
            Text(title)
                .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                .foregroundStyle(RunvealTheme.colors.text.primary)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// 이름은 줄바꿈될 수 있고 라이선스 이름은 한 줄로 붙여 둔다 — 길이가 제각각이라 양끝 정렬은 안 쓴다.
private struct LicenseRow: View {
    let name: String
    let license: String

    var body: some View {
        VStack(alignment: .leading, spacing: RunvealSpacing.space2) {
            Text(name)
                .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                .foregroundStyle(RunvealTheme.colors.text.secondary)
            Text(license)
                .runvealTextStyle(RunvealTheme.typography.captionMRegular)
                .foregroundStyle(RunvealTheme.colors.text.tertiary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#Preview {
    TermsDetailScreen(item: .openSource, onBack: {})
}
