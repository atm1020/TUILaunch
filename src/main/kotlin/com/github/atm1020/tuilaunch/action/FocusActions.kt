package com.github.atm1020.tuilaunch.action

import com.github.atm1020.tuilaunch.services.TuiAppLaunchService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

/** Reveals the TUILaunch tool window and focuses the active TUI session. No-op if none is open. */
class FocusTuiAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.focusTui()
    }
}

/** Returns keyboard focus to the editor. */
class FocusEditorAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.focusEditor()
    }
}

/** Toggles focus between the active TUI session and the editor. */
class ToggleFocusAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.toggleFocus()
    }
}

/** Toggles TUILaunch tool window visibility without intentionally moving focus. */
class ToggleToolWindowAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.toggleToolWindow()
    }
}

/** Closes the currently selected TUI session tab. */
class CloseActiveTuiAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.closeActiveTui()
    }
}

/** Moves focus to the next open TUI session tab. */
class NextTuiTabAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.nextTuiTab()
    }
}

/** Selects the next open TUI session tab without requesting terminal focus. */
class NextTuiTabWithoutFocusAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.nextTuiTabWithoutFocus()
    }
}

/** Moves focus to the previous open TUI session tab. */
class PreviousTuiTabAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.previousTuiTab()
    }
}

/** Selects the previous open TUI session tab without requesting terminal focus. */
class PreviousTuiTabWithoutFocusAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.previousTuiTabWithoutFocus()
    }
}
