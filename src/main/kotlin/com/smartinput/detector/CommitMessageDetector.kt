package com.smartinput.detector

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiComment
import com.smartinput.settings.SmartInputSettings

/**
 * Detects if the current editor is a Commit Message editor.
 * Checks for:
 * - VCS commit message dialogs
 * - Git commit message files
 * - Commit tool window
 */
class CommitMessageDetector : ContextDetector {
    private val logger = Logger.getInstance(CommitMessageDetector::class.java)

    // Patterns that indicate commit message files
    private val commitFilePatterns = listOf(
        "COMMIT_EDITMSG",
        "MERGE_MSG",
        "SQUASH_MSG",
        ".git/COMMIT_EDITMSG"
    )

    // Commit message content patterns
    private val commitContentPatterns = listOf(
        "^# Please enter the commit message",
        "^# Changes to be committed:",
        "^# On branch ",
        "^Merge branch ",
        "^Merge pull request "
    )

    override fun isActive(editor: Editor?, file: VirtualFile?): Boolean {
        val settings = SmartInputSettings.getInstance()
        if (!settings.state.switchInCommitMessage) return false
        if (editor == null || file == null) return false

        return isCommitMessageContext(editor, file)
    }

    override fun getRecommendedInputMethod(): InputMethodRecommendation {
        return InputMethodRecommendation.CHINESE
    }

    override fun getPriority(): Int = 15

    private fun isCommitMessageContext(editor: Editor, file: VirtualFile): Boolean {
        // Check file name
        val fileName = file.name
        if (commitFilePatterns.any { fileName.equals(it, ignoreCase = true) || fileName.endsWith(it) }) {
            logger.debug("Detected commit message by file name: $fileName")
            return true
        }

        // Check file path
        val filePath = file.path
        if (filePath.contains("/.git/") && (fileName == "COMMIT_EDITMSG" || fileName == "MERGE_MSG")) {
            logger.debug("Detected commit message by file path: $filePath")
            return true
        }

        // Check content patterns
        val document = editor.document
        val text = document.text
        if (commitContentPatterns.any { Regex(it).containsMatchIn(text) }) {
            logger.debug("Detected commit message by content pattern")
            return true
        }

        // Check if we're in a commit message tool window
        val project = editor.project
        if (project != null) {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val commitWindow = toolWindowManager.getToolWindow("Commit")
            if (commitWindow != null && commitWindow.isVisible) {
                // Check if this editor belongs to the commit tool window
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                if (psiFile != null && isCommitMessagePsiFile(psiFile)) {
                    logger.debug("Detected commit message in Commit tool window")
                    return true
                }
            }
        }

        return false
    }

    private fun isCommitMessagePsiFile(psiFile: PsiFile): Boolean {
        // Check if the PSI file looks like a commit message
        val text = psiFile.text
        return text.startsWith("#") ||
               text.contains("Signed-off-by:") ||
               text.contains("Change-Id:") ||
               commitContentPatterns.any { Regex(it).containsMatchIn(text) }
    }
}
