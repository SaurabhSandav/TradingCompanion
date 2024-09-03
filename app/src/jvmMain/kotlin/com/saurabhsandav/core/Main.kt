package com.saurabhsandav.core

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option

fun main(args: Array<String>) = Main().main(args)

class Main : CliktCommand() {

    private val debugMode: Boolean by option("--debug", "-D").flag().help("Run in debug mode")

    override fun run() {

        System.setProperty("debugMode", debugMode.toString())

        runApp()
    }
}
