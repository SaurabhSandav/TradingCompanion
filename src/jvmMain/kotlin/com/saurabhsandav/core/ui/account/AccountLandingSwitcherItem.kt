package com.saurabhsandav.core.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem

internal class AccountLandingSwitcherItem(
    accountModule: AccountModule,
) : LandingSwitcherItem {

    private val presenter = accountModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        AccountScreen(
            transactions = state.transactions,
        )
    }
}
