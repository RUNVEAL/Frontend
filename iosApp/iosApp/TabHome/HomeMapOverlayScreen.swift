import SharedLogic
import SwiftUI

enum TabHomeStrings {
    static let currentLocation = "현재 위치"
    // 동 이름에 붙는 꼬리표 — 탐험률 최고 동이면 "내 동네", 지금 서 있는 동이면 "현위치".
    static let tagMyDong = "내 동네"
    static let tagCurrentLocation = "현위치"
    static let headline = "오늘도 새로운 길을\n밝혀볼까요?"
    /// 홈 지도 토글 — 지도에 **길이 보이냐 동네가 보이냐**를 고른다(v2). 예전의 "탐험한 길 /
    /// 안 가본 길"을 대신한다. Android와 문구가 같아야 한다.
    static let toggleScaleRoad = "길 단위"
    static let toggleScaleDong = "동네 단위"
    static let exploredSuffix = "탐험했어요"
    static let createCourse = "코스 만들기"
    static let myLocation = "내 위치"
    /// "누적 35.1km" / "전체 128.4km" — **소수 한 자리까지만**. 미터 단위 원자료를 그대로 쓰면
    /// "3.4821…km"가 된다(Android `appendDistanceLine`과 같은 규칙).
    static func cumulative(_ exploredKm: Double, _ totalKm: Double) -> String {
        "누적 \(kilometerLabel(exploredKm))\n전체 \(kilometerLabel(totalKm))"
    }

    /// 홈에서 쓰는 거리 표기 — 값이 얼마든 소수 한 자리로 끊는다.
    static func kilometerLabel(_ km: Double) -> String {
        String(format: "%.1fkm", km)
    }
}

/// 보기 전환 토글에서 "안 가본 길"의 인덱스 — 아래 옵션 목록 순서와 짝이다.
/// 토글 순서 — 0: 길 단위, 1: 동네 단위. **기본은 동네 단위**다: 앱을 열면 탐험률이 높은 동으로
/// 포커스하는 기존 흐름이 동네 단위 보기라, 길 단위를 기본으로 하면 첫 화면이 바뀐다.
private let toggleIndexRoad = 0
private let toggleIndexDong = 1

/// 홈 지도 오버레이. 지도 자체와 위치 권한 요청은 Map Shell([RunvealHomeShell])이 맡고,
/// 여기서는 줌 레벨을 읽어 그 위에 얹는 UI만 그린다. Android `HomeMapOverlayScreen`에 해당한다.
struct HomeMapOverlayScreen: View {
    @ObservedObject var mapState: RunvealMapState
    var onCreateCourseClick: () -> Void = {}
    var onRunStartClick: () -> Void = {}

    /// 동 조회가 **서버 실패**로 끝났는지 알린다 — 배너는 홈 셸이 그리고(탭 화면 밖), 조회는 여기가
    /// 한다. 서비스 지역 밖·위치 권한 없음은 false다(정상 상황이라 안내할 게 없다).
    var onDongLoadFailedChange: (Bool) -> Void = { _ in }

    /**
     줌아웃 하단 요약 카드가 쓸 값 — 지도 가운데 동의 이름·코드와 꼬리표.

     카드는 홈 셸이 그리고(탭 화면 밖) 조회는 여기가 하므로, Android가 홀더로 넘기는 것을 iOS는
     콜백으로 올린다([onDongLoadFailedChange]와 같은 패턴).
     */
    var onSummaryChange: (DongSummary?) -> Void = { _ in }

    /// 셸의 "다시 시도"가 올리는 값 — 바뀔 때마다 동 조회를 다시 돌린다(값 자체는 쓰지 않는다).
    /// Swift는 호출 인자 순서가 선언을 따라야 해서, 콜백들을 모으고 이 값을 맨 뒤에 둔다.
    var retryToken: Int = 0

    @State private var selectedToggleIndex = toggleIndexDong

    /// 현재 위치의 행정동 — 서버에 한 번 물어 둔다(`IosCurrentDong`). 못 받으면 nil이고 화면은
    /// 대체 문구를 쓴다.
    @State private var currentDong: CurrentDong?

    /// 지도 가운데 보이는 행정동 — 카메라가 멈출 때마다 다시 받는다(이름·탐험률·거리).
    /// 지도를 끌어 다른 동네를 둘러보는 게 목적이라, 어느 줌에서든 화면에 보이는 곳의 이름이 맞다.
    @State private var centerDong: CurrentDong?

