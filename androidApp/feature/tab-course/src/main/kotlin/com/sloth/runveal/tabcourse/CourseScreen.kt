package com.sloth.runveal.tabcourse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.list.RunvealEmptyState
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.sloth.runveal.designsystem.component.overlay.RunvealDialog
import com.sloth.runveal.designsystem.component.overlay.RunvealDialogType
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabcourse.R
import com.sloth.runveal.tabcourse.component.CourseMoreMenuActions
import com.sloth.runveal.tabcourse.component.CourseRenameSheet
import com.sloth.runveal.tabcourse.component.CourseRecommendationCard
import com.sloth.runveal.tabcourse.component.SavedCourseListItem
import com.sloth.runveal.tabcourse.component.SavedCourseSectionHeader
import com.sloth.runveal.tabcourse.contract.CourseListIntent
import com.sloth.runveal.tabcourse.contract.CourseListState
import com.sloth.runveal.tabcourse.contract.SavedCourseUiModel
import com.sloth.runveal.ui.toast.NotReadyToastHost
import com.sloth.runveal.ui.toast.rememberNotReadyToastState

@Composable
fun CourseScreen(
    onCreateCourseClick: () -> Unit,
    /** 목록에서 한 건을 눌렀다 — 코스 상세로 간다. */
    onCourseClick: (SavedCourseUiModel) -> Unit,
    onStartCourse: (SavedCourseUiModel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 탭을 나갔다 들어올 때마다(restoreState로 화면만 재구성) 새로 저장한 코스가 반영되게 매번 다시 불러온다.
    LaunchedEffect(Unit) { viewModel.onIntent(CourseListIntent.Load) }
    val readyState = state as? CourseListState.Ready
    CourseListContent(
        courses = readyState?.courses ?: emptyList(),
        featured = readyState?.featuredCourse,
        isLoading = state is CourseListState.Loading,
        onCreateCourseClick = onCreateCourseClick,
        onCourseClick = onCourseClick,
        onStartCourse = onStartCourse,
        onDeleteCourseClick = { viewModel.onIntent(CourseListIntent.DeleteCourseClicked(it.id)) },
        isDeleteDialogVisible = readyState?.pendingDeleteCourseId != null,
        onDeleteConfirm = { viewModel.onIntent(CourseListIntent.DeleteConfirmed) },
        onDeleteDismiss = { viewModel.onIntent(CourseListIntent.DeleteDismissed) },
        onRenameCourseClick = { viewModel.onIntent(CourseListIntent.RenameCourseClicked(it.id)) },
        renamingCourseName = readyState?.pendingRenameCourse?.title,
        onRenameConfirm = { viewModel.onIntent(CourseListIntent.RenameConfirmed(it)) },
        onRenameDismiss = { viewModel.onIntent(CourseListIntent.RenameDismissed) },
        modifier = modifier,
    )
}

@Composable
private fun CourseListContent(
    courses: List<SavedCourseUiModel>,
    /** 위 큰 카드에 띄울 코스 — 가장 최근에 저장한 코스. */
    featured: SavedCourseUiModel?,
    isLoading: Boolean,
    onCreateCourseClick: () -> Unit,
    onCourseClick: (SavedCourseUiModel) -> Unit,
    onStartCourse: (SavedCourseUiModel) -> Unit,
    /** 더보기 메뉴에서 "삭제"를 골랐다 — 바로 지우지 않고 확인 다이얼로그를 띄운다. */
    onDeleteCourseClick: (SavedCourseUiModel) -> Unit,
    isDeleteDialogVisible: Boolean,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    /** 더보기 메뉴에서 "이름 수정"을 골랐다 — 입력 시트를 띄운다. */
    onRenameCourseClick: (SavedCourseUiModel) -> Unit,
    /** 이름 수정 시트에 채워 둘 지금 이름. null이면 시트를 띄우지 않는다. */
    renamingCourseName: String?,
    onRenameConfirm: (String) -> Unit,
    onRenameDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val hasSavedCourses = courses.isNotEmpty()
    val notReadyToast = rememberNotReadyToastState()
    // 코스마다 "그 코스를" 고치고 지우므로 메뉴도 코스 단위로 만든다.
    // TODO: GPX 내보내기는 아직 할 일이 없어 "개발 중" 안내만 띄운다.
    val moreMenuFor = { course: SavedCourseUiModel ->
        CourseMoreMenuActions(
            onRename = { onRenameCourseClick(course) },
            onExportGpx = notReadyToast::show,
            onDelete = { onDeleteCourseClick(course) },
        )
    }

    Box(modifier = modifier.fillMaxSize().background(colors.bg.canvas)) {
        // 탭바 위에 띄운다 — 탭바는 Map Shell이 이 화면 위에 겹쳐 그린다.
        NotReadyToastHost(
            notReadyToast,
            bottomInset = RunvealTheme.layout.sizeTabBar + RunvealTheme.spacing.space8,
        )

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
                .verticalScroll(rememberScrollState())
                // 목록이 탭바 **뒤로 지나가는 건** 그대로 두고(글래스 탭바 아래로 보인다), 끝까지
                // 내렸을 때 마지막 항목이 탭바·OS 내비게이션에 가리지 않게 그만큼 여백을 둔다.
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(bottom = RunvealTheme.layout.sizeTabBar + RunvealTheme.spacing.space24),
        ) {
            RunvealTopAppBarLarge(
                title = stringResource(R.string.tab_course_title),
                metaLabel = stringResource(R.string.tab_course_meta_label, courses.size),
                action = {
                    CourseTopBarActionButton(onClick = onCreateCourseClick)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (hasSavedCourses && featured != null) {
                CourseRecommendationCard(
                    badgeLabel = stringResource(R.string.tab_course_recommend_badge),
                    title = featured.title,
                    distanceLabel = featured.distanceLabel,
                    durationLabel = featured.durationLabel,
                    tag = featured.tagLabel,
                    newRoadLabel = featured.newRoadLabel,
                    notRunLabel = featured.notRunLabel,
                    moreMenu = moreMenuFor(featured),
                    onStartClick = { onStartCourse(featured) },
                    thumbnailRequest = featured.snapshotRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RunvealTheme.layout.marginScreen),
                )

                SavedCourseSectionHeader(
                    title = stringResource(R.string.tab_course_section_title),
                    sortLabel = stringResource(R.string.tab_course_sort_label),
                    // TODO: 정렬 바꾸기(최근 저장순/거리순 등) — 고를 게 생기면 onSortClick을 주면
                    //  화살표가 돌아온다. 지금은 최근 저장순 하나뿐이라 보여주기만 한다.
                    onSortClick = null,
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
                            moreMenu = moreMenuFor(course),
                            onClick = { onCourseClick(course) },
                            onStartClick = { onStartCourse(course) },
                            thumbnailRequest = course.snapshotRequest,
                        )
                    }
                }
            }
        }

        // RunvealDialog는 내용만 그리는 컴포넌트다(그 KDoc 참고) — Compose의 Dialog로 감싸
        // 바깥 탭·뒤로가기로 닫히게 한다(MyScreen의 로그아웃 다이얼로그와 같은 방식).
        if (isDeleteDialogVisible) {
            Dialog(onDismissRequest = onDeleteDismiss) {
                RunvealDialog(
                    title = stringResource(R.string.tab_course_delete_dialog_title),
                    description = stringResource(R.string.tab_course_delete_dialog_description),
                    confirmLabel = stringResource(R.string.tab_course_delete_dialog_confirm),
                    cancelLabel = stringResource(R.string.tab_course_delete_dialog_cancel),
                    onConfirm = onDeleteConfirm,
                    onCancel = onDeleteDismiss,
                    type = RunvealDialogType.Destructive,
                )
            }
        }

        if (renamingCourseName != null) {
            CourseRenameModalSheet(
                courseName = renamingCourseName,
                onConfirm = onRenameConfirm,
                onDismiss = onRenameDismiss,
            )
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

/** 이름 수정 시트 — 필터 시트(`FilterBottomSheet`)와 같은 방식으로 띄운다: 모양은
 * [CourseRenameSheet]가 그리고 ModalBottomSheet는 scrim·드래그 닫기만 맡는다(그래서 배경은
 * 투명, 드래그 핸들은 시트가 직접 그린다). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseRenameModalSheet(
    courseName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        CourseRenameSheet(initialName = courseName, onConfirm = onConfirm, onCancel = onDismiss)
    }
}

@Preview
@Composable
private fun CourseListContentPreview() {
    RunvealTheme {
        CourseListContent(
            featured = null,
            onCourseClick = {},
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
                    isLoop = true,
                    snapshotRequest = null,
                ),
            ),
            isLoading = false,
            onCreateCourseClick = {},
            onStartCourse = {},
            onDeleteCourseClick = {},
            isDeleteDialogVisible = false,
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onRenameCourseClick = {},
            renamingCourseName = null,
            onRenameConfirm = {},
            onRenameDismiss = {},
        )
    }
}

@Preview
@Composable
private fun CourseListContentEmptyPreview() {
    RunvealTheme {
        CourseListContent(
            featured = null,
            onCourseClick = {},
            courses = emptyList(),
            isLoading = false,
            onCreateCourseClick = {},
            onStartCourse = {},
            onDeleteCourseClick = {},
            isDeleteDialogVisible = false,
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onRenameCourseClick = {},
            renamingCourseName = null,
            onRenameConfirm = {},
            onRenameDismiss = {},
        )
    }
}
