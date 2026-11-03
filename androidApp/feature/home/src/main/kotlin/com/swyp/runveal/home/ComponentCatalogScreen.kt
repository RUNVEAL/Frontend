package com.swyp.runveal.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.basic.RunvealBadge
import com.swyp.runveal.designsystem.component.basic.RunvealBadgeSize
import com.swyp.runveal.designsystem.component.basic.RunvealBadgeTone
import com.swyp.runveal.designsystem.component.basic.RunvealTag
import com.swyp.runveal.designsystem.component.button.RunvealButton
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupEqual
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupPrimaryText
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupSecondaryPrimary
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupSingle
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupStacked
import com.swyp.runveal.designsystem.component.button.RunvealButtonSize
import com.swyp.runveal.designsystem.component.button.RunvealButtonStyle
import com.swyp.runveal.designsystem.component.button.RunvealIconButton
import com.swyp.runveal.designsystem.component.button.RunvealIconButtonSize
import com.swyp.runveal.designsystem.component.button.RunvealMapAction
import com.swyp.runveal.designsystem.component.button.RunvealRunStartButton
import com.swyp.runveal.designsystem.component.card.RunvealHomeBottomPanel
import com.swyp.runveal.designsystem.component.card.RunvealHomeLocationPermission
import com.swyp.runveal.designsystem.component.card.RunvealSummaryCard
import com.swyp.runveal.designsystem.component.input.RunvealChipCondition
import com.swyp.runveal.designsystem.component.input.RunvealChipConditionIcon
import com.swyp.runveal.designsystem.component.input.RunvealChipDropdown
import com.swyp.runveal.designsystem.component.input.RunvealChipOption
import com.swyp.runveal.designsystem.component.input.RunvealCheckbox
import com.swyp.runveal.designsystem.component.input.RunvealCheckboxState
import com.swyp.runveal.designsystem.component.input.RunvealTextInput
import com.swyp.runveal.designsystem.component.input.RunvealToggleSwitch
import com.swyp.runveal.designsystem.component.input.RunvealWheelPicker
import com.swyp.runveal.designsystem.component.list.RunvealCourseCard
import com.swyp.runveal.designsystem.component.list.RunvealCourseCardState
import com.swyp.runveal.designsystem.component.list.RunvealCourseListItem
import com.swyp.runveal.designsystem.component.list.RunvealCourseListItemRunState
import com.swyp.runveal.designsystem.component.list.RunvealCourseThumbnail
import com.swyp.runveal.designsystem.component.list.RunvealCourseThumbnailType
import com.swyp.runveal.designsystem.component.list.RunvealEmptyState
import com.swyp.runveal.designsystem.component.list.RunvealListItem
import com.swyp.runveal.designsystem.component.list.RunvealListItemSize
import com.swyp.runveal.designsystem.component.list.RunvealListItemTrailing
import com.swyp.runveal.designsystem.component.list.RunvealRouteStep
import com.swyp.runveal.designsystem.component.list.RunvealRouteStepType
import com.swyp.runveal.designsystem.component.list.RunvealSectionHeader
import com.swyp.runveal.designsystem.component.map.RunvealDongLabel
import com.swyp.runveal.designsystem.component.map.RunvealDongLabelType
import com.swyp.runveal.designsystem.component.map.RunvealDongThumbnail
import com.swyp.runveal.designsystem.component.map.RunvealLegend
import com.swyp.runveal.designsystem.component.map.RunvealMapMarker
import com.swyp.runveal.designsystem.component.map.RunvealMapMarkerSize
import com.swyp.runveal.designsystem.component.map.RunvealMapMarkerStyle
import com.swyp.runveal.designsystem.component.map.RunvealMapMarkerType
import com.swyp.runveal.designsystem.component.map.RunvealNavigationBanner
import com.swyp.runveal.designsystem.component.navigation.RunvealPageIndicator
import com.swyp.runveal.designsystem.component.navigation.RunvealSegmentedToggle
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTabBarContainer
import com.swyp.runveal.designsystem.component.navigation.RunvealTabItemSpec
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.swyp.runveal.designsystem.component.overlay.RunvealBottomSheet
import com.swyp.runveal.designsystem.component.overlay.RunvealBottomSheetHeader
import com.swyp.runveal.designsystem.component.overlay.RunvealBottomSheetHeaderType
import com.swyp.runveal.designsystem.component.overlay.RunvealDialog
import com.swyp.runveal.designsystem.component.overlay.RunvealDialogType
import com.swyp.runveal.designsystem.component.overlay.RunvealHint
import com.swyp.runveal.designsystem.component.overlay.RunvealToast
import com.swyp.runveal.designsystem.component.overlay.RunvealToastType
import com.swyp.runveal.designsystem.component.stat.RunvealProgressBar
import com.swyp.runveal.designsystem.component.stat.RunvealStat
import com.swyp.runveal.designsystem.component.stat.RunvealStatType
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 디자인 시스템 전체 컴포넌트를 눈으로 확인하기 위한 테스트용 카탈로그. 탭을 눌러 카테고리 페이지를 넘긴다. */
private enum class CatalogTab(val label: String) {
    Buttons("버튼"),
    Inputs("입력"),
    Navigation("내비"),
    Lists("리스트"),
    StatMap("통계·지도"),
    Overlay("오버레이"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ComponentCatalogScreen(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { CatalogTab.entries.size })

    Column(modifier = modifier.fillMaxSize().background(RunvealTheme.colors.bg.canvas)) {
        CatalogTabRow(pagerState = pagerState)
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (CatalogTab.entries[page]) {
                CatalogTab.Buttons -> ButtonsPage()
                CatalogTab.Inputs -> InputsPage()
                CatalogTab.Navigation -> NavigationPage()
                CatalogTab.Lists -> ListsPage()
                CatalogTab.StatMap -> StatMapPage()
                CatalogTab.Overlay -> OverlayPage()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CatalogTabRow(pagerState: PagerState) {
    val colors = RunvealTheme.colors
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CatalogTab.entries.forEachIndexed { index, tab ->
            val active = index == pagerState.currentPage
            Text(
                tab.label,
                style = if (active) RunvealTheme.typography.bodyMSemibold else RunvealTheme.typography.bodyMRegular,
                color = if (active) colors.text.accent else colors.text.tertiary,
                modifier = Modifier.clickable {
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
            )
        }
    }
}

@Composable
private fun CatalogPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        content()
    }
}

@Composable
private fun CatalogSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RunvealSectionHeader(title = title)
        content()
    }
}

