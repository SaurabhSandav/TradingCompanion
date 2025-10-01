package com.saurabhsandav.trading.core

internal actual fun throwNoKeyException(): Nothing = throw NoKeyExceptionImpl()

private class NoKeyExceptionImpl : Exception() {

    override fun fillInStackTrace(): Throwable = this
}
