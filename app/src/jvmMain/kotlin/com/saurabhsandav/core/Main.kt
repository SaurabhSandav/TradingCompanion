package com.saurabhsandav.core

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlin.system.exitProcess

suspend fun main(args: Array<String>) {
    Main().main(args)
    // Workaround issue that causes app to not exit immediately.
    // To Reproduce: Open Chart. Exit. App exits after roughly a minute. Sometimes more.
    // Issue only happens when using Jetbrains Runtime. Narrowed down the issue to coroutine usage in the
    // ChartsPresenter class. Couldn't narrow it down further.
    exitProcess(0)
}

class Main : SuspendingCliktCommand() {

    private val debugMode: Boolean by option("--debug", "-D").flag().help("Run in debug mode")

    override suspend fun run() {

        val isDebugMode = BuildKonfig.DEBUG_MODE || debugMode

        runApp(isDebugMode)
    }
}
