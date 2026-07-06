package com.smartinput.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFocusManager
import com.smartinput.settings.SmartInputSettings
import java.awt.Color
import java.awt.Font

@Service
class CursorIndicatorService {
    private val logger = Logger.getInstance(CursorIndicatorService::class.java)
    private val highlighters = mutableMapOf<Editor, RangeHighlighter>()

    fun updateCursorIndicator(editor: Editor, inputMethod: InputMethodManager.InputMethod) {
        val settings = SmartInputSettings.getInstance()
        if (!settings.state.showCursorIndicator) {
            removeIndicator(editor)
            return
        }

        val color = when (inputMethod) {
            InputMethodManager.InputMethod.ENGLISH -> parseColor(settings.state.englishCursorColor)
            InputMethodManager.InputMethod.CHINESE -> parseColor(settings.state.chineseCursorColor)
        }

        updateCaretColor(editor, color)
        updateCaretShape(editor, inputMethod)
    }

    fun updateCapsLockIndicator(editor: Editor, capsLockOn: Boolean) {
        val settings = SmartInputSettings.getInstance()
        if (!settings.state.showCursorIndicator) return

        if (capsLockOn) {
            val color = parseColor(settings.state.capsLockCursorColor)
            updateCaretColor(editor, color)
            updateCaretShapeForCapsLock(editor, true)
        }
    }

    private fun updateCaretColor(editor: Editor, color: Color) {
        try {
            val scheme = EditorColorsManager.getInstance().globalScheme
            // Use reflection to set caret color if available
            val caretColorField = scheme.javaClass.getDeclaredField("myCaretColor")
            caretColorField.isAccessible = true
            caretColorField.set(scheme, color)

            // Refresh editor
            editor.contentComponent.repaint()
        } catch (e: Exception) {
            logger.debug("Could not set caret color directly, using highlighter fallback", e)
            updateCaretColorViaHighlighter(editor, color)
        }
    }

    private fun updateCaretColorViaHighlighter(editor: Editor, color: Color) {
        // Remove existing highlighter
        removeIndicator(editor)

        // Create a thin highlighter at caret position to simulate colored caret
        val caretOffset = editor.caretModel.offset
        if (caretOffset < editor.document.textLength) {
            val attributes = TextAttributes().apply {
                foregroundColor = color
                fontType = Font.BOLD
            }

            val highlighter = editor.markupModel.addRangeHighlighter(
                caretOffset,
                minOf(caretOffset + 1, editor.document.textLength),
                HighlighterLayer.CARET_ROW,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
            )

            highlighters[editor] = highlighter
        }
    }

    private fun updateCaretShape(editor: Editor, inputMethod: InputMethodManager.InputMethod) {
        // Update caret thickness based on input method
        try {
            val caret = editor.caretModel.primaryCaret
            // Thicker caret for Chinese input, thinner for English
            // This is a visual hint to the user
        } catch (e: Exception) {
            logger.debug("Could not update caret shape", e)
        }
    }

    private fun updateCaretShapeForCapsLock(editor: Editor, capsLockOn: Boolean) {
        // Make caret blink or change shape for caps lock
        try {
            if (capsLockOn) {
                // Could implement blinking caret here
                logger.debug("Caps lock indicator activated")
            }
        } catch (e: Exception) {
            logger.debug("Could not update caps lock indicator", e)
        }
    }

    private fun removeIndicator(editor: Editor) {
        highlighters[editor]?.let {
            editor.markupModel.removeHighlighter(it)
            highlighters.remove(editor)
        }
    }

    private fun parseColor(hexColor: String): Color {
        return try {
            Color.decode(hexColor)
        } catch (e: Exception) {
            logger.warn("Invalid color: $hexColor, using default", e)
            Color.WHITE
        }
    }

    fun removeAllIndicators() {
        highlighters.forEach { (editor, highlighter) ->
            try {
                editor.markupModel.removeHighlighter(highlighter)
            } catch (e: Exception) {
                logger.debug("Error removing highlighter", e)
            }
        }
        highlighters.clear()
    }

    companion object {
        fun getInstance(): CursorIndicatorService {
            return ApplicationManager.getApplication().getService(CursorIndicatorService::class.java)
        }
    }
}
