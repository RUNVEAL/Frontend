package com.sloth.runveal.designsystem.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.theme.RunvealTheme

enum class RunvealToastType { Default, Error }

/**
 * 토스트. 불투명 배경(글래스 아님), 두 줄까지. 화면에 띄우고 3초 후 사라지게 하는 타이머는 호출하는
 * 쪽이 맡는다.
 *
 * **너비는 주어진 폭을 꽉 채운다**(시안 `Overlay/Toast`: "화면 좌우 16"은 마진이고 기본이 전체 폭).
 * 감싸는 쪽이 좌우 [RunvealTheme.layout.marginScreen]만 주면 된다 — 여기서 채우지 않으면 문구 길이에
 * 따라 너비가 들쭉날쭉해진다(짧은 안내는 좁고 긴 안내만 꽉 차 보인다).
 */
@Composable
fun RunvealToast(message: String, modifier: Modifier = Modifier, type: RunvealToastType = RunvealToastType.Default) {
    val colors = RunvealTheme.colors
    val shape = RunvealTheme.shapes.radius16

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.raised, shape)
            .border(1.dp, colors.border.subtle, shape)
            .padding(vertical = RunvealTheme.spacing.space12, horizontal = RunvealTheme.spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(RunvealTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (type == RunvealToastType.Error) {
            Icon(imageVector = RunvealIcons.Alert, contentDescription = null, tint = colors.status.error, modifier = Modifier.size(RunvealTheme.layout.sizeIcon))
        }
        Text(
            message,
            style = RunvealTheme.typography.bodyMRegular,
            color = colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun RunvealToastPreview() {
    RunvealTheme {
        RunvealToast(message = "코스를 저장했어요", type = RunvealToastType.Default)
    }
}
