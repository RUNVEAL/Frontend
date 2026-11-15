package com.swyp.runveal.tabhome.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swyp.runveal.core.map.LocalMapOverlayBottomInset
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.component.button.RunvealIconButton
import com.swyp.runveal.designsystem.component.button.RunvealRunStartButton
import com.swyp.runveal.designsystem.component.navigation.RunvealSegmentedToggle
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** 보기 전환 토글에서 "안 가본 길"의 인덱스 — 아래 옵션 목록 순서와 짝이다. */
internal const val TOGGLE_INDEX_UNEXPLORED = 1

/** A3-4(줌 인) 오버레이. 지도/탭바는 제외, 요약 카드 + 하단 액션 줄만 담당. */
@Composable
fun ZoomedInMapOverlay(
    dongName: String,
    ratePercent: Int,
    exploredKm: Double,
    totalKm: Double,
    selectedToggleIndex: Int,
    onToggleSelect: (Int) -> Unit,
    onDongClick: () -> Unit,
    onCreateCourseClick: () -> Unit,
    onRunStartClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
        ) {
            ExploreSummaryCard(
                dongName = dongName,
                ratePercent = ratePercent,
                exploredKm = exploredKm,
                totalKm = totalKm,
                onClick = onDongClick,
                modifier = Modifier.fillMaxWidth(),
            )
            RunvealSegmentedToggle(
                options = listOf("탐험한 길", "안 가본 길"),
                selectedIndex = selectedToggleIndex,
                onSelect = onToggleSelect,
                modifier = Modifier.padding(start = RunvealTheme.layout.marginScreen),
            )
        }

        MapActionRow(
            onCreateCourseClick = onCreateCourseClick,
            onRunStartClick = onRunStartClick,
            onMyLocationClick = onMyLocationClick,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** 하단 액션 줄 - 코스 만들기 / 러닝 시작 / 내 위치. */
@Composable
private fun MapActionRow(
    onCreateCourseClick: () -> Unit,
    onRunStartClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 탭바와 같은 기준선에서 재야 한다 — 탭바도 이 inset 위에 떠 있어서, 이게 빠지면 제스처
            // 바/3버튼 바가 두꺼운 기기(예: 플립)에서 이 줄만 그만큼 내려와 탭바와 겹친다.
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(
                start = RunvealTheme.layout.marginMapControls,
                end = RunvealTheme.layout.marginMapControls,
                // 플로팅 탭바(sizeTabBar + 상하 space8) 위로 띄운다. 위치 권한/서비스 배너가 탭바 위에
                // 얹혀 더 높아진 경우는 Map Shell이 LocalMapOverlayBottomInset으로 그 차이를 알려준다.
                bottom = RunvealTheme.layout.sizeTabBar + RunvealTheme.spacing.space32 + LocalMapOverlayBottomInset.current,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        MapActionButton(icon = RunvealIcons.Course, label = "코스 만들기", onClick = onCreateCourseClick)
        RunvealRunStartButton(onClick = onRunStartClick)
        MapActionButton(icon = RunvealIcons.Locate, label = "내 위치", onClick = onMyLocationClick)
    }
}

/** 상단 고정 헤더 카드. 아래 모서리만 둥글게(20dp — 매칭되는 radius 토큰 없어 하드코딩) 상태 바에 바로 붙는다. */
@Composable
private fun ExploreSummaryCard(
    dongName: String,
    ratePercent: Int,
    exploredKm: Double,
    totalKm: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunvealTheme.colors
    val shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface.raised, shape)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = RunvealTheme.layout.marginScreen, vertical = RunvealTheme.layout.marginCardPadding),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("내 동네", style = RunvealTheme.typography.bodyLRegular, color = colors.text.secondary)
            Text(dongName, style = RunvealTheme.typography.heading3, color = colors.text.primary)
            Icon(
                imageVector = RunvealIcons.Chevron,
                contentDescription = null,
                tint = colors.icon.tertiary,
                modifier = Modifier.size(14.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$ratePercent",
                    style = RunvealTheme.typography.numL,
                    color = colors.text.accent,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "%",
                    style = RunvealTheme.typography.unitL,
                    color = colors.text.accent,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(modifier = Modifier.width(RunvealTheme.spacing.space4))
                Text(
                    "탐험했어요",
                    style = RunvealTheme.typography.bodyLRegular,
                    color = colors.text.primary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Text(
                "누적 ${exploredKm}km\n전체 ${totalKm}km",
                style = RunvealTheme.typography.bodyMRegular,
                color = colors.text.tertiary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun MapActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
    ) {
        RunvealIconButton(icon = icon, contentDescription = label, onClick = onClick)
        Text(label, style = RunvealTheme.typography.captionSRegular, color = RunvealTheme.colors.text.secondary)
    }
}
