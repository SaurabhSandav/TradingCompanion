package com.saurabhsandav.core.ui.loginservice.fyers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed class FyersLoginState {

    data object InitialLogin : FyersLoginState()

    data object ReLogin : FyersLoginState()

    class RefreshLogin : FyersLoginState() {
        var pin by mutableStateOf("")
    }
}
