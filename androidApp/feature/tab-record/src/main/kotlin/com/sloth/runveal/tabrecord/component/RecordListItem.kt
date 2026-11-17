package com.sloth.runveal.tabrecord.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.core.map.CourseSnapshotImage
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.map.snapshot.SnapshotRequest

/** 기록 목록의 한 줄 — 날짜 + 타임라인 마커(점/세로선) + 러닝 카드(지도/코스/거리·시간·페이스).
 * [thumbnailRequest]가 null이면(매칭 경로 없음) 지도 자리는 빈 플레이스홀더로 남는다.
 *
 * 탭·길게 누르기는 **카드에만** 건다 — 왼쪽 타임라인(점·세로선)은 줄을 잇는 장식이라 누를 자리가
 * 아니다. [onClick]은 아직 갈 곳이 없어 비어 있다(기록 상세 TODO). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordListItem(
    dateLabel: String,
    courseTitle: String,
    distanceLabel: String,
    durationLabel: String,
    paceLabel: String,
    modifier: Modifier = Modifier,
    thumbnailRequest: SnapshotRequest? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val colors = RunvealTheme.colors
    val cardShape = RunvealTheme.shapes.radius16
    val thumbnailShape = RunvealTheme.shapes.radius8

    // 타임라인 세로선 그라디언트 — 피그마 스펙(좌상단→우하단, 0/50/100% stop, 전체 opacity 50% 곱).
    // 0% 색은 color/gold/100 토큰(= colors.accent.default), 나머지는 이 선 전용이라 하드코딩.
    val timelineLineBrush = Brush.linearGradient(
        0f to colors.accent.default.copy(alpha = 1f * 0.5f),
        0.5f to Color(0xFFDFD4A7).copy(alpha = 0.726f * 0.5f),
        1f to Color(0xFF958E70).copy(alpha = 0f),
    )

    Row(
        modifier = modifier.height(154.dp),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.width(8.dp).height(154.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(1.dp)
                    .height(148.dp)
                    .background(timelineLineBrush),
            )
            // 타임라인 점 — 세로선과 짝. 색은 의미(semantic) 토큰이 아니라 RunvealTheme.palette의
            // 원시 gold 토큰을 직접 쓴다(Gold100 배경 + Gold500 테두리).
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RunvealTheme.palette.gold100)
                    .border(2.dp, RunvealTheme.palette.gold500, CircleShape),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        ) {
            Text(dateLabel, style = RunvealTheme.typography.captionMRegular, color = colors.text.tertiary, modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier
                    .height(106.dp)
                    .fillMaxWidth()
                    .clip(cardShape)
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .background(colors.surface.default, cardShape)
                    .border(1.dp, colors.border.default, cardShape)
                    .padding(RunvealTheme.spacing.space12),
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 달린 경로 지도 스냅샷 — 캐시가 없으면 렌더링이 끝날 때까지 빈 지도색으로 보인다.
                CourseSnapshotImage(
                    request = thumbnailRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(thumbnailShape)
                        .background(colors.bg.map, thumbnailShape)
                        .border(1.dp, colors.border.subtle, thumbnailShape),
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                    Text(courseTitle, style = RunvealTheme.typography.bodyLSemibold, color = colors.text.primary, modifier = Modifier.fillMaxWidth())
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(distanceLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                        Text(durationLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                        Text(paceLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.secondary)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecordListItemPreview() {
    RunvealTheme {
        RecordListItem(
            dateLabel = "11월 3일 화요일",
            courseTitle = "코스 A",
            distanceLabel = "5.21km",
            durationLabel = "31:40",
            paceLabel = "6′05″",
        )
    }
}
