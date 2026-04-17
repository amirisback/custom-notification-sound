import javax.sound.sampled.*
import java.io.File
import java.io.BufferedInputStream
import java.util.concurrent.CountDownLatch

fun main(args: Array<String>) {
    try {
        val file = File(args[0])
        println("File exists: ${file.exists()}, length: ${file.length()}")
        var stream = AudioSystem.getAudioInputStream(file)
        
        val format = stream.format
        println("Format: $format")
        
        val decodedFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            format.sampleRate,
            16,
            format.channels,
            format.channels * 2,
            format.sampleRate,
            false
        )

        val decodedStream = if (format.encoding != AudioFormat.Encoding.PCM_SIGNED) {
            println("Decoding format...")
            AudioSystem.getAudioInputStream(decodedFormat, stream)
        } else {
            stream
        }
        
        println("Decoded Format: ${decodedStream.format}")

        val info = DataLine.Info(Clip::class.java, decodedStream.format)
        println("Line supported: ${AudioSystem.isLineSupported(info)}")
        
        if (!AudioSystem.isLineSupported(info)) return
        
        val clip = AudioSystem.getLine(info) as Clip
        println("Opening clip...")
        clip.open(decodedStream)

        val latch = CountDownLatch(1)
        clip.addLineListener { event ->
            println("Event type: ${event.type}")
            if (event.type == LineEvent.Type.STOP || event.type == LineEvent.Type.CLOSE) {
                latch.countDown()
            }
        }
        
        println("Starting clip...")
        clip.start()
        
        println("Awaiting latch...")
        latch.await()
        println("Finished playback.")
        clip.close()
    } catch(e: Exception) {
        e.printStackTrace()
    }
}
