import Foundation
import SharedLogic

enum RecordDetailIntent: UiIntent {
    /// 화면에 들어오면서 어떤 기록인지 알려준다 — 라우트는 trackId만 들고 있고 값은 DB에서 읽는다.
    case load(trackId: String)

    /// 더보기 메뉴에서 "이름 수정"을 골랐다 — 지금 이름을 채운 입력 시트를 띄운다.
    case renameClicked
    /// 입력 시트에서 "저장"을 눌렀다. 앞뒤 공백은 데이터 레이어가 다듬고, 빈 이름은 무시한다.
    case renameConfirmed(name: String)
    case renameDismissed

    /// 더보기 메뉴에서 "삭제"를 골랐다 — 되돌릴 수 없는 일이라 바로 지우지 않고 한 번 더 묻는다.
    case deleteClicked
    case deleteConfirmed
    case deleteDismissed
}

enum RecordDetailEffect: UiEffect {
    /// 기록을 지웠거나 읽을 기록이 없다 — 보여줄 게 없으니 목록으로 돌아간다.
    case navigateBack
}

/// 전부 화면에 그대로 찍히는 라벨이다(포맷은 Kotlin `IosRecordDetail`이 끝낸다).
struct RecordDetailReady {
    let data: RecordDetailData
    /// 삭제 확인 다이얼로그가 떠 있는지.
    var isDeleteDialogVisible = false
    /// 이름 수정 시트가 떠 있는지.
    var isRenameSheetVisible = false
}

enum RecordDetailState: UiState {
    case loading
    /// 목록에서 지운 기록을 뒤로가기로 되돌아와 다시 열었을 때처럼, 읽을 기록이 없는 경우.
    case notFound
    case ready(RecordDetailReady)

    var ready: RecordDetailReady? {
        if case .ready(let value) = self { return value }
        return nil
    }
}
