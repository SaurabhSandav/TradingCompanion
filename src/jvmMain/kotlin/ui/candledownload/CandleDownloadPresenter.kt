package ui.candledownload

import AppModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import fyers_api.FyersApi
import fyers_api.model.response.FyersResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import launchUnit
import ui.candledownload.model.CandleDownloadEvent
import ui.candledownload.model.CandleDownloadState
import ui.common.CollectEffect
import utils.PrefKeys

internal class CandleDownloadPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
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
            "" -> CandleDownloadState.Login.NeedsToLogin(fyersApi.getLoginURL())
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
        appPrefs.getStringFlow(PrefKeys.FyersAccessToken).onEach { delay(2000) }
    }

    private fun getAccessToken(redirectUrl: String) = coroutineScope.launchUnit {

        val accessToken = when (val response = fyersApi.getAccessToken(redirectUrl)) {
            is FyersResponse.Failure -> error(response.message)
            is FyersResponse.Success -> response.result.accessToken
        }

        appPrefs.putString(PrefKeys.FyersAccessToken, accessToken)
    }
}
