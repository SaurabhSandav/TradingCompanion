package com.saurabhsandav.core.utils

import androidx.compose.ui.platform.UriHandler
import org.jetbrains.skiko.URIManager

class AppUriHandler : UriHandler {

    private val delegate = URIManager()

    override fun openUri(uri: String) {
        delegate.openUri(uri)
    }
}
