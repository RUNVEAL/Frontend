package com.sloth.runveal.designsystem.component.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.Dp
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.button.RunvealButton
import com.sloth.runveal.designsystem.component.button.RunvealButtonStyle
import com.sloth.runveal.designsystem.component.button.RunvealIconButton
import com.sloth.runveal.designsystem.component.button.RunvealIconButtonSize
import com.sloth.runveal.designsystem.theme.RunvealTheme

/** Close=흐름 종료(러닝 결과), Back=하위 화면, None=왼쪽 아이콘 없음(탭 첫 화면). */
enum class RunvealTopAppBarLeading { Close, Back, None }

/** Default=일반 화면, Map=지도 위 화면용(왼쪽 버튼이 글래스 원형 Icon Button M). */
enum class RunvealTopAppBarStyle { Default, Map }

/**
 * 상단 앱바. 배경 투명(화면 위에 얹음). 오른쪽 Text 버튼(건너뛰기 등)은 [actionLabel]이 있을 때만
 * 보이고, 글자 대신 아이콘 같은 걸 놓고 싶으면 [action] 슬롯에 직접 넣는다([RunvealTopAppBarLarge]와
 * 같은 방식). 둘 다 주면 [action]이 이긴다.
 */
@Composable
fun RunvealTopAppBar(
    leading: RunvealTopAppBarLeading,
    onLeadingClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RunvealTopAppBarStyle = RunvealTopAppBarStyle.Default,
    leadingContentDescription: String? = null,
    title: String? = null,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    action: (@Composable () -> Unit)? = null,
) {
    val colors = RunvealTheme.colors
    val leadingIcon = when (leading) {
        RunvealTopAppBarLeading.Close -> RunvealIcons.Close
        RunvealTopAppBarLeading.Back -> RunvealIcons.Back
        RunvealTopAppBarLeading.None -> null
    }
    val paddingStart: Dp
    val paddingEnd: Dp
    val gap: Dp
    when {
        style == RunvealTopAppBarStyle.Map -> {
            paddingStart = RunvealTheme.spacing.space16
            paddingEnd = RunvealTheme.spacing.space16
            gap = RunvealTheme.spacing.space12
        }
        leading == RunvealTopAppBarLeading.None -> {
            paddingStart = RunvealTheme.spacing.space16
            paddingEnd = RunvealTheme.spacing.space4
            gap = RunvealTheme.spacing.space4
        }
        else -> {
            paddingStart = RunvealTheme.spacing.space4
            paddingEnd = RunvealTheme.spacing.space16
            gap = RunvealTheme.spacing.space4
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RunvealTheme.layout.sizeTopAppBar)
            .padding(start = paddingStart, end = paddingEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            if (style == RunvealTopAppBarStyle.Map) {
                RunvealIconButton(
                    icon = leadingIcon,
                    contentDescription = leadingContentDescription,
                    onClick = onLeadingClick,
                    size = RunvealIconButtonSize.M,
                )
            } else {
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
            }
            Spacer(Modifier.width(gap))
        }
        val hasAction = action != null || actionLabel != null
        if (title != null) {
            Text(
                title,
                style = RunvealTheme.typography.heading3,
                color = colors.text.primary,
                modifier = if (hasAction) Modifier.weight(1f) else Modifier,
            )
        } else if (hasAction) {
            Spacer(Modifier.weight(1f))
        }
        when {
            action != null -> action()
            actionLabel != null -> RunvealButton(label = actionLabel, onClick = onActionClick, style = RunvealButtonStyle.Text)
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

@Preview
@Composable
private fun RunvealTopAppBarMapPreview() {
    RunvealTheme {
        RunvealTopAppBar(
            leading = RunvealTopAppBarLeading.Back,
            onLeadingClick = {},
            style = RunvealTopAppBarStyle.Map,
            actionLabel = "건너뛰기",
            onActionClick = {},
        )
    }
}
