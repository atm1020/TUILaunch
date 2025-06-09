package com.github.atm1020.tuilaunch.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.sh.run.ShRunner


@Service(Service.Level.PROJECT)
class TuiAppLaunchService(private val project: Project) {
    fun launchApp(command: String, title: String) {
        ApplicationManager.getApplication().getService(ShRunner::class.java)
            .run(project, "${command};exit", project.basePath!!, title, true)
    }
}