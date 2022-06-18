import compose.ImageComposer
import data.DataLoader
import data.Trump
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.opencv.core.Core
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    initOpenCV()

    val dataLoader = DataLoader()
    val imageComposer = ImageComposer(dataLoader)

    val originalTrumps = dataLoader.getOriginalTrumps("/trump/").shuffled()
    val imageUrls = dataLoader.getImageUrls("/urls/").shuffled()

    val disposableTrumps = originalTrumps.toMutableList()
    val chunkedUrls = imageUrls.chunked(imageUrls.size / originalTrumps.size)

    val saveDir = File("${System.getProperty("user.dir")}/TrumpDataset/")
    if(!saveDir.exists()) saveDir.mkdir()

    val createdData = mutableMapOf<Trump, Int>()

    runBlocking {
        for (urls in chunkedUrls) {
            for (data in urls) {
                if(disposableTrumps.isEmpty()) disposableTrumps.addAll(originalTrumps)

                val trumps = disposableTrumps.take(3)
                disposableTrumps.removeAll(trumps)

                for (trump in trumps) {
                    val image = imageComposer.compose(trump, data) ?: continue
                    val name = "${trump.id.value}-DM-${createdData[trump.id] ?: 0}"

                    withContext(Dispatchers.IO) { ImageIO.write(image, "JPG", File(saveDir, "$name.jpg")) }
                    createdData[trump.id] = createdData[trump.id]?.plus(1) ?: 1

                    println("${name}, ${data.image.take(40)}...")
                }
            }
        }
    }

    println("[FINISH] Created ${createdData.toList().sumOf { it.second }} data. [${saveDir.absolutePath}]")
}

fun initOpenCV() {
    try {
        val libDir = File("libs")
        val libPath = libDir.absolutePath
        val dllPath = "$libPath\\${Core.NATIVE_LIBRARY_NAME}.dll"

        if(File(dllPath).exists()) {
            System.load(dllPath)
            return
        }

        val jarPath = System.getProperty("java.class.path")
        val jarDir = File(jarPath).parentFile

        System.load("${jarDir.absolutePath}\\libs\\${Core.NATIVE_LIBRARY_NAME}.dll")
    } catch (e: UnsatisfiedLinkError) {
        println("ERROR: Can't load OpenCV.")
        println("ERROR: Place the JAR file in the initial location and run it.")
    }
}