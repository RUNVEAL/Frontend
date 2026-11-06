package com.swyp.runveal

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.auth.detachAuthPlatform
import com.swyp.runveal.auth.initAuthPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 다크 모드 단일 테마라 시스템 설정과 무관하게 상태 바·내비게이션 바 아이콘을 항상 흰색(light) 고정.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // 소셜 로그인(sharedLogic의 auth 패키지)이 쓰는 Context/Activity/SDK 초기화.
        initAuthPlatform(this)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        detachAuthPlatform(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
