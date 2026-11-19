package com.sloth.runveal.tabcourse.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.sloth.runveal.designsystem.RunvealIcons
import com.sloth.runveal.designsystem.component.overlay.RunvealMenu
import com.sloth.runveal.designsystem.component.overlay.RunvealMenuItem
import com.sloth.runveal.designsystem.component.overlay.RunvealMenuItemType
import com.sloth.runveal.designsystem.theme.RunvealTheme
import com.sloth.runveal.feature.tabcourse.R

/** 더보기 아이콘 크기 — 목록/카드 어디서나 같다(토큰 없이 디자인 값 그대로). */
private val MORE_ICON_SIZE = 24.dp

/** 아이콘과 메뉴 사이 간격. */
private val MENU_ANCHOR_GAP = 4.dp

/** 저장한 코스 하나에 대해 더보기 메뉴가 할 일들. */
data class CourseMoreMenuActions(
    val onRename: () -> Unit,
    val onExportGpx: () -> Unit,
    val onDelete: () -> Unit,
)

/**
 * 더보기(⋯) 아이콘 + 눌렀을 때 그 아래에 뜨는 메뉴(이름 수정·GPX 내보내기·삭제).
 *
 * 메뉴 모양은 디자인시스템([RunvealMenu])이 그리고 여기서는 띄울 자리와 닫기만 맡는다. 열렸는지는
 * 순수 UI 상태라 이 컴포저블이 직접 들고 있고, 항목을 고르면 메뉴를 먼저 닫고 [CourseMoreMenuActions]를
 * 부른다 — 부르는 쪽이 매번 닫아 주지 않아도 되게.
 *
 * [actions]가 null이면 **아이콘 자체를 그리지 않는다** — 열 메뉴가 없는데 아이콘만 달려 있으면
 * 눌러 보게 되고, 눌러도 아무 일이 없어 고장으로 보인다.
 */
@Composable
internal fun CourseMoreMenuIcon(actions: CourseMoreMenuActions?, modifier: Modifier = Modifier) {
    if (actions == null) return

    var isMenuOpen by remember { mutableStateOf(false) }
    val gapPx = with(LocalDensity.current) { MENU_ANCHOR_GAP.roundToPx() }
    val positionProvider = remember(gapPx) { MenuBelowAnchorPositionProvider(gapPx) }

    Box(modifier = modifier) {
        Icon(
            imageVector = RunvealIcons.More,
            contentDescription = stringResource(R.string.tab_course_more_content_description),
            tint = RunvealTheme.colors.icon.tertiary,
            modifier = Modifier
                .size(MORE_ICON_SIZE)
                .clickable { isMenuOpen = true },
        )

        if (isMenuOpen) {
            val closeMenu = { isMenuOpen = false }
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = closeMenu,
                // focusable이어야 바깥 탭·뒤로가기로 닫힌다.
                properties = PopupProperties(focusable = true),
            ) {
                RunvealMenu {
                    RunvealMenuItem(
                        label = stringResource(R.string.tab_course_more_rename),
                        icon = RunvealIcons.Edit,
                        onClick = {
                            closeMenu()
                            actions.onRename()
                        },
                    )
                    RunvealMenuItem(
                        label = stringResource(R.string.tab_course_more_export_gpx),
                        icon = RunvealIcons.Download,
                        onClick = {
                            closeMenu()
                            actions.onExportGpx()
                        },
                    )
                    RunvealMenuItem(
                        label = stringResource(R.string.tab_course_more_delete),
                        icon = RunvealIcons.Delete,
                        type = RunvealMenuItemType.Danger,
                        onClick = {
                            closeMenu()
                            actions.onDelete()
                        },
                    )
                }
            }
        }
    }
}

/**
 * 메뉴를 아이콘 바로 아래, 오른쪽 끝을 아이콘 오른쪽에 맞춰 놓는다(메뉴가 아이콘보다 넓어 왼쪽으로 펼쳐진다).
 * 아래 공간이 모자라면 아이콘 위로 뒤집는다 — 목록 맨 아랫줄에서 메뉴가 화면 밖으로 잘리지 않게.
 */
private class MenuBelowAnchorPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
        val below = anchorBounds.bottom + gapPx
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - gapPx - popupContentSize.height).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}
