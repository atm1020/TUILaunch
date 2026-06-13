package com.github.atm1020.tuilaunch.terminal

import com.intellij.openapi.Disposable
import javax.swing.JComponent

/** One running TUI app: its UI component, focus control, and exit signal. */
class TerminalSession(
    val component: JComponent,
    private val requestFocus: () -> Unit,
    private val registerTerminationCallback: ((() -> Unit) -> Unit),
) {
    fun requestFocus() = requestFocus.invoke()

    /** Registers a callback fired (on the EDT) when the underlying process exits. */
    fun onTerminated(callback: () -> Unit) = registerTerminationCallback(callback)
}

interface TerminalSessionFactory {
    fun create(parent: Disposable, command: String): TerminalSession

    fun createAsync(
        parent: Disposable,
        command: String,
        onCreated: (TerminalSession) -> Unit,
        onFailed: (Throwable) -> Unit,
    ) {
        try {
            onCreated(create(parent, command))
        } catch (throwable: Throwable) {
            onFailed(throwable)
        }
    }
}
