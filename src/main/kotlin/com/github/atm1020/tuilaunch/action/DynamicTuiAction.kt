package com.github.atm1020.tuilaunch.action

import com.github.atm1020.tuilaunch.services.TuiAppLaunchService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class DynamicUserAction(private val command: String, private val title: String) :
    AnAction(command, "TUILaunch app: ${command}", null) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<TuiAppLaunchService>()?.launchApp(command, title)
    }
}
