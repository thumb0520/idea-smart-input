package com.smartinput.detector

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.smartinput.settings.SmartInputSettings

/**
 * Detects if the cursor is positioned inside a comment in the editor.
 * Supports single-line comments, multi-line comments, and documentation comments.
 */
class CommentContextDetector : ContextDetector {
    private val logger = Logger.getInstance(CommentContextDetector::class.java)

    override fun isActive(editor: Editor?, file: VirtualFile?): Boolean {
        val settings = SmartInputSettings.getInstance()
        if (!settings.state.switchInComments) return false
        if (editor == null || file == null) return false

        return isInComment(editor)
    }

    override fun getRecommendedInputMethod(): InputMethodRecommendation {
        return InputMethodRecommendation.CHINESE
    }

    override fun getPriority(): Int = 10

    private fun isInComment(editor: Editor): Boolean {
        try {
            val project = editor.project ?: return false
            return ApplicationManager.getApplication().runReadAction<Boolean> {
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return@runReadAction false
                val offset = editor.caretModel.offset
                if (offset < 0 || offset > psiFile.textLength) return@runReadAction false

                val element = psiFile.findElementAt(offset) ?: return@runReadAction false
                isCommentElement(element)
            }
        } catch (e: Exception) {
            logger.debug("Error checking comment context", e)
            return false
        }
    }

    private fun isCommentElement(element: PsiElement): Boolean {
        // Direct comment check
        if (element is PsiComment) {
            return true
        }

        // Check parent elements for nested comments
        var current: PsiElement? = element.parent
        while (current != null && current !is PsiFile) {
            if (current is PsiComment) {
                return true
            }
            current = current.parent
        }

        // Check token type for languages that don't use PsiComment
        val elementType = element.node?.elementType?.toString() ?: ""
        val commentTypes = listOf(
            "COMMENT", "END_OF_LINE_COMMENT", "C_STYLE_COMMENT",
            "BLOCK_COMMENT", "DOC_COMMENT", "JAVA_DOC_COMMENT",
            "LINE_COMMENT", "MULTI_LINE_COMMENT"
        )

        return commentTypes.any { elementType.contains(it, ignoreCase = true) }
    }
}
