import javax.sound.sampled.*
import java.io.File
import java.io.BufferedInputStream
import java.util.concurrent.CountDownLatch
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader

fun main() {
    println("Test starting...")
    val file = File("C:/Users/tech/Downloads/music/gta-san-andreas-_RZMwPB0.mp3") 
    // We don't have this file so let's test a WAV that exists.
    val f = File("src/main/resources/sounds/apk_success.wav")
    println("File: ${f.absolutePath}, exists: ${f.exists()}")
    try {
        val stream = AudioSystem.getAudioInputStream(f)
        println("Encoding: ${stream.format.encoding}")
        
        val decodedStream = if (stream.format.encoding != AudioFormat.Encoding.PCM_SIGNED) {
            AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, stream)
        } else {
            stream
        }
        
        val info = DataLine.Info(Clip::class.java, decodedStream.format)
        println("Line supported: ${AudioSystem.isLineSupported(info)}")
        val clip = AudioSystem.getLine(info) as Clip
        clip.open(decodedStream)
        
        val latch = CountDownLatch(1)
        clip.addLineListener { e ->
            println("Event: ${e.type}")
            if(e.type == LineEvent.Type.STOP) latch.countDown()
        }
        clip.start()
        latch.await()
        clip.close()
        println("Done.")
    } catch(e: Exception) {
        e.printStackTrace()
    }
}
main()
