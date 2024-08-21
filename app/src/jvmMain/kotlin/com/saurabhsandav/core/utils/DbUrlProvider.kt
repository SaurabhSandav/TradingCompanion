package com.saurabhsandav.core.utils

import java.nio.file.Path
import kotlin.io.path.absolutePathString

interface DbUrlProvider {

    fun getAppDbUrl(): String

    fun getCandlesDbUrl(): String

    fun getTradingRecordDbUrl(path: Path): String

    companion object {

        operator fun invoke(
            appPaths: AppPaths,
        ): DbUrlProvider = object : DbUrlProvider {

            override fun getAppDbUrl(): String {
                return "jdbc:sqlite:${appPaths.appDBPath.absolutePathString()}"
            }

            override fun getCandlesDbUrl(): String {
                return "jdbc:sqlite:${appPaths.candlesDBPath.absolutePathString()}"
            }

            override fun getTradingRecordDbUrl(path: Path): String {
                return "jdbc:sqlite:${path.absolutePathString()}/Trades.db"
            }
        }
    }
}
