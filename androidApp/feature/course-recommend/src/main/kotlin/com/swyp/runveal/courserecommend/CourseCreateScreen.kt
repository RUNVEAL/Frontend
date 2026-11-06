package com.swyp.runveal.courserecommend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.swyp.runveal.designsystem.theme.RunvealTheme

private val DistanceOptions = listOf("3km", "5km", "7km", "10km")
private val FilterOptions = listOf("신호/횡단보도 적은 길", "완만한 길", "공원 지나가기", "하천길 따라가기")

/** 1km ~ 42km, 0.5km 단위. 인덱스 22 = "12" (휠 피커 기본값). */
private val CustomDistanceValues = (2..84).map { step ->
    val km = step / 2.0
    if (km % 1.0 == 0.0) km.toInt().toString() else km.toString()
}
private const val DEFAULT_CUSTOM_DISTANCE_INDEX = 22

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCreateScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    var avoidVisitedPreferred by remember { mutableStateOf(true) }
    var selectedDistance by remember { mutableStateOf("5km") }
    var isCustomDistanceSelected by remember { mutableStateOf(false) }
    var customDistanceIndex by remember { mutableIntStateOf(DEFAULT_CUSTOM_DISTANCE_INDEX) }
    var selectedFilters by remember { mutableStateOf(emptySet<String>()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val distanceLabel = if (isCustomDistanceSelected) "${CustomDistanceValues[customDistanceIndex]}km" else selectedDistance

    // 지도(Map Shell)는 그대로 보이고 이 위에 앱바/패널만 얹는다 — 배경을 깔지 않는다.
    Box(modifier = modifier.fillMaxSize()) {
        RunvealTopAppBar(
            leading = RunvealTopAppBarLeading.Back,
            onLeadingClick = onBack,
            style = RunvealTopAppBarStyle.Map,
            title = "코스 만들기",
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        )

        // TODO: 지도, 출발/도착 선택, 실제 경로 추천 연동 추가 예정
        MakeCoursePanel(
            startLocation = "현위치",
            endLocation = "출발지로 돌아오기",
            distanceLabel = distanceLabel,
            avoidVisitedPreferred = avoidVisitedPreferred,
            onStartLocationClick = {},
            onEndLocationClick = {},
            onDistanceClick = { showFilterSheet = true },
            onAvoidVisitedPreferredChange = { avoidVisitedPreferred = it },
            onFilterClick = { showFilterSheet = true },
            onRecommendClick = {},
            selectedFilterCount = selectedFilters.size,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(
                    horizontal = RunvealTheme.layout.marginScreen,
                    vertical = RunvealTheme.spacing.space8,
                ),
        )
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.Transparent,
            dragHandle = null,
        ) {
            FilterBottomSheet(
                distanceOptions = DistanceOptions,
                selectedDistance = selectedDistance,
                onDistanceSelect = {
                    selectedDistance = it
                    isCustomDistanceSelected = false
                },
                isCustomDistanceSelected = isCustomDistanceSelected,
                customDistanceValues = CustomDistanceValues,
                customDistanceIndex = customDistanceIndex,
                onCustomDistanceChipClick = { isCustomDistanceSelected = true },
                onCustomDistanceIndexChange = { customDistanceIndex = it },
                avoidVisitedPreferred = avoidVisitedPreferred,
                onAvoidVisitedPreferredChange = { avoidVisitedPreferred = it },
                filterOptions = FilterOptions,
                selectedFilters = selectedFilters,
                onFilterToggle = { option ->
                    selectedFilters = if (option in selectedFilters) selectedFilters - option else selectedFilters + option
                },
                onBack = { showFilterSheet = false },
                onReset = {
                    selectedDistance = "5km"
                    isCustomDistanceSelected = false
                    customDistanceIndex = DEFAULT_CUSTOM_DISTANCE_INDEX
                    avoidVisitedPreferred = true
                    selectedFilters = emptySet()
                },
                onApply = { showFilterSheet = false },
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
