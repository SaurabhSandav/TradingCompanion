package com.saurabhsandav.core.trading

import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.fyersapi.FyersApi
import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import com.saurabhsandav.trading.market.india.ZerodhaBroker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.WeakHashMap

@SingleIn(AppScope::class)
@Inject
class AppBrokerProvider(
    private val appDispatchers: AppDispatchers,
    private val fyersApi: FyersApi,
) : BrokerProvider {

    private val brokers = WeakHashMap<String, Broker>()

    override fun getAllIds(): List<BrokerId> = listOf(
        FinvasiaBroker.Id,
        ZerodhaBroker.Id,
    )

    override fun getBroker(id: BrokerId): Broker = brokers.getOrPut(id.value) {

        when (id) {
            FinvasiaBroker.Id -> FinvasiaBroker(appDispatchers.IO, fyersApi)
            ZerodhaBroker.Id -> ZerodhaBroker()
            else -> error("Broker with id (${id.value}) not found")
        }
    }
}
