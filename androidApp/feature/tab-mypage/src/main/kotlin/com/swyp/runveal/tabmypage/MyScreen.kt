package com.swyp.runveal.tabmypage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swyp.runveal.designsystem.component.input.RunvealToggleSwitch
import com.swyp.runveal.designsystem.component.list.RunvealListItem
import com.swyp.runveal.designsystem.component.list.RunvealListItemTrailing
import com.swyp.runveal.designsystem.component.list.RunvealSectionHeader
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.swyp.runveal.designsystem.component.overlay.RunvealDialog
import com.swyp.runveal.designsystem.component.overlay.RunvealToast
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabmypage.R
import com.swyp.runveal.permission.openAppSettings
import com.swyp.runveal.permission.rememberHealthConnectPermissionGate
import com.swyp.runveal.permission.rememberLocationPermissionState
import com.swyp.runveal.tabmypage.component.ProfileSummaryCard
import com.swyp.runveal.tabmypage.contract.MyEffect
import com.swyp.runveal.tabmypage.contract.MyIntent
import com.swyp.runveal.tabmypage.contract.MyToast
import com.swyp.runveal.ui.mvi.CollectEffect
import kotlinx.coroutines.delay

/** `mypage_bg.png`의 원본 크기(2062x2032px, xxxhdpi 4배). 화면 너비(360dp)보다 넓은 건 의도 —
 * 양옆으로 번져 나가는 배경이라 가운데 정렬로 잘라 쓴다(ResultScreen의 result_glow와 같은 방식). */
private val MYPAGE_BG_WIDTH = 515.5.dp
private val MYPAGE_BG_HEIGHT = 508.dp

/**
 * 배경이 "설정" 섹션 바로 위에서 끝나도록 **내용 시작점 기준**으로 잡은 높이. 상단 앱바(84, 고정)
 * + 프로필 카드(패딩 20 + Stat 48 + 패딩 20 = 88) + 섹션 간격(32)이다.
 *
 * TODO(로그인 도입): 닉네임 줄(32)과 그 아래 간격(24)을 되살리면 카드가 144로 늘어난다 —
 *  이 값도 88 → 144로 되돌려야 배경이 "설정" 바로 위에서 끝난다.
 *
 * statusBar 높이는 기기마다 달라서 화면 맨 위 기준으로 잡으면 기기마다 끝나는 위치가 달라진다 —
 * 그래서 이미지에도 내용과 같은 statusBar 인셋을 주고 거기서부터 거리를 센다(인셋은 레이아웃만
 * 밀고 그리기는 안 막으므로, 아래 offset으로 끌어올리면 상태바 뒤까지 그대로 번진다).
 *
 * 닉네임이 두 줄이 되면 카드가 커져 조금 어긋나는데, 가장자리가 흐려지는 이미지라 티가 나지 않는다.
 */
private val MYPAGE_BG_VISIBLE_HEIGHT = 84.dp + 88.dp + 32.dp

/** 안내 토스트 노출 시간. */
private const val TOAST_DURATION_MILLIS = 3000L

