package com.swyp.runveal.tabmypage.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.swyp.runveal.designsystem.component.stat.RunvealStat
import com.swyp.runveal.designsystem.component.stat.RunvealStatType
import com.swyp.runveal.designsystem.theme.RunvealTheme
import com.swyp.runveal.feature.tabmypage.R

/**
 * 마이페이지 맨 위 프로필 카드 — 닉네임과 러닝 요약 두 개(탐험한 동, 누적 거리).
 *
 * "탐험한 동"만 숫자를 금색으로 강조한다(`accent`) — 이 앱이 채워 가는 핵심 지표라 누적 거리와
 * 무게를 다르게 둔 시안 의도다.
 *
 * @param nickname null이면 닉네임 줄을 **그리지 않는다**(빈 줄로 자리를 차지하지도 않는다).
 *  로그인 없이 나가는 1차 출시에서 쓰는 모양이다 — 보여 줄 이름 자체가 없다.
 */
@Composable
internal fun ProfileSummaryCard(
    nickname: String?,
    exploredDongCount: Int,
    totalDistanceLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBackground, RunvealTheme.shapes.radius16)
            .padding(RunvealTheme.layout.marginCardPadding),
        verticalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space24),
    ) {
        if (nickname != null) {
            Text(
                text = nickname,
                style = RunvealTheme.typography.heading2,
                color = RunvealTheme.colors.text.primary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RunvealStat(
                value = exploredDongCount.toString(),
                label = stringResource(R.string.my_stat_explored_dong_label),
                unit = stringResource(R.string.my_stat_explored_dong_unit),
                type = RunvealStatType.Large,
                accent = true,
                modifier = Modifier.weight(1f),
            )
            RunvealStat(
                value = totalDistanceLabel,
                label = stringResource(R.string.my_stat_total_distance_label),
                unit = stringResource(R.string.my_stat_total_distance_unit),
                type = RunvealStatType.Large,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 카드 배경(시안 `#FFFAE8` 4%). 지금 디자인 토큰에는 이 값에 해당하는 semantic 색이 없다 —
 * `surface.*`는 전부 중성색이고, 값이 같은 `map.dong.fill2`는 지도 전용이라 의미가 안 맞는다.
 * TODO: 디자이너와 토큰 이름을 정해 `RunvealColors`로 옮긴다.
 */
private val CardBackground = Color(0xFFFFFAE8).copy(alpha = 0.04f)
