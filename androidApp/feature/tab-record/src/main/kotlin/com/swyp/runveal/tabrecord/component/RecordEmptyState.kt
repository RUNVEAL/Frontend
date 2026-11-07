package com.swyp.runveal.tabrecord.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.component.list.RunvealEmptyState
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabrecord.R

/** 이번 달 달린 기록이 없을 때(기록 탭) 보여주는 빈 상태. 그래픽은 코스 탭과 같은 empty_course 이미지를 쓴다. */
@Composable
fun RecordEmptyState(onStartRunClick: () -> Unit, modifier: Modifier = Modifier) {
    RunvealEmptyState(
        title = "아직 달린 기록이 없어요",
        description = "코스를 만들고 첫 길을 밝혀보세요",
        actionLabel = "러닝 시작하기",
        onActionClick = onStartRunClick,
        graphic = { Image(painter = painterResource(R.drawable.empty_course), contentDescription = null) },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun RecordEmptyStatePreview() {
    RunvealTheme {
        RecordEmptyState(onStartRunClick = {})
    }
}