@Composable
fun MyScreen(
    onLoggedOut: () -> Unit,
    onTermsClick: () -> Unit,
    onImportRecordsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            MyEffect.LoggedOut -> onLoggedOut()
        }
    }
    val context = LocalContext.current
    // 권한은 Activity/런처에 묶여 있어 ViewModel이 아니라 화면이 직접 다룬다(HomeScreen과 같은 방식).
    val locationPermissionState = rememberLocationPermissionState()

    // 앱은 이미 허용된 권한을 **끌 수 없다** — 끄려면 시스템 설정으로 보내야 한다. 2회 이상 거부해
    // 시스템이 더 이상 다이얼로그를 안 띄워 주는 상태(영구 거부)도 마찬가지다. 그 두 경우만 설정으로
    // 보내고, 나머지(미요청·1회 거부)는 그 자리에서 권한 다이얼로그를 띄운다.
    val onLocationPermissionClick = {
        if (locationPermissionState.isGranted || locationPermissionState.isPermanentlyDenied) {
            context.openAppSettings()
        } else {
            locationPermissionState.request()
        }
    }

    // Health Connect로 가는 입구마다 권한을 확인한다 — 여기서 받아 두면 안내 화면의 불러오기
    // 버튼에서 다시 묻지 않고 바로 넘어간다(안 받으면 그 버튼이 한 번 더 요청한다).
    val onImportRecordsItemClick = rememberHealthConnectPermissionGate(
        onGranted = onImportRecordsClick,
        onUnavailable = { viewModel.onIntent(MyIntent.ToastRequested(MyToast.HEALTH_CONNECT_UNAVAILABLE)) },
        onDenied = { viewModel.onIntent(MyIntent.ToastRequested(MyToast.HEALTH_CONNECT_PERMISSION_DENIED)) },
    )

    // 토스트는 3초만 띄운다 — 사라지는 타이머는 띄우는 쪽이 맡는다(RunvealToast 주석, HomeScreen과 같은 방식).
    LaunchedEffect(state.toast) {
        if (state.toast == null) return@LaunchedEffect
        delay(TOAST_DURATION_MILLIS)
        viewModel.onIntent(MyIntent.ToastShown)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas),
    ) {
        // 제일 뒤에 깔리는 배경 — statusBar 인셋을 안 받아서 그 밑까지 그대로 번진다. width()를 쓰면
        // 화면 너비로 coerce되면서 세로가 센터 정렬돼 상단에 빈 여백이 생기므로, requiredWidth/
        // requiredHeight로 원본 크기를 강제한다(ResultScreen의 result_glow와 같은 이유).
        //
        // 위로 끌어올려서 아래쪽 일부만 보이게 한다 — 이미지의 세로 중앙선이 statusBar 근처에 오고
        // 거기서부터 빛이 아래로 번지며, 바닥이 "설정" 바로 위에서 끝난다. offset은 레이아웃 크기를
        // 바꾸지 않고 그리는 위치만 옮기므로 다른 요소 배치에 영향을 주지 않는다.
        Image(
            painter = painterResource(R.drawable.mypage_bg),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .requiredWidth(MYPAGE_BG_WIDTH)
                .requiredHeight(MYPAGE_BG_HEIGHT)
                .offset(y = MYPAGE_BG_VISIBLE_HEIGHT - MYPAGE_BG_HEIGHT),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                // 하단 탭바는 HomeScreen이 이 화면 위에 띄우므로 여기서 그리지 않는다. 다만 목록이
                // 탭바 뒤로 들어가지 않도록 그 높이만큼 아래 여백을 둔다.
                .verticalScroll(rememberScrollState()),
        ) {
            RunvealTopAppBarLarge(
                title = stringResource(R.string.my_title),
                metaLabel = "",
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RunvealTheme.layout.marginScreen)
                    .padding(bottom = RunvealTheme.layout.sizeTabBar + RunvealTheme.spacing.space24),
                verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space32),
            ) {
                ProfileSummaryCard(
                    // TODO(로그인 도입): 로그인 없이 나가는 1차 출시라 보여 줄 이름이 없다 —
                    //  닉네임 줄을 통째로 비운다. 로그인을 켤 때 아래 줄을 되살린다.
                    //  nickname = state.nickname.ifEmpty { stringResource(R.string.my_nickname_placeholder) },
                    nickname = null,
                    exploredDongCount = state.exploredDongCount,
                    totalDistanceLabel = state.totalDistanceLabel,
                )

                Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24)) {
                    SettingsSection(
                        isLocationPermissionGranted = locationPermissionState.isGranted,
                        onLocationPermissionClick = onLocationPermissionClick,
                        onTermsClick = onTermsClick,
                        onImportRecordsClick = onImportRecordsItemClick,
                        onIntent = viewModel::onIntent,
                    )
                    // TODO(로그인 도입): 로그인 없이 나가는 1차 출시에서는 계정 섹션(로그아웃·회원
                    //  탈퇴)을 숨긴다 — 로그인한 적이 없으니 할 수 있는 게 없다. 로그인을 켤 때
                    //  이 줄을 되살리면 된다(AccountSection은 지우지 않고 그대로 뒀다).
                    // AccountSection(onIntent = viewModel::onIntent)
                }
            }
        }

        // RunvealDialog는 내용만 그리는 컴포넌트다(scrim·모달 동작은 호출부 몫, 그 KDoc 참고) —
        // Compose의 Dialog로 감싸 바깥 탭·뒤로가기로 닫히게 한다.
        if (state.showLogoutDialog) {
            Dialog(onDismissRequest = { viewModel.onIntent(MyIntent.LogoutDismissed) }) {
                RunvealDialog(
                    title = stringResource(R.string.my_logout_dialog_title),
                    description = stringResource(R.string.my_logout_dialog_description),
                    confirmLabel = stringResource(R.string.my_logout_dialog_confirm),
                    cancelLabel = stringResource(R.string.my_logout_dialog_cancel),
                    onConfirm = { viewModel.onIntent(MyIntent.LogoutConfirmed) },
                    onCancel = { viewModel.onIntent(MyIntent.LogoutDismissed) },
                )
            }
        }

        state.toast?.let { toast ->
            RunvealToast(
                message = stringResource(toast.messageRes),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space16),
            )
        }
    }
}

