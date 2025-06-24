package com.saurabhsandav.core.ui.landing

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.record.model.ProfileId
import com.saurabhsandav.core.ui.account.AccountLandingSwitcherItem
import com.saurabhsandav.core.ui.landing.model.LandingState
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.Account
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.Reviews
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.Stats
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.Tags
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.TradeExecutions
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.TradeSizing
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.Trades
import com.saurabhsandav.core.ui.reviews.ReviewsLandingSwitcherItem
import com.saurabhsandav.core.ui.sizing.SizingLandingSwitcherItem
import com.saurabhsandav.core.ui.stats.StatsLandingSwitcherItem
import com.saurabhsandav.core.ui.tags.screen.TagsLandingSwitcherItem
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutions.TradeExecutionsLandingSwitcherItem
import com.saurabhsandav.core.ui.trades.TradesLandingSwitcherItem
import kotlinx.coroutines.CoroutineScope

internal class LandingModule(
    private val appModule: AppModule,
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

    val switcherItems: Map<LandingState.LandingScreen, LandingSwitcherItem> = run {

        val screensModule = appModule.screensModule

        mapOf(
            Account to AccountLandingSwitcherItem(screensModule.accountModule(coroutineScope)),
            TradeSizing to SizingLandingSwitcherItem(screensModule.sizingModule(coroutineScope, profileId)),
            TradeExecutions to TradeExecutionsLandingSwitcherItem(
                screensModule.tradeExecutionsModule(coroutineScope, profileId),
            ),
            Trades to TradesLandingSwitcherItem(screensModule.tradesModule(coroutineScope, profileId)),
            Tags to TagsLandingSwitcherItem(screensModule.tagsScreenModule(coroutineScope, profileId)),
            Reviews to ReviewsLandingSwitcherItem(screensModule.reviewsModule(coroutineScope, profileId)),
            Stats to StatsLandingSwitcherItem(screensModule.statsModule(coroutineScope, profileId)),
        )
    }

    val tradeContentLauncher: TradeContentLauncher
        get() = appModule.tradeContentLauncher
}
