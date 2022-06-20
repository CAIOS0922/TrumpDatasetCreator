import compose.ImageComposer
import data.DataLoader
import data.Trump
import kotlinx.coroutines.*
import org.opencv.core.Core
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    initOpenCV()

    val dataLoader = DataLoader()
    val imageComposer = ImageComposer(dataLoader)

    print("Enter whether you want to load images from a directory [D] or from JSON [J] > ")

    when (readLine()?.uppercase()) {
        "D"  -> composeFromFile(dataLoader, imageComposer)
        "J"  -> composeFromJson(dataLoader, imageComposer)
        else -> println("Invalid input.")
    }
}

fun composeFromFile(dataLoader: DataLoader, imageComposer: ImageComposer) {
    val currentDir = File(System.getProperty("user.dir"))

    val originalTrumps = dataLoader.getOriginalTrumps("/trump/").sortedBy { it.id.ordinal }.toMutableList()
    val imageFiles = dataLoader.getImageFiles("/back_image/")

    val saveDir = getDir(currentDir, "/dataset/")
    val trainDir = getDir(saveDir, "/train/")
    val validDir = getDir(saveDir, "/valid/")

    val disposableTrumps = originalTrumps.toMutableList()
    val createdData = mutableMapOf<Trump, Int>()

    runBlocking {
        for (backImageFile in imageFiles) {
            if(disposableTrumps.isEmpty()) disposableTrumps.addAll(originalTrumps)

            val trumps = disposableTrumps.take(3)
            disposableTrumps.removeAll(trumps)

            trumps.map { async {
                val image = imageComposer.compose(it, backImageFile) ?: return@async
                val name = "${it.id.value}-DM-${createdData[it.id] ?: 0}"
                val saveFile = File(getDir(trainDir, it.id.value), "$name.jpg")

                withContext(Dispatchers.IO) { ImageIO.write(image, "JPG", saveFile) }
                createdData[it.id] = createdData[it.id]?.plus(1) ?: 1

                println("${name}, ${backImageFile.name}...")
            } }.awaitAll()
        }
    }

    println("[FINISH] Created ${createdData.toList().sumOf { it.second }} data. [${saveDir.absolutePath}]")
}

fun composeFromJson(dataLoader: DataLoader, imageComposer: ImageComposer) {
    val originalTrumps = dataLoader.getOriginalTrumps("/trump/").shuffled()
    val imageUrls = dataLoader.getImageUrls("/urls/").shuffled()

    val disposableTrumps = originalTrumps.toMutableList()
    val chunkedUrls = imageUrls.chunked(imageUrls.size / originalTrumps.size)

    val saveDir = File("${System.getProperty("user.dir")}/dataset/")
    if(!saveDir.exists()) saveDir.mkdir()

    val trainDir = getDir(saveDir, "/train/")
    val validDir = getDir(saveDir, "/valid/")

    val createdData = mutableMapOf<Trump, Int>()

    runBlocking {
        for (urls in chunkedUrls) {
            for (data in urls) {
                if(disposableTrumps.isEmpty()) disposableTrumps.addAll(originalTrumps)

                val trumps = disposableTrumps.take(5)
                disposableTrumps.removeAll(trumps)

                for (trump in trumps) {
                    val image = imageComposer.compose(trump, data) ?: continue
                    val name = "${trump.id.value}-DM-${createdData[trump.id] ?: 0}"
                    val saveFile = File(getDir(if((createdData[trump.id] ?: 0) < 3) validDir else trainDir, trump.id.value), "$name.jpg")

                    withContext(Dispatchers.IO) { ImageIO.write(image, "JPG", saveFile) }
                    createdData[trump.id] = createdData[trump.id]?.plus(1) ?: 1

                    println("${name}, ${data.image.take(40)}...")
                }
            }
        }
    }

    println("[FINISH] Created ${createdData.toList().sumOf { it.second }} data. [${saveDir.absolutePath}]")
}

fun getDir(parentDir: File, name: String): File {
    val file = File(parentDir, name)
    if(!file.exists()) file.mkdir()
    return file
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