    /// 마지막으로 중심 동을 물어본 자리 — 여기서 [centerDongRefreshDistanceM]을 넘게 움직였을
    /// 때만 다시 묻는다.
    @State private var lastCenterQueried: LatLon?

    /// 스플래시가 미리 만들어 둔 오버레이 — 탐험률·거리를 여기서 꺼낸다.
    @ObservedObject private var overlayHolder = RunvealDongOverlayHolder.shared

    /**
     지도 중심 동을 다시 물어볼 최소 이동 거리(m). 줌아웃 화면에서 이 정도는 거의 같은 동이라,
     넘지 않으면 묻지 않는다 — 멈출 때마다 묻게 두면 지도를 조금씩 미는 동안 요청이 쏟아진다.
     Android와 같은 값이다.
     */
    private static let centerDongRefreshDistanceM: Double = 300

    /// `.task(id:)`에 쓸 키 — `LatLon`은 Kotlin 타입이라 Equatable을 기대할 수 없어 문자열로 만든다.
    private var centerKey: String {
        "\(mapState.currentCenter.lat),\(mapState.currentCenter.lon)"
    }

    /// 꼬리표 기준 — 홈 요약 카드와 같은 함수를 써서 두 패널이 같은 동을 "내 동네"로 본다.
    private var topExploredDongCode: String? {
        overlayHolder.bundle?.query.topDongCode()
    }

    /**
     셸의 하단 요약 카드가 쓸 값 — **지도를 움직여도 바뀌지 않는다.**

     지도 가운데 동을 따라가게 두면 다른 동네를 둘러보는 동안 "내 동네" 카드가 그 동네 카드로
     바뀐다. 어디를 보고 있는지는 좌측 상단 위치 라벨이 알려주므로 카드는 **내 기준**에 머문다 —
     기록이 있으면 탐험률이 가장 높은 동("내 동네"), 없으면 지금 서 있는 동("현위치"). 둘 다
     없으면 nil이라 카드가 대체 문구를 쓴다(Android `HomePanelDong`과 같은 기준).
     */
    private var summary: DongSummary? {
        let query = overlayHolder.bundle?.query
        // 이름까지 있어야 쓴다 — 이름을 못 찾으면 코드와 다른 동을 가리키게 된다.
        if let query, let topCode = query.topDongCode(), let name = query.nameOf(admCd2: topCode) {
            return DongSummary(
                dongName: name,
                dongCode: topCode,
                // Kotlin `Int`는 Swift에 `Int32`로 넘어온다.
                ratePercent: Int(query.percentOf(admCd2: topCode)),
                tagLabel: TabHomeStrings.tagMyDong
            )
        }
        guard let dong = currentDong else { return nil }
        return DongSummary(
            dongName: dong.name,
            dongCode: dong.code,
            ratePercent: Int(query?.percentOf(admCd2: dong.code) ?? 0),
            tagLabel: TabHomeStrings.tagCurrentLocation
        )
    }

    /// 동 이름 뒤(줌아웃)·앞(줌인)에 붙는 꼬리표.
    private var dongTagLabel: String? {
        guard let code = centerDong?.code else { return nil }
        if code == topExploredDongCode { return TabHomeStrings.tagMyDong }
        if code == currentDong?.code { return TabHomeStrings.tagCurrentLocation }
        return nil
    }

