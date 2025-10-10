package com.saurabhsandav.core.utils

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class AppDispatchers(
    val Main: CoroutineContext = Dispatchers.Main,
    val Default: CoroutineContext = Dispatchers.Default,
    val IO: CoroutineContext = Dispatchers.IO,
    val Unconfined: CoroutineContext = Dispatchers.Unconfined,
)
