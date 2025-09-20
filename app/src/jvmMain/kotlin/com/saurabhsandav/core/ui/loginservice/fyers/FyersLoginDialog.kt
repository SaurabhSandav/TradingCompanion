package com.saurabhsandav.core.ui.loginservice.fyers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.common.trim
import com.saurabhsandav.core.ui.loginservice.fyers.FyersLoginState.InitialLogin
import com.saurabhsandav.core.ui.loginservice.fyers.FyersLoginState.ReLogin
import com.saurabhsandav.core.ui.loginservice.fyers.FyersLoginState.RefreshLogin
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun FyersLoginDialog(
    loginState: FyersLoginState,
    onLoginCancelled: () -> Unit,
    onSubmitRefreshPin: (String) -> Unit,
) {

    AppDialog(
        onDismissRequest = onLoginCancelled,
    ) {

        when (loginState) {
            InitialLogin, ReLogin -> LoginDialog(loginState = loginState)
            is RefreshLogin -> RefreshPinDialog(
                refreshLogin = loginState,
                onSubmit = onSubmitRefreshPin,
            )
        }
    }
}

@Composable
private fun LoginDialog(loginState: FyersLoginState) {

    Row(
        modifier = Modifier
            .padding(MaterialTheme.dimens.containerPadding),
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.rowHorizontalSpacing,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        CircularProgressIndicator()

        Text(
            text = when (loginState) {
                is ReLogin -> "Session expired. Awaiting login..."
                else -> "Awaiting login..."
            },
        )
    }
}

@Composable
private fun RefreshPinDialog(
    refreshLogin: RefreshLogin,
    onSubmit: (String) -> Unit,
) {

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val textFieldState = rememberTextFieldState()

    OutlinedSecureTextField(
        modifier = Modifier
            .padding(MaterialTheme.dimens.containerPadding)
            .wrapContentSize()
            .focusRequester(focusRequester),
        state = textFieldState,
        inputTransformation = InputTransformation.trim().then {

            refreshLogin.isError = false

            val text = toString()

            when {
                text.isEmpty() -> Unit
                text.toIntOrNull() == null || text.length > FyersPinLength -> revertAllChanges()
            }
        },
        isError = refreshLogin.isError,
        enabled = refreshLogin.isEnabled,
        trailingIcon = {

            TextButton(
                onClick = { onSubmit(textFieldState.text.toString()) },
                enabled = textFieldState.text.length == FyersPinLength && refreshLogin.isEnabled,
                content = { Text("Login") },
            )
        },
        label = { Text("Fyers Pin") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        onKeyboardAction = KeyboardActionHandler {
            val canSubmit = textFieldState.text.length == FyersPinLength && refreshLogin.isEnabled
            if (canSubmit) onSubmit(textFieldState.text.toString())
        },
    )
}

private const val FyersPinLength = 4
