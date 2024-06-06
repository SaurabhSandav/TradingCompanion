package com.saurabhsandav.core.ui.stockchart

import kotlinx.datetime.Instant

class LoadConfig(
    val initialLoadBefore: () -> Instant,
    val loadMoreCount: Int = 200,
    val initialLoadCount: Int = loadMoreCount * 2,
    val loadMoreThreshold: Int = 50,
    val maxCandleCount: Int? = null,
)
