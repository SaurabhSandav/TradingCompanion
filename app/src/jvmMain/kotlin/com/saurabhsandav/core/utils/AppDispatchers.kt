package com.saurabhsandav.core.utils

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@SingleIn(AppScope::class)
@Inject
class AppDispatchers(
    val Main: CoroutineContext = Dispatchers.Main,
    val Default: CoroutineContext = Dispatchers.Default,
    val IO: CoroutineContext = Dispatchers.IO,
    val Unconfined: CoroutineContext = Dispatchers.Unconfined,
)
