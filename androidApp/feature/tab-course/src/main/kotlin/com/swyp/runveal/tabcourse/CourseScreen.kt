package com.swyp.runveal.tabcourse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.swyp.runveal.designsystem.theme.RunvealTheme

@Composable
fun CourseScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunvealTheme.colors.bg.canvas),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "내 코스", color = RunvealTheme.colors.text.primary)
    }
}
