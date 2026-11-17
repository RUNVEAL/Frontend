package com.swyp.runveal.tabmypage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.tabmypage.ImportRecordsProgressScreen
import com.swyp.runveal.tabmypage.ImportRecordsResultScreen
import com.swyp.runveal.tabmypage.ImportRecordsScreen
import com.swyp.runveal.tabmypage.MyScreen
import com.swyp.runveal.tabmypage.TermsDetailScreen
import com.swyp.runveal.tabmypage.TermsPolicyScreen
import com.swyp.runveal.tabmypage.contract.TermsPolicyItem

/**
 * @param onLoggedOut 로그아웃 후 이동. 로그인 화면은 탭 바깥(루트 그래프)에 있어서 여기 [navigator]
 *  (탭 네비게이터)로는 못 간다 — 루트 네비게이터를 쥔 `homeGraph`가 넘겨준다.
 */
fun NavGraphBuilder.myGraph(
    navigator: RunVealNavigator,
    onLoggedOut: () -> Unit,
    onCreateCourseClick: () -> Unit,
    onGoHomeClick: () -> Unit,
) {
    composable<RunVealRoute.HomeTab.My> {
        MyScreen(
            onLoggedOut = onLoggedOut,
            // 약관 목록은 마이페이지 안에 펼쳐 둬서 중간 화면 없이 바로 본문으로 간다.
            onTermsItemClick = { item ->
                navigator.navigate(RunVealRoute.HomeTab.TermsDetail(item.name))
            },
            onImportRecordsClick = { navigator.navigate(RunVealRoute.HomeTab.ImportRecords) },
        )
    }
    composable<RunVealRoute.HomeTab.ImportRecords> {
        ImportRecordsScreen(
            onBackClick = { navigator.navigateUp() },
            // 진행 화면은 안내 화면을 대체한다(popUpTo inclusive) — 가져오기가 끝나고 돌아갈
            // 자리는 안내 화면이 아니라 마이페이지다. 가져오는 중 뒤로가기도 마이페이지로 간다.
            onReadyToImport = {
                navigator.navigate(
                    route = RunVealRoute.HomeTab.ImportRecordsProgress,
                    navOptions = navOptions {
                        popUpTo<RunVealRoute.HomeTab.ImportRecords> { inclusive = true }
                    },
                )
            },
            onLaterClick = { navigator.navigateUp() },
        )
    }
    composable<RunVealRoute.HomeTab.ImportRecordsProgress> {
        ImportRecordsProgressScreen(
            // 결과 화면도 진행 화면을 대체한다 — 결과를 보다가 뒤로 누르면 돌아갈 자리는 이미
            // 끝난 진행 화면이 아니라 마이페이지다.
            onFinished = {
                navigator.navigate(
                    route = RunVealRoute.HomeTab.ImportRecordsResult,
                    navOptions = navOptions {
                        popUpTo<RunVealRoute.HomeTab.ImportRecordsProgress> { inclusive = true }
                    },
                )
            },
            onClose = { navigator.navigateUp() },
        )
    }
    composable<RunVealRoute.HomeTab.ImportRecordsResult> {
        ImportRecordsResultScreen(
            onBackClick = { navigator.navigateUp() },
            onCreateCourseClick = onCreateCourseClick,
            onGoHomeClick = onGoHomeClick,
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
