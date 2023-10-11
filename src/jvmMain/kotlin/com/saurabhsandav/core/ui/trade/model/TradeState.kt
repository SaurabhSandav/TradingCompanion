package com.saurabhsandav.core.ui.trade.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Immutable
internal data class TradeState(
    val title: String,
    val details: Details?,
    val executions: ImmutableList<Execution>,
    val newExecutionEnabled: Boolean,
    val stops: ImmutableList<TradeStop>,
    val previewStop: (BigDecimal) -> Flow<TradeStop?>,
    val targets: ImmutableList<TradeTarget>,
    val previewTarget: (BigDecimal) -> Flow<TradeTarget?>,
    val mfeAndMae: MfeAndMae?,
    val notes: ImmutableList<TradeNote>,
    val tags: ImmutableList<TradeTag>,
    val tagSuggestions: (String) -> Flow<ImmutableList<TradeTag>>,
    val attachments: ImmutableList<TradeAttachment>,
    val eventSink: (TradeEvent) -> Unit,
) {

    @Immutable
    internal data class Details(
        val id: Long,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val duration: Flow<String>,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
    )

    @Immutable
    internal data class Execution(
        val id: Long,
        val quantity: String,
        val side: String,
        val price: String,
        val timestamp: String,
        val locked: Boolean,
    )

    @Immutable
    internal data class MfeAndMae(
        val mfePrice: String,
        val maePrice: String,
    )

    @Immutable
    internal data class TradeStop(
        val price: BigDecimal,
        val priceText: String,
        val risk: String,
        val netRisk: String,
    )

    @Immutable
    internal data class TradeTarget(
        val price: BigDecimal,
        val priceText: String,
        val profit: String,
        val netProfit: String,
    )

    @Immutable
    internal data class TradeTag(
        val id: Long,
        val name: String,
        val description: String,
    )

    @Immutable
    internal data class TradeAttachment(
        val id: Long,
        val name: String,
        val description: String?,
        val path: String,
        val extension: String?,
    )

    @Immutable
    internal data class TradeNote(
        val id: Long,
        val note: String,
        val dateText: String,
    )
}

@Stable
internal class AttachmentFormModel(
    val validator: FormValidator,
    initial: Initial,
) {

    val nameField = validator.addField(initial.name) { isRequired() }

    val descriptionField = validator.addField(initial.description)

    var path = ""

    class Initial(
        val name: String = "",
        val description: String = "",
    )
}
