package com.saurabhsandav.core.utils

import androidx.compose.ui.platform.UriHandler
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.jetbrains.skiko.URIManager

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class AppUriHandler : UriHandler {

    private val delegate = URIManager()

    override fun openUri(uri: String) {
        delegate.openUri(uri)
    }
}
