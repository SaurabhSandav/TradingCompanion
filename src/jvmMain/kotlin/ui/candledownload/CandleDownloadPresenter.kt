package ui.candledownload

import AppModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import fyers.Fyers
import fyers.model.CandleResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import launchUnit
import ui.candledownload.model.CandleDownloadEvent
import ui.candledownload.model.CandleDownloadState
import ui.common.CollectEffect
import utils.FyersCandleDownloader
import kotlin.time.Duration.Companion.seconds

internal class CandleDownloadPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val fyers: Fyers = appModule.fyersFactory(),
    private val appPrefs: FlowSettings = appModule.appPrefs,
) {

    private val events = MutableSharedFlow<CandleDownloadEvent>(extraBufferCapacity = Int.MAX_VALUE)

    init {
/*
        coroutineScope.launch {

            delay(2.seconds)

            val accessToken = appPrefs.getStringFlow("fyers_access_token").first()

            appModule.appDB
                .closedTradeQueries
                .getAll()
                .asFlow()
                .mapToList()
                .collect { trade ->
                    val symbols = trade.map { it.ticker }.distinct()
                    symbols.forEach {
                        FyersCandleDownloader(fyers).download(
                            accessToken = accessToken,
                            symbol = it,
                            resolution = CandleResolution.D1,
                        )
                    }
                    println("Done")
                }

        }
*/
    }

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->
            when (event) {
                is CandleDownloadEvent.LoginSuccess -> getAccessToken(event.redirectionUrl)
            }
        }

        val accessToken by isLoggedInFlow().collectAsState(null)
        val loginState = when (accessToken) {
            null -> null
            "" -> CandleDownloadState.Login.NeedsToLogin(fyers.getLoginURL())
            else -> CandleDownloadState.Login.LoggedIn
        }

        return@launchMolecule CandleDownloadState(
            login = loginState,
        )
    }

    fun event(event: CandleDownloadEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun isLoggedInFlow() = remember {
        appPrefs.getStringFlow("fyers_access_token")
    }

    private fun getAccessToken(redirectUrl: String) = coroutineScope.launchUnit {

        val accessToken = fyers.getAccessToken(redirectUrl)

        appPrefs.putString("fyers_access_token", accessToken)
    }
}
