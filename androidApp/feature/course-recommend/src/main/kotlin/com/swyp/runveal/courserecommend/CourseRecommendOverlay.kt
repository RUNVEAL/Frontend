package com.swyp.runveal.courserecommend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 지도를 유지한 채 탭바 위로 올라가는 코스 추천 흐름(뼈대만). */
@Composable
internal fun CourseRecommendOverlay(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "코스 추천", color = RunvealTheme.colors.text.primary)
    }
}
