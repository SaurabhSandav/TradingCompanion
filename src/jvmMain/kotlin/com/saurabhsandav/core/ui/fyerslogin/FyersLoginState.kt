package com.saurabhsandav.core.ui.fyerslogin

import androidx.compose.runtime.Stable
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.fyers_api.FyersApi
import com.saurabhsandav.core.utils.PrefKeys

@Stable
internal class FyersLoginState(
    private val fyersApi: FyersApi,
    private val appPrefs: FlowSettings,
    val onCloseRequest: () -> Unit,
    private val onLoginSuccess: () -> Unit,
    private val onLoginFailure: (String?) -> Unit,
) {

    val url = fyersApi.getLoginURL()

    suspend fun onLoginSuccess(redirectUrl: String) {

        val response = fyersApi.validateLogin(redirectUrl)

        when (response.result) {
            null -> {
                onCloseRequest()
                onLoginFailure(response.message)
            }

            else -> {
                appPrefs.putString(PrefKeys.FyersAccessToken, response.result.accessToken)
                onCloseRequest()
                onLoginSuccess()
            }
        }
    }
}
