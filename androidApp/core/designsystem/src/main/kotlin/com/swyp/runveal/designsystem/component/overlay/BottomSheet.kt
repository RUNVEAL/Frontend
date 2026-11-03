package com.swyp.runveal.designsystem.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupSingle
import com.swyp.runveal.designsystem.theme.RunvealTheme

/**
 * 하단 시트 내용(손잡이 → 머리 → 내용 → 버튼). 뒤 bg/scrim과 보이기/숨기기, 드래그 닫기는
 * 호출하는 쪽의 시트 호스트(Dialog/ModalBottomSheet 등)가 맡는다 — 이 컴포저블은 시트 모양만 그린다.
 * 내용이 길면 [content]를 스크롤 가능한 컨테이너로 감싸서 전달한다.
 */
@Composable
fun RunvealBottomSheet(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val colors = RunvealTheme.colors
    val shape = RoundedCornerShape(topStart = RunvealTheme.radius.radius24, topEnd = RunvealTheme.radius.radius24)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.raised, shape)
            .padding(top = RunvealTheme.spacing.space12),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(RunvealTheme.shapes.full)
                .background(colors.border.default, RunvealTheme.shapes.full),
        )
        Column(
            modifier = Modifier.padding(horizontal = RunvealTheme.layout.marginScreen).padding(top = RunvealTheme.spacing.space20),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space20),
        ) {
            if (header != null) header()
            content()
        }
        if (footer != null) {
            Spacer(Modifier.height(RunvealTheme.spacing.space20))
            footer()
        } else {
            Spacer(Modifier.height(RunvealTheme.spacing.space32))
        }
    }
}

@Preview
@Composable
private fun RunvealBottomSheetPreview() {
    RunvealTheme {
        RunvealBottomSheet(
            header = {
                RunvealBottomSheetHeader(type = RunvealBottomSheetHeaderType.Title, title = "코스 정렬")
            },
            footer = {
                RunvealButtonGroup {
                    RunvealButtonGroupSingle(label = "적용", onClick = {})
                }
            },
            content = {
                Column {
                    Text("거리순")
                    Text("최신순")
                }
            },
        )
    }
}
