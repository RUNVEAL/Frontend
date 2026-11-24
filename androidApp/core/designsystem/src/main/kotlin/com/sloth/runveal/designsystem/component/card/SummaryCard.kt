package com.sloth.runveal.designsystem.component.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sloth.runveal.designsystem.R
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.designsystem.component.map.RunvealDongThumbnail

/** 홈 요약 카드. Home Bottom Panel 안에서만 쓴다(배경 없음) — 카드 전체 터치 시 동 상세. */
@Composable
fun RunvealSummaryCard(
    dongName: String,
    ratePercent: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * 동 이름 뒤에 붙는 꼬리표("내 동네" / "현위치"). null이면 안 붙는다.
     *
     * 어떤 동일 때 어떤 문구인지는 **호출부가 정한다** — 디자인시스템은 "꼬리표 한 줄"만 알고
     * 그 의미(탐험률 최고 동인지, 지금 서 있는 곳인지)는 모른다.
     */
    tagLabel: String? = null,
    /**
     * 썸네일 오른쪽 `>` 아이콘을 눌렀을 때. null이면 아이콘을 아예 그리지 않는다 —
     * 눌러도 아무 일 없는 아이콘을 남기면 고장으로 보인다.
     */
    onChevronClick: (() -> Unit)? = null,
    /** 썸네일 프레임 안에 그릴 것 — 동 지도 그림. 안 주면 빈 프레임만 보인다. */
    thumbnail: @Composable BoxScope.() -> Unit = {},
) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius24

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = RunvealTheme.layout.marginCardPadding, vertical = RunvealTheme.spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dongName, style = RunvealTheme.typography.bodyMSemibold, color = colors.text.primary)
                if (tagLabel != null) {
                    Text(
                        tagLabel,
                        style = RunvealTheme.typography.captionSRegular,
                        color = colors.text.tertiary,
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$ratePercent",
                    style = RunvealTheme.typography.numDisplay,
                    color = colors.text.primary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    stringResource(R.string.percent_unit),
                    style = RunvealTheme.typography.unitDisplay,
                    color = colors.text.secondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }

        RunvealDongThumbnail(content = thumbnail)

        if (onChevronClick != null) {
            Icon(
                imageVector = RunvealIcons.Chevron,
                contentDescription = stringResource(R.string.summary_card_zoom_in_content_description),
                tint = colors.icon.tertiary,
                // 아이콘만 따로 받는다 — 카드 전체 터치(동 상세)와 다른 동작이라 겹치면 안 된다.
                modifier = Modifier
                    .clip(RunvealTheme.shapes.radius8)
                    .clickable(onClick = onChevronClick),
            )
        }
    }
}

@Preview
@Composable
private fun RunvealSummaryCardPreview() {
    RunvealTheme {
        RunvealSummaryCard(
            dongName = "성수동",
            ratePercent = 37,
            onClick = {},
            tagLabel = "내 동네",
        )
    }
}
