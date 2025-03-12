package com.saurabhsandav.core.ui.tradereview.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.paging.PagingData
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.MarkedTradeItem
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.Tab
import com.saurabhsandav.core.ui.tradereview.model.TradeReviewState.TradeItem
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TradesTableSwitcher(
    selectedTab: Tab,
    trades: Flow<PagingData<TradeItem>>,
    markedTrades: List<MarkedTradeItem>?,
    onMarkTrade: (profileTradeId: ProfileTradeId, isMarked: Boolean) -> Unit,
    onSelectTrade: (profileTradeId: ProfileTradeId) -> Unit,
    onOpenDetails: (profileTradeId: ProfileTradeId) -> Unit,
) {

    val saveableStateHolder = rememberSaveableStateHolder()

    Crossfade(selectedTab) { tab ->

        saveableStateHolder.SaveableStateProvider(tab) {

            when (tab) {
                Tab.Profile -> ProfileTradesTable(
                    trades = trades,
                    onMarkTrade = onMarkTrade,
                    onSelectTrade = onSelectTrade,
                    onOpenDetails = onOpenDetails,
                )

                Tab.Marked -> MarkedTradesTable(
                    markedTrades = markedTrades,
                    onUnMarkTrade = { profileTradeId -> onMarkTrade(profileTradeId, false) },
                    onSelectTrade = onSelectTrade,
                    onOpenDetails = onOpenDetails,
                )
            }
        }
    }
}
