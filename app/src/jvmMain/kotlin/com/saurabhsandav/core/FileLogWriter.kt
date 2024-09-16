package com.saurabhsandav.core

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.Writer
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories

class FileLogWriter(
    appDispatchers: AppDispatchers,
    coroutineScope: CoroutineScope,
    private val appPaths: AppPaths,
) : LogWriter() {

    // Current time
    private val currentTime = Clock.System.now().epochSeconds

    private val logs = MutableSharedFlow<String>(extraBufferCapacity = Int.MAX_VALUE)

    private var writer: Writer? = null

    init {

        coroutineScope.launch {

            logs.collect { log ->

                withContext(appDispatchers.IO + NonCancellable) {

                    writer = writer ?: getLogFileWriter()

                    writer!!.write(log)
                    writer!!.flush()
                }
            }
        }
    }

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {

        logs.tryEmit("$severity: ($tag) $message\n")

        throwable?.let {
            logs.tryEmit("${it.stackTraceToString()}\n")
        }
    }

    private fun getLogFileWriter(): Writer {

        val logDirectory = appPaths.appDataPath.resolve("logs")

        // Create log directory
        logDirectory.createDirectories()

        val logFile = logDirectory.resolve("$currentTime.log")

        return logFile.bufferedWriter(options = arrayOf(CREATE, APPEND))
    }

    fun destroy() {

        writer?.apply {
            flush()
            close()
        }
    }
}
