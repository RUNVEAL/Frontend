package com.swyp.runveal.tabmypage

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroup
import com.swyp.runveal.designsystem.component.button.RunvealButtonGroupPrimaryText
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.overlay.RunvealToast
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.external.ImportUnavailableReason
import com.swyp.runveal.feature.tabmypage.R
import com.swyp.runveal.health.HealthConnectImporter
import com.swyp.runveal.health.HealthConnectPermissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** `health.png`의 원본 크기(240x240px, xxxhdpi 4배). */
private val HEALTH_LOGO_SIZE = 60.dp

/** 안내 토스트 노출 시간(MyScreen과 같은 값). */
private const val TOAST_DURATION_MILLIS = 3000L

/**
 * Health Connect 기록 불러오기 안내(디자인 `A4-1`). 무엇을 가져오고 무엇이 안 되는지 먼저 알리고
 * 동의를 받는 화면이라, 실제 가져오기는 "불러오기"를 누른 뒤에 시작한다.
 *
 * "불러오기"를 누르면 권한부터 확인한다 — 다 갖춰졌을 때만 [onReadyToImport]로 진행 화면으로
 * 넘긴다. 모자라면 Health Connect 권한 화면을 띄우고, 거기서도 못 받으면 안내만 하고 머문다.
 * 권한은 Activity/런처에 묶여 있어 ViewModel이 아니라 화면이 직접 다룬다(MyScreen과 같은 방식).
 */
@Composable
internal fun ImportRecordsScreen(
    onBackClick: () -> Unit,
    onReadyToImport: () -> Unit,
    onLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 권한 확인에만 쓰는 인스턴스 — 가져오기는 진행 화면의 ViewModel이 제 것으로 돌린다.
    val importer = remember(context) { HealthConnectImporter(context.applicationContext) }
    var toastMessageRes by remember { mutableStateOf<Int?>(null) }

    // Health Connect 권한은 OS 다이얼로그가 아니라 Health Connect 앱 화면에서 사용자가 허용한다.
    // 돌아온 결과를 직접 보지 않고 다시 확인하는 건, 필요한 권한이 세션·경로 두 묶음이고 "무엇이
    // 모자라면 못 쓰는지" 판단이 importer에 이미 있어서다.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        scope.launch {
            if (importer.unavailableReason() == null) {
                onReadyToImport()
            } else {
                toastMessageRes = R.string.import_records_permission_denied
            }
        }
    }

    val onImportClick: () -> Unit = {
        scope.launch {
            when (importer.unavailableReason()) {
                null -> onReadyToImport()
                // 기기에 Health Connect가 없거나 업데이트가 필요한 경우 — 권한 요청을 띄울 수조차 없다.
                // TODO: Play 스토어의 Health Connect 설치 화면으로 보낸다.
                ImportUnavailableReason.PROVIDER_NOT_AVAILABLE ->
                    toastMessageRes = R.string.import_records_unavailable

                ImportUnavailableReason.PERMISSION_DENIED,
                ImportUnavailableReason.ROUTE_PERMISSION_DENIED,
                -> permissionLauncher.launch(HealthConnectPermissions.all)
            }
        }
        Unit
    }

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
                        stringResource(R.string.import_records_notice_no_route),
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(
                        horizontal = RunvealTheme.layout.marginScreen,
                        vertical = RunvealTheme.spacing.space16,
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
