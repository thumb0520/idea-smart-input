package com.smartinput.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.smartinput.settings.SmartInputSettings
import java.io.IOException

class InputMethodManager {
    private val logger = Logger.getInstance(InputMethodManager::class.java)
    @Volatile
    private var currentInputMethod: InputMethod = InputMethod.ENGLISH

    enum class InputMethod {
        ENGLISH, CHINESE
    }

    fun switchToEnglish() {
        if (currentInputMethod == InputMethod.ENGLISH) return
        val settings = SmartInputSettings.getInstance()
        if (!settings.state.enabled) return

        currentInputMethod = InputMethod.ENGLISH
        ApplicationManager.getApplication().executeOnPooledThread {
            switchInputMethod(settings.state.englishInputMethodId)
            logger.info("Switched to English input method")
        }
    }

    fun switchToChinese() {
        if (currentInputMethod == InputMethod.CHINESE) return
        val settings = SmartInputSettings.getInstance()
        if (!settings.state.enabled) return

        currentInputMethod = InputMethod.CHINESE
        ApplicationManager.getApplication().executeOnPooledThread {
            switchInputMethod(settings.state.chineseInputMethodId)
            logger.info("Switched to Chinese input method")
        }
    }

    fun getCurrentInputMethod(): InputMethod = currentInputMethod

    private fun switchInputMethod(inputMethodId: String) {
        try {
            // Use macism command (requires: brew install macism)
            val process = ProcessBuilder("macism", inputMethodId)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val output = process.inputStream.bufferedReader().readText()
                logger.warn("macism command failed with exit code $exitCode: $output")

                // Fallback: try using osascript
                switchInputMethodViaAppleScript(inputMethodId)
            }
        } catch (e: IOException) {
            logger.warn("macism not found, trying AppleScript fallback", e)
            switchInputMethodViaAppleScript(inputMethodId)
        }
    }

    private fun switchInputMethodViaAppleScript(inputMethodId: String) {
        try {
            val script = """
                tell application "System Events"
                    tell process "TextInputMenuAgent"
                        set inputMethods to menu items of menu 1 of menu bar item 1 of menu bar 1
                        repeat with im in inputMethods
                            if name of im contains "$inputMethodId" then
                                click im
                                exit repeat
                            end if
                        end repeat
                    end tell
                end tell
            """.trimIndent()

            val process = ProcessBuilder("osascript", "-e", script)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val output = process.inputStream.bufferedReader().readText()
                logger.error("AppleScript fallback failed: $output")
            }
        } catch (e: Exception) {
            logger.error("Failed to switch input method via AppleScript", e)
        }
    }

    companion object {
        fun getInstance(): InputMethodManager {
            return ApplicationManager.getApplication().getService(InputMethodManager::class.java)
        }
    }
}
