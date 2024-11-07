package com.saurabhsandav.core.ui.landing

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.account.AccountLandingSwitcherItem
import com.saurabhsandav.core.ui.landing.model.LandingState
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.*
import com.saurabhsandav.core.ui.reviews.ReviewsLandingSwitcherItem
import com.saurabhsandav.core.ui.sizing.SizingLandingSwitcherItem
import com.saurabhsandav.core.ui.stats.StatsLandingSwitcherItem
import com.saurabhsandav.core.ui.tags.TagsLandingSwitcherItem
import com.saurabhsandav.core.ui.tradeexecutions.TradeExecutionsLandingSwitcherItem
import com.saurabhsandav.core.ui.trades.TradesLandingSwitcherItem
import kotlinx.coroutines.CoroutineScope

internal class LandingModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter: () -> LandingPresenter = {

        LandingPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
        )
    }

    val switcherItems: Map<LandingState.LandingScreen, LandingSwitcherItem> = mapOf(
        Account to AccountLandingSwitcherItem(appModule.accountModule(coroutineScope)),
        TradeSizing to SizingLandingSwitcherItem(appModule.sizingModule(coroutineScope, profileId)),
        TradeExecutions to TradeExecutionsLandingSwitcherItem(
            appModule.tradeExecutionsModule(coroutineScope, profileId)
        ),
        Trades to TradesLandingSwitcherItem(appModule.tradesModule(coroutineScope, profileId)),
        Tags to TagsLandingSwitcherItem(appModule.tagsModule(coroutineScope, profileId)),
        Reviews to ReviewsLandingSwitcherItem(appModule.reviewsModule(coroutineScope, profileId)),
        Stats to StatsLandingSwitcherItem(appModule.statsModule(coroutineScope, profileId)),
    )
}
