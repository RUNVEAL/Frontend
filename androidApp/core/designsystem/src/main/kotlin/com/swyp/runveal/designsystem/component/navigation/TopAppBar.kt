package com.swyp.runveal.designsystem.component.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.swyp.runveal.designsystem.RunvealIcons
import com.swyp.runveal.designsystem.theme.RunvealTheme

/** Close=흐름 종료(러닝 결과), Back=하위 화면. */
enum class RunvealTopAppBarLeading { Close, Back }

/** 상단 앱바. 배경 투명(화면 위에 얹음) — 배경 없는 순수 아이콘, 터치 영역만 48 확보. */
@Composable
fun RunvealTopAppBar(
    leading: RunvealTopAppBarLeading,
    onLeadingClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContentDescription: String? = null,
    title: String? = null,
) {
    val colors = RunvealTheme.colors
    val leadingIcon = when (leading) {
        RunvealTopAppBarLeading.Close -> RunvealIcons.Close
        RunvealTopAppBarLeading.Back -> RunvealIcons.Back
    }
    Row(
        modifier = modifier
            .height(RunvealTheme.layout.sizeTopAppBar)
            .padding(start = RunvealTheme.spacing.space4, end = RunvealTheme.spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(RunvealTheme.layout.sizeTouchTargetMin)
                .clickable(onClick = onLeadingClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = leadingContentDescription,
                tint = colors.icon.primary,
                modifier = Modifier.size(RunvealTheme.layout.sizeIcon),
            )
        }
        if (title != null) {
            Box(Modifier.width(RunvealTheme.spacing.space4))
            Text(title, style = RunvealTheme.typography.heading3, color = colors.text.primary)
        }
    }
}

@Preview
@Composable
private fun RunvealTopAppBarPreview() {
    RunvealTheme {
        RunvealTopAppBar(leading = RunvealTopAppBarLeading.Close, onLeadingClick = {}, title = "러닝 완료!")
    }
}
