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

    // ⚠️ 테스트용 — 샘플 GPX 넣기. 번들 에셋을 읽어야 해서 데이터 레이어가 아니라 화면이 맡는다.
    // 수천 점을 도로 인덱스에 맞춰 보느라 몇 초 걸리므로, 끝날 때까지 다시 눌러도 무시한다.
    @State private var isImportingSampleGpx = false
    @State private var sampleGpxToast: String?

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

            // ⚠️ 테스트용 — 샘플 GPX 넣기 결과 안내. 머지 전에 지울 것.
            if let sampleGpxToast {
                VStack {
                    RunvealToast(message: sampleGpxToast)
                        .padding(.horizontal, RunvealLayout.marginScreen)
                        .padding(.vertical, RunvealSpacing.space16)
                    Spacer()
                }
            }
        }
        .task {
            // 집계는 전국 구간 인덱스를 훑어서 메인 스레드에서 돌리면 탭 전환이 걸린다.
            summary = await Task.detached(priority: .userInitiated) {
                IosMyPageData.shared.loadRunningSummary()
            }.value
        }
        // 토스트 자체는 타이머를 모르는 순수 컴포넌트라 사라지는 건 여기서 재운다.
        .task(id: sampleGpxToast) {
            guard sampleGpxToast != nil else { return }
            try? await Task.sleep(for: .seconds(3))
            sampleGpxToast = nil
        }
    }

    // MARK: - 테스트용 샘플 GPX

    /// ⚠️ **테스트용** — 번들에 넣어 둔 UT 샘플 GPX를 로컬 기록으로 저장한다(`IosUtSampleGpx`).
    /// 머지 전에 이 함수와 그 파일, 번들의 `ut-gpx` 에셋을 지울 것.
    ///
    /// 번들 읽기는 여기서 하고(경로를 아는 건 `RunvealBundleAsset` 쪽이다), 저장·매칭·적립은 전부
    /// Kotlin이 맡는다 — 러닝 종료와 같은 코드를 타야 넣은 기록이 실제로 달린 기록과 똑같이 취급된다.
    private func importSampleGpx() {
        guard !isImportingSampleGpx else { return }
        isImportingSampleGpx = true

        Task {
            let gpxByFileName = Self.loadSampleGpx()
            let hierarchy = (try? RunvealBundleAsset.text(RunvealBundleAsset.koreaRegionHierarchy)) ?? ""
            if gpxByFileName.isEmpty || hierarchy.isEmpty {
                isImportingSampleGpx = false
                sampleGpxToast = MyStrings.sampleGpxFailedToast
                return
            }

            // 수천 점을 도로 인덱스에 맞춰 보느라 몇 초 걸린다 — 메인 스레드에서 돌리면 화면이 멈춘다.
            let result = try? await Task.detached(priority: .userInitiated) {
                try await IosUtSampleGpx.shared.importSamples(
                    gpxByFileName: gpxByFileName,
                    regionHierarchyJson: hierarchy
                )
            }.value

            isImportingSampleGpx = false
            summary = await Task.detached(priority: .userInitiated) {
                IosMyPageData.shared.loadRunningSummary()
            }.value

            guard let result else {
                sampleGpxToast = MyStrings.sampleGpxFailedToast
                return
            }
            sampleGpxToast = result.imported > 0
                ? MyStrings.sampleGpxImportedToast
                : MyStrings.sampleGpxAlreadyToast
        }
    }

    /// 번들 루트에 평평하게 복사된 `*.gpx`를 전부 읽는다(Gradle `generateRunvealBundleAssetsIos`).
    private static func loadSampleGpx() -> [String: String] {
        let urls = Bundle.main.urls(forResourcesWithExtension: "gpx", subdirectory: nil) ?? []
        var result: [String: String] = [:]
        for url in urls {
            guard let text = try? String(contentsOf: url, encoding: .utf8) else { continue }
            result[url.lastPathComponent] = text
        }
        return result
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

            // ⚠️ 테스트용 — UT 샘플 기록을 로컬에 넣는다(`IosUtSampleGpx`). 머지 전에 이 줄과
            // 그 파일, 번들의 `ut-gpx` 에셋을 지울 것.
            RunvealListItem(
                title: MyStrings.menuImportSampleGpx,
                description: isImportingSampleGpx
                    ? MyStrings.menuImportSampleGpxRunning
                    : MyStrings.menuImportSampleGpxDescription,
                onClick: { importSampleGpx() }
            )
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