@Composable
private fun ButtonsPage() = CatalogPage {
    CatalogSection("Button") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RunvealButton(label = "Gold / L", onClick = {}, style = RunvealButtonStyle.Gold)
            RunvealButton(label = "Secondary / L", onClick = {}, style = RunvealButtonStyle.Secondary)
            RunvealButton(label = "Danger / L", onClick = {}, style = RunvealButtonStyle.Danger)
            RunvealButton(label = "Text", onClick = {}, style = RunvealButtonStyle.Text)
            RunvealButton(label = "Gold / M + Icon", onClick = {}, style = RunvealButtonStyle.Gold, size = RunvealButtonSize.M, leadingIcon = RunvealIcons.Plus)
            RunvealButton(label = "Disabled", onClick = {}, style = RunvealButtonStyle.Gold, enabled = false)
        }
    }
    CatalogSection("IconButton / MapAction / RunStartButton") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RunvealIconButton(icon = RunvealIcons.Locate, contentDescription = "내 위치", onClick = {}, size = RunvealIconButtonSize.L)
            RunvealIconButton(icon = RunvealIcons.Close, contentDescription = "닫기", onClick = {}, size = RunvealIconButtonSize.M)
            RunvealMapAction(icon = RunvealIcons.Course, label = "코스 만들기", onClick = {})
            RunvealRunStartButton(onClick = {})
        }
    }
    CatalogSection("ButtonGroup") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RunvealButtonGroup { RunvealButtonGroupSingle(label = "Single", onClick = {}) }
            RunvealButtonGroup { RunvealButtonGroupStacked("카카오 로그인", {}, "구글 로그인", {}) }
            RunvealButtonGroup { RunvealButtonGroupPrimaryText("확인", {}, "건너뛰기", {}) }
            RunvealButtonGroup { RunvealButtonGroupSecondaryPrimary("취소", {}, "확인", {}) }
            RunvealButtonGroup { RunvealButtonGroupEqual("취소", {}, "확인", {}) }
        }
    }
}

