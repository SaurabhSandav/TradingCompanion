package com.saurabhsandav.core.trading

import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import com.saurabhsandav.trading.market.india.ZerodhaBroker
import java.util.WeakHashMap

class AppBrokerProvider : BrokerProvider {

    private val brokers = WeakHashMap<String, Broker>()

    override fun getBroker(id: BrokerId): Broker = brokers.getOrPut(id.value) {

        when (id) {
            FinvasiaBroker.Id -> FinvasiaBroker()
            ZerodhaBroker.Id -> ZerodhaBroker()
            else -> error("Broker with id (${id.value}) not found")
        }
    }
}