@Composable
private fun SettingsSection(
    isLocationPermissionGranted: Boolean,
    onLocationPermissionClick: () -> Unit,
    onTermsClick: () -> Unit,
    onImportRecordsClick: () -> Unit,
    onIntent: (MyIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RunvealSectionHeader(title = stringResource(R.string.my_section_settings))
        // TODO(로그인 도입): 로그인이 없으면 바꿀 닉네임 자체가 없어 숨긴다. 켤 때 되살린다.
        // RunvealListItem(
        //     title = stringResource(R.string.my_menu_change_nickname),
        //     onClick = { onIntent(MyIntent.NicknameChangeClicked) },
        //     trailing = RunvealListItemTrailing.Chevron,
        // )
        RunvealListItem(
            title = stringResource(R.string.my_menu_import_records),
            onClick = onImportRecordsClick,
            trailing = RunvealListItemTrailing.Chevron,
        )
        // 위치 권한만 chevron 대신 토글이라 RunvealListItem의 trailing으로 못 그린다 — 줄 전체와
        // 스위치가 같은 동작을 하도록 묶어 준다.
        //
        // 토글은 상태를 **비추기만** 한다: 눌러도 그 자리에서 뒤집히지 않고, 권한 다이얼로그나 앱
        // 설정 화면으로 간다. 거기서 바뀐 결과는 rememberLocationPermissionState가 ON_RESUME에
        // 다시 읽어 반영하므로, 돌아오면 토글이 실제 권한에 맞춰져 있다.
        Box(modifier = Modifier.fillMaxWidth()) {
            RunvealListItem(
                title = stringResource(R.string.my_menu_location_permission),
                description = stringResource(
                    if (isLocationPermissionGranted) {
                        R.string.my_menu_location_permission_granted
                    } else {
                        R.string.my_menu_location_permission_denied
                    },
                ),
                onClick = onLocationPermissionClick,
            )
            RunvealToggleSwitch(
                checked = isLocationPermissionGranted,
                onCheckedChange = { onLocationPermissionClick() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = RunvealTheme.spacing.space12),
            )
        }
        RunvealListItem(
            title = stringResource(R.string.my_menu_terms),
            onClick = onTermsClick,
            trailing = RunvealListItemTrailing.Chevron,
        )
    }
}

/** 로그인 도입 전까지는 화면에 올리지 않는다 — 위 호출부의 TODO 참고. */
@Suppress("unused")
@Composable
private fun AccountSection(onIntent: (MyIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RunvealSectionHeader(title = stringResource(R.string.my_section_account))
        RunvealListItem(
            title = stringResource(R.string.my_menu_logout),
            onClick = { onIntent(MyIntent.LogoutClicked) },
            trailing = RunvealListItemTrailing.Chevron,
        )
        // 회원 탈퇴는 목록 항목이 아니라 작은 글씨 한 줄 — 되돌릴 수 없는 행동이라 시안에서도
        // 눈에 덜 띄게 뒀다.
        Text(
            text = stringResource(R.string.my_menu_withdraw),
            style = RunvealTheme.typography.captionMRegular,
            color = RunvealTheme.colors.text.tertiary,
            modifier = Modifier
                // 작은 텍스트 한 줄이라 리플 없이 탭만 받는다.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onIntent(MyIntent.WithdrawClicked) },
                )
                .padding(vertical = RunvealTheme.spacing.space12),
        )
    }
}

/** 토스트 종류별 문구. 상태는 "무엇을 알릴지"만 들고 있고, 문구 선택은 화면 몫이다. */
private val MyToast.messageRes: Int
    get() = when (this) {
        MyToast.NOT_READY -> R.string.my_not_ready_toast
        MyToast.HEALTH_CONNECT_UNAVAILABLE -> R.string.import_records_unavailable
        MyToast.HEALTH_CONNECT_PERMISSION_DENIED -> R.string.import_records_permission_denied
    }
