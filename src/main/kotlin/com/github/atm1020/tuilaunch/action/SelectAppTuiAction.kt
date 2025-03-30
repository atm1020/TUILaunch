package com.github.atm1020.tuilaunch.action

import com.github.atm1020.tuilaunch.services.TuiAppLaunchService
import com.github.atm1020.tuilaunch.services.TuiLauncherSettings
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class TuiLauncherActionGroup : ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val settings = TuiLauncherSettings.getInstance()
        return settings.state.tuiApps.map { appConfig ->
            object : AnAction(appConfig.name, appConfig.description, null) {
                override fun actionPerformed(e: AnActionEvent) {
                    e.project?.service<TuiAppLaunchService>()?.launchApp(appConfig.command)
                }
            }
        }.toTypedArray()
    }
}
