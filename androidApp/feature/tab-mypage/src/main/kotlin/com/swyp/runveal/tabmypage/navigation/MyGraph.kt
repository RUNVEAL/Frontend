package com.swyp.runveal.tabmypage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.tabmypage.MyScreen
import com.swyp.runveal.tabmypage.TermsDetailScreen
import com.swyp.runveal.tabmypage.TermsPolicyScreen
import com.swyp.runveal.tabmypage.contract.TermsPolicyItem

/**
 * @param onLoggedOut 로그아웃 후 이동. 로그인 화면은 탭 바깥(루트 그래프)에 있어서 여기 [navigator]
 *  (탭 네비게이터)로는 못 간다 — 루트 네비게이터를 쥔 `homeGraph`가 넘겨준다.
 */
fun NavGraphBuilder.myGraph(navigator: RunVealNavigator, onLoggedOut: () -> Unit) {
    composable<RunVealRoute.HomeTab.My> {
        MyScreen(
            onLoggedOut = onLoggedOut,
            onTermsClick = { navigator.navigate(RunVealRoute.HomeTab.Terms) },
        )
    }
    composable<RunVealRoute.HomeTab.Terms> {
        TermsPolicyScreen(
            onBackClick = { navigator.navigateUp() },
            onItemClick = { item -> navigator.navigate(RunVealRoute.HomeTab.TermsDetail(item.name)) },
        )
    }
    composable<RunVealRoute.HomeTab.TermsDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<RunVealRoute.HomeTab.TermsDetail>()
        // 라우트에는 enum 이름만 싣는다 — 모르는 값이 들어오면(앱 업데이트로 항목이 바뀐 경우 등)
        // 첫 항목으로 떨어뜨려 화면이 죽지 않게 한다.
        val item = TermsPolicyItem.entries.firstOrNull { it.name == route.item }
            ?: TermsPolicyItem.entries.first()
        TermsDetailScreen(item = item, onBackClick = { navigator.navigateUp() })
    }
}
