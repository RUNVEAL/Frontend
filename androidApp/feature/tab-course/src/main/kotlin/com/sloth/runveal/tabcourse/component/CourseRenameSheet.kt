package com.sloth.runveal.tabcourse.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupEqual
import com.sloth.runveal.designsystem.component.input.RunvealTextInput
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheet
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheetHeader
import com.sloth.runveal.designsystem.component.overlay.RunvealBottomSheetHeaderType
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabcourse.R

/**
 * 코스 이름 수정 시트 — 지금 이름을 채워 둔 입력 한 줄 + 취소/저장.
 *
 * 입력값은 시트가 직접 들고 있다(화면을 벗어나면 버릴 값이라 ViewModel까지 올리지 않는다) — 저장할
 * 때만 [onConfirm]으로 넘긴다. 공백만 남기면 "저장"이 꺼진다. 뒤 scrim·열고 닫기는 호출부
 * (`ModalBottomSheet`)가 맡는다([RunvealBottomSheet]와 같은 규칙).
 */
@Composable
internal fun CourseRenameSheet(
    initialName: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 다른 코스의 시트로 바뀌면(initialName 변경) 입력값도 그 이름으로 다시 시작한다.
    var name by remember(initialName) { mutableStateOf(initialName) }

    RunvealBottomSheet(
        modifier = modifier,
        header = {
            RunvealBottomSheetHeader(
                type = RunvealBottomSheetHeaderType.Title,
                title = stringResource(R.string.tab_course_rename_sheet_title),
            )
        },
        content = {
            RunvealTextInput(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.tab_course_rename_input_label),
                placeholder = stringResource(R.string.tab_course_rename_input_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        footer = {
            RunvealButtonGroup(
                // 키보드가 올라오면 그만큼, 안 올라왔으면 OS BottomNav만큼 띄운다 —
                // safeDrawing이 둘 중 큰 쪽을 주므로 두 번 더해지지 않는다.
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                ),
            ) {
                RunvealButtonGroupEqual(
                    firstLabel = stringResource(R.string.tab_course_rename_cancel),
                    onFirstClick = onCancel,
                    secondLabel = stringResource(R.string.tab_course_rename_confirm),
                    onSecondClick = { onConfirm(name) },
                    enabled = name.isNotBlank(),
                )
            }
        },
    )
}

@Preview
@Composable
private fun CourseRenameSheetPreview() {
    RunvealTheme {
        CourseRenameSheet(initialName = "서울숲 근처 러닝", onConfirm = {}, onCancel = {})
    }
}
