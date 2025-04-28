package com.saurabhsandav.core.ui.loginservice.fyers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.UriHandler
import co.touchlab.kermit.Logger
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.ui.loginservice.LoginService
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.fyersapi.FyersApi
import com.saurabhsandav.fyersapi.model.response.FyersError
import com.slack.eithernet.ApiResult.Failure
import com.slack.eithernet.ApiResult.Failure.ApiFailure
import com.slack.eithernet.ApiResult.Failure.HttpFailure
import com.slack.eithernet.ApiResult.Failure.NetworkFailure
import com.slack.eithernet.ApiResult.Failure.UnknownFailure
import com.slack.eithernet.ApiResult.Success
import com.slack.eithernet.successOrNull
import io.ktor.http.ContentType
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days

internal class FyersLoginService private constructor(
    private val appDispatchers: AppDispatchers,
    private val coroutineScope: CoroutineScope,
    private val resultHandle: LoginService.ResultHandle,
    private val uriHandler: UriHandler,
    private val fyersApi: FyersApi,
    private val appPrefs: FlowSettings,
) : LoginService {

    private var server: EmbeddedServer<*, *>? = null

    private var loginState by mutableStateOf<FyersLoginState?>(null)

    init {
        initiateLogin()
    }

    @Composable
    override fun Dialogs() {

        val loginState = loginState ?: return

        FyersLoginDialog(
            loginState = loginState,
            onLoginCancelled = ::onLoginCancelled,
            onSubmitRefreshPin = ::onSubmitRefreshPin,
        )
    }

    private fun initiateLogin() = coroutineScope.launchUnit {

        val authTokens = getAuthTokensFromPrefs(appPrefs).first()

        suspend fun isLoggedIn(authTokens: FyersAuthTokens): Boolean {
            return fyersApi.getProfile(authTokens.accessToken).successOrNull() != null
        }

        Logger.d(DebugTag) { "Checking login status" }

        when {
            authTokens == null -> loginStage1()
            isLoggedIn(authTokens) -> resultHandle.onSuccess()
            else -> when (val canRefresh = (Clock.System.now() - authTokens.initialLoginInstant) < 14.days) {
                canRefresh -> refreshLogin()
                else -> loginStage1()
            }
        }
    }

    private fun loginStage1(reLogin: Boolean = false) {

        Logger.d(DebugTag) { "Initiating stage 1 (Login to Fyers website)" }

        loginState = if (reLogin) FyersLoginState.ReLogin else FyersLoginState.InitialLogin

        // Launch embedded server to capture redirect url
        server = embeddedServer(
            factory = Netty,
            port = PORT,
        ) {

            routing {

                get("/") {

                    val parameters = this.call.parameters

                    val loginResultText = when (parameters["s"]) {
                        "ok" -> {
                            loginStage2(this.call.request.uri)
                            "Login successful!"
                        }

                        else -> {
                            onLoginCancelled(parameters["message"])
                            "Login failed!"
                        }
                    }

                    // Respond with login status
                    call.respondText(
                        text = """
                        |<!DOCTYPE html>
                        |<html lang="en">
                        |    <head>
                        |        <meta charset="UTF-8" />
                        |
                        |        <title>Fyers Login</title>
                        |    </head>
                        |    <body>
                        |        <h1>$loginResultText</h1>
                        |    </body>
                        |</html>
                        |
                        """.trimMargin(),
                        contentType = ContentType.Text.Html,
                    )

                    server?.stop()
                    server = null
                }
            }
        }.start(wait = false)

        Logger.d(DebugTag) { "Launched redirect capture page at http://127.0.0.1:$PORT" }

        // Generate login url
        val loginUrl = fyersApi.getLoginURL(redirectUrl = "http://127.0.0.1:$PORT")

        // Launch login url in browser
        coroutineScope.launch(appDispatchers.IO) {

            Logger.d(DebugTag) { "Launched Fyers login page in browser" }

            uriHandler.openUri(loginUrl)
        }
    }

    private fun loginStage2(redirectUrl: String) = coroutineScope.launchUnit {

        Logger.d(DebugTag) { "Initiating login stage 2 (Login Validation)" }

        val result = fyersApi.validateLogin(redirectUrl)

        when (result) {
            is Success -> {

                val authTokens = FyersAuthTokens(
                    accessToken = result.value.accessToken,
                    refreshToken = result.value.refreshToken,
                    initialLoginInstant = Clock.System.now(),
                )

                saveAuthTokensToPrefs(appPrefs, authTokens)

                resultHandle.onSuccess()

                Logger.d(DebugTag) { "Login successful" }
            }

            is ApiFailure -> onLoginCancelled(result.error?.message)
            is HttpFailure -> onLoginCancelled(result.error?.message)
            is NetworkFailure -> onLoginCancelled(result.error.message)
            is UnknownFailure -> onLoginCancelled(result.error.message)
        }
    }

    private fun refreshLogin() = coroutineScope.launchUnit {

        Logger.d(DebugTag) { "Refresh login. Awaiting Pin..." }

        loginState = FyersLoginState.RefreshLogin()
    }

    private fun onSubmitRefreshPin(pin: String) = coroutineScope.launchUnit {

        val loginState = loginState as FyersLoginState.RefreshLogin

        loginState.isEnabled = false

        val authTokens = checkNotNull(getAuthTokensFromPrefs(appPrefs).first()) { "Fyers credentials don't exist" }

        val result = fyersApi.refreshLogin(
            refreshToken = authTokens.refreshToken,
            pin = pin,
        )

        when (result) {
            is Success -> {

                val refreshedAuthTokens = authTokens.copy(accessToken = result.value.accessToken)

                saveAuthTokensToPrefs(appPrefs, refreshedAuthTokens)

                resultHandle.onSuccess()

                Logger.d(DebugTag) { "Refresh login successful" }
            }

            is Failure -> {

                suspend fun clearAuthTokensAndRelogin() {
                    saveAuthTokensToPrefs(appPrefs, null)
                    loginStage1(reLogin = true)
                }

                when (result) {
                    is ApiFailure -> clearAuthTokensAndRelogin()
                    is HttpFailure -> when (result.error?.type) {
                        FyersError.Type.RefreshPinInvalid -> loginState.isError = true
                        FyersError.Type.RefreshTokenInvalidOrExpired -> clearAuthTokensAndRelogin()
                        else -> onLoginCancelled(result.error?.message)
                    }

                    is NetworkFailure -> onLoginCancelled(result.error.message)
                    is UnknownFailure -> onLoginCancelled(result.error.message)
                }

                Logger.d(DebugTag) { "Refresh login failed" }
            }
        }

        loginState.isEnabled = true
    }

    private fun onLoginCancelled(failureMessage: String? = null) = coroutineScope.launchUnit {

        server?.stop()
        server = null

        Logger.d(DebugTag) { if (failureMessage == null) "Login Cancelled" else "Login failed" }

        when {
            failureMessage != null -> {

                saveAuthTokensToPrefs(appPrefs, null)

                resultHandle.onFailure(failureMessage)
            }

            else -> resultHandle.onCancel()
        }
    }

    @Suppress("ktlint:standard:no-blank-line-in-list")
    @Serializable
    data class FyersAuthTokens(

        @SerialName("access_token")
        val accessToken: String,

        @SerialName("refresh_token")
        val refreshToken: String,

        @SerialName("initial_login_instant")
        val initialLoginInstant: Instant,
    )

    class Builder(
        private val appDispatchers: AppDispatchers,
        private val fyersApi: FyersApi,
        private val uriHandler: UriHandler,
        private val appPrefs: FlowSettings,
    ) : LoginService.Builder {

        override val key: Any = "Fyers Login"

        override fun build(
            coroutineScope: CoroutineScope,
            resultHandle: LoginService.ResultHandle,
        ): LoginService = FyersLoginService(
            appDispatchers = appDispatchers,
            coroutineScope = coroutineScope,
            resultHandle = resultHandle,
            uriHandler = uriHandler,
            fyersApi = fyersApi,
            appPrefs = appPrefs,
        )
    }

    companion object {

        private const val PORT = 57108
        private const val DebugTag = "Fyers Login"

        fun getAuthTokensFromPrefs(appPrefs: FlowSettings): Flow<FyersAuthTokens?> {
            return appPrefs.getStringOrNullFlow(PrefKeys.FyersAuthTokens)
                .map { prefString ->
                    prefString?.let { Json.decodeFromString<FyersAuthTokens>(prefString) }
                }
        }

        suspend fun saveAuthTokensToPrefs(
            appPrefs: FlowSettings,
            authTokens: FyersAuthTokens?,
        ) {

            when (authTokens) {
                null -> appPrefs.remove(PrefKeys.FyersAuthTokens)
                else -> {

                    val prefString = Json.encodeToString<FyersAuthTokens>(authTokens)

                    appPrefs.putString(PrefKeys.FyersAuthTokens, prefString)
                }
            }
        }
    }
}
