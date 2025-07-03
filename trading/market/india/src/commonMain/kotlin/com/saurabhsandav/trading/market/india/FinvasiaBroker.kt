package com.saurabhsandav.trading.market.india

import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.broker.brokerage
import com.saurabhsandav.trading.core.Instrument
import java.math.BigDecimal

class FinvasiaBroker : Broker {

    override val id: BrokerId = Id

    override val name: String = "Finvasia"

    override fun calculateBrokerage(
        instrument: Instrument,
        entry: BigDecimal,
        exit: BigDecimal,
        quantity: BigDecimal,
        isLong: Boolean,
    ): Brokerage = brokerage(
        brokerId = id,
        instrument = instrument,
        entry = entry,
        exit = exit,
        quantity = quantity,
        isLong = isLong,
    )

    companion object {
        val Id = BrokerId("Finvasia")
    }
}