    var body: some View {
        // 패널은 **줌과 무관하게 하나**다(v2) — 예전의 줌아웃 전용 오버레이([HomeMapOverlay])로
        // 갈라지지 않는다.
        //
        // TODO: 동 상세 진입은 아직 — Android도 `onDongClick = {}`으로 비워 둔 상태다.
        ZoomedInMapOverlay(
            dongName: centerDong?.name ?? TabHomeStrings.currentLocation,
            tagLabel: dongTagLabel,
            ratePercent: ratePercent,
            exploredKm: exploredKm,
            totalKm: totalKm,
            selectedToggleIndex: selectedToggleIndex,
            onToggleSelect: { selectedToggleIndex = $0 },
            onDongClick: {},
            onCreateCourseClick: onCreateCourseClick,
            onRunStartClick: onRunStartClick,
            onMyLocationClick: { mapState.recenterToCurrentLocation() }
        )
        // retryToken이 바뀌면 다시 돈다 — 셸의 "다시 시도" 버튼이 그 값을 올린다.
        .task(id: retryToken) {
            // 위치를 못 받으면(권한 없음·위치 꺼짐) 동 조회를 건너뛴다 — 흔한 상황이라 오류가 아니다.
            // 이때는 실패로도 알리지 않는다(위치 안내 배너가 먼저 뜬다).
            guard let location = await RunvealCurrentLocation().fetch() else { return }
            // Kotlin suspend 함수는 Swift에 async throws로 들어온다(완료 핸들러가 NSError를
            // 받기 때문) — 던지면 못 받은 것으로 보고 실패를 알린다.
            guard let result = try? await IosCurrentDong.shared.fetch(lat: location.lat, lon: location.lon)
            else {
                onDongLoadFailedChange(true)
                return
            }
            currentDong = result.dong
            onDongLoadFailedChange(result.loadFailed)
        }
        // 카메라가 멈출 때마다 지도 가운데 동을 다시 묻는다 — `currentCenter`는 멈출 때만 바뀌므로
        // (regionDidChangeAnimated) 끌고 있는 동안에는 요청이 나가지 않는다.
        .task(id: centerKey) { await loadCenterDong(mapState.currentCenter) }
        // 지도 라벨도 같은 동을 "메인"으로 크게 그린다 — 패널 이름과 지도에서 도드라지는 동이
        // 달라지면 어디를 보고 있는지 헷갈린다.
        .onChange(of: centerDong?.code) { _, code in
            mapState.mainDongCode = code
        }
        // 카드 값은 **계산된 값 자체**를 기준으로 올린다 — 현재 위치 동이 늦게 오든 탐험률 집계가
        // 러닝 끝에 갱신되든 한 군데서 잡히고, 클로저가 옛 상태를 읽어 한 박자 늦게 올리는 일도
        // 없다. `initial: true`로 화면에 들어올 때 한 번 올린다 — 스플래시가 오버레이를 미리
        // 만들어 둬서 홈에 들어온 뒤로 아무것도 안 바뀌는 경우가 흔한데, 그때 안 올리면 카드가
        // 빈 채로 남는다.
        .onChange(of: summary, initial: true) { _, new in onSummaryChange(new) }
        .onDisappear { mapState.mainDongCode = nil }
        // 토글이 정한 보기 단위를 지도에 알린다 — 줌은 더 이상 이걸 정하지 않는다(v2).
        // 셸이 이 값으로 동 경계 묶음과 가본 길 소스를 가른다.
        .onChange(of: selectedToggleIndex, initial: true) { _, index in
            mapState.homeMapScale = index == toggleIndexRoad ? .road : .dong
        }
    }

    // 탐험률·거리는 지도 빛과 **같은 집계**에서 꺼낸다 — 다른 데서 계산하면 헤더는 8%인데 지도는
    // 안 빛나는 상태가 된다. 동 코드를 아직 못 받았거나 그 동이 집계에 없으면 0으로 둔다.
    //
    // 기준은 **지도 가운데 동**이다 — 이름은 가운데 동인데 퍼센트가 내가 선 동이면 둘이 어긋난다.
    private var ratePercent: Int {
        guard let code = centerDong?.code else { return 0 }
        // Kotlin `Int`는 Swift에 `Int32`로 넘어온다.
        return Int(overlayHolder.bundle?.query.percentOf(admCd2: code) ?? 0)
    }

    private var exploredKm: Double {
        guard let code = centerDong?.code else { return 0 }
        return (overlayHolder.bundle?.query.walkedMetersOf(admCd2: code) ?? 0) / 1000.0
    }

    private var totalKm: Double {
        guard let code = centerDong?.code else { return 0 }
        return (overlayHolder.bundle?.query.totalMetersOf(admCd2: code) ?? 0) / 1000.0
    }

    /**
     지도 가운데 동을 받아 온다.

     **조금 움직인 건 무시한다** — 멈출 때마다 묻게 두면 지도를 조금씩 미는 동안 요청이 쏟아지는데,
     줌아웃 화면에서 몇백 미터는 거의 같은 동이라 결과도 대개 똑같다.

     서비스 지역 밖(바다·해외)이나 실패면 이름을 비운다 — 옛 이름을 그대로 두면 지도를 바다로
     끌어도 엉뚱한 동네 이름이 남는다. 실패한 경우에는 "물어본 자리"를 되돌려 같은 자리에서 다시
     시도할 수 있게 한다.
     */
    private func loadCenterDong(_ center: LatLon) async {
        if let last = lastCenterQueried,
           IosGeo.shared.distanceMeters(from: last, to: center) < Self.centerDongRefreshDistanceM {
            return
        }
        lastCenterQueried = center
        guard let dong = try? await IosMapCenterDong.shared.fetch(lat: center.lat, lon: center.lon) else {
            lastCenterQueried = nil
            centerDong = nil
            return
        }
        // 그 사이 지도를 더 끌었으면 버린다 — 늦게 온 응답이 지금 보는 곳과 다른 동일 수 있다.
        guard lastCenterQueried?.lat == center.lat, lastCenterQueried?.lon == center.lon else { return }
        centerDong = dong
    }

}

