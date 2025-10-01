package com.saurabhsandav.fyersapi

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun getClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(OkHttp)
}
