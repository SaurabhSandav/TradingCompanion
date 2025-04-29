package com.saurabhsandav.core.ui.loginservice.fyers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.saurabhsandav.core.ui.common.app.AppDialog
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

    OutlinedTextField(
        modifier = Modifier
            .padding(MaterialTheme.dimens.containerPadding)
            .wrapContentSize()
            .focusRequester(focusRequester),
        value = refreshLogin.pin,
        onValueChange = {
            if (it.isEmpty() || (it.length <= 4 && it.toIntOrNull() != null)) {
                refreshLogin.pin = it
            }
        },
        singleLine = true,
        trailingIcon = {

            TextButton(
                onClick = { onSubmit(refreshLogin.pin) },
                enabled = refreshLogin.pin.length == FyersPinLength,
                content = { Text("Login") },
            )
        },
        label = { Text("Fyers Pin") },
        placeholder = { Text("Fyers Pin") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (refreshLogin.pin.length == FyersPinLength) onSubmit(refreshLogin.pin) },
        ),
    )
}

private const val FyersPinLength = 4
