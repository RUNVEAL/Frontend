package com.swyp.runveal.tabcourse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabcourse.R
import com.swyp.runveal.tabcourse.component.CourseEmptyState

// TODO: 저장한 코스가 있으면 목록을, 없으면 이 빈 상태를 보여주도록 분기 추가 예정 (코스 저장 기능 연동 전)
@Composable
fun CourseScreen(onCreateCourseClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg.canvas)) {
        // statusBar 영역까지 포함해서 화면 맨 위부터 깔리는 히어로 이미지 — 원본 비율(1920:544)대로 보여준다.
        Image(
            painter = painterResource(R.drawable.my_course),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .aspectRatio(1920f / 544f),
        )

        RunvealTopAppBarLarge(
            title = "내 코스",
            savedCourse = "저장한 코스 0개",
            action = {
                CourseTopBarActionButton(onClick = onCreateCourseClick)
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        )

        CourseEmptyState(
            onCreateClick = onCreateCourseClick,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** 앱바 오른쪽의 코스 추가 버튼. 지도 위 글래스 버튼(RunvealIconButton)과 달리 불투명 카드 배경이라 여기서 직접 그린다. */
@Composable
private fun CourseTopBarActionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.full

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(shape)
            .background(colors.surface.default, shape)
            .border(1.dp, colors.border.default, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = RunvealIcons.Plus,
            contentDescription = "코스 추가",
            tint = colors.icon.primary,
            modifier = Modifier.size(RunvealTheme.layout.sizeIcon),
        )
    }
}

@Preview
@Composable
private fun CourseScreenPreview() {
    RunvealTheme {
        CourseScreen(onCreateCourseClick = {})
    }
}
