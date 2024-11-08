package com.saurabhsandav.core

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option

suspend fun main(args: Array<String>) = Main().main(args)

class Main : SuspendingCliktCommand() {

    private val debugMode: Boolean by option("--debug", "-D").flag().help("Run in debug mode")

    override suspend fun run() {

        System.setProperty("debugMode", debugMode.toString())

        runApp()
    }
}
