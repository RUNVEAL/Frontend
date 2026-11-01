package com.swyp.runveal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.auth.detachAuthPlatform
import com.swyp.runveal.auth.initAuthPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
