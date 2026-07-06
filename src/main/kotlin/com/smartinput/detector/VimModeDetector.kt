package com.smartinput.detector

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.smartinput.settings.SmartInputSettings

/**
 * Detects IdeaVim command mode.
 * This detector is only active when IdeaVim plugin is installed.
 *
 * Note: This is a placeholder implementation. Full IdeaVim integration
 * requires the IdeaVim plugin API and proper event listeners.
 */
class VimModeDetector : ContextDetector {
    private val logger = Logger.getInstance(VimModeDetector::class.java)
    private var isInCommandMode = false
    private var vimAvailable = false

    init {
        checkVimAvailability()
    }

    private fun checkVimAvailability() {
        try {
            // Check if IdeaVim plugin is available
            val pluginClass = Class.forName("com.maddyhome.idea.vim.VimPlugin")
            vimAvailable = true
            logger.info("IdeaVim plugin detected")
        } catch (e: ClassNotFoundException) {
            vimAvailable = false
            logger.info("IdeaVim plugin not found, VimModeDetector will be inactive")
        }
    }

    override fun isActive(editor: Editor?, file: VirtualFile?): Boolean {
        val settings = SmartInputSettings.getInstance()
        if (!settings.state.switchInVimCommandMode) return false
        if (!vimAvailable) return false
        if (editor == null) return false

        return isInCommandMode
    }

    override fun getRecommendedInputMethod(): InputMethodRecommendation {
        return InputMethodRecommendation.ENGLISH
    }

    override fun getPriority(): Int = 20

    /**
     * Update the Vim mode state. Called by VimModeChangeListener.
     */
    fun updateVimMode(inCommandMode: Boolean) {
        if (isInCommandMode != inCommandMode) {
            isInCommandMode = inCommandMode
            logger.info("Vim command mode: $inCommandMode")
        }
    }

    /**
     * Check if IdeaVim is available in the current installation.
     */
    fun isVimAvailable(): Boolean = vimAvailable
}
