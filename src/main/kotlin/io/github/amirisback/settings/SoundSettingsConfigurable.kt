package io.github.amirisback.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import io.github.amirisback.sound.SUPPORTED_EXTENSIONS
import io.github.amirisback.sound.SoundPlayer
import io.github.amirisback.sound.SoundType
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

/**
 * Settings panel for Custom Notification Sound plugin.
 * Accessible via Settings > Tools > Custom Notification Sound.
 * Provides controls for enabling/disabling sounds, choosing custom sound files,
 * adjusting volume, and previewing sounds.
 */
class SoundSettingsConfigurable : Configurable {

    // UI Components
    private var mainPanel: JPanel? = null
    private var enableSoundCheckbox: JBCheckBox? = null

    private var enableGradleSuccessCheckbox: JBCheckBox? = null
    private var successSoundField: TextFieldWithBrowseButton? = null
    private var successPreviewButton: JButton? = null

    private var enableApkSuccessCheckbox: JBCheckBox? = null
    private var apkSuccessSoundField: TextFieldWithBrowseButton? = null
    private var apkSuccessPreviewButton: JButton? = null

    private var enableErrorCheckbox: JBCheckBox? = null
    private var errorSoundField: TextFieldWithBrowseButton? = null
    private var errorPreviewButton: JButton? = null

    private var volumeSlider: JSlider? = null
    private var volumeLabel: JBLabel? = null

    private var resetButton: JButton? = null

    override fun getDisplayName(): String = "Custom Notification Sound"

