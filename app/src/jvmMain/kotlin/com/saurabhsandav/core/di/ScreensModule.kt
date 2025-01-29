package com.saurabhsandav.core.di

import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.account.AccountModule
import com.saurabhsandav.core.ui.attachmentform.AttachmentFormModule
import com.saurabhsandav.core.ui.barreplay.BarReplayModule
import com.saurabhsandav.core.ui.charts.ChartsModule
import com.saurabhsandav.core.ui.landing.LandingModule
import com.saurabhsandav.core.ui.profiles.ProfilesModule
import com.saurabhsandav.core.ui.profiles.form.ProfileFormModule
import com.saurabhsandav.core.ui.review.ReviewModule
import com.saurabhsandav.core.ui.reviews.ReviewsModule
import com.saurabhsandav.core.ui.settings.SettingsModule
import com.saurabhsandav.core.ui.sizing.SizingModule
import com.saurabhsandav.core.ui.stats.StatsModule
import com.saurabhsandav.core.ui.tags.form.TagFormModule
import com.saurabhsandav.core.ui.tags.screen.TagsScreenModule
import com.saurabhsandav.core.ui.trade.TradeModule
import com.saurabhsandav.core.ui.tradecontent.ProfileReviewId
import com.saurabhsandav.core.ui.tradeexecutionform.TradeExecutionFormModule
import com.saurabhsandav.core.ui.tradeexecutions.TradeExecutionsModule
import com.saurabhsandav.core.ui.tradereview.TradeReviewModule
import com.saurabhsandav.core.ui.trades.TradesModule
import com.saurabhsandav.core.ui.tradesfiltersheet.TradesFilterModule
import kotlinx.coroutines.CoroutineScope

internal class ScreensModule(
    private val appModule: AppModule,
) {

    val accountModule: (CoroutineScope) -> AccountModule = { coroutineScope ->
        AccountModule(appModule, coroutineScope)
    }

    val barReplayModule: (CoroutineScope) -> BarReplayModule = { coroutineScope ->
        BarReplayModule(appModule, coroutineScope)
    }

    val chartsModule: (CoroutineScope) -> ChartsModule = { coroutineScope ->
        ChartsModule(appModule, coroutineScope)
    }

    val tradeReviewModule: (CoroutineScope) -> TradeReviewModule = { coroutineScope ->
        TradeReviewModule(appModule, coroutineScope)
    }

    val landingModule: (CoroutineScope, ProfileId) -> LandingModule = { coroutineScope, profileId ->
        LandingModule(appModule, coroutineScope, profileId)
    }

    val profilesModule: (CoroutineScope) -> ProfilesModule = { coroutineScope ->
        ProfilesModule(appModule, coroutineScope)
    }

    val profileFormModule: (CoroutineScope) -> ProfileFormModule = { coroutineScope ->
        ProfileFormModule(appModule, coroutineScope)
    }

    val reviewsModule: (CoroutineScope, ProfileId) -> ReviewsModule = { coroutineScope, profileId ->
        ReviewsModule(appModule, coroutineScope, profileId)
    }

    val reviewModule: (CoroutineScope, ProfileReviewId) -> ReviewModule = { coroutineScope, profileReviewId ->
        ReviewModule(appModule, coroutineScope, profileReviewId)
    }

    val settingsModule: (CoroutineScope) -> SettingsModule = { coroutineScope ->
        SettingsModule(appModule, coroutineScope)
    }

    val sizingModule: (CoroutineScope, ProfileId) -> SizingModule = { coroutineScope, profileId ->
        SizingModule(appModule, coroutineScope, profileId)
    }

    val statsModule: (CoroutineScope, ProfileId) -> StatsModule = { coroutineScope, profileId ->
        StatsModule(appModule, coroutineScope, profileId)
    }

    val tagsScreenModule: (CoroutineScope, ProfileId) -> TagsScreenModule = { coroutineScope, profileId ->
        TagsScreenModule(appModule, coroutineScope, profileId)
    }

    val tagFormModule: (CoroutineScope) -> TagFormModule = { coroutineScope ->
        TagFormModule(appModule, coroutineScope)
    }

    val tradeModule: (CoroutineScope) -> TradeModule = { coroutineScope ->
        TradeModule(appModule, coroutineScope)
    }

    val tradeExecutionFormModule: (CoroutineScope) -> TradeExecutionFormModule = { coroutineScope ->
        TradeExecutionFormModule(appModule, coroutineScope)
    }

    val tradeExecutionsModule: (CoroutineScope, ProfileId) -> TradeExecutionsModule = { coroutineScope, profileId ->
        TradeExecutionsModule(appModule, coroutineScope, profileId)
    }

    val tradesModule: (CoroutineScope, ProfileId) -> TradesModule = { coroutineScope, profileId ->
        TradesModule(appModule, coroutineScope, profileId)
    }

    val tradesFilterModule: (CoroutineScope, ProfileId) -> TradesFilterModule = { coroutineScope, profileId ->
        TradesFilterModule(appModule, coroutineScope, profileId)
    }

    val attachmentFormModule: (CoroutineScope) -> AttachmentFormModule = { coroutineScope ->
        AttachmentFormModule(appModule, coroutineScope)
    }
}
