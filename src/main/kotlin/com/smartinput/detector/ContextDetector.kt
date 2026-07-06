package com.smartinput.detector

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile

/**
 * Interface for detecting the current context/scenario in the editor.
 * Implementations should determine what input method is appropriate for the current context.
 */
interface ContextDetector {
    /**
     * Check if this detector is active for the given context.
     * @return true if this detector should handle the context
     */
    fun isActive(editor: Editor?, file: VirtualFile?): Boolean

    /**
     * Get the recommended input method for the current context.
     * @return the recommended input method, or null if no recommendation
     */
    fun getRecommendedInputMethod(): InputMethodRecommendation?

    /**
     * Priority of this detector. Higher priority detectors are checked first.
     */
    fun getPriority(): Int = 0
}

/**
 * Recommendation for which input method to use
 */
enum class InputMethodRecommendation {
    ENGLISH,
    CHINESE,
    KEEP_CURRENT
}
