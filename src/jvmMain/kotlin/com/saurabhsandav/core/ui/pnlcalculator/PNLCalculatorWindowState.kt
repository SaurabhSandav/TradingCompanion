package com.saurabhsandav.core.ui.pnlcalculator

import androidx.compose.runtime.*
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.form2.FormValidator
import com.saurabhsandav.core.ui.pnlcalculator.PNLCalculatorWindowParams.OperationType.*
import com.saurabhsandav.core.utils.Brokerage
import com.saurabhsandav.core.utils.brokerage
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@Composable
internal fun rememberPNLCalculatorWindowState(
    params: PNLCalculatorWindowParams,
): PNLCalculatorWindowState {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current

    return remember {
        PNLCalculatorWindowState(
            params = params,
            coroutineScope = scope,
            appModule = appModule,
        )
    }
}

@Stable
internal class PNLCalculatorWindowState(
    val params: PNLCalculatorWindowParams,
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
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
                is FromOpenTrade -> openFromOpenTrade(params.operationType.id)
                is FromClosedTrade -> openFromClosedTrade(params.operationType.id)
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

    private suspend fun openFromOpenTrade(id: Long) {

        val openTrade = withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.getById(id).executeAsOne()
        }

        model.quantityField.value = openTrade.quantity
        model.isLongField.value = TradeSide.fromString(openTrade.side) == TradeSide.Long
        model.entryField.value = openTrade.entry
        model.exitField.value = openTrade.target ?: openTrade.stop ?: openTrade.entry

        model.enableModification = false

        openTrade.stop?.let {

            val brokerage = brokerage(
                quantity = openTrade.quantity.toBigDecimal(),
                entry = openTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLongField.value) TradeSide.Long else TradeSide.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = openTrade.side.uppercase(),
                quantity = openTrade.quantity,
                entry = openTrade.entry,
                exit = "$it\n(Stop)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }

        openTrade.target?.let {

            val brokerage = brokerage(
                quantity = openTrade.quantity.toBigDecimal(),
                entry = openTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLongField.value) TradeSide.Long else TradeSide.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = openTrade.side.uppercase(),
                quantity = openTrade.quantity,
                entry = openTrade.entry,
                exit = "$it (Target)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }
    }

    private suspend fun openFromClosedTrade(id: Long) {

        val closedTrade = withContext(Dispatchers.IO) {
            appModule.appDB.closedTradeQueries.getById(id).executeAsOne()
        }

        model.quantityField.value = closedTrade.quantity
        model.isLongField.value = TradeSide.fromString(closedTrade.side) == TradeSide.Long
        model.entryField.value = closedTrade.entry
        model.exitField.value = closedTrade.exit

        model.enableModification = false

        closedTrade.stop?.let {

            val brokerage = brokerage(
                quantity = closedTrade.quantity.toBigDecimal(),
                entry = closedTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLongField.value) TradeSide.Long else TradeSide.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = closedTrade.side.uppercase(),
                quantity = closedTrade.quantity,
                entry = closedTrade.entry,
                exit = "$it\n(Stop)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }

        closedTrade.target?.let {

            val brokerage = brokerage(
                quantity = closedTrade.quantity.toBigDecimal(),
                entry = closedTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLongField.value) TradeSide.Long else TradeSide.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = closedTrade.side.uppercase(),
                quantity = closedTrade.quantity,
                entry = closedTrade.entry,
                exit = "$it\n(Target)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }

        closedTrade.exit.let {

            val brokerage = brokerage(
                quantity = closedTrade.quantity.toBigDecimal(),
                entry = closedTrade.entry.toBigDecimal(),
                exit = it.toBigDecimal(),
                side = if (model.isLongField.value) TradeSide.Long else TradeSide.Short,
            )

            model.pnlEntries += PNLEntry(
                id = maxId++,
                side = closedTrade.side.uppercase(),
                quantity = closedTrade.quantity,
                entry = closedTrade.entry,
                exit = "$it\n(Exit)",
                breakeven = brokerage.breakeven.toPlainString(),
                pnl = brokerage.pnl.toPlainString(),
                isProfitable = brokerage.pnl > BigDecimal.ZERO,
                charges = brokerage.totalCharges.toPlainString(),
                netPNL = brokerage.netPNL.toPlainString(),
                isNetProfitable = brokerage.netPNL > BigDecimal.ZERO,
            )
        }
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

        data class FromOpenTrade(val id: Long) : OperationType()

        data class FromClosedTrade(val id: Long) : OperationType()
    }
}
