package com.saurabhsandav.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.trade.TradeWindow
import com.saurabhsandav.core.ui.tradeexecutionform.TradeExecutionFormWindow
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType

internal class TradeContentLauncher {

    private val executionFormWindowsManager = AppWindowsManager<TradeExecutionFormWindowParams>()
    private val tradeWindowsManager = AppWindowsManager<TradeWindowParams>()

    fun openExecutionForm(
        profileId: Long,
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

    fun openTrade(
        profileId: Long,
        tradeId: Long,
    ) {

        val window = tradeWindowsManager.windows.find {
            it.params.profileId == profileId && it.params.tradeId == tradeId
        }

        when (window) {

            // Open new window
            null -> {

                val params = TradeWindowParams(
                    profileId = profileId,
                    tradeId = tradeId
                )

                tradeWindowsManager.newWindow(params)
            }

            // Window already open. Bring to front.
            else -> window.toFront()
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
                profileId = window.params.profileId,
                tradeId = window.params.tradeId,
                tradeContentLauncher = this@TradeContentLauncher,
                onCloseRequest = window::close,
            )
        }
    }

    @Immutable
    private data class TradeExecutionFormWindowParams(
        val profileId: Long,
        val formType: TradeExecutionFormType,
    )

    @Immutable
    private data class TradeWindowParams(
        val profileId: Long,
        val tradeId: Long,
    )
}
