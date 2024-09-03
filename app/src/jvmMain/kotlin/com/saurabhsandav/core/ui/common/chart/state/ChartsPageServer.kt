package com.saurabhsandav.core.ui.common.chart.state

import co.touchlab.kermit.Logger
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

object ChartsPageServer {

    private var server: EmbeddedServer<*, *>? = null

    suspend fun startIfNotStarted() {

        if (server != null) return

        server = embeddedServer(
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

        server.let(::checkNotNull).start(wait = false)

        Logger.d { "Initialized Charts page at ${getUrl()}" }
    }

    suspend fun getUrl(): String {
        val serverEngine = server ?: error("Server not initialized")
        val port = serverEngine.engine.resolvedConnectors()[0].port
        return "http://localhost:$port"
    }
}
