package com.saurabhsandav.core.ui.landing

import androidx.compose.runtime.Composable

interface LandingSwitcherItem {

    @Composable
    fun Content()

    @Composable
    fun Windows() = Unit
}
