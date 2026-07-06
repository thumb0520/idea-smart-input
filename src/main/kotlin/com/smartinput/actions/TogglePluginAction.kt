package com.smartinput.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.smartinput.SmartInputPlugin
import com.smartinput.settings.SmartInputSettings

/**
 * Action to toggle the Smart Input plugin on/off.
 */
class TogglePluginAction : AnAction(), DumbAware {
    private val logger = Logger.getInstance(TogglePluginAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val plugin = project.getService(SmartInputPlugin::class.java)

        plugin.toggleEnabled()

        val status = if (plugin.isEnabled()) "enabled" else "disabled"
        logger.info("Smart Input plugin $status")

        // Show notification
        Messages.showInfoMessage(
            "Smart Input plugin has been $status.",
            "Smart Input"
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val plugin = project.getService(SmartInputPlugin::class.java)
        val settings = SmartInputSettings.getInstance()

        e.presentation.text = if (plugin.isEnabled()) {
            "Disable Smart Input"
        } else {
            "Enable Smart Input"
        }

        e.presentation.isEnabled = true
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
