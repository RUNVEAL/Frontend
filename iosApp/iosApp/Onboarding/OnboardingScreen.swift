import SwiftUI

enum OnboardingStrings {
    static let skip = "건너뛰기"
    static let next = "다음"
    /// 마지막 장의 버튼 — 누르면 온보딩이 끝나고 홈으로 간다.
    static let start = "시작하기"
    static let step1Title = "매일 달리는 길이\n나만의 탐험 지도가 돼요"
    static let step2Title = "안 가본 길로 코스를\n추천해요"
    static let step3Title = "달릴수록 우리 동네의\n탐험률이 높아져요"
}

/// 온보딩 한 장 — 제목과 그래픽만 다르고 나머지 구성은 같다. Android `OnboardingStep`.
///
/// 그래픽 원본은 `design/images/onboarding{1,2,3}.png` 하나로, Gradle이 Android drawable과 iOS
/// 번들에 같이 복사한다(`RunvealImages`는 그때 함께 만들어지는 생성 코드다).
private enum OnboardingStep: Int, CaseIterable {
    case exploreMap = 0
    case courseRecommend = 1
    case dongRate = 2

    var title: String {
        switch self {
        case .exploreMap: return OnboardingStrings.step1Title
        case .courseRecommend: return OnboardingStrings.step2Title
        case .dongRate: return OnboardingStrings.step3Title
        }
    }

    var image: Image {
        switch self {
        case .exploreMap: return RunvealImages.onboarding1
        case .courseRecommend: return RunvealImages.onboarding2
        case .dongRate: return RunvealImages.onboarding3
        }
    }
}

/**
 첫 실행에 한 번 보여주는 온보딩(디자인 `L1_온보딩_Step1~3`) — Android `OnboardingScreen`.

 세 장을 스와이프하거나 "다음"으로 넘기고, 어느 장에서든 "건너뛰기"로 끝낼 수 있다. 둘 다 "봤다"로
 기록하고 홈으로 간다(`OnboardingStore`).
 */
struct OnboardingScreen: View {
    let onFinished: () -> Void

    /// 단계가 바뀔 때 제목·그래픽이 겹쳐 바뀌는 시간.
    private static let stepTransitionDuration: TimeInterval = 0.3

    /// 이만큼 옆으로 끌면 단계가 넘어간다 — 살짝 스친 것까지 넘기면 글을 읽다가도 장이 바뀐다.
    private static let swipeThreshold: CGFloat = 56

    /// 밀려 들어오고 나가는 거리. Android는 콘텐츠 너비의 1/8을 쓰는데, 좌우 여백을 뺀 폭이
    /// 대체로 360 안팎이라 그 1/8에 해당하는 값으로 고정했다(화면을 가로지르면 산만하다).
    private static let slideDistance: CGFloat = 45

    @StateObject private var viewModel = OnboardingViewModel()

    @State private var stepIndex = 0
    /// 다음 장으로 가는 중이면 1, 이전 장으로 돌아가는 중이면 -1 — 밀리는 방향을 정한다.
    @State private var direction: CGFloat = 1

    private let steps = OnboardingStep.allCases

