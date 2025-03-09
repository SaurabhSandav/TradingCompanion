package com.saurabhsandav.core.ui.tradecontent

import androidx.compose.runtime.Composable
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.charts.ChartsHandle
import com.saurabhsandav.core.ui.charts.ChartsScreen
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.review.ReviewWindow
import com.saurabhsandav.core.ui.trade.TradeWindow
import com.saurabhsandav.core.ui.tradeexecutionform.TradeExecutionFormWindow
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradereview.TradeReviewHandle
import com.saurabhsandav.core.ui.tradereview.TradeReviewWindow

internal class TradeContentLauncher {

    private val executionFormWindowsManager = AppWindowsManager<TradeExecutionFormWindowParams>()
    private val tradeWindowsManager = AppWindowsManager<ProfileTradeId>()
    private val reviewWindowsManager = AppWindowsManager<ProfileReviewId>()
    private val chartsWindowsManager = AppWindowsManager<ChartsHandle>()
    private val tradeReviewWindowsManager = AppWindowsManager<TradeReviewWindowParams>()

    fun openExecutionForm(
        profileId: ProfileId,
        formType: TradeExecutionFormType,
    ) {

        val window = when (formType) {
            is TradeExecutionFormType.Edit -> executionFormWindowsManager.windows.find {
                it.params.formType is TradeExecutionFormType.Edit && it.params.formType.id == formType.id
            }

            else -> null
        }

        when (window) {
            // Open new window
            null -> {

                val params = TradeExecutionFormWindowParams(
                    profileId = profileId,
                    formType = formType,
                )

                executionFormWindowsManager.newWindow(params)
            }

            // Window already open. Bring to front.
            else -> window.toFront()
        }
    }

    fun openTrade(profileTradeId: ProfileTradeId) {

        val window = tradeWindowsManager.windows.find { it.params == profileTradeId }

        when (window) {

            // Open new window
            null -> tradeWindowsManager.newWindow(profileTradeId)

            // Window already open. Bring to front.
            else -> window.toFront()
        }
    }

    fun openReview(profileReviewId: ProfileReviewId) {

        val window = reviewWindowsManager.windows.find { it.params == profileReviewId }

        when (window) {

            // Open new window
            null -> reviewWindowsManager.newWindow(profileReviewId)

            // Window already open. Bring to front.
            else -> window.toFront()
        }
    }

    fun openCharts() {

        when (val existingWindow = chartsWindowsManager.windows.singleOrNull()) {

            // Open new window
            null -> chartsWindowsManager.newWindow(ChartsHandle())

            // Window already open. Bring to front.
            else -> existingWindow.owner.childrenToFront()
        }
    }

    private fun closeCharts() {
        chartsWindowsManager.closeAll()
        tradeReviewWindowsManager.closeAll()
    }

    fun openTradeReview(tradeId: ProfileTradeId? = null) {
        openTradeReview(listOfNotNull(tradeId))
    }

    fun openTradeReview(tradeIds: List<ProfileTradeId>) {

        openCharts()

        when (val existingWindow = tradeReviewWindowsManager.windows.singleOrNull()) {

            // Open new window
            null -> {
                val chartsHandle = chartsWindowsManager.windows.single().params
                val params = TradeReviewWindowParams(chartsHandle, TradeReviewHandle())
                tradeReviewWindowsManager.newWindow(params)
            }

            // Window already open. Bring to front if no trade ids were provided.
            // If trade ids are provided, assume the user wants to see the trade(s) on the Chart.
            // Or else, assume the user wants to see the Trade Review window.
            else if (tradeIds.isEmpty()) -> existingWindow.owner.childrenToFront()
        }

        if (tradeIds.isNotEmpty()) {
            val tradeReviewHandle = tradeReviewWindowsManager.windows.single().params.tradeReviewHandle
            tradeReviewHandle.markTrades(tradeIds)
        }
    }

    @Composable
    fun Windows() {

        // Trade execution form windows
        executionFormWindowsManager.Windows { window ->

            TradeExecutionFormWindow(
                profileId = window.params.profileId,
                formType = window.params.formType,
                onCloseRequest = window::close,
            )
        }

        // Trade windows
        tradeWindowsManager.Windows { window ->

            TradeWindow(
                profileTradeId = window.params,
                onCloseRequest = window::close,
            )
        }

        // Review windows
        reviewWindowsManager.Windows { window ->

            ReviewWindow(
                profileReviewId = window.params,
                onCloseRequest = window::close,
            )
        }

        // Charts
        chartsWindowsManager.Windows { window ->

            ChartsScreen(
                onCloseRequest = ::closeCharts,
                chartsHandle = window.params,
                onOpenTradeReview = ::openTradeReview,
            )
        }

        // Trade review
        tradeReviewWindowsManager.Windows { window ->

            TradeReviewWindow(
                onCloseRequest = window::close,
                chartsHandle = window.params.chartsHandle,
                tradeReviewHandle = window.params.tradeReviewHandle,
            )
        }
    }

    private data class TradeExecutionFormWindowParams(
        val profileId: ProfileId,
        val formType: TradeExecutionFormType,
    )

    private data class TradeReviewWindowParams(
        val chartsHandle: ChartsHandle,
        val tradeReviewHandle: TradeReviewHandle,
    )
}
