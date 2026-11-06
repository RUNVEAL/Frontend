package com.swyp.runveal.courserecommend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBar
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLeading
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarStyle
import com.swyp.runveal.designsystem.theme.RunvealTheme

@Composable
fun CourseCreateScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    var avoidVisitedPreferred by remember { mutableStateOf(true) }

    // 지도(Map Shell)는 그대로 보이고 이 위에 앱바/패널만 얹는다 — 배경을 깔지 않는다.
    Box(modifier = modifier.fillMaxSize()) {
        RunvealTopAppBar(
            leading = RunvealTopAppBarLeading.Back,
            onLeadingClick = onBack,
            style = RunvealTopAppBarStyle.Map,
            title = "코스 만들기",
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        )

        // TODO: 지도, 출발/도착 선택, 거리 조정, 경로 추천 연동 추가 예정
        MakeCoursePanel(
            startLocation = "현위치",
            endLocation = "출발지로 돌아오기",
            distanceLabel = "5km",
            avoidVisitedPreferred = avoidVisitedPreferred,
            onStartLocationClick = {},
            onEndLocationClick = {},
            onDistanceClick = {},
            onAvoidVisitedPreferredChange = { avoidVisitedPreferred = it },
            onFilterClick = {},
            onRecommendClick = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(
                    horizontal = RunvealTheme.layout.marginScreen,
                    vertical = RunvealTheme.spacing.space8,
                ),
        )
    }
}

@Preview
@Composable
private fun CourseCreateScreenPreview() {
    RunvealTheme {
        CourseCreateScreen(onBack = {})
    }
}
