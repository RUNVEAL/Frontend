package com.sloth.runveal.tabcourse.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.core.map.CourseSnapshotImage
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButton
import com.sloth.runveal.designsystem.component.button.RunvealButtonSize
import com.sloth.runveal.designsystem.component.button.RunvealButtonStyle
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.map.snapshot.SnapshotRequest

/** 다음에 달려볼 코스 추천 카드 (내 코스 탭, TopAppBar 바로 아래). 저장한 코스가 있을 때 보여준다. */
@Composable
fun CourseRecommendationCard(
    badgeLabel: String,
    title: String,
    distanceLabel: String,
    durationLabel: String,
    tag: String,
    newRoadLabel: String,
    notRunLabel: String,
    /** 더보기(점 세 개) 메뉴 — null이면 아이콘 자체를 그리지 않는다([CourseMoreMenuIcon]). */
    moreMenu: CourseMoreMenuActions?,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailRequest: SnapshotRequest? = null,
) {
    val colors = RunvealTheme.colors
    val cardShape = RunvealTheme.shapes.radius24

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surface.default, cardShape)
            .border(1.dp, colors.border.default, cardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(colors.bg.map),
        ) {
            CourseSnapshotImage(
                request = thumbnailRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(RunvealTheme.spacing.space12)
                    .clip(RunvealTheme.shapes.full)
                    .background(colors.surface.raised, RunvealTheme.shapes.full)
                    // 10/6dp에 맞는 spacing 토큰이 없어 그대로 하드코딩.
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(badgeLabel, style = RunvealTheme.typography.captionSSemibold, color = colors.text.accent)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(18.dp)
                    .clip(RunvealTheme.shapes.full)
                    .background(colors.accent.glow, RunvealTheme.shapes.full)
                    .border(2.dp, colors.bg.map, RunvealTheme.shapes.full),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(RunvealTheme.spacing.space16),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                    Text(title, style = RunvealTheme.typography.heading3, color = colors.text.primary)

                    Column(verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(distanceLabel, style = RunvealTheme.typography.bodyLRegular, color = colors.text.secondary)
                            Text(durationLabel, style = RunvealTheme.typography.bodyLRegular, color = colors.text.secondary)
                            Box(
                                modifier = Modifier
                                    // 4dp 코너 반경 토큰이 없어 그대로 하드코딩.
                                    .background(colors.surface.tag, RoundedCornerShape(4.dp))
                                    .padding(horizontal = RunvealTheme.spacing.space4, vertical = RunvealTheme.spacing.space2),
                            ) {
                                Text(tag, style = RunvealTheme.typography.captionSRegular, color = colors.text.secondary)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(newRoadLabel, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.accent)
                            Text(notRunLabel, style = RunvealTheme.typography.bodyMRegular, color = colors.text.accent)
                        }
                    }
                }
                CourseMoreMenuIcon(actions = moreMenu)
            }

            RunvealButton(
                label = "이 코스로 러닝 시작",
                onClick = onStartClick,
                style = RunvealButtonStyle.Gold,
                size = RunvealButtonSize.L,
                fillWidth = true,
                leadingIcon = RunvealIcons.Play,
            )
        }
    }
}

@Preview
@Composable
private fun CourseRecommendationCardPreview() {
    RunvealTheme {
        CourseRecommendationCard(
            badgeLabel = "다음에 달려볼 코스",
            title = "코스 A",
            distanceLabel = "7.3km",
            durationLabel = "45분",
            tag = "순환",
            newRoadLabel = "새 길 1.3km",
            notRunLabel = "아직 안달림",
            moreMenu = CourseMoreMenuActions(onRename = {}, onExportGpx = {}, onDelete = {}),
            onStartClick = {},
        )
    }
}
