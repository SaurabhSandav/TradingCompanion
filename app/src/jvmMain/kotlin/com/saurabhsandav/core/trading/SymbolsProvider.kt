package com.saurabhsandav.core.trading

import androidx.paging.PagingSource
import com.saurabhsandav.core.utils.ListPagingSource
import com.saurabhsandav.core.utils.NIFTY500
import com.saurabhsandav.trading.core.SymbolId

interface SymbolsProvider {

    fun getSymbols(filterQuery: String): PagingSource<Int, SymbolId>
}

fun SymbolsProvider(): SymbolsProvider = AppSymbolsProvider()

private class AppSymbolsProvider : SymbolsProvider {

    override fun getSymbols(filterQuery: String): PagingSource<Int, SymbolId> {
        return ListPagingSource(NIFTY500.filter { it.value.contains(filterQuery, ignoreCase = true) })
    }
}
