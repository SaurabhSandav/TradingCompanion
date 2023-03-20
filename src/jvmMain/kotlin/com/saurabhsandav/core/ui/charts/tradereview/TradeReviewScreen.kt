package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.runtime.*
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.MarkTrade
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.SelectTrade
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeListItem
import com.saurabhsandav.core.ui.charts.tradereview.ui.TradesTable
import com.saurabhsandav.core.ui.common.app.AppWindow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.Instant

@Composable
internal fun TradeReviewWindow(
    onCloseRequest: () -> Unit,
    onOpenChart: (
        ticker: String,
        start: Instant,
        end: Instant?,
    ) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { TradeReviewPresenter(scope, appModule, onOpenChart) }
    val state by presenter.state.collectAsState()


    AppWindow(
        onCloseRequest = onCloseRequest,
        title = "Trade Review",
    ) {

        TradeReviewScreen(
            tradesItems = state.tradesItems,
            onMarkTrade = { id, isMarked -> presenter.event(MarkTrade(id, isMarked)) },
            onSelectTrade = { id -> presenter.event(SelectTrade(id)) },
        )
    }
}

@Composable
internal fun TradeReviewScreen(
    tradesItems: ImmutableList<TradeListItem>,
    onMarkTrade: (id: Long, isMarked: Boolean) -> Unit,
    onSelectTrade: (id: Long) -> Unit,
) {

    TradesTable(
        tradesItems = tradesItems,
        onMarkTrade = onMarkTrade,
        onSelectTrade = onSelectTrade,
    )
}
