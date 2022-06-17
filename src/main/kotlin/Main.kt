import compose.ImageComposer
import data.DataLoader
import kotlinx.coroutines.runBlocking
import org.opencv.core.Core
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

fun main(args: Array<String>) {
    initOpenCV()

    val dataLoader = DataLoader()
    val imageComposer = ImageComposer(dataLoader)

    val trumps = dataLoader.getOriginalTrumps("/trump/")
    val imageUrls = dataLoader.getImageUrls("/urls/")

    runBlocking {
        val mat = imageComposer.compose(trumps[45], imageUrls[0]) ?: return@runBlocking
        //Imgcodecs.imwrite("${System.getProperty("user.dir")}/test.jpg", mat)
    }
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