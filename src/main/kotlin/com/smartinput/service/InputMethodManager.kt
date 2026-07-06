package com.smartinput.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.smartinput.settings.SmartInputSettings
import java.io.File
import java.io.IOException

class InputMethodManager {
    private val logger = Logger.getInstance(InputMethodManager::class.java)
    @Volatile
    private var currentInputMethod: InputMethod? = null

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

    fun getCurrentInputMethod(): InputMethod = currentInputMethod ?: InputMethod.ENGLISH

    private fun switchInputMethod(inputMethodId: String) {
        // Method 1: Try macism (if installed)
        if (tryMacism(inputMethodId)) return

        // Method 2: Try Carbon API via Python (no permissions needed)
        if (tryCarbonApi(inputMethodId)) return

        // Method 3: Fallback to AppleScript
        switchInputMethodViaAppleScript(inputMethodId)
    }

    private fun tryMacism(inputMethodId: String): Boolean {
        return try {
            val process = ProcessBuilder("macism", inputMethodId)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                true
            } else {
                val output = process.inputStream.bufferedReader().readText()
                logger.warn("macism command failed with exit code $exitCode: $output")
                false
            }
        } catch (e: IOException) {
            logger.debug("macism not available", e)
            false
        }
    }

    private fun tryCarbonApi(inputMethodId: String): Boolean {
        return try {
            val script = getCarbonSwitchScript(inputMethodId)
            val scriptFile = File.createTempFile("smartinput_switch_", ".py")
            scriptFile.writeText(script)
            scriptFile.deleteOnExit()

            val process = ProcessBuilder("python3", scriptFile.absolutePath)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText().trim()
            scriptFile.delete()

            if (exitCode == 0 && output.contains("OK")) {
                true
            } else {
                logger.warn("Carbon API switch failed (exit=$exitCode): $output")
                false
            }
        } catch (e: Exception) {
            logger.debug("Carbon API not available", e)
            false
        }
    }

    private fun getCarbonSwitchScript(inputMethodId: String): String {
        // Language injection is intentional — this is a Python script template
        return """
import ctypes
import ctypes.util
import sys

def switch_input(target_id):
    carbon = ctypes.cdll.LoadLibrary(ctypes.util.find_library('Carbon'))

    carbon.TISCopyCurrentKeyboardInputSource.restype = ctypes.c_void_p
    carbon.TISGetInputSourceProperty.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
    carbon.TISGetInputSourceProperty.restype = ctypes.c_void_p
    carbon.TISSelectInputSource.argtypes = [ctypes.c_void_p]
    carbon.TISSelectInputSource.restype = ctypes.c_int
    carbon.TISCreateInputSourceList.argtypes = [ctypes.c_void_p, ctypes.c_bool]
    carbon.TISCreateInputSourceList.restype = ctypes.c_void_p
    carbon.CFStringCreateWithCString.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_int]
    carbon.CFStringCreateWithCString.restype = ctypes.c_void_p
    carbon.CFArrayGetCount.argtypes = [ctypes.c_void_p]
    carbon.CFArrayGetCount.restype = ctypes.c_int
    carbon.CFArrayGetValueAtIndex.argtypes = [ctypes.c_void_p, ctypes.c_int]
    carbon.CFArrayGetValueAtIndex.restype = ctypes.c_void_p
    carbon.CFDictionaryCreate.argtypes = [ctypes.c_void_p,
        ctypes.POINTER(ctypes.c_void_p), ctypes.POINTER(ctypes.c_void_p),
        ctypes.c_int, ctypes.c_void_p, ctypes.c_void_p]
    carbon.CFDictionaryCreate.restype = ctypes.c_void_p

    kTISPropertyInputSourceID = ctypes.c_void_p.in_dll(carbon, 'kTISPropertyInputSourceID')

    # Check if already on target input method
    current = carbon.TISCopyCurrentKeyboardInputSource()
    current_id_ptr = carbon.TISGetInputSourceProperty(current, kTISPropertyInputSourceID)
    if current_id_ptr:
        buf = ctypes.create_string_buffer(256)
        carbon.CFStringGetCString.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_int, ctypes.c_int]
        carbon.CFStringGetCString.restype = ctypes.c_bool
        if carbon.CFStringGetCString(current_id_ptr, buf, 256, 134217984):
            if buf.value.decode('utf-8') == target_id:
                print("OK:already_on_target")
                return

    # Find and select target input source
    cf_target = carbon.CFStringCreateWithCString(None, target_id.encode('utf-8'), 134217984)
    keys = (ctypes.c_void_p * 1)(kTISPropertyInputSourceID)
    values = (ctypes.c_void_p * 1)(cf_target)
    cf_dict = carbon.CFDictionaryCreate(None, keys, values, 1, None, None)

    sources = carbon.TISCreateInputSourceList(cf_dict, False)
    count = carbon.CFArrayGetCount(sources)

    if count > 0:
        source = carbon.CFArrayGetValueAtIndex(sources, 0)
        result = carbon.TISSelectInputSource(source)
        if result == 0:
            print("OK:switched")
        else:
            print("FAIL:TISSelectInputSource returned " + str(result))
    else:
        print("FAIL:no_matching_source")

if __name__ == '__main__':
    switch_input("$inputMethodId")
""".trimIndent()
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
