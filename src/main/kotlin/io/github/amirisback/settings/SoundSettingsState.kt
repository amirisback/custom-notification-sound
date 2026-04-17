package io.github.amirisback.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent state for Custom Notification Sound settings.
 * Stores user preferences for sound files and playback configuration.
 */
@Service
@State(
    name = "CustomNotificationSoundSettings",
    storages = [Storage("customNotificationSound.xml")]
)
class SoundSettingsState : PersistentStateComponent<SoundSettingsState> {

    /** Whether sound notifications are enabled */
    var enableSound: Boolean = true

    /** Custom file path for general Gradle build success sound (empty = use default) */
    var successSoundPath: String = ""

    /** Custom file path for APK/Bundle build success sound (empty = use default) */
    var apkSuccessSoundPath: String = ""

    /** Custom file path for build error sound (empty = use default) */
    var errorSoundPath: String = ""

    /** Volume level (0-100) */
    var volume: Int = 80

    /** Enable sound for general Gradle build success */
    var enableGradleSuccessSound: Boolean = true

    /** Enable sound for APK/Bundle build success */
    var enableApkSuccessSound: Boolean = true

    /** Enable sound for build errors */
    var enableErrorSound: Boolean = true

    override fun getState(): SoundSettingsState = this

    override fun loadState(state: SoundSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(): SoundSettingsState {
            return ApplicationManager.getApplication().getService(SoundSettingsState::class.java)
        }
    }
}
