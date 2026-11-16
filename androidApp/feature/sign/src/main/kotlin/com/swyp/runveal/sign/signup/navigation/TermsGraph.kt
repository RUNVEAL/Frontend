package com.swyp.runveal.sign.signup.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.swyp.runveal.navigation.RunVealNavigator
import com.swyp.runveal.navigation.RunVealRoute
import com.swyp.runveal.sign.signup.TermsAgreementScreen

fun NavGraphBuilder.termsGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Sign.Terms> {
        TermsAgreementScreen(
            onBackClick = { navigator.navigateUp() },
            onNextClick = { navigator.navigate(RunVealRoute.Sign.Nickname) },
        )
    }
}
