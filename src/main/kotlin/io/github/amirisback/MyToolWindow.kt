package io.github.amirisback

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import io.github.amirisback.settings.SoundSettingsConfigurable
import io.github.amirisback.settings.SoundSettingsState
import io.github.amirisback.sound.SoundPlayer
import io.github.amirisback.sound.SoundType
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Tool window panel for Custom Notification Sound plugin.
 * Provides quick access to sound preview and settings.
 */
class MyToolWindow(private val project: Project) {

    val content = JBPanel<JBPanel<*>>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(16)

        // Title
        add(JBLabel("🔔 Custom Notification Sound").apply {
            font = font.deriveFont(Font.BOLD, 18f)
            alignmentX = Component.LEFT_ALIGNMENT
        })

        // Subtitle
        add(JBLabel("<html>Build notification sounds for your IDE</html>").apply {
            font = font.deriveFont(12f)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyTop(4)
        })

        add(Box.createRigidArea(Dimension(0, 16)))

        // Status
        val settings = SoundSettingsState.getInstance()
        val statusText = if (settings.enableSound) "✅ Sounds Enabled" else "❌ Sounds Disabled"
        add(JBLabel(statusText).apply {
            font = font.deriveFont(Font.BOLD, 14f)
            alignmentX = Component.LEFT_ALIGNMENT
        })

        add(Box.createRigidArea(Dimension(0, 16)))

        // Preview buttons
        add(JBLabel("Preview Sounds:").apply {
            font = font.deriveFont(Font.BOLD, 12f)
            alignmentX = Component.LEFT_ALIGNMENT
        })

        add(Box.createRigidArea(Dimension(0, 8)))

        add(JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            add(JButton("▶ Gradle Success").apply {
                addActionListener { SoundPlayer.play(SoundType.GRADLE_SUCCESS) }
            })
            add(JButton("▶ APK/Bundle Success").apply {
                addActionListener { SoundPlayer.play(SoundType.APK_SUCCESS) }
            })
            add(JButton("▶ Error").apply {
                addActionListener { SoundPlayer.play(SoundType.BUILD_ERROR) }
            })
        })

        add(Box.createRigidArea(Dimension(0, 16)))

        // Settings button
        add(JButton("⚙ Open Settings").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    project,
                    SoundSettingsConfigurable::class.java
                )
            }
        })

        add(Box.createRigidArea(Dimension(0, 16)))

        // Credits
        add(JBLabel("<html>Created by: <a href=\"https://github.com/amirisback\">amirisback</a></html>").apply {
            font = font.deriveFont(11f)
            alignmentX = Component.LEFT_ALIGNMENT
        })
    }
}