/// 줌아웃 상태 오버레이 — 상단 배경 + 빛 이미지 + 현재 위치 라벨 + 헤드라인.
///
/// **v2에서 쓰이지 않는다** — 패널을 줌인 모양 하나로 통일하면서 띄울 자리가 없어졌다. 지우지 않고
/// 두는 건 줌아웃 전용 화면이 되살아날 여지가 남아서다.
private struct HomeMapOverlay: View {
    let locationName: String

    var body: some View {
        ZStack(alignment: .topLeading) {
            VStack(spacing: 0) {
                RunvealTheme.colors.bg.canvas
                    .frame(height: 200)
                Spacer()
            }
            .ignoresSafeArea(edges: .top)

            // 위아래로 서서히 사라지는 마스크를 씌운 빛 이미지 — Android는 `BlendMode.DstIn`으로
            // 같은 그라데이션을 적용한다.
            RunvealImages.homeLightImg
                .resizable()
                .scaledToFit()
                .mask(
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: .white, location: 0.5171),
                            .init(color: .white, location: 0.5211),
                            .init(color: .clear, location: 0.9471),
                            .init(color: .clear, location: 1),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .padding(.top, 72)
                .padding(.trailing, 40)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .allowsHitTesting(false)

            VStack(alignment: .leading, spacing: 0) {
                CurrentLocationLabel(locationName: locationName)
                Spacer().frame(height: 10)
                Text(TabHomeStrings.headline)
                    .runvealTextStyle(RunvealTheme.typography.heading1Semibold)
                    .foregroundStyle(RunvealTheme.colors.text.primary)
            }
            .padding(.leading, RunvealSpacing.space24)
            .padding(.top, 48)
            .padding(.trailing, RunvealLayout.marginScreen)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

private struct CurrentLocationLabel: View {
    let locationName: String

    var body: some View {
        let colors = RunvealTheme.colors
        HStack(spacing: RunvealSpacing.space4) {
            RunvealIcon(RunvealIcons.pin, tint: colors.icon.primary)
            Text(locationName)
                .runvealTextStyle(RunvealTheme.typography.bodyLSemibold)
                .foregroundStyle(colors.text.primary)
        }
    }
}

/// A3-4(줌 인) 오버레이. 지도/탭바는 제외, 요약 카드 + 하단 액션 줄만 담당.
private struct ZoomedInMapOverlay: View {
    let dongName: String
    /// 동 이름 앞에 붙는 꼬리표("내 동네" / "현위치"). nil이면 안 붙는다.
    let tagLabel: String?
    let ratePercent: Int
    let exploredKm: Double
    let totalKm: Double
    let selectedToggleIndex: Int
    let onToggleSelect: (Int) -> Void
    let onDongClick: () -> Void
    let onCreateCourseClick: () -> Void
    let onRunStartClick: () -> Void
    let onMyLocationClick: () -> Void

    @Environment(\.runvealMapOverlayBottomInset) private var overlayBottomInset

    var body: some View {
        ZStack(alignment: .top) {
            VStack(alignment: .leading, spacing: RunvealSpacing.space16) {
                ExploreSummaryCard(
                    dongName: dongName,
                    tagLabel: tagLabel,
                    ratePercent: ratePercent,
                    exploredKm: exploredKm,
                    totalKm: totalKm,
                    onClick: onDongClick
                )
                RunvealSegmentedToggle(
                    options: [TabHomeStrings.toggleScaleRoad, TabHomeStrings.toggleScaleDong],
                    selectedIndex: selectedToggleIndex,
                    onSelect: onToggleSelect
                )
                .padding(.leading, RunvealLayout.marginScreen)
                Spacer()
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack {
                Spacer()
                MapActionRow(
                    onCreateCourseClick: onCreateCourseClick,
                    onRunStartClick: onRunStartClick,
                    onMyLocationClick: onMyLocationClick,
                    bottomInset: overlayBottomInset
                )
            }
        }
    }
}

/// 하단 액션 줄 - 코스 만들기 / 러닝 시작 / 내 위치.
private struct MapActionRow: View {
    let onCreateCourseClick: () -> Void
    let onRunStartClick: () -> Void
    let onMyLocationClick: () -> Void
    /// 위치 권한/서비스 배너가 탭바 위에 얹혀 더 높아진 경우 Map Shell이 알려주는 차이.
    let bottomInset: CGFloat

    var body: some View {
        HStack(alignment: .bottom) {
            MapActionButton(icon: RunvealIcons.course, label: TabHomeStrings.createCourse, onClick: onCreateCourseClick)
            Spacer()
            RunvealRunStartButton(onClick: onRunStartClick)
            Spacer()
            MapActionButton(icon: RunvealIcons.locate, label: TabHomeStrings.myLocation, onClick: onMyLocationClick)
        }
        .padding(.horizontal, RunvealLayout.marginMapControls)
        // 플로팅 탭바(sizeTabBar + 상하 space8) 위로 띄운다.
        .padding(.bottom, RunvealLayout.sizeTabBar + RunvealSpacing.space32 + bottomInset)
        .frame(maxWidth: .infinity)
    }
}

private struct MapActionButton: View {
    let icon: RunvealIconSpec
    let label: String
    let onClick: () -> Void

    var body: some View {
        VStack(spacing: RunvealSpacing.space8) {
            RunvealIconButton(icon: icon, accessibilityLabel: label, onClick: onClick)
            Text(label)
                .runvealTextStyle(RunvealTheme.typography.captionSRegular)
                .foregroundStyle(RunvealTheme.colors.text.secondary)
        }
    }
}

/**
 줌아웃 요약 카드가 그릴 값 한 벌 — 카드가 가리키는 동의 이름·탐험률과 꼬리표.

 조회는 홈 지도 오버레이가 하고 카드는 홈 셸이 그리므로, 그 사이를 이 한 덩어리로 넘긴다.
 */
struct DongSummary: Equatable {
    let dongName: String
    let dongCode: String
    let ratePercent: Int
    /// "내 동네" / "현위치" / nil — 판정은 오버레이 쪽이 한다(탐험률 집계와 현재 위치를 거기서 안다).
    let tagLabel: String?
}

/// 상단 고정 헤더 카드. 아래 모서리만 둥글게(20 — 매칭되는 radius 토큰 없어 하드코딩) 상태 바에 붙는다.
private struct ExploreSummaryCard: View {
    let dongName: String
    let tagLabel: String?
    let ratePercent: Int
    let exploredKm: Double
    let totalKm: Double
    let onClick: () -> Void

    var body: some View {
        let colors = RunvealTheme.colors
        let shape = UnevenRoundedRectangle(
            topLeadingRadius: 0,
            bottomLeadingRadius: 20,
            bottomTrailingRadius: 20,
            topTrailingRadius: 0
        )

        VStack(alignment: .leading, spacing: RunvealSpacing.space8) {
            Button(action: onClick) {
                HStack(spacing: RunvealSpacing.space4) {
                    if let tagLabel {
                        Text(tagLabel)
                            .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                            .foregroundStyle(colors.text.secondary)
                    }
                    Text(dongName)
                        .runvealTextStyle(RunvealTheme.typography.heading3)
                        .foregroundStyle(colors.text.primary)
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            HStack {
                HStack(alignment: .lastTextBaseline, spacing: 0) {
                    Text("\(ratePercent)")
                        .runvealTextStyle(RunvealTheme.typography.numL)
                        .foregroundStyle(colors.text.accent)
                    Text(RunvealDesignSystemStrings.percentUnit)
                        .runvealTextStyle(RunvealTheme.typography.unitL)
                        .foregroundStyle(colors.text.accent)
                    Spacer().frame(width: RunvealSpacing.space4)
                    Text(TabHomeStrings.exploredSuffix)
                        .runvealTextStyle(RunvealTheme.typography.bodyLRegular)
                        .foregroundStyle(colors.text.primary)
                }
                Spacer()
                Text(TabHomeStrings.cumulative(exploredKm, totalKm))
                    .runvealTextStyle(RunvealTheme.typography.bodyMRegular)
                    .foregroundStyle(colors.text.tertiary)
                    .multilineTextAlignment(.trailing)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, RunvealLayout.marginScreen)
        .padding(.vertical, RunvealLayout.marginCardPadding)
        .frame(maxWidth: .infinity)
        // **배경만 상태 바 뒤까지 채운다** — 그래야 그 영역도 카드와 같은 색으로 보인다(안 그러면
        // 지도가 비친다). 내용은 safe area 아래에 그대로 남는다. Android가 배경을 먼저 칠하고
        // 그 위에 statusBars 인셋을 더하는 것과 같은 결과다. 하단 시트와 같은 컨벤션이다.
        .background {
            shape.fill(colors.surface.raised).ignoresSafeArea(edges: .top)
        }
    }
}
