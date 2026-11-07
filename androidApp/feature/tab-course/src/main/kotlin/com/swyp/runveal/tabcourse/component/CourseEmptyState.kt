package com.swyp.runveal.tabcourse.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.component.list.RunvealEmptyState
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabcourse.R

/** 저장한 코스가 없을 때(내 코스 탭) 보여주는 빈 상태. */
@Composable
fun CourseEmptyState(onCreateClick: () -> Unit, modifier: Modifier = Modifier) {
    RunvealEmptyState(
        title = "아직 저장한 코스가 없어요",
        description = "만든 코스를 저장하면\n여기서 바로 달릴 수 있어요",
        actionLabel = "코스 만들기",
        onActionClick = onCreateClick,
        graphic = { Image(painter = painterResource(R.drawable.empty_course), contentDescription = null) },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun CourseEmptyStatePreview() {
    RunvealTheme {
        CourseEmptyState(onCreateClick = {})
    }
}
