package com.saurabhsandav.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AppDispatchers(
    val Main: CoroutineDispatcher = Dispatchers.Main,
    val Default: CoroutineDispatcher = Dispatchers.Default,
    val IO: CoroutineDispatcher = Dispatchers.IO,
    val Unconfined: CoroutineDispatcher = Dispatchers.Unconfined,
)
