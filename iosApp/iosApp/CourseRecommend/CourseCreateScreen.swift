import SharedLogic
import SwiftUI

/// B0 — 코스 만들기. 지도(Map Shell)는 그대로 보이고 이 위에 앱바/패널만 얹는다 — 배경을 깔지 않는다.
/// Android `CourseCreateScreen`에 해당한다.
struct CourseCreateScreen: View {
    @ObservedObject var mapState: RunvealMapState
    let onBack: () -> Void
    let onNavigateToResult: () -> Void

    @StateObject private var viewModel = CourseCreateViewModel()
    /// TODO: 임시 로딩 상태 — 디자인 적용된 로딩으로 교체되면 다시 검토. 탭한 즉시 로딩을 띄우려고
    /// 화면이 직접 들고 있다 — ViewModel 쪽은 현재 위치를 먼저 받아와야 해서 인텐트가 그만큼 늦게
    /// 도착하고, ViewModel 상태로만 띄우면 그 사이엔 아무 반응이 없어 보인다.
    @State private var isRecommending = false

    private let currentLocation = RunvealCurrentLocation()

    var body: some View {
        ZStack(alignment: .top) {
            topBar

            VStack {
                Spacer()
                bottomPanel
            }

            if isRecommending {
                loadingOverlay
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .collectEffect(viewModel.effects) { effect in
            switch effect {
            case .navigateToResult:
                isRecommending = false
                onNavigateToResult()
            }
        }
        // 출발/도착 선택 중 지도를 탭하면 그 위치에 핀을 띄운다.
        .onChange(of: mapState.lastMapClick) { _, click in
            guard let click else { return }
            mapState.lastMapClick = nil
            let target = viewModel.state.locationSelectionTarget
            guard target != .none else { return }
            mapState.pin = MapPin(location: click, type: target == .start ? .start : .end)
            viewModel.onIntent(.mapLocationPicked(click))
        }
        .onChange(of: viewModel.state.locationSelectionTarget) { _, target in
            if target == .none { mapState.pin = nil }
            updatePinMarkers()
        }
        .onChange(of: viewModel.state.startLatLon) { _, _ in updatePinMarkers() }
        .onChange(of: viewModel.state.endLatLon) { _, _ in updatePinMarkers() }
        .onDisappear {
            mapState.pin = nil
            mapState.pinMarkers = []
            viewModel.cancelRunningTasks()
        }
        .sheet(isPresented: Binding(
            get: { viewModel.state.showFilterSheet },
            set: { if !$0 { viewModel.onIntent(.closeFilterSheet) } }
        )) {
            FilterBottomSheet(
                selectedDistance: viewModel.state.selectedDistance,
                isCustomDistanceSelected: viewModel.state.isCustomDistanceSelected,
                customDistanceIndex: viewModel.state.customDistanceIndex,
                avoidVisitedPreferred: viewModel.state.avoidVisitedPreferred,
                selectedFilters: viewModel.state.selectedFilters,
                onDistanceSelect: { viewModel.onIntent(.selectPresetDistance($0)) },
                onCustomDistanceChipClick: { viewModel.onIntent(.selectCustomDistanceChip) },
                onCustomDistanceIndexChange: { viewModel.onIntent(.changeCustomDistanceIndex($0)) },
                onAvoidVisitedPreferredChange: { viewModel.onIntent(.setAvoidVisitedPreferred($0)) },
                onFilterToggle: { viewModel.onIntent(.toggleFilter($0)) },
                onBack: { viewModel.onIntent(.closeFilterSheet) },
                onReset: { viewModel.onIntent(.resetFilterSheet) },
                onApply: { viewModel.onIntent(.applyFilterSheet) }
            )
            .presentationBackground(Color.clear)
            .presentationDragIndicator(.hidden)
        }
    }

    // MARK: - 조각

    private var topBar: some View {
        VStack(spacing: 0) {
            RunvealTopAppBar(
                leading: .back,
                style: .map,
                title: viewModel.state.topBarTitle,
                onLeadingClick: backOrCancelSelection
            )
            if viewModel.state.locationSelectionTarget != .none {
                MapHintBanner(message: viewModel.state.locationHintMessage)
                    // 18에 맞는 spacing 토큰이 없어 그대로 하드코딩(Android와 같다).
                    .padding(.top, 18)
            }
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private var bottomPanel: some View {
        if viewModel.state.locationSelectionTarget == .none {
            MakeCoursePanel(
                startLocation: viewModel.state.startLocation,
                endLocation: viewModel.state.endLocation,
                distanceLabel: viewModel.state.distanceLabel,
                avoidVisitedPreferred: viewModel.state.avoidVisitedPreferred,
                endLocationIsDefault: viewModel.state.endLocationIsDefault,
                selectedFilterCount: viewModel.state.selectedFilters.count,
                recommendEnabled: !isRecommending,
                onStartLocationClick: { viewModel.onIntent(.startLocationClicked) },
                onEndLocationClick: { viewModel.onIntent(.endLocationClicked) },
                onDistanceClick: { viewModel.onIntent(.openFilterSheet) },
                onAvoidVisitedPreferredChange: { viewModel.onIntent(.setAvoidVisitedPreferred($0)) },
                onFilterClick: { viewModel.onIntent(.openFilterSheet) },
                onRecommendClick: recommend
            )
            .padding(.horizontal, RunvealLayout.marginScreen)
            .padding(.vertical, RunvealSpacing.space8)
        } else {
            // 바텀시트 베이스라 MakeCoursePanel과 달리 좌우 여백 없이 가장자리에 붙는다.
            LocationConfirmPanel(
                placeName: viewModel.state.selectedPlaceName,
                distanceLabel: CourseRecommendStrings.distanceNotImplemented,
                confirmLabel: viewModel.state.locationConfirmLabel,
                onConfirmClick: { viewModel.onIntent(.confirmLocationSelection) }
            )
        }
    }

    /// TODO: 디자이너가 준 로딩 모션은 적용했지만, 배경 딤·배치는 임시라 전체 로딩 화면 스펙이 나오면
    /// 교체 예정(Android도 같은 TODO 상태다).
    private var loadingOverlay: some View {
        ZStack {
            Color.black.opacity(0.4)
            RunvealLoading()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .ignoresSafeArea()
        // 응답 오기 전까지 뒤쪽 탭을 막는다.
        .contentShape(Rectangle())
        .onTapGesture {}
    }

    // MARK: - 동작

    /// 출발/도착 선택 모드일 땐 뒤로가기가 화면을 벗어나지 않고 선택만 취소한다.
    private func backOrCancelSelection() {
        if viewModel.state.locationSelectionTarget != .none {
            viewModel.onIntent(.cancelLocationSelection)
        } else {
            onBack()
        }
    }

    private func recommend() {
        isRecommending = true
        Task {
            let location = await currentLocation.fetch()
            viewModel.onIntent(.recommendClicked(currentLocation: location))
        }
    }

    /// 확정된 출발/도착 좌표는 Pin 마커로 계속 띄운다 — 선택 중 임시 핀과 별개다. 다만 지금 다시
    /// 고르고 있는 쪽은 기존 마커를 숨겨서 임시 핀과 중복으로 안 보이게 한다.
    private func updatePinMarkers() {
        let activeType = mapState.pin?.type
        var markers: [MapPin] = []
        if let start = viewModel.state.startLatLon, activeType != .start {
            markers.append(MapPin(location: start, type: .start))
        }
        if let end = viewModel.state.endLatLon, activeType != .end {
            markers.append(MapPin(location: end, type: .end))
        }
        mapState.pinMarkers = markers
    }
}
