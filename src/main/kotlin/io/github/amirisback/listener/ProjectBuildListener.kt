package io.github.amirisback.listener

import com.intellij.openapi.diagnostic.Logger
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskListener
import com.intellij.task.ProjectTaskManager
import io.github.amirisback.sound.SoundPlayer
import io.github.amirisback.sound.SoundType

/**
 * Listens for IDE internal build events (Make Project, Rebuild, etc.).
 * This captures builds that are NOT delegated to Gradle.
 * When build is delegated to Gradle, [GradleBuildListener] handles it instead.
 */
class ProjectBuildListener : ProjectTaskListener {

    private val log = Logger.getInstance(ProjectBuildListener::class.java)

    override fun finished(result: ProjectTaskManager.Result) {
        if (result.isAborted) {
            log.info("IDE build was aborted")
            return
        }

        if (result.hasErrors()) {
            log.info("IDE build finished with errors")
            SoundPlayer.play(SoundType.BUILD_ERROR)
        } else {
            log.info("IDE build finished successfully")
            SoundPlayer.play(SoundType.GRADLE_SUCCESS)
        }
    }
}
