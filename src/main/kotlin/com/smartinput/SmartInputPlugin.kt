package com.smartinput

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.util.messages.MessageBusConnection
import com.smartinput.detector.*
import com.smartinput.service.CursorIndicatorService
import com.smartinput.service.InputMethodManager
import com.smartinput.settings.SmartInputSettings
import java.awt.event.WindowFocusListener
import javax.swing.Timer

/**
 * Main plugin service that coordinates context detection and input method switching.
 */
@Service(Service.Level.PROJECT)
class SmartInputPlugin(private val project: Project) {
    private val logger = Logger.getInstance(SmartInputPlugin::class.java)
    private val inputMethodManager = InputMethodManager.getInstance()
    private val cursorIndicator = CursorIndicatorService.getInstance()
    private val settings = SmartInputSettings.getInstance()

    // Context detectors
    private val commentDetector = CommentContextDetector()
    private val commitDetector = CommitMessageDetector()
    private val terminalDetector = TerminalFocusDetector().apply { setProject(project) }
    private val vimDetector = VimModeDetector()

    // All detectors sorted by priority
    private val detectors: List<ContextDetector> = listOf(
        terminalDetector,
        vimDetector,
        commitDetector,
        commentDetector
    ).sortedByDescending { it.getPriority() }

    private var messageBusConnection: MessageBusConnection? = null
    private var windowFocusListener: WindowFocusListener? = null
    private var isEnabled = true
    private var switchTimer: Timer? = null
    private var documentListener: DocumentListener? = null
    private var documentListenerDocument: com.intellij.openapi.editor.Document? = null

    init {
        logger.info("SmartInputPlugin initialized for project: ${project.name}")
        setupListeners()
    }

    private fun setupListeners() {
        messageBusConnection = project.messageBus.connect()

        // Listen for editor changes
        messageBusConnection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                source.getSelectedTextEditor()?.let { setupEditorListeners(it) }
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                event.manager.selectedTextEditor?.let { editor ->
                    setupEditorListeners(editor)
                    analyzeAndSwitch(editor, event.newFile)
                }
            }
        })

        // Listen for tool window changes (Terminal focus)
        messageBusConnection?.subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                terminalDetector.stateChanged(toolWindowManager)
                // When terminal gains focus, trigger switch directly (no editor available)
                if (terminalDetector.isActive(null, null)) {
                    doSwitch(InputMethodRecommendation.ENGLISH, null)
                }
            }
        })

        // Listen for project window focus (triggers when switching between projects)
        WindowManager.getInstance().getFrame(project)?.let { frame ->
            windowFocusListener = object : WindowFocusListener {
                override fun windowGainedFocus(e: java.awt.event.WindowEvent) {
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                    val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
                    editor?.let { setupEditorListeners(it) }
                    analyzeAndSwitch(editor, file)
                }
                override fun windowLostFocus(e: java.awt.event.WindowEvent) {}
            }
            frame.addWindowFocusListener(windowFocusListener!!)
        }

        logger.info("Event listeners registered")
    }

    private fun setupEditorListeners(editor: Editor) {
        // Remove existing listeners to avoid duplicates
        editor.caretModel.removeCaretListener(caretListener)
        // Add caret listener
        editor.caretModel.addCaretListener(caretListener)
        addDocumentListenerToEditor(editor)
    }

    private fun addDocumentListenerToEditor(editor: Editor) {
        // Remove existing listener from the document it was registered on
        documentListener?.let { listener ->
            documentListenerDocument?.removeDocumentListener(listener)
        }
        documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val fileEditorManager = FileEditorManager.getInstance(project)
                val currentEditor = fileEditorManager.selectedTextEditor
                if (currentEditor == editor) {
                    val file = fileEditorManager.selectedFiles.firstOrNull()
                    analyzeAndSwitch(editor, file)
                }
            }
        }
        documentListenerDocument = editor.document
        editor.document.addDocumentListener(documentListener!!)
    }

    private val caretListener = object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            val editor = event.editor
            val fileEditorManager = FileEditorManager.getInstance(project)
            val file = fileEditorManager.selectedFiles.firstOrNull()
            analyzeAndSwitch(editor, file)
        }
    }

    /**
     * Analyze the current context and switch input method if needed.
     * Uses debouncing to avoid excessive switching on rapid caret movements.
     */
    fun analyzeAndSwitch(editor: Editor?, file: VirtualFile?) {
        if (!isEnabled || !settings.state.enabled) return
        if (editor == null) return

        switchTimer?.stop()
        switchTimer = Timer(100) {
            doAnalyzeAndSwitch(editor, file)
        }.apply { isRepeats = false; start() }
    }

    private fun doAnalyzeAndSwitch(editor: Editor?, file: VirtualFile?) {
        if (!isEnabled || !settings.state.enabled) return
        if (editor == null) return

        try {
            // Find the active detector
            val activeDetector = detectors.firstOrNull { it.isActive(editor, file) }

            val recommendation = activeDetector?.getRecommendedInputMethod()
                ?: InputMethodRecommendation.ENGLISH // No detector matched — regular code
            doSwitch(recommendation, editor)
        } catch (e: Exception) {
            logger.error("Error analyzing context", e)
        }
    }

    private fun doSwitch(recommendation: InputMethodRecommendation, editor: Editor?) {
        if (!isEnabled || !settings.state.enabled) return

        when (recommendation) {
            InputMethodRecommendation.ENGLISH -> {
                inputMethodManager.switchToEnglish()
                editor?.let { cursorIndicator.updateCursorIndicator(it, InputMethodManager.InputMethod.ENGLISH) }
            }
            InputMethodRecommendation.CHINESE -> {
                inputMethodManager.switchToChinese()
                editor?.let { cursorIndicator.updateCursorIndicator(it, InputMethodManager.InputMethod.CHINESE) }
            }
            InputMethodRecommendation.KEEP_CURRENT -> {
                // Do nothing, keep current input method
            }
        }
    }

    /**
     * Toggle plugin enabled state.
     */
    fun toggleEnabled() {
        isEnabled = !isEnabled
        logger.info("SmartInputPlugin ${if (isEnabled) "enabled" else "disabled"}")
    }

    /**
     * Check if plugin is enabled.
     */
    fun isEnabled(): Boolean = isEnabled && settings.state.enabled

    /**
     * Get the terminal focus detector for external access.
     */
    fun getTerminalDetector(): TerminalFocusDetector = terminalDetector

    /**
     * Get the vim mode detector for external access.
     */
    fun getVimDetector(): VimModeDetector = vimDetector

    fun dispose() {
        switchTimer?.stop()
        windowFocusListener?.let { listener ->
            WindowManager.getInstance().getFrame(project)?.removeWindowFocusListener(listener)
        }
        messageBusConnection?.disconnect()
        cursorIndicator.removeAllIndicators()
        logger.info("SmartInputPlugin disposed")
    }
}
