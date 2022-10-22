package fyers_api.model.response

import io.ktor.http.*
import kotlinx.serialization.Serializable

sealed interface FyersResponse<T> {

    val s: String

    data class Success<T>(
        override val s: String,
        val code: Int?,
        val message: String?,
        val result: T,
    ): FyersResponse<T>

    data class Failure<T>(
        override val s: String,
        val code: Int,
        val message: String,
        val statusCode: HttpStatusCode,
    ): FyersResponse<T>
}

@Serializable
sealed interface FyersResult