    override fun createComponent(): JComponent {
        // Initialize components
        enableSoundCheckbox = JBCheckBox("Enable notification sounds")

        // --- Gradle Success Section ---
        enableGradleSuccessCheckbox = JBCheckBox("Enable Gradle build success sound")
        successSoundField = TextFieldWithBrowseButton().apply {
            @Suppress("DEPRECATION")
            addBrowseFolderListener(
                "Select Success Sound",
                "Choose a WAV or MP3 file for Gradle build success notification",
                null,
                createAudioFileChooserDescriptor()
            )
        }
        successPreviewButton = JButton("▶ Preview").apply {
            addActionListener {
                SoundPlayer.playPreview(
                    successSoundField?.text,
                    SoundType.GRADLE_SUCCESS.defaultResource,
                    volumeSlider?.value ?: 80
                )
            }
        }

        // --- APK/Bundle Success Section ---
        enableApkSuccessCheckbox = JBCheckBox("Enable APK/Bundle build success sound")
        apkSuccessSoundField = TextFieldWithBrowseButton().apply {
            @Suppress("DEPRECATION")
            addBrowseFolderListener(
                "Select APK Success Sound",
                "Choose a WAV or MP3 file for APK/Bundle build success notification",
                null,
                createAudioFileChooserDescriptor()
            )
        }
        apkSuccessPreviewButton = JButton("▶ Preview").apply {
            addActionListener {
                SoundPlayer.playPreview(
                    apkSuccessSoundField?.text,
                    SoundType.APK_SUCCESS.defaultResource,
                    volumeSlider?.value ?: 80
                )
            }
        }

        // --- Error Section ---
        enableErrorCheckbox = JBCheckBox("Enable build error sound")
        errorSoundField = TextFieldWithBrowseButton().apply {
            @Suppress("DEPRECATION")
            addBrowseFolderListener(
                "Select Error Sound",
                "Choose a WAV or MP3 file for build error notification",
                null,
                createAudioFileChooserDescriptor()
            )
        }
        errorPreviewButton = JButton("▶ Preview").apply {
            addActionListener {
                SoundPlayer.playPreview(
                    errorSoundField?.text,
                    SoundType.BUILD_ERROR.defaultResource,
                    volumeSlider?.value ?: 80
                )
            }
        }

        // --- Volume ---
        volumeSlider = JSlider(0, 100, 80).apply {
            majorTickSpacing = 25
            minorTickSpacing = 5
            paintTicks = true
            paintLabels = true
            addChangeListener {
                volumeLabel?.text = "Volume: ${value}%"
            }
        }
        volumeLabel = JBLabel("Volume: 80%")

        // --- Reset Button ---
        resetButton = JButton("↻ Reset to Default").apply {
            addActionListener { resetToDefaults() }
        }

        // Build the form
        val successRow = createSoundRow(successSoundField!!, successPreviewButton!!)
        val apkSuccessRow = createSoundRow(apkSuccessSoundField!!, apkSuccessPreviewButton!!)
        val errorRow = createSoundRow(errorSoundField!!, errorPreviewButton!!)

        val volumeRow = JPanel(BorderLayout(8, 0)).apply {
            add(volumeSlider, BorderLayout.CENTER)
            add(volumeLabel, BorderLayout.EAST)
        }

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(enableSoundCheckbox!!)
            .addSeparator()
            .addComponent(JBLabel("Gradle Build Success").apply {
                font = font.deriveFont(java.awt.Font.BOLD)
                border = JBUI.Borders.emptyTop(8)
            })
            .addComponent(enableGradleSuccessCheckbox!!)
            .addLabeledComponent(JBLabel("Custom sound file:"), successRow)
            .addSeparator()
            .addComponent(JBLabel("APK / Bundle Build Success").apply {
                font = font.deriveFont(java.awt.Font.BOLD)
                border = JBUI.Borders.emptyTop(8)
            })
            .addComponent(enableApkSuccessCheckbox!!)
            .addLabeledComponent(JBLabel("Custom sound file:"), apkSuccessRow)
            .addSeparator()
            .addComponent(JBLabel("Build Error").apply {
                font = font.deriveFont(java.awt.Font.BOLD)
                border = JBUI.Borders.emptyTop(8)
            })
            .addComponent(enableErrorCheckbox!!)
            .addLabeledComponent(JBLabel("Custom sound file:"), errorRow)
            .addSeparator()
            .addComponent(JBLabel("Volume").apply {
                font = font.deriveFont(java.awt.Font.BOLD)
                border = JBUI.Borders.emptyTop(8)
            })
            .addComponent(volumeRow)
            .addSeparator()
            .addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(resetButton)
            })
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return mainPanel!!
    }

    private fun createSoundRow(fileField: TextFieldWithBrowseButton, previewButton: JButton): JPanel {
        return JPanel(BorderLayout(8, 0)).apply {
            add(fileField, BorderLayout.CENTER)
            add(previewButton, BorderLayout.EAST)
        }
    }

    /**
     * Creates a file chooser descriptor that accepts WAV, MP3, and AIFF audio files.
     */
    private fun createAudioFileChooserDescriptor(): FileChooserDescriptor {
        return FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withFileFilter { file ->
                val ext = file.extension?.lowercase() ?: ""
                ext in SUPPORTED_EXTENSIONS
            }
            .withTitle("Select Audio File")
            .withDescription("Supported formats: WAV, MP3, AIFF")
    }

    override fun isModified(): Boolean {
        val settings = SoundSettingsState.getInstance()
        return enableSoundCheckbox?.isSelected != settings.enableSound
                || enableGradleSuccessCheckbox?.isSelected != settings.enableGradleSuccessSound
                || enableApkSuccessCheckbox?.isSelected != settings.enableApkSuccessSound
                || enableErrorCheckbox?.isSelected != settings.enableErrorSound
                || successSoundField?.text != settings.successSoundPath
                || apkSuccessSoundField?.text != settings.apkSuccessSoundPath
                || errorSoundField?.text != settings.errorSoundPath
                || volumeSlider?.value != settings.volume
    }

    override fun apply() {
        val settings = SoundSettingsState.getInstance()
        settings.enableSound = enableSoundCheckbox?.isSelected ?: true
        settings.enableGradleSuccessSound = enableGradleSuccessCheckbox?.isSelected ?: true
        settings.enableApkSuccessSound = enableApkSuccessCheckbox?.isSelected ?: true
        settings.enableErrorSound = enableErrorCheckbox?.isSelected ?: true
        settings.successSoundPath = successSoundField?.text ?: ""
        settings.apkSuccessSoundPath = apkSuccessSoundField?.text ?: ""
        settings.errorSoundPath = errorSoundField?.text ?: ""
        settings.volume = volumeSlider?.value ?: 80
    }

    override fun reset() {
        val settings = SoundSettingsState.getInstance()
        enableSoundCheckbox?.isSelected = settings.enableSound
        enableGradleSuccessCheckbox?.isSelected = settings.enableGradleSuccessSound
        enableApkSuccessCheckbox?.isSelected = settings.enableApkSuccessSound
        enableErrorCheckbox?.isSelected = settings.enableErrorSound
        successSoundField?.text = settings.successSoundPath
        apkSuccessSoundField?.text = settings.apkSuccessSoundPath
        errorSoundField?.text = settings.errorSoundPath
        volumeSlider?.value = settings.volume
        volumeLabel?.text = "Volume: ${settings.volume}%"
    }

    private fun resetToDefaults() {
        enableSoundCheckbox?.isSelected = true
        enableGradleSuccessCheckbox?.isSelected = true
        enableApkSuccessCheckbox?.isSelected = true
        enableErrorCheckbox?.isSelected = true
        successSoundField?.text = ""
        apkSuccessSoundField?.text = ""
        errorSoundField?.text = ""
        volumeSlider?.value = 80
        volumeLabel?.text = "Volume: 80%"
    }

    override fun disposeUIResources() {
        mainPanel = null
        enableSoundCheckbox = null
        enableGradleSuccessCheckbox = null
        successSoundField = null
        successPreviewButton = null
        enableApkSuccessCheckbox = null
        apkSuccessSoundField = null
        apkSuccessPreviewButton = null
        enableErrorCheckbox = null
        errorSoundField = null
        errorPreviewButton = null
        volumeSlider = null
        volumeLabel = null
        resetButton = null
    }
}
