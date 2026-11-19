package com.sloth.runveal.courserecommend

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sloth.runveal.core.map.LocalMapState
import com.sloth.runveal.devicestate.currentLocationOrNull
import com.sloth.runveal.core.map.MapPin
import com.sloth.runveal.core.map.rememberMapState
import com.sloth.runveal.courserecommend.component.FilterBottomSheet
import com.sloth.runveal.courserecommend.component.LocationConfirmPanel
import com.sloth.runveal.courserecommend.component.MakeCoursePanel
import com.sloth.runveal.courserecommend.component.MapHintBanner
import com.sloth.runveal.courserecommend.contract.CourseCreateEffect
import com.sloth.runveal.courserecommend.contract.CourseCreateIntent
import com.sloth.runveal.courserecommend.contract.LocationSelectionTarget
import com.sloth.runveal.designsystem.component.map.RunvealMapMarkerType
import com.sloth.runveal.designsystem.component.overlay.RunvealLoading
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.map.LatLon
import com.sloth.runveal.ui.mvi.CollectEffect
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCreateScreen(
    onBack: () -> Unit,
    onNavigateToResult: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseCreateViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mapState = LocalMapState.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // TODO: 임시 로딩 상태 — 디자인 적용된 로딩으로 교체되면 다시 검토. 탭한 즉시 로딩을 띄우려고 화면이
    // 직접 들고 있는다 — ViewModel 쪽은 현재 위치를 먼저 받아와야 해서(fetchCurrentLocationOrNull)
    // 인텐트가 그만큼 늦게 도착해, ViewModel 상태로만 띄우면 그 사이엔 아무 반응이 없어 보인다.
    var isRecommending by remember { mutableStateOf(false) }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            CourseCreateEffect.NavigateToResult -> onNavigateToResult()
        }
    }

    // 출발/도착 선택 중 지도를 탭하면 그 위치에 핀을 띄운다. 선택을 취소·확정하거나 화면을 나가면 지운다.
    LaunchedEffect(mapState.lastMapClick) {
        val click = mapState.lastMapClick ?: return@LaunchedEffect
        mapState.lastMapClick = null
        val target = state.locationSelectionTarget
        if (target == LocationSelectionTarget.None) return@LaunchedEffect
        val type = if (target == LocationSelectionTarget.Start) RunvealMapMarkerType.Start else RunvealMapMarkerType.End
        mapState.pin = MapPin(click, type)
        viewModel.onIntent(CourseCreateIntent.MapLocationPicked(click))
    }
    LaunchedEffect(state.locationSelectionTarget) {
        if (state.locationSelectionTarget == LocationSelectionTarget.None) mapState.pin = null
    }
    // 확정된 출발/도착 좌표는 Pin 마커로 계속 띄운다 — 선택 중 임시 핀(pin)과 별개로, 둘 다 안 고르면 안 보인다.
    // 다만 지금 다시 고르고 있는 쪽(임시 핀이 그 타입으로 떠 있는 동안)은 기존 마커를 숨겨서 임시 핀과
    // 중복으로 안 보이게 한다 — 확정하면(pin이 사라지며) 새 좌표로, 취소하면 원래 좌표로 다시 보인다.
    LaunchedEffect(state.startLatLon, state.endLatLon, mapState.pin) {
        val activePinType = mapState.pin?.type
        mapState.pinMarkers = listOfNotNull(
            state.startLatLon
                ?.takeIf { activePinType != RunvealMapMarkerType.Start }
                ?.let { MapPin(it, RunvealMapMarkerType.Start) },
            state.endLatLon
                ?.takeIf { activePinType != RunvealMapMarkerType.End }
                ?.let { MapPin(it, RunvealMapMarkerType.End) },
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            mapState.pin = null
            mapState.pinMarkers = emptyList()
        }
    }

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
                onRecommendClick = {
                    isRecommending = true
                    coroutineScope.launch {
                        viewModel.onIntent(CourseCreateIntent.RecommendClicked(context.currentLocationOrNull()))
                    }
                },
                selectedFilterCount = state.selectedFilters.size,
                endLocationIsDefault = state.endLocationIsDefault,
                recommendEnabled = !isRecommending,
                onMyLocationClick = mapState::recenterToCurrentLocation,
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
                onMyLocationClick = mapState::recenterToCurrentLocation,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // TODO: 디자이너가 준 로딩 모션(RunvealLoading)은 적용했지만, 배경 딤·배치는 임시라 전체
        // 로딩 화면 스펙이 나오면 교체 예정.
        if (isRecommending) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = true, onClick = {}), // 응답 오기 전까지 뒤쪽 탭을 막는다.
                contentAlignment = Alignment.Center,
            ) {
                RunvealLoading()
            }
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

/** 출발지를 한 번도 안 골랐을 때 쓸 현재 위치 — [CurrentLocationLayer][com.sloth.runveal.core.map.internal.maplibre.CurrentLocationLayer]
 * 처럼 권한을 먼저 확인하고 없으면 조용히 null을 돌려준다(위치 서비스가 꺼져 있어도 못 받으면 null). */
@SuppressLint("MissingPermission")

@Preview
@Composable
private fun CourseCreateScreenPreview() {
    RunvealTheme {
        // CourseCreateScreen이 LocalMapState.current로 지도 탭/핀 상태를 읽어 더미 값을 제공한다.
        CompositionLocalProvider(LocalMapState provides rememberMapState()) {
            CourseCreateScreen(onBack = {}, onNavigateToResult = {})
        }
    }
}
