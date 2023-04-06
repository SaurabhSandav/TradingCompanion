package com.saurabhsandav.core.ui.studies.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
internal interface Study {

    @Composable
    fun render()

    @Stable
    interface Factory<T : Study> {

        val name: String

        fun create(): T
    }
}
