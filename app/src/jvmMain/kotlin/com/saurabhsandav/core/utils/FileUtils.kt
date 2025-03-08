package com.saurabhsandav.core.utils

import java.awt.Desktop
import java.io.File

fun File.openExternally(onOpenNotSupported: () -> Unit = { error("Opening file externally is not supported") }) {

    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        onOpenNotSupported()
    }

    Desktop.getDesktop().open(this)
}
