package com.saurabhsandav.trading.broker

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Instrument
import kotlin.time.Instant

interface Broker {

    val id: BrokerId

    val name: String

    fun calculateBrokerage(
        instrument: Instrument,
        entry: KBigDecimal,
        exit: KBigDecimal,
        quantity: KBigDecimal,
        isLong: Boolean,
    ): Brokerage

    fun areSymbolsExpired(lastDownloadInstant: Instant): Boolean

    suspend fun downloadSymbols(onSave: (List<Symbol>) -> Unit)
}
