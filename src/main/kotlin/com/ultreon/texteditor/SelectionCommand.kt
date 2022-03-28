package com.ultreon.texteditor

import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener

class SelectionCommand(private val command: () -> Unit) : SelectionListener {
    override fun widgetSelected(e: SelectionEvent) {
        command()
    }

    override fun widgetDefaultSelected(e: SelectionEvent?) {

    }
}