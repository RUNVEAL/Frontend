package com.swyp.runveal.tabcourse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.swyp.runveal.tabcourse.component.CourseRecommendationCard
import com.swyp.runveal.tabcourse.component.SavedCourseListItem
import com.swyp.runveal.tabcourse.component.SavedCourseSectionHeader

// TODO: 코스 저장 기능 연동 전까지 쓰는 임시 플래그/데이터 — 실제로는 저장한 코스 목록으로 갈린다.
private const val HAS_SAVED_COURSES = true

private data class SampleSavedCourse(
    val title: String,
    val distanceLabel: String,
    val durationLabel: String,
    val tag: String,
    val runCountLabel: String,
    val lastRunLabel: String,
)

private val SampleSavedCourses = listOf(
    SampleSavedCourse(
        title = "서울숲 근처 러닝",
        distanceLabel = "7.3km",
        durationLabel = "45분",
        tag = "순환",
        runCountLabel = "2회 달림",
        lastRunLabel = "최근 10.31",
    ),
)

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

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState()),
        ) {
            RunvealTopAppBarLarge(
                title = "내 코스",
                savedCourse = if (HAS_SAVED_COURSES) "저장한 코스 1개" else "저장한 코스 0개",
                action = {
                    CourseTopBarActionButton(onClick = onCreateCourseClick)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (HAS_SAVED_COURSES) {
                CourseRecommendationCard(
                    badgeLabel = "다음에 달려볼 코스",
                    title = "코스 A",
                    distanceLabel = "7.3km",
                    durationLabel = "45분",
                    tag = "순환",
                    newRoadLabel = "새 길 1.3km",
                    notRunLabel = "아직 안달림",
                    onMoreClick = {},
                    onStartClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RunvealTheme.layout.marginScreen),
                )

                SavedCourseSectionHeader(
                    title = "저장한 코스",
                    sortLabel = "최근 저장순",
                    onSortClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = RunvealTheme.layout.marginScreen,
                            end = RunvealTheme.layout.marginScreen,
                            top = RunvealTheme.spacing.space24,
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RunvealTheme.layout.marginScreen),
                ) {
                    SampleSavedCourses.forEach { course ->
                        SavedCourseListItem(
                            title = course.title,
                            distanceLabel = course.distanceLabel,
                            durationLabel = course.durationLabel,
                            tag = course.tag,
                            runCountLabel = course.runCountLabel,
                            lastRunLabel = course.lastRunLabel,
                            onMoreClick = {},
                            onStartClick = {},
                        )
                    }
                }
            }
        }

        if (!HAS_SAVED_COURSES) {
            CourseEmptyState(
                onCreateClick = onCreateCourseClick,
                modifier = Modifier.align(Alignment.Center),
            )
        }
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