    var body: some View {
        VStack(spacing: 0) {
            RunvealTopAppBar(
                leading: RunvealTopAppBarLeading.none,
                actionLabel: OnboardingStrings.skip,
                onLeadingClick: {},
                onActionClick: { viewModel.onIntent(.finished) }
            )

            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                // 좌우로 끌어서도 넘긴다. 페이저처럼 손가락을 따라 콘텐츠가 움직이지는 않고
                // (인디케이터를 시안 자리에 고정하려고 한 장짜리로 뒀다), 끌어 놓은 방향만 보고
                // 단계를 바꾼다 — 바뀌는 모습은 "다음"을 눌렀을 때와 같은 전환이다.
                .contentShape(Rectangle())
                .gesture(
                    DragGesture()
                        .onEnded { value in
                            // 양 끝에서 더 끌어도 아무 일 없다 — 마지막 장을 넘겨서 온보딩이 끝나
                            // 버리면 실수로 지나치게 된다.
                            if value.translation.width <= -Self.swipeThreshold {
                                move(to: stepIndex + 1)
                            } else if value.translation.width >= Self.swipeThreshold {
                                move(to: stepIndex - 1)
                            }
                        }
                )

            RunvealButtonGroup {
                RunvealButtonGroupSingle(
                    // 마지막 장에서는 "다음"이 아니라 "시작하기" — 다음 장이 없고 홈으로 나간다.
                    label: isLastStep ? OnboardingStrings.start : OnboardingStrings.next,
                    onClick: {
                        if isLastStep {
                            viewModel.onIntent(.finished)
                        } else {
                            move(to: stepIndex + 1)
                        }
                    }
                )
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(RunvealTheme.colors.bg.canvas)
        .collectEffect(viewModel.effects) { effect in
            switch effect {
            case .navigateToHome: onFinished()
            }
        }
        .onAppear { RunvealTrace.step(.onboardingShown) }
        .onDisappear { viewModel.cancelRunningTasks() }
    }

    private var isLastStep: Bool { stepIndex == steps.count - 1 }

    /// 제목 → 그래픽 → 페이지 인디케이터를 32 간격으로 쌓고(시안 `Onboarding / Content`), 셋을 한
    /// 덩어리로 상단 앱바와 버튼 사이 가운데에 둔다.
    ///
    /// 단계가 바뀌면 인디케이터 위쪽(제목·그래픽)만 갈린다 — 다음 장이 오른쪽에서 밀려 들어오며
    /// 겹쳐 바뀐다. 인디케이터는 제자리에서 활성 점만 움직인다.
    private var content: some View {
        VStack(spacing: RunvealSpacing.space32) {
            VStack(spacing: RunvealSpacing.space32) {
                Text(steps[stepIndex].title)
                    .runvealTextStyle(RunvealTheme.typography.heading1Semibold)
                    .foregroundStyle(RunvealTheme.colors.text.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                graphic(for: steps[stepIndex])
            }
            // 장이 바뀌면 통째로 새로 그려지며 전환이 걸린다.
            .id(stepIndex)
            .transition(.stepSlide(distance: Self.slideDistance * direction))

            RunvealPageIndicator(count: steps.count, current: stepIndex)
        }
        .padding(.horizontal, RunvealLayout.marginScreen)
    }

    /// 단계별 그래픽(시안 `Graphic/Onboarding/1~3`) — 좌우 여백을 뺀 정사각형 자리에 원본 비율 그대로.
    private func graphic(for step: OnboardingStep) -> some View {
        step.image
            .resizable()
            .scaledToFit()
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .clipShape(RunvealShapes.radius24)
    }

    private func move(to index: Int) {
        let clamped = min(max(index, 0), steps.count - 1)
        guard clamped != stepIndex else { return }
        direction = clamped > stepIndex ? 1 : -1
        withAnimation(.easeInOut(duration: Self.stepTransitionDuration)) {
            stepIndex = clamped
        }
    }
}

/// 들어올 때는 미는 방향에서, 나갈 때는 그 반대쪽으로 [distance]만큼만 움직이며 겹쳐 바뀐다 —
/// Android `AnimatedContent`의 `slideInHorizontally` + `fadeIn` 조합에 해당한다.
private struct OnboardingSlideFade: ViewModifier {
    let offsetX: CGFloat
    let opacity: Double

    func body(content: Content) -> some View {
        content
            .offset(x: offsetX)
            .opacity(opacity)
    }
}

private extension AnyTransition {
    static func stepSlide(distance: CGFloat) -> AnyTransition {
        .asymmetric(
            insertion: .modifier(
                active: OnboardingSlideFade(offsetX: distance, opacity: 0),
                identity: OnboardingSlideFade(offsetX: 0, opacity: 1)
            ),
            removal: .modifier(
                active: OnboardingSlideFade(offsetX: -distance, opacity: 0),
                identity: OnboardingSlideFade(offsetX: 0, opacity: 1)
            )
        )
    }
}
