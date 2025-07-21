package com.saurabhsandav.core.ui.loginservice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import com.saurabhsandav.core.ui.loginservice.LoginService.Builder
import com.saurabhsandav.core.ui.loginservice.LoginService.ResultHandle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

@SingleIn(AppScope::class)
@Inject
class LoginServicesManager {

    private val holdersMap = mutableStateMapOf<Any, ServiceHolder>()

    @Composable
    fun Dialogs() {

        holdersMap.values.forEach { holder ->

            key(holder) {

                holder.service.Dialogs()
            }
        }
    }

    fun addService(
        serviceBuilder: Builder,
        resultHandle: ResultHandle,
    ) {

        val holder = holdersMap.getOrPut(serviceBuilder.key) {

            val coroutineScope = MainScope()
            val defaultResultHandle = DefaultResultHandle {
                coroutineScope.cancel()
                holdersMap.remove(serviceBuilder.key)
            }

            val service = serviceBuilder.build(
                coroutineScope = coroutineScope,
                resultHandle = defaultResultHandle,
            )

            ServiceHolder(
                service = service,
                resultHandle = defaultResultHandle,
            )
        }

        holder.resultHandle.resultHandles += resultHandle
    }

    private class ServiceHolder(
        val service: LoginService,
        val resultHandle: DefaultResultHandle,
    )

    private class DefaultResultHandle(
        private val onFinished: () -> Unit,
    ) : ResultHandle {

        val resultHandles = mutableListOf<ResultHandle>()

        override fun onCancel() {
            onFinished()
            resultHandles.forEach { it.onCancel() }
        }

        override fun onFailure(message: String?) {
            onFinished()
            resultHandles.forEach { it.onFailure(message) }
        }

        override fun onSuccess() {
            onFinished()
            resultHandles.forEach { it.onSuccess() }
        }
    }
}
