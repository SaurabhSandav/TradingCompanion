package com.saurabhsandav.core.ui.common.chart.state

import co.touchlab.kermit.Logger
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

object ChartsPageServer {

    private var serverEngine: ApplicationEngine? = null

    suspend fun startIfNotStarted() {

        if (serverEngine != null) return

        serverEngine = embeddedServer(
            factory = Netty,
            port = 0,
        ) {

            routing {

                staticResources(
                    remotePath = "/",
                    basePackage = "charts_page",
                )
            }
        }

        serverEngine.let(::checkNotNull).start(wait = false)

        Logger.d { "Initialized Charts page at ${getUrl()}" }
    }

    suspend fun getUrl(): String {
        val serverEngine = serverEngine ?: error("Server not initialized")
        val port = serverEngine.resolvedConnectors()[0].port
        return "http://localhost:$port"
    }
}
