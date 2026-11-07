package com.swyp.runveal.tabrecord

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swyp.runveal.designsystem.component.navigation.RunvealTopAppBarLarge
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabrecord.R
import com.swyp.runveal.tabrecord.component.MonthlyRunningSummary
import com.swyp.runveal.tabrecord.component.RecordEmptyState
import com.swyp.runveal.tabrecord.component.RecordListItem

// TODO: 월 선택 동작, 실제 러닝 기록 데이터 연동 전까지 쓰는 임시 플래그/샘플 데이터
private const val HAS_RECORDS = true

private data class SampleRecord(
    val dateLabel: String,
    val courseTitle: String,
    val distanceLabel: String,
    val durationLabel: String,
    val paceLabel: String,
)

private val SampleRecords = listOf(
    SampleRecord("11월 3일 화요일", "코스 A", "5.21km", "31:40", "6′05″"),
    SampleRecord("11월 1일 일요일", "코스 A", "7.30km", "45:12", "6′11″"),
)

@Composable
fun RecordScreen(modifier: Modifier = Modifier) {
    val colors = RunvealTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg.canvas)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState()),
        ) {
            RunvealTopAppBarLarge(
                title = "기록",
                metaLabel = "2026년 11월",
                titleStyle = RunvealTheme.typography.heading1,
                onMetaLabelClick = {},
                modifier = Modifier.fillMaxWidth(),
            )

            MonthlyRunningSummary(
                runCountLabel = "8회",
                totalDistanceValue = "32.40",
                totalDistanceUnit = "km",
                newRoadLabel = "새로 탐험한 길",
                newRoadValue = "+6.20",
                newRoadUnit = "km",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RunvealTheme.layout.marginScreen),
            )

            Image(
                painter = painterResource(R.drawable.record),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.spacing.space12)
                    .clip(RunvealTheme.shapes.radius16)
                    .aspectRatio(1816f / 405f),
            )

            if (HAS_RECORDS) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = RunvealTheme.layout.marginScreen),
                    verticalArrangement = Arrangement.spacedBy((-12).dp),
                ) {
                    SampleRecords.forEach { record ->
                        RecordListItem(
                            dateLabel = record.dateLabel,
                            courseTitle = record.courseTitle,
                            distanceLabel = record.distanceLabel,
                            durationLabel = record.durationLabel,
                            paceLabel = record.paceLabel,
                        )
                    }
                }
            } else {
                // 60dp에 맞는 spacing 토큰이 없어 그대로 하드코딩.
                RecordEmptyState(
                    onStartRunClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = RunvealTheme.layout.marginScreen,
                            end = RunvealTheme.layout.marginScreen,
                            top = 60.dp,
                        ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun RecordScreenPreview() {
    RunvealTheme {
        RecordScreen()
    }
}
