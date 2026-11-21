package com.sloth.runveal.tabmypage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroup
import com.sloth.runveal.designsystem.component.button.RunvealButtonGroupPrimaryText
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.component.overlay.RunvealToast
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabmypage.R
import com.sloth.runveal.permission.rememberHealthConnectPermissionGate
import kotlinx.coroutines.delay

/** `health.png`의 원본 크기(240x240px, xxxhdpi 4배). */
private val HEALTH_LOGO_SIZE = 60.dp

/** 안내 토스트 노출 시간(MyScreen과 같은 값). */
private const val TOAST_DURATION_MILLIS = 3000L

/**
 * Health Connect 기록 불러오기 안내(디자인 `A4-1`). 무엇을 가져오고 무엇이 안 되는지 먼저 알리고
 * 동의를 받는 화면이라, 실제 가져오기는 "불러오기"를 누른 뒤에 시작한다.
 *
 * Health Connect 권한을 묻는 자리는 여기 하나다 — 마이페이지의 "기록 불러오기"는 이 화면으로만
 * 넘기고, 권한은 안내를 읽고 "불러오기"를 누른 뒤에 요청한다. 다 갖춰졌을 때만 [onReadyToImport]로
 * 진행 화면으로 넘기고, 모자라면 Health Connect 권한 화면을 띄운다 — 거기서도 못 받으면 안내만
 * 하고 머문다. 권한은 Activity/런처에 묶여 있어 ViewModel이 아니라 화면이 직접 다룬다.
 */
@Composable
internal fun ImportRecordsScreen(
    onBackClick: () -> Unit,
    onReadyToImport: () -> Unit,
    onLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var toastMessageRes by remember { mutableStateOf<Int?>(null) }

    // 권한은 Activity/런처에 묶여 있어 ViewModel이 아니라 화면이 직접 다룬다(MyScreen의 위치
    // 권한과 같은 방식). 들어올 때 미리 확인하지 않고 이 버튼을 누른 순간 확인한다 — 그 사이
    // Health Connect 앱에서 권한을 끄고 돌아왔을 수도 있다.
    val onImportClick = rememberHealthConnectPermissionGate(
        onGranted = onReadyToImport,
        // TODO: Play 스토어의 Health Connect 설치 화면으로 보낸다.
        onUnavailable = { toastMessageRes = R.string.import_records_unavailable },
        onDenied = { toastMessageRes = R.string.import_records_permission_denied },
    )

    // 토스트는 3초만 띄운다 — 사라지는 타이머는 띄우는 쪽이 맡는다(RunvealToast 주석).
    LaunchedEffect(toastMessageRes) {
        if (toastMessageRes == null) return@LaunchedEffect
        delay(TOAST_DURATION_MILLIS)
        toastMessageRes = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            RunvealTopAppBar(
                leading = RunvealTopAppBarLeading.Back,
                onLeadingClick = onBackClick,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = RunvealTheme.layout.marginScreen)
                    .padding(top = RunvealTheme.spacing.space24),
                verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
            ) {
                // 색이 들어 있는 그래픽이라 Icon(tint)이 아니라 Image로 그린다.
                // design/images/health.png — 240x240px(= 60dp, xxxhdpi 4배).
                Image(
                    painter = painterResource(R.drawable.health),
                    contentDescription = null,
                    modifier = Modifier.size(HEALTH_LOGO_SIZE),
                )

                Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
                    Text(
                        text = stringResource(R.string.import_records_title),
                        style = RunvealTheme.typography.heading2,
                        color = RunvealTheme.colors.text.primary,
                    )
                    Text(
                        text = stringResource(R.string.import_records_description),
                        style = RunvealTheme.typography.bodyMRegular,
                        color = RunvealTheme.colors.text.secondary,
                    )
                }

                NoticeCard(
                    notices = listOf(
                        stringResource(R.string.import_records_notice_what),
                        stringResource(R.string.import_records_notice_skipped),
                        stringResource(R.string.import_records_notice_disconnect),
                    ),
                )
            }

            RunvealButtonGroup(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) {
                RunvealButtonGroupPrimaryText(
                    primaryLabel = stringResource(R.string.import_records_start),
                    onPrimaryClick = onImportClick,
                    textLabel = stringResource(R.string.import_records_later),
                    onTextClick = onLaterClick,
                )
            }
        }

        toastMessageRes?.let { messageRes ->
            RunvealToast(
                message = stringResource(messageRes),
                // 시안(Overlay/Toast)은 하단 — 내비게이션 바 위 8.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(
                        start = RunvealTheme.layout.marginScreen,
                        end = RunvealTheme.layout.marginScreen,
                        bottom = RunvealTheme.spacing.space8,
                    ),
            )
        }
    }
}

/**
 * 안내 문구 묶음. 위치 권한 시트(`LocationPermissionSheet`)에 같은 모양이 private으로 하나 더 있다 —
 * 배경색이 달라(그쪽은 시트 위라 `bg.canvas`) 공용화하지 않았다. 세 번째 쓰임이 생기면 그때
 * 디자인시스템으로 올리는 게 맞다.
 */
@Composable
private fun NoticeCard(notices: List<String>, modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface.default, RunvealTheme.shapes.radius16)
            .padding(RunvealTheme.layout.marginCardPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8)) {
            notices.forEach { notice ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 불릿은 색이 들어 있는 벡터라 tint를 주지 않는다.
                    Icon(
                        imageVector = RunvealIcons.Bullet,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(8.dp),
                    )
                    Text(
                        text = notice,
                        style = RunvealTheme.typography.bodyMRegular,
                        color = colors.text.secondary,
                    )
                }
            }
        }
    }
}
