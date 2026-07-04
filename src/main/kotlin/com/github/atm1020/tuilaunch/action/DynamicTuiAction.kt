package com.github.atm1020.tuilaunch.action

import com.github.atm1020.tuilaunch.services.TuiAppLaunchService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class DynamicUserAction(
    private val actionId: String,
    private var command: String,
    private var title: String,
) : AnAction(title, "TUILaunch app: $command", null) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.toggle(actionId, command, title)
    }

    fun update(command: String, title: String) {
        this.command = command
        this.title = title
        templatePresentation.text = title
        templatePresentation.description = "TUILaunch app: $command"
    }
}
