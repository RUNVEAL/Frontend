package com.swyp.runveal.courserecommend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.courserecommend.component.FilterBottomSheet
import com.swyp.runveal.courserecommend.component.LocationConfirmPanel
import com.swyp.runveal.courserecommend.component.MakeCoursePanel
import com.swyp.runveal.courserecommend.component.MapHintBanner
import com.swyp.runveal.courserecommend.contract.CourseCreateIntent
import com.swyp.runveal.courserecommend.contract.LocationSelectionTarget
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.swyp.runveal.designsystem.theme.RunvealTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCreateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseCreateViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 출발/도착 선택 모드일 땐 뒤로가기가 화면을 벗어나지 않고 선택만 취소한다.
    val onBackOrCancelSelection = {
        if (state.locationSelectionTarget != LocationSelectionTarget.None) {
            viewModel.onIntent(CourseCreateIntent.CancelLocationSelection)
        } else {
            onBack()
        }
    }
    BackHandler(onBack = onBackOrCancelSelection)

    // 지도(Map Shell)는 그대로 보이고 이 위에 앱바/패널만 얹는다 — 배경을 깔지 않는다.
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = onBackOrCancelSelection,
                style = RunvealTopAppBarStyle.Map,
                title = state.topBarTitle,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.locationSelectionTarget != LocationSelectionTarget.None) {
                MapHintBanner(
                    message = state.locationHintMessage,
                    // 18dp에 맞는 spacing 토큰이 없어 그대로 하드코딩.
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp),
                )
            }
        }

        // TODO: 지도, 출발/도착 선택, 실제 경로 추천 연동 추가 예정
        if (state.locationSelectionTarget == LocationSelectionTarget.None) {
            MakeCoursePanel(
                startLocation = state.startLocation,
                endLocation = state.endLocation,
                distanceLabel = state.distanceLabel,
                avoidVisitedPreferred = state.avoidVisitedPreferred,
                onStartLocationClick = { viewModel.onIntent(CourseCreateIntent.StartLocationClicked) },
                onEndLocationClick = { viewModel.onIntent(CourseCreateIntent.EndLocationClicked) },
                onDistanceClick = { viewModel.onIntent(CourseCreateIntent.OpenFilterSheet) },
                onAvoidVisitedPreferredChange = { viewModel.onIntent(CourseCreateIntent.SetAvoidVisitedPreferred(it)) },
                onFilterClick = { viewModel.onIntent(CourseCreateIntent.OpenFilterSheet) },
                onRecommendClick = { viewModel.onIntent(CourseCreateIntent.RecommendClicked) },
                selectedFilterCount = state.selectedFilters.size,
                endLocationIsDefault = state.endLocationIsDefault,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(
                        horizontal = RunvealTheme.layout.marginScreen,
                        vertical = RunvealTheme.spacing.space8,
                    ),
            )
        } else {
            // 바텀시트 베이스(RunvealBottomSheet)라 MakeCoursePanel과 달리 좌우 여백 없이 가장자리에 붙는다.
            // 배경은 OS NavigationBar 뒤까지 그대로 채워야 해서 여기서 inset을 주지 않는다(버튼만 내부에서 띄움).
            LocationConfirmPanel(
                placeName = state.selectedPlaceName,
                distanceLabel = state.selectedPlaceDistance,
                confirmLabel = state.locationConfirmLabel,
                onConfirmClick = { viewModel.onIntent(CourseCreateIntent.ConfirmLocationSelection) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (state.showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(CourseCreateIntent.CloseFilterSheet) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            dragHandle = null,
        ) {
            FilterBottomSheet(
                distanceOptions = state.distanceOptions,
                selectedDistance = state.selectedDistance,
                onDistanceSelect = { viewModel.onIntent(CourseCreateIntent.SelectPresetDistance(it)) },
                isCustomDistanceSelected = state.isCustomDistanceSelected,
                customDistanceValues = state.customDistanceValues,
                customDistanceIndex = state.customDistanceIndex,
                onCustomDistanceChipClick = { viewModel.onIntent(CourseCreateIntent.SelectCustomDistanceChip) },
                onCustomDistanceIndexChange = { viewModel.onIntent(CourseCreateIntent.ChangeCustomDistanceIndex(it)) },
                avoidVisitedPreferred = state.avoidVisitedPreferred,
                onAvoidVisitedPreferredChange = { viewModel.onIntent(CourseCreateIntent.SetAvoidVisitedPreferred(it)) },
                filterOptions = state.filterOptions,
                selectedFilters = state.selectedFilters,
                onFilterToggle = { viewModel.onIntent(CourseCreateIntent.ToggleFilter(it)) },
                onBack = { viewModel.onIntent(CourseCreateIntent.CloseFilterSheet) },
                onReset = { viewModel.onIntent(CourseCreateIntent.ResetFilterSheet) },
                onApply = { viewModel.onIntent(CourseCreateIntent.ApplyFilterSheet) },
            )
        }
    }
}

@Preview
@Composable
private fun CourseCreateScreenPreview() {
    RunvealTheme {
        CourseCreateScreen(onBack = {})
    }
}
