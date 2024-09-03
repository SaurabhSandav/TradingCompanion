package com.saurabhsandav.core

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.saurabhsandav.core.utils.AppPaths
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import java.io.Writer
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories

class FileLogWriter(
    coroutineScope: CoroutineScope,
) : LogWriter() {

    // Current time
    private val currentTime = Clock.System.now().epochSeconds

    private val logs = MutableSharedFlow<String>(extraBufferCapacity = Int.MAX_VALUE)

    private var writer: Writer? = null

    init {

        coroutineScope.launch {

            logs.collect { log ->

                withContext(Dispatchers.IO + NonCancellable) {

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

        val logDirectory = AppPaths.appDataPath.resolve("logs")

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
