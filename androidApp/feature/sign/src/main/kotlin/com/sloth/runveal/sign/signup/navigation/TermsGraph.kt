package com.sloth.runveal.sign.signup.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.sloth.runveal.navigation.RunVealNavigator
import com.sloth.runveal.navigation.RunVealRoute
import com.sloth.runveal.sign.signup.TermsAgreementScreen

fun NavGraphBuilder.termsGraph(navigator: RunVealNavigator) {
    composable<RunVealRoute.Sign.Terms> {
        TermsAgreementScreen(
            onBackClick = { navigator.navigateUp() },
            onNextClick = { navigator.navigate(RunVealRoute.Sign.Nickname) },
        )
    }
}
