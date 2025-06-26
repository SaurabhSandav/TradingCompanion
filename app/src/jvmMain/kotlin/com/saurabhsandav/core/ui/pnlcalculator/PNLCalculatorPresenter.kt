package com.saurabhsandav.core.ui.pnlcalculator

import androidx.compose.runtime.mutableStateListOf
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorEvent
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorEvent.Calculate
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorEvent.RemoveCalculation
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorFormModel
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorState
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLEntry
import com.saurabhsandav.core.utils.Brokerage
import com.saurabhsandav.core.utils.brokerage
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

internal class PNLCalculatorPresenter(
    coroutineScope: CoroutineScope,
) {

    private var maxId = 0
    private val pnlEntries = mutableStateListOf<PNLEntry>()

    private val formModel = PNLCalculatorFormModel(
        quantity = "1",
        isLong = true,
        entry = "100",
        exit = "110",
    )

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule PNLCalculatorState(
            formModel = formModel,
            pnlEntries = pnlEntries,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: PNLCalculatorEvent) {

        when (event) {
            Calculate -> onCalculate()
            is RemoveCalculation -> onRemoveCalculation(event.id)
        }
    }

    private fun onCalculate() {

        val side = if (formModel.isLongField.value) "LONG" else "SHORT"

        if (pnlEntries.any {
                it.quantity == formModel.quantityField.value &&
                    it.side == side &&
                    it.entry == formModel.entryField.value &&
                    it.exit == formModel.exitField.value
            }
        ) {
            return
        }

        val brokerage = brokerage(
            quantity = formModel.quantityField.value.toBigDecimal(),
            entry = formModel.entryField.value.toBigDecimal(),
            exit = formModel.exitField.value.toBigDecimal(),
            side = if (formModel.isLongField.value) TradeSide.Long else TradeSide.Short,
        )

        pnlEntries += PNLEntry(
            id = maxId++,
            side = side,
            quantity = formModel.quantityField.value,
            entry = formModel.entryField.value,
            exit = formModel.exitField.value,
            breakeven = brokerage.breakeven.toPlainString(),
            pnl = brokerage.pnl.toPlainString(),
            isProfitable = brokerage.pnl > BigDecimal.ZERO,
            netPNL = brokerage.netPNL.toPlainString(),
            charges = brokerage.totalCharges.toPlainString(),
            isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            isRemovable = true,
        )
    }

    private fun onRemoveCalculation(id: Int) {
        pnlEntries.removeIf { it.id == id }
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
