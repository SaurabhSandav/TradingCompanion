package com.saurabhsandav.core.ui.loginservice

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

interface LoginService {

    @Composable
    fun Windows()

    interface Builder {

        val key: Any

        fun build(
            coroutineScope: CoroutineScope,
            resultHandle: ResultHandle,
        ): LoginService
    }

    interface ResultHandle {

        fun onCancel()

        fun onFailure(message: String?)

        fun onSuccess()
    }
}

@Suppress("ktlint:standard:function-naming")
inline fun ResultHandle(
    crossinline onCancel: () -> Unit = {},
    crossinline onFailure: (message: String?) -> Unit = {},
    crossinline onSuccess: () -> Unit = {},
) = object : LoginService.ResultHandle {

    override fun onCancel() = onCancel()

    override fun onFailure(message: String?) = onFailure(message)

    override fun onSuccess() = onSuccess()
}
