package com.smartinput.detector

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.ToolWindowType
import com.smartinput.settings.SmartInputSettings

/**
 * Detects when the Terminal tool window gains focus.
 * Automatically switches to English input method for terminal commands.
 */
class TerminalFocusDetector : ContextDetector, ToolWindowManagerListener {
    private val logger = Logger.getInstance(TerminalFocusDetector::class.java)
    private var isTerminalFocused = false
    private var currentProject: Project? = null

    companion object {
        private const val TERMINAL_WINDOW_ID = "Terminal"
    }

    fun setProject(project: Project) {
        currentProject = project
    }

    override fun isActive(editor: Editor?, file: VirtualFile?): Boolean {
        val settings = SmartInputSettings.getInstance()
        if (!settings.state.switchInTerminal) return false

        return isTerminalFocused
    }

    override fun getRecommendedInputMethod(): InputMethodRecommendation {
        return InputMethodRecommendation.ENGLISH
    }

    override fun getPriority(): Int = 25

    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        val project = currentProject ?: return
        val terminalWindow = toolWindowManager.getToolWindow(TERMINAL_WINDOW_ID)

        if (terminalWindow != null) {
            val wasTerminalFocused = isTerminalFocused
            isTerminalFocused = terminalWindow.isVisible &&
                               terminalWindow.isActive

            if (wasTerminalFocused != isTerminalFocused) {
                logger.info("Terminal focus changed: $isTerminalFocused")
            }
        }
    }

    /**
     * Check if the terminal window is currently active.
     */
    fun isTerminalActive(): Boolean {
        val project = currentProject ?: return false
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val terminalWindow = toolWindowManager.getToolWindow(TERMINAL_WINDOW_ID)
        return terminalWindow?.isActive == true
    }

    /**
     * Reset terminal focus state.
     */
    fun resetFocus() {
        isTerminalFocused = false
    }
}
