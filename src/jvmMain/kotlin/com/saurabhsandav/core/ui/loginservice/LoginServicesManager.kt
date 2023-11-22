package com.saurabhsandav.core.ui.loginservice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import com.saurabhsandav.core.ui.common.app.AppWindowOwner
import com.saurabhsandav.core.ui.loginservice.LoginService.Builder
import com.saurabhsandav.core.ui.loginservice.LoginService.ResultHandle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class LoginServicesManager {

    private val holdersMap = mutableStateMapOf<Any, ServiceHolder>()

    @Composable
    fun Windows() {

        holdersMap.values.forEach { holder ->

            key(holder) {

                holder.owner.Window {

                    holder.service.Windows()
                }
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

        // Bring window to front
        holder.owner.childrenToFront()

        holder.resultHandle.resultHandles += resultHandle
    }

    @Stable
    private class ServiceHolder(
        val service: LoginService,
        val resultHandle: DefaultResultHandle,
        val owner: AppWindowOwner = AppWindowOwner(),
    )

    @Stable
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
