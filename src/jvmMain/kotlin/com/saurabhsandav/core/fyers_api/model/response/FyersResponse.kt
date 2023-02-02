package com.saurabhsandav.core.fyers_api.model.response

import io.ktor.http.*

data class FyersResponse<T>(
    val s: String?,
    val code: Int?,
    val message: String?,
    val statusCode: HttpStatusCode,
    val result: T?,
)
