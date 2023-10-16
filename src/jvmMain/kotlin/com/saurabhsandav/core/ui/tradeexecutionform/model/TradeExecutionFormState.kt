package com.saurabhsandav.core.ui.tradeexecutionform.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.nanoseconds

@Immutable
internal data class TradeExecutionFormState(
    val title: String,
    val formModel: TradeExecutionFormModel?,
    val onSaveExecution: () -> Unit,
)

@Stable
internal class TradeExecutionFormModel(
    val validator: FormValidator,
    initial: Initial,
) {

    val instrumentField = validator.addField(initial.instrument) { isRequired() }

    val tickerField = validator.addField(initial.ticker) { isRequired() }

    val quantityField = validator.addField(initial.quantity) {
        isRequired()
        isInt {
            isPositive()
        }
    }

    val lotsField = validator.addField(initial.lots) {
        isInt {
            isPositive()
        }
    }

    val isBuyField = validator.addField(initial.isBuy)

    val priceField = validator.addField(initial.price) {
        isRequired()
        isBigDecimal {
            isPositive()
        }
    }

    val dateField = validator.addField(initial.timestamp.date) {
        check(this <= currentLocalDateTime().date) { "Cannot be in the future" }
    }

    val timeField = validator.addField(initial.timestamp.time) {
        check(validated(dateField).atTime(this) < currentLocalDateTime()) { "Cannot be in the future" }
    }

    val timestamp: LocalDateTime
        get() = dateField.value.atTime(timeField.value)

    private fun currentLocalDateTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    class Initial(
        val instrument: Instrument? = null,
        val ticker: String? = null,
        val quantity: String = "",
        val lots: String = "",
        val isBuy: Boolean = true,
        val price: String = "",
        val timestamp: LocalDateTime = run {
            val currentTime = Clock.System.now()
            val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
            currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
        },
    )
}
