package ui.fyerslogin

import com.russhwolf.settings.coroutines.FlowSettings
import fyers_api.FyersApi
import utils.PrefKeys

internal class FyersLoginState(
    private val fyersApi: FyersApi,
    private val appPrefs: FlowSettings,
    val onDismiss: () -> Unit,
    private val onLoginSuccess: () -> Unit,
    private val onLoginFailure: (String?) -> Unit,
) {

    val url = fyersApi.getLoginURL()

    suspend fun onLoginSuccess(redirectUrl: String) {

        val response = fyersApi.getAccessToken(redirectUrl)

        when (response.result) {
            null -> {
                onDismiss()
                onLoginFailure(response.message)
            }

            else -> {
                appPrefs.putString(PrefKeys.FyersAccessToken, response.result.accessToken)
                onDismiss()
                onLoginSuccess()
            }
        }
    }
}
