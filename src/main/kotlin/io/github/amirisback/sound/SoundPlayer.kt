package io.github.amirisback.sound

import com.intellij.openapi.diagnostic.Logger
import io.github.amirisback.settings.SoundSettingsState
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import javax.sound.sampled.*

/**
 * Enumeration of sound types available in the plugin.
 */
enum class SoundType(val defaultResource: String, val displayName: String) {
    GRADLE_SUCCESS("/sounds/success.wav", "Gradle Build Success"),
    APK_SUCCESS("/sounds/apk_success.wav", "APK/Bundle Build Success"),
    BUILD_ERROR("/sounds/error.wav", "Build Error")
}

/** Supported audio file extensions */
val SUPPORTED_EXTENSIONS = arrayOf("wav", "mp3", "aiff")

/**
 * Utility object for playing notification sounds.
 * Supports bundled default sounds and custom user-provided WAV/MP3 files.
 * Playback is non-blocking (runs on a background thread).
 *
 * MP3 support is provided via JLayer/MP3SPI libraries which register
 * as Java Sound SPI providers, enabling transparent MP3 decoding.
 */
object SoundPlayer {

    private val LOG = Logger.getInstance(SoundPlayer::class.java)

    init {
        // Force-load the MP3 SPI provider so AudioSystem recognizes MP3 format.
        // This is needed because IntelliJ uses a custom classloader.
        try {
            Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader")
        } catch (e: ClassNotFoundException) {
            LOG.warn("MP3 SPI provider not found, MP3 playback may not work", e)
        }
    }

    /**
     * Play a notification sound based on the given [SoundType].
     * Checks settings to determine if sound is enabled and which file to use.
     */
    fun play(soundType: SoundType) {
        val settings = SoundSettingsState.getInstance()

        // Check global enable
        if (!settings.enableSound) return

        // Check per-type enable
        when (soundType) {
            SoundType.GRADLE_SUCCESS -> if (!settings.enableGradleSuccessSound) return
            SoundType.APK_SUCCESS -> if (!settings.enableApkSuccessSound) return
            SoundType.BUILD_ERROR -> if (!settings.enableErrorSound) return
        }

        // Get custom path if configured
        val customPath = when (soundType) {
            SoundType.GRADLE_SUCCESS -> settings.successSoundPath
            SoundType.APK_SUCCESS -> settings.apkSuccessSoundPath
            SoundType.BUILD_ERROR -> settings.errorSoundPath
        }

        // Play on background thread
        Thread({
            val oldClassLoader = Thread.currentThread().contextClassLoader
            try {
                Thread.currentThread().contextClassLoader = SoundPlayer::class.java.classLoader
                val audioStream = if (customPath.isNotBlank() && File(customPath).exists()) {
                    getAudioStreamFromFile(customPath)
                } else {
                    getAudioStreamFromResource(soundType.defaultResource)
                }

                audioStream?.let { playAudioStream(it, settings.volume) }
            } catch (e: Exception) {
                LOG.warn("Failed to play sound: ${soundType.displayName}", e)
            } finally {
                Thread.currentThread().contextClassLoader = oldClassLoader
            }
        }, "CustomNotificationSound-Player").start()
    }

    /**
     * Play a sound file directly from a file path (used for preview in settings).
     */
    fun playPreview(filePath: String?, defaultResource: String, volume: Int) {
        Thread({
            val oldClassLoader = Thread.currentThread().contextClassLoader
            try {
                Thread.currentThread().contextClassLoader = SoundPlayer::class.java.classLoader
                val audioStream = if (!filePath.isNullOrBlank() && File(filePath).exists()) {
                    getAudioStreamFromFile(filePath)
                } else {
                    getAudioStreamFromResource(defaultResource)
                }
                audioStream?.let { playAudioStream(it, volume) }
            } catch (e: Exception) {
                LOG.warn("Failed to play preview sound", e)
            } finally {
                Thread.currentThread().contextClassLoader = oldClassLoader
            }
        }, "CustomNotificationSound-Preview").start()
    }

    private fun getAudioStreamFromFile(path: String): AudioInputStream? {
        return try {
            val file = File(path)
            AudioSystem.getAudioInputStream(file)
        } catch (e: Exception) {
            LOG.warn("Cannot open audio file: $path", e)
            null
        }
    }

    private fun getAudioStreamFromResource(resource: String): AudioInputStream? {
        return try {
            val inputStream: InputStream = SoundPlayer::class.java.getResourceAsStream(resource)
                ?: throw IllegalStateException("Resource not found: $resource")
            val buffered = BufferedInputStream(inputStream)
            AudioSystem.getAudioInputStream(buffered)
        } catch (e: Exception) {
            LOG.warn("Cannot open audio resource: $resource", e)
            null
        }
    }

    private fun playAudioStream(audioStream: AudioInputStream, volumePercent: Int) {
        audioStream.use { stream ->
            val format = stream.format

            // Convert to PCM if necessary using explicit format (required by MP3 SPI)
            val decodedStream = if (format.encoding != AudioFormat.Encoding.PCM_SIGNED) {
                val decodedFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.sampleRate,
                    16,
                    format.channels,
                    format.channels * 2,
                    format.sampleRate,
                    false
                )
                try {
                    AudioSystem.getAudioInputStream(decodedFormat, stream)
                } catch (e: Exception) {
                    LOG.warn("Failed to decode audio stream format: $format", e)
                    return
                }
            } else {
                stream
            }

            val info = DataLine.Info(Clip::class.java, decodedStream.format)
            if (!AudioSystem.isLineSupported(info)) {
                LOG.warn("Audio line not supported for format: ${decodedStream.format}")
                return
            }

            val clip = AudioSystem.getLine(info) as Clip
            clip.open(decodedStream)

            // Set volume
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val volume = volumePercent / 100.0f
                val dB = if (volume > 0) (20.0 * Math.log10(volume.toDouble())).toFloat() else gainControl.minimum
                val clampedDb = dB.coerceIn(gainControl.minimum, gainControl.maximum)
                gainControl.value = clampedDb
            }

            // Wait for playback to complete
            val latch = java.util.concurrent.CountDownLatch(1)
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP || event.type == LineEvent.Type.CLOSE) {
                    latch.countDown()
                }
            }

            clip.start()

            // Block thread until clip finishes playing (with 30-second timeout for safety)
            try {
                latch.await(30, java.util.concurrent.TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                // Ignore interruption
            } finally {
                clip.close()
            }
        }
    }
}
