package com.saurabhsandav.core.ui.barreplay.session.replayorderform.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.ui.common.form2.FormValidator
import com.saurabhsandav.core.ui.common.form2.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form2.validations.isInt
import com.saurabhsandav.core.ui.common.form2.validations.isPositive
import com.saurabhsandav.core.ui.common.form2.validations.isRequired

@Immutable
internal data class ReplayOrderFormState(
    val title: String,
    val formModel: ReplayOrderFormModel?,
    val onSaveOrder: () -> Unit,
)

@Stable
internal class ReplayOrderFormModel(
    val validator: FormValidator,
    instrument: Instrument?,
    ticker: String?,
    quantity: String,
    lots: String,
    isBuy: Boolean,
    price: String,
    stop: String,
    target: String,
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

    val stop = validator.addField(stop) {
        isBigDecimal {
            isPositive()

            check(
                value = when {
                    validated(isBuyField) -> this < validated(priceField).toBigDecimal()
                    else -> this > validated(priceField).toBigDecimal()
                },
                errorMessage = { "Invalid Stop" },
            )
        }
    }

    val target = validator.addField(target) {
        isBigDecimal {
            isPositive()

            check(
                value = when {
                    validated(isBuyField) -> this > validated(priceField).toBigDecimal()
                    else -> this < validated(priceField).toBigDecimal()
                },
                errorMessage = { "Invalid Target" },
            )
        }
    }
}
