package com.saurabhsandav.core

import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

@Suppress("TestFunctionName", "ktlint:standard:function-naming")
fun FakeAppDispatchers(scope: TestScope): AppDispatchers {

    val testDispatcher = StandardTestDispatcher(scope.testScheduler)

    return AppDispatchers(
        Main = testDispatcher,
        Default = testDispatcher,
        IO = testDispatcher,
        Unconfined = testDispatcher,
    )
}