@Composable
private fun InputsPage() = CatalogPage {
    CatalogSection("TextInput") {
        var value by remember { mutableStateOf("") }
        RunvealTextInput(value = value, onValueChange = { value = it }, label = "코스 이름", placeholder = "코스 이름을 입력하세요")
    }
    CatalogSection("ToggleSwitch / Checkbox") {
        var checked by remember { mutableStateOf(true) }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            RunvealToggleSwitch(checked = checked, onCheckedChange = { checked = it })
            RunvealCheckbox(state = RunvealCheckboxState.Unchecked)
            RunvealCheckbox(state = RunvealCheckboxState.Mixed)
            RunvealCheckbox(state = RunvealCheckboxState.Checked)
        }
    }
    CatalogSection("ChipOption / ChipDropdown / ChipCondition") {
        var selectedChip by remember { mutableIntStateOf(0) }
        var conditionOn by remember { mutableStateOf(true) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("5km", "10km", "하프").forEachIndexed { index, label ->
                    RunvealChipOption(label = label, selected = index == selectedChip, onClick = { selectedChip = index })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RunvealChipDropdown(label = "거리순", onClick = {})
                RunvealChipCondition(checked = conditionOn, onCheckedChange = { conditionOn = it }, label = "안 가본 길 우선")
                RunvealChipCondition(checked = false, onCheckedChange = {}, icon = RunvealChipConditionIcon.Filter)
            }
        }
    }
    CatalogSection("SegmentedToggle") {
        var selected by remember { mutableIntStateOf(0) }
        RunvealSegmentedToggle(options = listOf("탐험한 길", "안 가본 길"), selectedIndex = selected, onSelect = { selected = it })
    }
    CatalogSection("WheelPicker") {
        var index by remember { mutableIntStateOf(4) }
        RunvealWheelPicker(values = (1..20).map { it.toString() }, selectedIndex = index, onSelectedIndexChange = { index = it }, unit = "km")
    }
}

@Composable
private fun NavigationPage() = CatalogPage {
    CatalogSection("TopAppBar") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RunvealTopAppBar(leading = RunvealTopAppBarLeading.Close, onLeadingClick = {}, title = "러닝 완료!")
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = {},
                style = RunvealTopAppBarStyle.Map,
                actionLabel = "건너뛰기",
                onActionClick = {},
            )
            RunvealTopAppBar(leading = RunvealTopAppBarLeading.None, onLeadingClick = {}, title = "홈")
        }
    }
    CatalogSection("TopAppBarLarge") {
        RunvealTopAppBarLarge(
            title = "내 코스",
            savedCourse = "저장한 코스 4개",
            subtitle = "| 아직 안 달린 코스 2개",
            action = { RunvealIconButton(icon = RunvealIcons.Plus, contentDescription = "추가", onClick = {}, size = RunvealIconButtonSize.M) },
        )
    }
    CatalogSection("TabBar") {
        var selected by remember { mutableIntStateOf(0) }
        RunvealTabBar(
            items = listOf(
                RunvealTabItemSpec(RunvealIcons.Home, "홈"),
                RunvealTabItemSpec(RunvealIcons.Course, "내 코스"),
                RunvealTabItemSpec(RunvealIcons.Record, "기록"),
                RunvealTabItemSpec(RunvealIcons.My, "MY"),
            ),
            selectedIndex = selected,
            onSelect = { selected = it },
            container = RunvealTabBarContainer.Floating,
        )
    }
    CatalogSection("PageIndicator") {
        RunvealPageIndicator(count = 3, current = 1)
    }
}

