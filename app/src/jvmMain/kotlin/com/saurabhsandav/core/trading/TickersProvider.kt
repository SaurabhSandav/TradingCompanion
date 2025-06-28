package com.saurabhsandav.core.trading

import androidx.paging.PagingSource
import com.saurabhsandav.core.utils.ListPagingSource
import com.saurabhsandav.core.utils.NIFTY500

interface TickersProvider {

    fun getTickers(filterQuery: String): PagingSource<Int, String>
}

class AppTickersProvider : TickersProvider {

    override fun getTickers(filterQuery: String): PagingSource<Int, String> {
        return ListPagingSource(NIFTY500.filter { it.contains(filterQuery, ignoreCase = true) })
    }
}
