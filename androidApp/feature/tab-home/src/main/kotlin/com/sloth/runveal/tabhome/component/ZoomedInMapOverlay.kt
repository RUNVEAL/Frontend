package com.sloth.runveal.tabhome.component

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sloth.runveal.core.map.LocalMapOverlayBottomInset
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealIconButton
import com.sloth.runveal.designsystem.component.button.RunvealRunStartButton
import com.sloth.runveal.designsystem.component.navigation.RunvealSegmentedToggle
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabhome.R

/** 헤더의 보조 글자(내 동네, 누적·전체 거리)에 거는 투명도 — 시안은 색 토큰 대신 블록 투명도를 쓴다. */
private const val HEADER_SUB_TEXT_ALPHA = 0.8f

/** 지도 하단 컨트롤 줄 높이(시안) — 러닝 시작 버튼(92)보다 커서 위아래로 조금씩 남는다. */
private val CONTROLS_HEIGHT = 100.dp

/** "누적 35.1km"처럼 라벨 뒤에 거리를 붙인다. 소수 한 자리까지만 — 미터 단위 원자료를 그대로 쓰면
 * "3.4821…km"가 된다. */
private fun AnnotatedString.Builder.appendDistanceLine(label: String, km: Double, valueColor: Color) {
    append("$label ")
    withStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.SemiBold)) {
        append("%.1fkm".format(km))
    }
}

/**
 * 보기 단위 토글의 인덱스 — 아래 옵션 목록 순서와 짝이다. 지도에 가본 길을 보여줄지(길 단위)
 * 행정동 경계를 보여줄지(동네 단위)를 이 선택만으로 정한다(줌은 관여하지 않는다).
 */
internal const val TOGGLE_INDEX_ROAD = 0
internal const val TOGGLE_INDEX_DONG = 1

/** A3-4(줌 인) 오버레이. 지도/탭바는 제외, 요약 카드 + 하단 액션 줄만 담당. */
@Composable
fun ZoomedInMapOverlay(
    dongName: String,
    /** 동 이름 앞에 붙는 꼬리표("내 동네" / "현위치"). 어떤 동일 때 무엇인지는 호출부가 정한다. */
    tagLabel: String?,
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
                tagLabel = tagLabel,
                ratePercent = ratePercent,
                exploredKm = exploredKm,
                totalKm = totalKm,
                onClick = onDongClick,
                modifier = Modifier.fillMaxWidth(),
            )
            RunvealSegmentedToggle(
                options = listOf(
                    stringResource(R.string.home_overlay_level_road),
                    stringResource(R.string.home_overlay_level_dong),
                ),
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
                // 탭바(sizeTabBar)가 내비게이션 바 위 space8에 떠 있고, 이 줄은 그 위 space8이다(시안).
                bottom = RunvealTheme.layout.sizeTabBar + RunvealTheme.spacing.space16 +
                    LocalMapOverlayBottomInset.current,
            )
            .height(CONTROLS_HEIGHT),
        horizontalArrangement = Arrangement.SpaceBetween,
        // 시안은 100 높이 안에서 가운데 정렬이라, 러닝 시작(92)과 양옆 버튼의 아래가 안 맞는다.
        verticalAlignment = Alignment.CenterVertically,
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
    tagLabel: String?,
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
            if (tagLabel != null) {
                Text(
                    tagLabel,
                    style = RunvealTheme.typography.bodyLRegular,
                    color = colors.text.secondary,
                    // 시안은 색 토큰을 바꾸지 않고 글자 블록에 투명도를 건다.
                    modifier = Modifier.alpha(HEADER_SUB_TEXT_ALPHA),
                )
            }
            Text(dongName, style = RunvealTheme.typography.heading3, color = colors.text.primary)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
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
                buildAnnotatedString {
                    // 라벨("누적")과 거리의 굵기·색이 다르다 — 거리가 더 밝고 굵다(시안).
                    appendDistanceLine(label = "누적", km = exploredKm, valueColor = colors.text.secondary)
                    append("\n")
                    appendDistanceLine(label = "전체", km = totalKm, valueColor = colors.text.secondary)
                },
                style = RunvealTheme.typography.bodyMRegular,
                color = colors.text.tertiary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).alpha(HEADER_SUB_TEXT_ALPHA),
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
        Text(label, style = RunvealTheme.typography.captionMRegular, color = RunvealTheme.colors.text.secondary)
    }
}
