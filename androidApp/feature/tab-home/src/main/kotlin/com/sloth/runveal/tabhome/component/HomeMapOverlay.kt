package com.sloth.runveal.tabhome.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabhome.R

@Composable
fun HomeMapOverlay(locationName: String, modifier: Modifier = Modifier) {
    val lightImageFade = Brush.verticalGradient(
        0f to Color.Transparent,
        0.5171f to Color.White,
        0.5211f to Color.White,
        0.9471f to Color.Transparent,
        1f to Color.Transparent,
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(200.dp)
                .background(RunvealTheme.colors.bg.canvas),
        )

        Image(
            painter = painterResource(R.drawable.home_light_img),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 40.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(brush = lightImageFade, blendMode = BlendMode.DstIn)
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = RunvealTheme.spacing.space24, top = 48.dp, end = RunvealTheme.layout.marginScreen),
        ) {
            CurrentLocationLabel(locationName = locationName)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "오늘도 새로운 길을\n밝혀볼까요?",
                style = RunvealTheme.typography.heading1Semibold,
                color = RunvealTheme.colors.text.primary,
            )
        }
    }
}
