import SwiftUI

/**
 기록 이름 수정 시트 — 지금 이름을 채워 둔 입력 한 줄 + 취소/저장. Android `RecordRenameSheet`.

 입력값은 시트가 직접 들고 있다(화면을 벗어나면 버릴 값이라 ViewModel까지 올리지 않는다) — 저장할
 때만 [onConfirm]으로 넘긴다. 공백만 남기면 "저장"이 꺼진다. 뒤 scrim·열고 닫기는 호출부가 맡는다
 (`RunvealBottomSheet`와 같은 규칙).
 */
struct RecordRenameSheet: View {
    let initialName: String
    let onConfirm: (String) -> Void
    let onCancel: () -> Void

    @State private var name: String

    init(initialName: String, onConfirm: @escaping (String) -> Void, onCancel: @escaping () -> Void) {
        self.initialName = initialName
        self.onConfirm = onConfirm
        self.onCancel = onCancel
        _name = State(initialValue: initialName)
    }

    private var isNameBlank: Bool {
        name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        RunvealBottomSheet {
            RunvealBottomSheetHeader(type: .title, title: TabRecordStrings.renameSheetTitle)
        } content: {
            RunvealTextInput(
                text: $name,
                label: TabRecordStrings.renameInputLabel,
                placeholder: TabRecordStrings.renameInputPlaceholder
            )
        } footer: {
            RunvealButtonGroup {
                RunvealButtonGroupEqual(
                    firstLabel: TabRecordStrings.renameCancel,
                    onFirstClick: onCancel,
                    secondLabel: TabRecordStrings.renameConfirm,
                    onSecondClick: { onConfirm(name) },
                    enabled: !isNameBlank
                )
            }
        }
    }
}
