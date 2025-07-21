package com.saurabhsandav.core.ui.landing

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.account.AccountGraph
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.reviews.ReviewsGraph
import com.saurabhsandav.core.ui.sizing.SizingGraph
import com.saurabhsandav.core.ui.stats.StatsGraph
import com.saurabhsandav.core.ui.tags.screen.TagsScreenGraph
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutions.TradeExecutionsGraph
import com.saurabhsandav.core.ui.trades.TradesGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.MapKey
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope

@GraphExtension(LandingGraph::class)
internal interface LandingGraph {

    val presenterFactory: LandingPresenter.Factory

    val tradeContentLauncher: TradeContentLauncher

    val switcherItems: Map<LandingScreen, LandingSwitcherItem>

    val accountGraphFactory: AccountGraph.Factory

    val reviewGraphFactory: ReviewsGraph.Factory

    val sizingGraphFactory: SizingGraph.Factory

    val statsGraphFactory: StatsGraph.Factory

    val tagsScreenGraphFactory: TagsScreenGraph.Factory

    val tradeExecutionsGraphFactory: TradeExecutionsGraph.Factory

    val tradesGraphFactory: TradesGraph.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides coroutineScope: CoroutineScope,
            @Provides profileId: ProfileId,
        ): LandingGraph
    }
}

@MustBeDocumented
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE,
)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
internal annotation class LandingSwitcherItemKey(
    val key: LandingScreen,
)
