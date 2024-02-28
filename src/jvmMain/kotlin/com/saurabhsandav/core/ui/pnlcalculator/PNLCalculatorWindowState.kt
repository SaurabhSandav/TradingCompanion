package com.saurabhsandav.core.ui.pnlcalculator

import androidx.compose.runtime.*
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindowParams.OperationType.New
import com.saurabhsandav.core.utils.Brokerage
import com.saurabhsandav.core.utils.brokerage
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
internal fun rememberPNLCalculatorWindowState(
    params: PNLCalculatorWindowParams,
): PNLCalculatorWindowState {

    val scope = rememberCoroutineScope()

    return remember {
        PNLCalculatorWindowState(
            params = params,
            coroutineScope = scope,
        )
    }
}

internal class PNLCalculatorWindowState(
    val params: PNLCalculatorWindowParams,
    private val coroutineScope: CoroutineScope,
) {

    private val formValidator = FormValidator(coroutineScope)
    private var maxId = 0

    var isReady by mutableStateOf(false)
        private set

    val model = PNLCalculatorModel(
        validator = formValidator,
        quantity = "1",
        isLong = true,
        entry = "100",
        exit = "110",
    )

    init {

        coroutineScope.launch {

            when (params.operationType) {
                New -> Unit
            }

            isReady = true
        }
    }

    fun onCalculate() = coroutineScope.launchUnit {

        val side = if (model.isLongField.value) "LONG" else "SHORT"

        if (!formValidator.validate() || model.pnlEntries.any {
                it.quantity == model.quantityField.value &&
                        it.side == side &&
                        it.entry == model.entryField.value &&
                        it.exit == model.exitField.value
            }) return@launchUnit

        val brokerage = brokerage(
            quantity = model.quantityField.value.toBigDecimal(),
            entry = model.entryField.value.toBigDecimal(),
            exit = model.exitField.value.toBigDecimal(),
            side = if (model.isLongField.value) TradeSide.Long else TradeSide.Short,
        )

        model.pnlEntries += PNLEntry(
            id = maxId++,
            side = side,
            quantity = model.quantityField.value,
            entry = model.entryField.value,
            exit = model.exitField.value,
            breakeven = brokerage.breakeven.toPlainString(),
            pnl = brokerage.pnl.toPlainString(),
            isProfitable = brokerage.pnl > BigDecimal.ZERO,
            netPNL = brokerage.netPNL.toPlainString(),
            charges = brokerage.totalCharges.toPlainString(),
            isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            isRemovable = true,
        )
    }

    fun onRemoveCalculation(id: Int) {
        model.pnlEntries.removeIf { it.id == id }
    }

    private fun brokerage(
        quantity: BigDecimal,
        entry: BigDecimal,
        exit: BigDecimal,
        side: TradeSide,
    ): Brokerage = brokerage(
        broker = "Finvasia",
        instrument = Instrument.Equity,
        entry = entry,
        exit = exit,
        quantity = quantity,
        side = side,
    )
}

internal class PNLCalculatorWindowParams(
    val operationType: OperationType,
    val onCloseRequest: () -> Unit,
) {

    sealed class OperationType {

        data object New : OperationType()
    }
}
