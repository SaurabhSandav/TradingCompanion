package com.saurabhsandav.trading.broker

fun interface BrokerProvider {

    fun getBroker(id: BrokerId): Broker
}
