import FirebaseCrashlytics
import Foundation

/// 앱이 **튕기지 않고 멈춘** 상황을 밖에서 들여다보는 진단 창구.
///
/// 로컬에 Mac이 없어 Xcode 콘솔을 볼 수 없다 — `print`는 아무 데도 남지 않는다. Crashlytics의
/// 크래시 로그(`log(_:)`)도 크래시가 나야 함께 전송되므로, 멈춘 채 아무 일도 일어나지 않는
/// 증상에는 쓸 수 없다. 비치명적 이벤트(`record(error:)`)는 크래시 없이도 올라가서, 단계마다
/// 하나씩 남기면 어디까지 갔는지 콘솔에서 볼 수 있다.
///
/// **전송은 다음 실행 때다.** 멈춰 있는 동안에는 올라오지 않는다 — 멈추면 앱을 강제 종료하고 다시
/// 켜야 지난 세션 기록이 Crashlytics에 보인다.
///
/// 스플래시가 왜 홈으로 넘어가지 않는지 잡으려고 넣었다. 원인을 찾으면 이 파일과 호출부를 지운다.
enum RunvealTrace {

    /// 진행 단계.
    ///
    /// **code가 단계마다 달라야 한다** — Crashlytics는 비치명적 이벤트를 `domain`+`code`로 묶어서,
    /// 같은 code를 쓰면 전부 한 이슈로 합쳐져 어디까지 갔는지 구분할 수 없다.
    enum Step: Int {
        case bootstrapStart = 1
        case overlayWarmUpStart = 2
        case bundleAssetsRead = 3
        case overlayBuilt = 4
        case overlayPublished = 5
        case overlayFailed = 6
        case metaDone = 7
        case metaFailed = 8
        case overlayWaitFinished = 9
        case overlayWaitTimedOut = 10
        case navigatingHome = 11
        case homeShellAppeared = 12

        /// Crashlytics 콘솔에 제목으로 보이는 이름.
        var name: String {
            switch self {
            case .bootstrapStart: return "스플래시 시작"
            case .overlayWarmUpStart: return "오버레이 사전 생성 시작"
            case .bundleAssetsRead: return "번들 경계 에셋 읽음"
            case .overlayBuilt: return "오버레이 생성 완료"
            case .overlayPublished: return "오버레이 홀더에 전달"
            case .overlayFailed: return "오버레이 생성 실패"
            case .metaDone: return "GET /meta 성공"
            case .metaFailed: return "GET /meta 실패"
            case .overlayWaitFinished: return "오버레이 대기 완료"
            case .overlayWaitTimedOut: return "오버레이 대기 상한 초과"
            case .navigatingHome: return "홈으로 전환 호출"
            case .homeShellAppeared: return "홈 셸 표시됨"
            }
        }
    }

    private static let domain = "RunvealTrace"

    /// 앱이 켜진 시점 — 각 단계가 몇 초째에 지나갔는지 재는 기준.
    private static let appStart = ContinuousClock.now

    /// 단계 하나를 남긴다. 멈춘 지점을 찾는 게 목적이라, 마지막으로 올라온 단계가 답이 된다.
    static func step(_ step: Step, _ detail: String? = nil) {
        var info: [String: Any] = [
            NSLocalizedDescriptionKey: step.name,
            "elapsedSeconds": elapsedText(),
            // 메인 액터 밖에서 돌아야 하는 무거운 작업이 메인에 올라와 있지 않은지 같이 본다.
            "thread": Thread.isMainThread ? "main" : "background",
        ]
        if let detail {
            info["detail"] = detail
        }
        Crashlytics.crashlytics().record(error: NSError(domain: domain, code: step.rawValue, userInfo: info))
        // 시뮬레이터·Xcode로 볼 수 있을 때는 콘솔에서도 바로 보이게 남긴다.
        print("[RunvealTrace] \(elapsedText())s \(step.name) \(detail ?? "")")
    }

    private static func elapsedText() -> String {
        let elapsed = ContinuousClock.now - appStart
        let seconds = Double(elapsed.components.seconds)
            + Double(elapsed.components.attoseconds) / 1_000_000_000_000_000_000
        return String(format: "%.2f", seconds)
    }
}
