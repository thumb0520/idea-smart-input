package com.smartinput

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * Eagerly initializes SmartInputPlugin when a project opens.
 * Without this, the plugin service is lazy and never created
 * (no listeners registered, no input method switching happens).
 */
class SmartInputStartupActivity : StartupActivity.DumbAware {
    private val logger = Logger.getInstance(SmartInputStartupActivity::class.java)

    override fun runActivity(project: Project) {
        project.getService(SmartInputPlugin::class.java)
        logger.info("SmartInputPlugin started for project: ${project.name}")
    }
}
