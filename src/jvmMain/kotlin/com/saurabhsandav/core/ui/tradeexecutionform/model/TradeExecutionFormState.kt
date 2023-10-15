package com.saurabhsandav.core.ui.tradeexecutionform.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.ui.common.form2.FormValidator
import com.saurabhsandav.core.ui.common.form2.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form2.validations.isInt
import com.saurabhsandav.core.ui.common.form2.validations.isPositive
import com.saurabhsandav.core.ui.common.form2.validations.isRequired
import kotlinx.datetime.*

@Immutable
internal data class TradeExecutionFormState(
    val title: String,
    val formModel: TradeExecutionFormModel?,
    val onSaveExecution: () -> Unit,
)

@Stable
internal class TradeExecutionFormModel(
    val validator: FormValidator,
    instrument: Instrument?,
    ticker: String?,
    quantity: String,
    lots: String,
    isBuy: Boolean,
    price: String,
    timestamp: LocalDateTime,
) {

    val instrumentField = validator.addField(instrument) { isRequired() }

    val tickerField = validator.addField(ticker) { isRequired() }

    val quantityField = validator.addField(quantity) {
        isRequired()
        isInt {
            isPositive()
        }
    }

    val lotsField = validator.addField(lots) {
        isInt {
            isPositive()
        }
    }

    val isBuyField = validator.addField(isBuy)

    val priceField = validator.addField(price) {
        isRequired()
        isBigDecimal {
            isPositive()
        }
    }

    val dateField = validator.addField(timestamp.date) {
        check(this <= currentLocalDateTime().date) { "Cannot be in the future" }
    }

    val timeField = validator.addField(timestamp.time) {
        check(validated(dateField).atTime(this) < currentLocalDateTime()) { "Cannot be in the future" }
    }

    val timestamp: LocalDateTime
        get() = dateField.value.atTime(timeField.value)

    private fun currentLocalDateTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}
