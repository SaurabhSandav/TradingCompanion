package com.saurabhsandav.fyersapi

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

internal actual fun getClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(Js)
}
