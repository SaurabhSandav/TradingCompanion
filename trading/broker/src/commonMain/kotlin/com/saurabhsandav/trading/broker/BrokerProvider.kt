package com.saurabhsandav.trading.broker

interface BrokerProvider {

    fun getAllIds(): List<BrokerId>

    fun getBroker(id: BrokerId): Broker
}
