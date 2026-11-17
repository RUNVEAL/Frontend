import SharedLogic
import SwiftUI

/**
 "MY" 탭 — Android `MyScreen`과 같은 구성(프로필 카드 → 설정 → 약관 및 정책).

 아직 없는 것:
 - **닉네임·계정 섹션** — 로그인 없이 나가는 1차 출시라 Android도 숨겨 뒀다(그쪽 TODO 참고).
 - **기록 불러오기** — Android는 Health Connect로 가져오지만 iOS 대응(HealthKit)이 아직 없다.
   HealthKit은 App ID Capability와 `NSHealthShareUsageDescription`이 함께 필요해서, 넣기로
   정해지면 그때 줄을 되살린다. 없는 기능을 줄로 띄워 두지는 않는다.
 */
struct MyScreen: View {
    @ObservedObject var permissionState: RunvealLocationPermissionState

    /// 약관 한 건을 열 때 — 전체 화면이라 탭바를 숨겨야 해서 호출부(`RunvealHomeShell`)가 띄운다.
    let onTermsItemClick: (TermsPolicyItem) -> Void

    @State private var summary: MyRunningSummary?

    var body: some View {
        ZStack(alignment: .top) {
            RunvealTheme.colors.bg.canvas.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    RunvealTopAppBarLarge(title: MyStrings.title, metaLabel: "")

                    VStack(spacing: RunvealSpacing.space32) {
                        ProfileSummaryCard(
                            exploredDongCount: Int(summary?.exploredDongCount ?? 0),
                            totalDistanceLabel: summary?.totalDistanceLabel ?? "0.00"
                        )

                        VStack(spacing: RunvealSpacing.space24) {
                            settingsSection
                            termsPolicySection
                        }
                    }
                    .padding(.horizontal, RunvealLayout.marginScreen)
                }
                // 하단 탭바는 RunvealHomeShell이 이 화면 위에 띄우므로, 목록이 그 뒤로 들어가지
                // 않도록 높이만큼 아래 여백을 둔다.
                .padding(.bottom, RunvealLayout.sizeTabBar + RunvealSpacing.space24)
            }
        }
        .task {
            // 집계는 전국 구간 인덱스를 훑어서 메인 스레드에서 돌리면 탭 전환이 걸린다.
            summary = await Task.detached(priority: .userInitiated) {
                IosMyPageData.shared.loadRunningSummary()
            }.value
        }
    }

    // MARK: - 설정

    private var settingsSection: some View {
        VStack(spacing: 0) {
            RunvealSectionHeader(title: MyStrings.sectionSettings)

            // 위치 권한만 chevron 대신 토글이라, 줄 전체와 스위치가 같은 동작을 하도록 겹쳐 둔다.
            //
            // 토글은 상태를 **비추기만** 한다: 눌러도 그 자리에서 뒤집히지 않고 권한 다이얼로그나
            // 설정 화면으로 간다. 거기서 바뀐 결과는 `RunvealLocationPermissionState`가 앱으로
            // 돌아올 때 다시 읽어 반영하므로, 돌아오면 토글이 실제 권한에 맞춰져 있다.
            ZStack(alignment: .trailing) {
                RunvealListItem(
                    title: MyStrings.menuLocationPermission,
                    description: permissionState.isGranted
                        ? MyStrings.menuLocationPermissionGranted
                        : MyStrings.menuLocationPermissionDenied,
                    onClick: onLocationPermissionClick
                )
                RunvealToggleSwitch(
                    checked: permissionState.isGranted,
                    onCheckedChange: { _ in onLocationPermissionClick() }
                )
                .padding(.trailing, RunvealSpacing.space12)
            }
        }
        .frame(maxWidth: .infinity)
    }

    /// 앱은 이미 허용된 권한을 **끌 수 없다** — 끄려면 시스템 설정으로 보내야 한다. 거부가 굳어져
    /// 시스템이 더 이상 다이얼로그를 안 띄우는 상태도 마찬가지다. 그 두 경우만 설정으로 보내고,
    /// 나머지(미요청)는 그 자리에서 권한 다이얼로그를 띄운다. Android와 같은 분기다.
    private func onLocationPermissionClick() {
        if permissionState.isGranted || permissionState.isPermanentlyDenied {
            permissionState.openSettings()
        } else {
            permissionState.request()
        }
    }

    // MARK: - 약관 및 정책

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
    MyScreen(permissionState: RunvealLocationPermissionState(), onTermsItemClick: { _ in })
}
