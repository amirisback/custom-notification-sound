package io.github.amirisback.listener

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import io.github.amirisback.sound.SoundPlayer
import io.github.amirisback.sound.SoundType

/**
 * Listens for Gradle (External System) build events.
 * Differentiates between APK/Bundle builds and general Gradle tasks
 * to play appropriate notification sounds.
 *
 * Registered declaratively in plugin.xml as a project listener.
 */
class GradleBuildListener : ExternalSystemTaskNotificationListener {

    private val log = Logger.getInstance(GradleBuildListener::class.java)

    /**
     * Gradle task names that indicate APK or Bundle builds.
     * Matches both debug/release variants and flavored builds.
     */
    private val apkBundleTaskPatterns = listOf(
        "assemble",       // assembleDebug, assembleRelease, assembleFreeDebug, etc.
        "bundle",         // bundleDebug, bundleRelease, bundleFreeDebug, etc.
        "installDebug",   // install tasks also mean APK was built
        "installRelease"
    )

    /** Track last seen task name per ID to identify specific Gradle tasks */
    private val taskNames = mutableMapOf<ExternalSystemTaskId, String>()

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
        // No action needed on start
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        if (stdOut) {
            // Look for task execution patterns like "> Task :app:assembleDebug"
            val taskPattern = Regex("""> Task\s+:[\w:]*:(assemble\w*|bundle\w*|install\w*)""", RegexOption.IGNORE_CASE)
            val match = taskPattern.find(text)
            if (match != null) {
                taskNames[id] = match.groupValues[1]
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onSuccess(id: ExternalSystemTaskId) {
        if (id.type == ExternalSystemTaskType.EXECUTE_TASK) {
            val taskName = taskNames.remove(id) ?: ""

            val isApkBundleTask = apkBundleTaskPatterns.any { pattern ->
                taskName.startsWith(pattern, ignoreCase = true)
            }

            if (isApkBundleTask) {
                log.info("APK/Bundle build succeeded: $taskName")
                SoundPlayer.play(SoundType.APK_SUCCESS)
            } else {
                log.info("Gradle build succeeded: $taskName")
                SoundPlayer.play(SoundType.GRADLE_SUCCESS)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
        if (id.type == ExternalSystemTaskType.EXECUTE_TASK) {
            val taskName = taskNames.remove(id) ?: ""
            log.info("Build failed: $taskName")
            SoundPlayer.play(SoundType.BUILD_ERROR)
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCancel(id: ExternalSystemTaskId) {
        taskNames.remove(id)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onEnd(id: ExternalSystemTaskId) {
        taskNames.remove(id)
    }

    override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {
        // Not used
    }
}
