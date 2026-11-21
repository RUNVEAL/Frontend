import Foundation
import SharedLogic

/// Android `RecordDetailViewModel`에 해당한다 — DB 조회·포맷은 Kotlin `IosRecordDetail`이 하고,
/// 여기서는 화면 상태(시트·다이얼로그 노출)와 의도 처리만 맡는다.
@MainActor
final class RecordDetailViewModel: MviViewModel<RecordDetailIntent, RecordDetailState, RecordDetailEffect> {

    /// 지금 보고 있는 기록 — 이름 수정·삭제에 다시 쓴다(상태는 화면에 찍을 값만 들고 있다).
    private var trackId: String?

    init() {
        super.init(initialState: .loading)
    }

    override func handleIntent(_ intent: RecordDetailIntent) async {
        switch intent {
        case .load(let trackId):
            await load(trackId)

        case .renameClicked:
            updateReady { $0.isRenameSheetVisible = true }
        case .renameDismissed:
            updateReady { $0.isRenameSheetVisible = false }
        case .renameConfirmed(let name):
            await rename(name)

        case .deleteClicked:
            updateReady { $0.isDeleteDialogVisible = true }
        case .deleteDismissed:
            updateReady { $0.isDeleteDialogVisible = false }
        case .deleteConfirmed:
            await deleteTrack()
        }
    }

    private func load(_ trackId: String) async {
        self.trackId = trackId
        do {
            // 동 이름은 번들 `korea_region_hierarchy.json`에서 온다 — 번들을 읽는 건 Swift 몫이라
            // 여기서 꺼내 넘긴다(`RunvealDongOverlayLoader`와 같은 규칙).
            let hierarchy = (try? RunvealBundleAsset.text(RunvealBundleAsset.koreaRegionHierarchy)) ?? ""
            let data = try await IosRecordDetail.shared.load(trackId: trackId, regionHierarchyJson: hierarchy)
            guard let data else {
                setState { _ in .notFound }
                sendEffect(.navigateBack)
                return
            }
            setState { _ in .ready(RecordDetailReady(data: data)) }
        } catch {
            // 읽다 실패해도 빈 화면에 머물게 두지 않는다 — 목록으로 돌려보낸다.
            print("[RecordDetail] 기록을 못 읽었다: \(error)")
            setState { _ in .notFound }
            sendEffect(.navigateBack)
        }
    }

    /// 이름을 바꾸고 화면을 다시 읽는다 — 상단 제목이 바로 갈린다. 공백만 들어오면 아무것도 하지
    /// 않는다(시트의 "저장" 버튼도 같은 조건으로 꺼진다).
    private func rename(_ name: String) async {
        guard let trackId else { return }
        updateReady { $0.isRenameSheetVisible = false }
        do {
            let changed = try await IosRecordDetail.shared.rename(trackId: trackId, name: name)
            if changed.boolValue {
                await load(trackId)
            }
        } catch {
            print("[RecordDetail] 이름 수정 실패: \(error)")
        }
    }

    private func deleteTrack() async {
        guard let trackId else { return }
        updateReady { $0.isDeleteDialogVisible = false }
        do {
            try await IosRecordDetail.shared.delete(trackId: trackId)
        } catch {
            print("[RecordDetail] 기록 삭제 실패: \(error)")
        }
        sendEffect(.navigateBack)
    }

    /// 준비된 상태일 때만 바꾼다 — 로딩·없음 상태에는 시트도 다이얼로그도 없다.
    private func updateReady(_ change: (inout RecordDetailReady) -> Void) {
        setState { state in
            guard case .ready(var ready) = state else { return state }
            change(&ready)
            return .ready(ready)
        }
    }
}
