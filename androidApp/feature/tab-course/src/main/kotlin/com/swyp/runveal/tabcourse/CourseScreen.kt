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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.list.RunvealEmptyState
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabcourse.R
import com.swyp.runveal.map.LatLon
import com.swyp.runveal.tabcourse.component.CourseRecommendationCard
import com.swyp.runveal.tabcourse.component.SavedCourseListItem
import com.swyp.runveal.tabcourse.component.SavedCourseSectionHeader
import com.swyp.runveal.tabcourse.contract.CourseListIntent
import com.swyp.runveal.tabcourse.contract.CourseListState
import com.swyp.runveal.tabcourse.contract.SavedCourseUiModel

@Composable
fun CourseScreen(
    onCreateCourseClick: () -> Unit,
    onStartCourse: (List<LatLon>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 탭을 나갔다 들어올 때마다(restoreState로 화면만 재구성) 새로 저장한 코스가 반영되게 매번 다시 불러온다.
    LaunchedEffect(Unit) { viewModel.onIntent(CourseListIntent.Load) }
    CourseListContent(
        courses = (state as? CourseListState.Ready)?.courses ?: emptyList(),
        isLoading = state is CourseListState.Loading,
        onCreateCourseClick = onCreateCourseClick,
        onStartCourse = onStartCourse,
        modifier = modifier,
    )
}

@Composable
private fun CourseListContent(
    courses: List<SavedCourseUiModel>,
    isLoading: Boolean,
    onCreateCourseClick: () -> Unit,
    onStartCourse: (List<LatLon>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val hasSavedCourses = courses.isNotEmpty()

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
                title = stringResource(R.string.tab_course_title),
                metaLabel = stringResource(R.string.tab_course_meta_label, courses.size),
                action = {
                    CourseTopBarActionButton(onClick = onCreateCourseClick)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (hasSavedCourses) {
                val featured = courses.first()
                CourseRecommendationCard(
                    badgeLabel = stringResource(R.string.tab_course_recommend_badge),
                    title = featured.title,
                    distanceLabel = featured.distanceLabel,
                    durationLabel = featured.durationLabel,
                    tag = featured.tagLabel,
                    newRoadLabel = featured.newRoadLabel,
                    notRunLabel = featured.notRunLabel,
                    onMoreClick = {},
                    onStartClick = { onStartCourse(featured.path) },
                    thumbnailRequest = featured.snapshotRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RunvealTheme.layout.marginScreen),
                )

                SavedCourseSectionHeader(
                    title = stringResource(R.string.tab_course_section_title),
                    sortLabel = stringResource(R.string.tab_course_sort_label),
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
                    courses.forEach { course ->
                        SavedCourseListItem(
                            title = course.title,
                            distanceLabel = course.distanceLabel,
                            durationLabel = course.durationLabel,
                            tag = course.tagLabel,
                            runCountLabel = course.notRunLabel,
                            lastRunLabel = "",
                            onMoreClick = {},
                            onStartClick = { onStartCourse(course.path) },
                            thumbnailRequest = course.snapshotRequest,
                        )
                    }
                }
            }
        }

        if (!hasSavedCourses && !isLoading) {
            RunvealEmptyState(
                title = stringResource(R.string.tab_course_empty_title),
                description = stringResource(R.string.tab_course_empty_description),
                actionLabel = stringResource(R.string.tab_course_empty_action),
                onActionClick = onCreateCourseClick,
                graphic = { Image(painter = painterResource(R.drawable.empty_course), contentDescription = null) },
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
private fun CourseListContentPreview() {
    RunvealTheme {
        CourseListContent(
            courses = listOf(
                SavedCourseUiModel(
                    id = "preview-1",
                    title = "코스 A",
                    distanceLabel = "7.3km",
                    durationLabel = "45분",
                    tagLabel = "순환",
                    newRoadLabel = "새 길 1.3km",
                    notRunLabel = "아직 안달림",
                    path = emptyList(),
                    snapshotRequest = null,
                ),
            ),
            isLoading = false,
            onCreateCourseClick = {},
            onStartCourse = {},
        )
    }
}

@Preview
@Composable
private fun CourseListContentEmptyPreview() {
    RunvealTheme {
        CourseListContent(
            courses = emptyList(),
            isLoading = false,
            onCreateCourseClick = {},
            onStartCourse = {},
        )
    }
}
