import Darwin
import FirebaseCore
import FirebaseCrashlytics
import Foundation
import SwiftUI

/// 앱이 **튕기지 않고 멈춘** 상황을 밖에서 들여다보는 임시 진단 창구.
///
/// 로컬에 Mac도 아이폰도 없어 Xcode 콘솔과 크래시 로그를 볼 수 없다 — `print`는 아무 데도 남지
/// 않는다. 그래서 **지나온 단계를 화면에 그대로 쌓는다**([RunvealTraceOverlay]): 테스터가 화면을
/// 녹화해 보내주면 마지막 줄이 멈춘 지점이고, 줄마다 찍힌 경과 시간으로 어디서 오래 걸렸는지도
/// 바로 보인다.
///
/// Firebase가 켜져 있으면 비치명적 이벤트로도 같이 남긴다 — 다만 그쪽은 **다음 앱 실행 때** 전송
/// 되므로 멈춘 상태에서는 올라오지 않는다. 화면이 1차, Crashlytics가 보조다.
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
        case overlayFlagSet = 13

        /// 화면과 Crashlytics 콘솔에 보이는 이름.
        var name: String {
            switch self {
            case .bootstrapStart: return "스플래시 시작"
            case .overlayWarmUpStart: return "오버레이 생성 시작"
            case .bundleAssetsRead: return "번들 경계 읽음"
            case .overlayBuilt: return "오버레이 생성 완료"
            case .overlayPublished: return "오버레이 전달"
            case .overlayFailed: return "오버레이 실패"
            case .metaDone: return "GET /meta 성공"
            case .metaFailed: return "GET /meta 실패"
            case .overlayWaitFinished: return "오버레이 대기 완료"
            case .overlayWaitTimedOut: return "오버레이 대기 상한 초과"
            case .navigatingHome: return "홈으로 전환 호출"
            case .homeShellAppeared: return "홈 표시됨"
            case .overlayFlagSet: return "대기 플래그 설정"
            }
        }
    }

    private static let domain = "RunvealTrace"

    /// 앱이 켜진 시점 — 각 단계가 몇 초째에 지나갔는지 재는 기준.
    private static let appStart = ContinuousClock.now

    /// 화면에 길게 흐르지 않도록 자르는 상세 길이.
    private static let detailLimit = 80

    /// 단계 하나를 남긴다. 멈춘 지점을 찾는 게 목적이라, 마지막으로 남은 단계가 답이 된다.
    static func step(_ step: Step, _ detail: String? = nil) {
        let elapsed = elapsedText()
        let thread = Thread.isMainThread ? "main" : "bg"
        let memory = memoryText()

        var line = "\(elapsed)s [\(thread)] \(memory) \(step.name)"
        if let detail {
            line += " — " + String(detail.prefix(detailLimit))
        }
        print("[RunvealTrace] \(line)")

        Task { @MainActor in
            RunvealTraceLog.shared.append(line)
        }

        record(step, detail: detail, elapsed: elapsed, thread: thread, memory: memory)
    }

    /// Firebase가 초기화된 경우에만 비치명적 이벤트를 남긴다.
    ///
    /// **`FirebaseApp.app()` 확인을 빼면 안 된다.** 설정 파일(`GoogleService-Info.plist`)이 번들에
    /// 없으면 `iosApp`이 `FirebaseApp.configure()`를 건너뛰는데, 그 상태에서 `Crashlytics`를 만지면
    /// 그 자리에서 앱이 죽는다 — 진단하려고 넣은 코드가 앱을 못 뜨게 만드는 셈이다.
    private static func record(_ step: Step, detail: String?, elapsed: String, thread: String, memory: String) {
        guard FirebaseApp.app() != nil else { return }
        var info: [String: Any] = [
            NSLocalizedDescriptionKey: step.name,
            "elapsedSeconds": elapsed,
            "thread": thread,
            "memory": memory,
        ]
        if let detail {
            info["detail"] = detail
        }
        Crashlytics.crashlytics().record(error: NSError(domain: domain, code: step.rawValue, userInfo: info))
    }

    /// 지금 앱이 쓰는 메모리(MB).
    ///
    /// iOS가 메모리 과다로 앱을 죽일 때(Jetsam) 보는 값이 `phys_footprint`다. 그렇게 종료되면
    /// 시그널이 아니라 프로세스가 통째로 사라져서 **크래시 리포트가 남지 않는다** — 전국 경계를
    /// 파싱해 GeoJSON 문자열로 다시 만드는 구간에서 그 한계를 넘는지 눈으로 보려고 같이 찍는다.
    private static func memoryText() -> String {
        var info = task_vm_info_data_t()
        var count = mach_msg_type_number_t(MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<natural_t>.size)
        let result = withUnsafeMutablePointer(to: &info) { pointer in
            pointer.withMemoryRebound(to: integer_t.self, capacity: Int(count)) { rebound in
                task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), rebound, &count)
            }
        }
        guard result == KERN_SUCCESS else { return "?MB" }
        return String(format: "%.0fMB", Double(info.phys_footprint) / 1_048_576)
    }

    private static func elapsedText() -> String {
        let elapsed = ContinuousClock.now - appStart
        let seconds = Double(elapsed.components.seconds)
            + Double(elapsed.components.attoseconds) / 1_000_000_000_000_000_000
        return String(format: "%.2f", seconds)
    }
}

/// [RunvealTrace]가 남긴 줄을 들고 있는 저장소 — 화면에 쌓아 보여주려고 둔다.
@MainActor
final class RunvealTraceLog: ObservableObject {
    static let shared = RunvealTraceLog()

    /// 화면에 남겨 두는 줄 수. 스플래시 단계가 열 개 남짓이라 전부 보이는 크기로 둔다.
    private static let limit = 14

    @Published private(set) var entries: [Entry] = []

    private init() {}

    struct Entry: Identifiable {
        let id = UUID()
        let text: String
    }

    func append(_ text: String) {
        entries.append(Entry(text: text))
        if entries.count > Self.limit {
            entries.removeFirst(entries.count - Self.limit)
        }
    }
}

/// 진단 로그를 화면 아래에 쌓아 보여주는 임시 패널.
///
/// 테스터의 화면 녹화로 읽는 게 목적이라 디자인시스템을 쓰지 않고 작고 단단한 고정 스타일로 둔다 —
/// 원인을 찾으면 이 파일과 호출부를 통째로 지운다.
struct RunvealTraceOverlay: View {
    @ObservedObject private var log = RunvealTraceLog.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            ForEach(log.entries) { entry in
                Text(entry.text)
                    .font(.system(size: 10, weight: .regular, design: .monospaced))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
        .padding(8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.black.opacity(0.6))
    }
}
