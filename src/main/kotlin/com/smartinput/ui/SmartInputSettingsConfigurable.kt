package com.smartinput.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.smartinput.settings.SmartInputSettings
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

/**
 * Settings configuration page for Smart Input plugin.
 */
class SmartInputSettingsConfigurable : Configurable {
    private var settingsPanel: JPanel? = null
    private val settings = SmartInputSettings.getInstance()

    // UI components
    private val enabledCheckbox = JBCheckBox("Enable Smart Input")
    private val switchInCommentsCheckbox = JBCheckBox("Switch to Chinese in comments")
    private val switchInVimCheckbox = JBCheckBox("Switch to English in IdeaVim command mode")
    private val switchInCommitCheckbox = JBCheckBox("Switch to Chinese in commit messages")
    private val switchInTerminalCheckbox = JBCheckBox("Switch to English in terminal")
    private val showCursorIndicatorCheckbox = JBCheckBox("Show cursor color indicator")

    private val englishInputField = JBTextField(30)
    private val chineseInputField = JBTextField(30)
    private val englishColorField = JBTextField(10)
    private val chineseColorField = JBTextField(10)
    private val capsLockColorField = JBTextField(10)

    override fun getDisplayName(): String = "Smart Input"

    override fun createComponent(): JComponent {
        if (settingsPanel == null) {
            settingsPanel = createSettingsPanel()
        }
        return settingsPanel!!
    }

    private fun createSettingsPanel(): JPanel {
        // General settings
        val generalPanel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckbox)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        // Context detection settings
        val contextPanel = FormBuilder.createFormBuilder()
            .addComponent(switchInCommentsCheckbox)
            .addComponent(switchInVimCheckbox)
            .addComponent(switchInCommitCheckbox)
            .addComponent(switchInTerminalCheckbox)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        // Input method settings
        val inputMethodPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("English Input Method ID:", englishInputField)
            .addLabeledComponent("Chinese Input Method ID:", chineseInputField)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        // Cursor indicator settings
        val cursorPanel = FormBuilder.createFormBuilder()
            .addComponent(showCursorIndicatorCheckbox)
            .addLabeledComponent("English Cursor Color:", createColorPanel(englishColorField))
            .addLabeledComponent("Chinese Cursor Color:", createColorPanel(chineseColorField))
            .addLabeledComponent("Caps Lock Color:", createColorPanel(capsLockColorField))
            .addComponentFillVertically(JPanel(), 0)
            .panel

        // Main panel with tabs
        val tabbedPane = JTabbedPane()
        tabbedPane.addTab("General", generalPanel)
        tabbedPane.addTab("Context Detection", contextPanel)
        tabbedPane.addTab("Input Methods", inputMethodPanel)
        tabbedPane.addTab("Cursor Indicator", cursorPanel)

        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
        mainPanel.border = JBUI.Borders.empty(10)

        return mainPanel
    }

    private fun createColorPanel(colorField: JTextField): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.add(colorField)

        val previewButton = JButton("Preview")
        previewButton.addActionListener {
            try {
                val color = Color.decode(colorField.text)
                colorField.background = color
            } catch (e: Exception) {
                Messages.showErrorDialog("Invalid color format. Use hex format like #FF0000", "Invalid Color")
            }
        }
        panel.add(previewButton)

        return panel
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return enabledCheckbox.isSelected != state.enabled ||
               switchInCommentsCheckbox.isSelected != state.switchInComments ||
               switchInVimCheckbox.isSelected != state.switchInVimCommandMode ||
               switchInCommitCheckbox.isSelected != state.switchInCommitMessage ||
               switchInTerminalCheckbox.isSelected != state.switchInTerminal ||
               showCursorIndicatorCheckbox.isSelected != state.showCursorIndicator ||
               englishInputField.text != state.englishInputMethodId ||
               chineseInputField.text != state.chineseInputMethodId ||
               englishColorField.text != state.englishCursorColor ||
               chineseColorField.text != state.chineseCursorColor ||
               capsLockColorField.text != state.capsLockCursorColor
    }

    override fun apply() {
        val state = settings.state
        state.enabled = enabledCheckbox.isSelected
        state.switchInComments = switchInCommentsCheckbox.isSelected
        state.switchInVimCommandMode = switchInVimCheckbox.isSelected
        state.switchInCommitMessage = switchInCommitCheckbox.isSelected
        state.switchInTerminal = switchInTerminalCheckbox.isSelected
        state.showCursorIndicator = showCursorIndicatorCheckbox.isSelected
        state.englishInputMethodId = englishInputField.text
        state.chineseInputMethodId = chineseInputField.text
        state.englishCursorColor = englishColorField.text
        state.chineseCursorColor = chineseColorField.text
        state.capsLockCursorColor = capsLockColorField.text
    }

    override fun reset() {
        val state = settings.state
        enabledCheckbox.isSelected = state.enabled
        switchInCommentsCheckbox.isSelected = state.switchInComments
        switchInVimCheckbox.isSelected = state.switchInVimCommandMode
        switchInCommitCheckbox.isSelected = state.switchInCommitMessage
        switchInTerminalCheckbox.isSelected = state.switchInTerminal
        showCursorIndicatorCheckbox.isSelected = state.showCursorIndicator
        englishInputField.text = state.englishInputMethodId
        chineseInputField.text = state.chineseInputMethodId
        englishColorField.text = state.englishCursorColor
        chineseColorField.text = state.chineseCursorColor
        capsLockColorField.text = state.capsLockCursorColor
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
