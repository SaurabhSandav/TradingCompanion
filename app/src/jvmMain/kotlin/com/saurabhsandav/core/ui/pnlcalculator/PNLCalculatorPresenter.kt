package com.saurabhsandav.core.ui.pnlcalculator

import androidx.compose.runtime.mutableStateListOf
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorEvent
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorEvent.Calculate
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorEvent.RemoveCalculation
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorFormModel
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorState
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLEntry
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import com.saurabhsandav.trading.record.model.TradeSide
import com.saurabhsandav.trading.record.model.isLong
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope

@AssistedInject
internal class PNLCalculatorPresenter(
    @Assisted coroutineScope: CoroutineScope,
    brokerProvider: BrokerProvider,
) {

    private var maxId = 0
    private val pnlEntries = mutableStateListOf<PNLEntry>()
    private val broker = brokerProvider.getBroker(FinvasiaBroker.Id)

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
            quantity = formModel.quantityField.value.toKBigDecimal(),
            entry = formModel.entryField.value.toKBigDecimal(),
            exit = formModel.exitField.value.toKBigDecimal(),
            side = if (formModel.isLongField.value) TradeSide.Long else TradeSide.Short,
        )

        pnlEntries += PNLEntry(
            id = maxId++,
            side = side,
            quantity = formModel.quantityField.value,
            entry = formModel.entryField.value,
            exit = formModel.exitField.value,
            breakeven = brokerage.breakeven.toString(),
            pnl = brokerage.pnl.toString(),
            isProfitable = brokerage.pnl > KBigDecimal.Zero,
            netPNL = brokerage.netPNL.toString(),
            charges = brokerage.totalCharges.toString(),
            isNetProfitable = brokerage.netPNL > KBigDecimal.Zero,
            isRemovable = true,
        )
    }

    private fun onRemoveCalculation(id: Int) {
        pnlEntries.removeIf { it.id == id }
    }

    private fun brokerage(
        quantity: KBigDecimal,
        entry: KBigDecimal,
        exit: KBigDecimal,
        side: TradeSide,
    ): Brokerage = broker.calculateBrokerage(
        instrument = Instrument.Equity,
        entry = entry,
        exit = exit,
        quantity = quantity,
        isLong = side.isLong,
    )

    @AssistedFactory
    fun interface Factory {

        fun create(coroutineScope: CoroutineScope): PNLCalculatorPresenter
    }
}
