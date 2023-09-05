package com.saurabhsandav.core.ui.sizing.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import kotlinx.collections.immutable.ImmutableList
import java.util.*

@Immutable
internal data class SizingState(
    val sizedTrades: ImmutableList<SizedTrade>,
    val executionFormWindowsManager: AppWindowsManager<TradeExecutionFormParams>,
    val eventSink: (SizingEvent) -> Unit,
) {

    @Immutable
    internal data class SizedTrade(
        val id: Long,
        val ticker: String,
        val entry: String,
        val stop: String,
        val side: String,
        val spread: String,
        val calculatedQuantity: String,
        val maxAffordableQuantity: String,
        val target: String,
        val color: Color,
    )

    @Immutable
    internal data class TradeExecutionFormParams(
        val id: UUID,
        val profileId: Long,
        val formType: TradeExecutionFormType,
        val onExecutionSaved: (executionId: Long) -> Unit,
    )
}
