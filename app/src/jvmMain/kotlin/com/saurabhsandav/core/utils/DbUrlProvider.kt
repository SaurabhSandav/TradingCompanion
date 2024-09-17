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
                return "jdbc:sqlite:${appPaths.appDataPath.absolutePathString()}/${appPaths.appName}.db"
            }

            override fun getCandlesDbUrl(): String {
                return "jdbc:sqlite:${appPaths.appDataPath.absolutePathString()}/Candles.db"
            }

            override fun getTradingRecordDbUrl(path: Path): String {
                return "jdbc:sqlite:${path.absolutePathString()}/Trades.db"
            }
        }
    }
}