@Composable
private fun ListsPage() = CatalogPage {
    CatalogSection("ListItem") {
        Column {
            RunvealListItem(
                title = "코스 이름",
                description = "성수동 · 5.21km",
                onClick = {},
                leadingIcon = RunvealIcons.Course,
                size = RunvealListItemSize.L,
                trailing = RunvealListItemTrailing.Chevron,
                showDivider = true,
            )
            RunvealListItem(title = "약관에 동의합니다", onClick = {}, leadingCheckbox = RunvealCheckboxState.Checked)
        }
    }
    CatalogSection("CourseListItem / CourseThumbnail") {
        Column {
            RunvealCourseListItem(
                title = "성수동 한 바퀴",
                distance = "5.21km",
                time = "32:10",
                tag = "순환",
                runState = RunvealCourseListItemRunState.Run,
                runCount = "3회 완주",
                lastRun = "2일 전",
                onClick = {},
                onMoreClick = {},
                onPlayClick = {},
                thumbnailType = RunvealCourseThumbnailType.Circulation,
                showDivider = true,
            )
            RunvealCourseListItem(
                title = "한강 러닝 코스",
                distance = "8.0km",
                time = "48:00",
                tag = "편도",
                runState = RunvealCourseListItemRunState.NotRun,
                onClick = {},
                onMoreClick = {},
                onPlayClick = {},
                thumbnailType = RunvealCourseThumbnailType.OneWay,
            )
        }
    }
    CatalogSection("RouteStep") {
        Column {
            RunvealRouteStep(place = "성수역 2번 출구", distance = "0km", type = RunvealRouteStepType.Start)
            RunvealRouteStep(place = "서울숲", distance = "1.2km", type = RunvealRouteStepType.Waypoint)
            RunvealRouteStep(place = "뚝섬한강공원", distance = "3.5km", type = RunvealRouteStepType.NewRoad, note = "새로 탐험한 길")
            RunvealRouteStep(place = "성수역 2번 출구", distance = "5.21km", type = RunvealRouteStepType.End, showConnector = false)
        }
    }
    CatalogSection("CourseCard") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RunvealCourseCard(
                name = "성수동 한 바퀴",
                tags = listOf("공원", "순환"),
                distance = "5.21km",
                time = "32분",
                elevation = "12m",
                newRoad = "1.3km",
                onClick = {},
                state = RunvealCourseCardState.Selected,
            )
            RunvealCourseCard(
                name = "한강 러닝",
                tags = listOf("하천길"),
                distance = "8.0km",
                time = "48분",
                elevation = "5m",
                newRoad = "0km",
                onClick = {},
            )
        }
    }
    CatalogSection("SummaryCard / HomeBottomPanel") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RunvealSummaryCard(dongName = "성수동", ratePercent = 37, onClick = {}, isMine = true)
            RunvealHomeBottomPanel(
                summary = { RunvealSummaryCard(dongName = "성수동", ratePercent = 37, onClick = {}, isMine = true) },
                tabBar = {
                    RunvealTabBar(
                        items = listOf(
                            RunvealTabItemSpec(RunvealIcons.Home, "홈"),
                            RunvealTabItemSpec(RunvealIcons.Course, "내 코스"),
                            RunvealTabItemSpec(RunvealIcons.Record, "기록"),
                            RunvealTabItemSpec(RunvealIcons.My, "MY"),
                        ),
                        selectedIndex = 0,
                        onSelect = {},
                        container = RunvealTabBarContainer.Embedded,
                    )
                },
            )
            RunvealHomeBottomPanel(
                summary = { RunvealHomeLocationPermission(onAllowClick = {}, onLoadRunHistoryClick = {}) },
                tabBar = {
                    RunvealTabBar(
                        items = listOf(
                            RunvealTabItemSpec(RunvealIcons.Home, "홈"),
                            RunvealTabItemSpec(RunvealIcons.Course, "내 코스"),
                            RunvealTabItemSpec(RunvealIcons.Record, "기록"),
                            RunvealTabItemSpec(RunvealIcons.My, "MY"),
                        ),
                        selectedIndex = 0,
                        onSelect = {},
                        container = RunvealTabBarContainer.Embedded,
                    )
                },
            )
        }
    }
    CatalogSection("EmptyState") {
        RunvealEmptyState(title = "아직 저장한 코스가 없어요", description = "원하는 코스를 만들어 보세요", actionLabel = "코스 만들기")
    }
    CatalogSection("Tag / Badge") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RunvealTag(label = "공원")
            RunvealTag(label = "순환")
            RunvealBadge(label = "새로 탐험한 길", value = "+0.8km", size = RunvealBadgeSize.L)
            RunvealBadge(label = "새로 탐험한 길", value = "+0.8km", size = RunvealBadgeSize.M, tone = RunvealBadgeTone.Accent)
        }
    }
}

