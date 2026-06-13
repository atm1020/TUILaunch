package com.github.atm1020.tuilaunch.terminal

import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions
import org.jetbrains.plugins.terminal.TerminalProjectOptionsProvider
import org.jetbrains.plugins.terminal.runner.LocalTerminalStartCommandBuilder
import java.awt.KeyboardFocusManager
import java.io.File
import java.awt.event.KeyEvent

class JediTermSessionFactory(
    private val project: Project,
    private val prefixCommandActions: () -> Map<Int, () -> Unit> = { emptyMap() },
) : TerminalSessionFactory {
    override fun create(parent: Disposable, command: String): TerminalSession {
        val shellPath = TerminalProjectOptionsProvider.getInstance(project).shellPath
        return create(parent, command, shellPath)
    }

    override fun createAsync(
        parent: Disposable,
        command: String,
        onCreated: (TerminalSession) -> Unit,
        onFailed: (Throwable) -> Unit,
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val shellPath = TerminalProjectOptionsProvider.getInstance(project).shellPath
                invokeLater {
                    if ((parent as? CheckedDisposable)?.isDisposed != true) {
                        onCreated(create(parent, command, shellPath))
                    }
                }
            } catch (throwable: Throwable) {
                invokeLater { onFailed(throwable) }
            }
        }
    }

    private fun create(parent: Disposable, command: String, shellPath: String): TerminalSession {
        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
        val workingDir = project.basePath ?: System.getProperty("user.home")
        val baseShellCommand = LocalTerminalStartCommandBuilder.convertShellPathToCommand(shellPath)
        val options = ShellStartupOptions.Builder()
            .shellCommand(baseShellCommand + runCommandArgs(baseShellCommand.first(), command))
            .workingDirectory(workingDir)
            .build()
        val widget = runner.startShellTerminalWidget(parent, options, false)
        installPrefixFocusSwitch(widget, parent)
        return TerminalSession(
            component = widget.component,
            requestFocus = { widget.requestFocus() },
            registerTerminationCallback = { callback ->
                widget.addTerminationCallback({ callback() }, parent)
            },
        )
    }

    /**
     * Picks the "run this command, then exit" argument for the given shell. The flag is shell-specific:
     * cmd.exe uses `/c`, PowerShell uses `-Command`, and POSIX shells (bash/zsh/sh/fish, including Git
     * Bash and WSL bash on Windows) use `-c`. All three cause the shell to exit once the command
     * finishes, which fires the termination callback that closes the tab.
     */
    private fun runCommandArgs(shellExe: String, command: String): List<String> =
        when (File(shellExe).name.removeSuffix(".exe").lowercase()) {
            "cmd" -> listOf("/c", command)
            "powershell", "pwsh" -> listOf("-Command", command)
            else -> listOf("-c", command)
        }

    /**
     * Lets the user leave a focused TUI app with the configured modifier combo (e.g. Ctrl+Space). The
     * dispatcher is unregistered when [parent] is disposed (i.e. when the tab closes).
     */
    private fun installPrefixFocusSwitch(widget: TerminalWidget, parent: Disposable) {
        val state = TuiLauncherSettings.getInstance().state
        val dispatcher = TerminalPrefixKeyDispatcher(
            terminalComponent = widget.component,
            escapeModifierMask = modifierMaskOf(state.escapeModifier),
            escapeKeyCode = state.escapeKeyCode,
            prefixCommandActions = prefixCommandActions(),
        )
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        focusManager.addKeyEventDispatcher(dispatcher)
        Disposer.register(parent) { focusManager.removeKeyEventDispatcher(dispatcher) }
    }

    /** Translates the persisted modifier name into an AWT extended-modifier mask. */
    private fun modifierMaskOf(modifier: String): Int = when (modifier) {
        "ALT" -> KeyEvent.ALT_DOWN_MASK
        else -> KeyEvent.CTRL_DOWN_MASK
    }
}
