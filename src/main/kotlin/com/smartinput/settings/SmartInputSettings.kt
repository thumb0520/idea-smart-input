package com.smartinput.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "SmartInputSettings",
    storages = [Storage("SmartInputSettings.xml")]
)
class SmartInputSettings : PersistentStateComponent<SmartInputSettings.State> {
    data class State(
        var enabled: Boolean = true,
        var switchInComments: Boolean = true,
        var switchInVimCommandMode: Boolean = true,
        var switchInCommitMessage: Boolean = true,
        var switchInTerminal: Boolean = true,
        var englishInputMethodId: String = "com.apple.keylayout.ABC",
        var chineseInputMethodId: String = "com.apple.inputmethod.SCIM.ITABC",
        var englishCursorColor: String = "#4A90D9",
        var chineseCursorColor: String = "#E74C3C",
        var capsLockCursorColor: String = "#F39C12",
        var showCursorIndicator: Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    companion object {
        fun getInstance(): SmartInputSettings {
            return ApplicationManager.getApplication().getService(SmartInputSettings::class.java)
        }
    }
}
