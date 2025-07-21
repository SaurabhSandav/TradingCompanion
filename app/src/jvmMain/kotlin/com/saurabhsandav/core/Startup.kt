package com.saurabhsandav.core

import co.touchlab.kermit.Logger
import com.saurabhsandav.core.di.AppCoroutineScope
import com.saurabhsandav.core.ui.common.webview.MyCefApp
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@Inject
class StartupManager(
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val fileLogWriter: FileLogWriter,
    private val myCefApp: Lazy<MyCefApp>,
    private val startupJobs: Set<StartupJob>,
) {

    init {
        setupLogging()
        runStartupJobs()
    }

    private fun setupLogging() {

        // Set FileLogWriter as the only LogWriter
        Logger.addLogWriter(fileLogWriter)

        val globalExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            Logger.e(e) { "Unhandled exception caught!" }
        }

        Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler)
    }

    private fun runStartupJobs() {

        startupJobs.forEach { job ->
            appScope.launch { job.run() }
        }
    }

    fun destroy() {
        fileLogWriter.destroy()
        if (myCefApp.isInitialized()) myCefApp.value.dispose()
        appScope.cancel()
    }
}

interface StartupJob {

    suspend fun run()
}
