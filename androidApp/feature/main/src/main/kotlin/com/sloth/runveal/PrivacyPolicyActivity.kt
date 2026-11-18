package com.sloth.runveal

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.sloth.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.main.R
import com.sloth.runveal.terms.TermsDocument
import com.sloth.runveal.ui.web.RunvealWebView

/**
 * 개인정보 처리방침만 띄우는 화면. **Health Connect 권한 근거 화면**이 여기로 들어온다 — 권한
 * 화면의 "개인정보처리방침" 링크를 누르면 열린다.
 *
 * 앱 본체(`MainActivity`)가 아니라 따로 둔 이유: 이 인텐트는 앱이 꺼져 있을 때도 들어온다.
 * `MainActivity`로 받으면 스플래시 → 로그인 판정을 거쳐 앱 첫 화면이 열려 버리고, 정작 보려던
 * 처리방침은 안 나온다. 여기로 받으면 앱 상태와 무관하게 문서만 보여 주고 뒤로가기로 끝난다.
 *
 * 마이페이지의 약관 화면과 **같은 원문**을 본다([TermsDocument.privacyUrl]).
 */
class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 다크 모드 단일 테마라 시스템 설정과 무관하게 상태 바 아이콘을 항상 흰색 고정(MainActivity와 같다).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            RunvealTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RunvealTheme.colors.bg.canvas)
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    RunvealTopAppBar(
                        leading = RunvealTopAppBarLeading.Back,
                        onLeadingClick = { finish() },
                        title = getString(R.string.privacy_policy_title),
                    )
                    RunvealWebView(url = TermsDocument.privacyUrl, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
