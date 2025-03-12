package com.saurabhsandav.core.ui.loginservice.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState
import co.touchlab.kermit.Logger
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.ui.common.app.AppDialogWindow
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.loginservice.LoginService
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.fyers_api.FyersApi
import com.slack.eithernet.ApiResult
import com.slack.eithernet.ApiResult.Failure.ApiFailure
import com.slack.eithernet.ApiResult.Failure.HttpFailure
import com.slack.eithernet.ApiResult.Failure.NetworkFailure
import com.slack.eithernet.ApiResult.Failure.UnknownFailure
import com.slack.eithernet.successOrNull
import io.ktor.http.ContentType
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
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

    private var loginState by mutableStateOf<LoginState?>(null)

    init {
        initiateLogin()
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
                canRefresh -> refreshLogin(authTokens)
                else -> loginStage1()
            }
        }
    }

    private fun loginStage1(reLogin: Boolean = false) {

        Logger.d(DebugTag) { "Initiating stage 1 (Login to Fyers website)" }

        loginState = if (reLogin) LoginState.ReLogin else LoginState.InitialLogin

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
            is ApiResult.Success -> {

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

    private fun refreshLogin(authTokens: FyersAuthTokens) = coroutineScope.launchUnit {

        Logger.d(DebugTag) { "Refresh login. Awaiting Pin..." }

        val pin = CompletableDeferred<String>()

        loginState = LoginState.RefreshLogin(pin)

        val result = fyersApi.refreshLogin(
            refreshToken = authTokens.refreshToken,
            pin = pin.await(),
        )

        when (result) {
            is ApiResult.Success -> {

                val refreshedAuthTokens = authTokens.copy(accessToken = result.value.accessToken)

                saveAuthTokensToPrefs(appPrefs, refreshedAuthTokens)

                resultHandle.onSuccess()

                Logger.d(DebugTag) { "Refresh login successful" }
            }

            is ApiResult.Failure -> {

                when (result) {
                    is ApiFailure -> {

                        saveAuthTokensToPrefs(appPrefs, null)

                        loginStage1(reLogin = true)
                    }

                    is HttpFailure -> onLoginCancelled(result.error?.message)
                    is NetworkFailure -> onLoginCancelled(result.error.message)
                    is UnknownFailure -> onLoginCancelled(result.error.message)
                }

                Logger.d(DebugTag) { "Refresh login failed" }
            }
        }
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

    @Composable
    override fun Windows() {

        when (val loginState = loginState) {
            null -> Unit
            LoginState.InitialLogin, LoginState.ReLogin -> {

                AppDialogWindow(
                    onCloseRequest = ::onLoginCancelled,
                    state = rememberDialogState(
                        width = 300.dp,
                        height = 100.dp,
                    ),
                    title = "Login to Fyers",
                    alwaysOnTop = true,
                ) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            space = MaterialTheme.dimens.rowHorizontalSpacing,
                            alignment = Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        CircularProgressIndicator()

                        Text(
                            text = when (loginState) {
                                is LoginState.ReLogin -> "Refresh failed. Awaiting login..."
                                else -> "Awaiting login..."
                            },
                        )
                    }
                }
            }

            is LoginState.RefreshLogin -> {

                AppDialogWindow(
                    onCloseRequest = ::onLoginCancelled,
                    state = rememberDialogState(
                        width = 300.dp,
                        height = 100.dp,
                    ),
                    title = "Enter Fyers pin",
                    alwaysOnTop = true,
                ) {

                    var pin by state { "" }
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(Unit) { focusRequester.requestFocus() }

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.dimens.containerPadding)
                            .wrapContentSize()
                            .focusRequester(focusRequester),
                        value = pin,
                        onValueChange = {
                            if (it.isEmpty() || (it.length <= 4 && it.toIntOrNull() != null)) {
                                pin = it
                            }
                        },
                        singleLine = true,
                        trailingIcon = {

                            TextButton(
                                onClick = { loginState.pin.complete(pin) },
                                enabled = pin.length == 4,
                                content = { Text("Login") },
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }
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

    private sealed class LoginState {

        data object InitialLogin : LoginState()

        data object ReLogin : LoginState()

        data class RefreshLogin(
            val pin: CompletableDeferred<String>,
        ) : LoginState()
    }

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