@Composable
private fun StatMapPage() = CatalogPage {
    CatalogSection("Stat") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RunvealStat(label = "거리", value = "5.21", unit = "km", type = RunvealStatType.Large)
            RunvealStat(label = "거리", value = "5.21", unit = "km", type = RunvealStatType.Small)
            RunvealStat(label = "새로 탐험한 길", value = "+1.3", unit = "km", type = RunvealStatType.Highlight)
        }
    }
    CatalogSection("ProgressBar") {
        RunvealProgressBar(filledPercent = 35f, gainedPercent = 2f)
    }
    CatalogSection("MapMarker") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            RunvealMapMarker(type = RunvealMapMarkerType.Current)
            RunvealMapMarker(type = RunvealMapMarkerType.CurrentStart)
            RunvealMapMarker(type = RunvealMapMarkerType.Start, size = RunvealMapMarkerSize.Small)
            RunvealMapMarker(type = RunvealMapMarkerType.End, size = RunvealMapMarkerSize.Small)
            RunvealMapMarker(type = RunvealMapMarkerType.Start, style = RunvealMapMarkerStyle.Pin)
            RunvealMapMarker(type = RunvealMapMarkerType.End, style = RunvealMapMarkerStyle.Pin)
        }
    }
    CatalogSection("DongLabel / DongThumbnail / Legend") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                RunvealDongLabel(name = "성수동", ratePercent = 37, type = RunvealDongLabelType.Mine)
                RunvealDongLabel(name = "역삼동", type = RunvealDongLabelType.Other)
            }
            RunvealDongThumbnail()
            RunvealLegend()
        }
    }
    CatalogSection("NavigationBanner") {
        RunvealNavigationBanner(icon = RunvealIcons.Chevron, instruction = "좌회전", description = "50m 앞")
    }
}

@Composable
private fun OverlayPage() = CatalogPage {
    CatalogSection("BottomSheet") {
        RunvealBottomSheet(
            header = { RunvealBottomSheetHeader(type = RunvealBottomSheetHeaderType.Title, title = "코스 정렬", subtitle = "원하는 기준을 골라주세요") },
            footer = { RunvealButtonGroup { RunvealButtonGroupSingle(label = "적용", onClick = {}) } },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("거리순")
                    Text("최신순")
                }
            },
        )
    }
    CatalogSection("Dialog") {
        RunvealDialog(
            title = "코스를 삭제할까요?",
            description = "삭제하면 되돌릴 수 없어요",
            confirmLabel = "삭제",
            cancelLabel = "취소",
            onConfirm = {},
            onCancel = {},
            type = RunvealDialogType.Destructive,
        )
    }
    CatalogSection("Toast / Hint") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RunvealToast(message = "코스를 저장했어요")
            RunvealToast(message = "오류가 발생했어요", type = RunvealToastType.Error)
            RunvealHint(message = "지도를 탭해 출발지를 선택하세요")
        }
    }
}
