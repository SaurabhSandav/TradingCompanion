package com.saurabhsandav.core

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.saurabhsandav.core.utils.AppPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.io.Writer
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

class FileLogWriter(
    coroutineScope: CoroutineScope,
) : LogWriter() {

    // Current time
    private val currentTime = Clock.System.now().epochSeconds

    private val logs = MutableSharedFlow<String>(extraBufferCapacity = Int.MAX_VALUE)

    private var writer: Writer? = null

    init {

        coroutineScope.launch(Dispatchers.IO + NonCancellable) {

            logs.collect { log ->

                writer = writer ?: getLogFileWriter()

                writer!!.write(log)
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

        val logDirectory = Path(AppPaths.getAppDataPath(), "logs")

        // Create log directory
        logDirectory.createDirectories()

        val logFile = logDirectory.resolve("$currentTime.log")

        return logFile.outputStream(CREATE, APPEND).bufferedWriter()
    }

    fun destroy() {

        writer?.apply {
            flush()
            close()
        }
    }
}
