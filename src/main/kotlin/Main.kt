import compose.ImageComposer
import data.DataLoader
import data.Trump
import kotlinx.coroutines.*
import org.opencv.core.Core
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.floor

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

    val validRate = 0.1

    val disposableTrumps = originalTrumps.toMutableList()
    val createdData = mutableMapOf<Trump, MutableList<File>>()
    val validData = mutableMapOf<Trump, Int>()

    runBlocking {
        for (backImageFile in imageFiles) {
            if(disposableTrumps.isEmpty()) disposableTrumps.addAll(originalTrumps)

            val trumps = disposableTrumps.take(10)
            disposableTrumps.removeAll(trumps)

            trumps.map { async {
                val image = imageComposer.compose(it, backImageFile) ?: return@async
                val name = "${it.id.value}-DM-${createdData[it.id]?.size ?: 0}"
                val saveFile = File(getDir(trainDir, it.id.value), "$name.jpg")

                withContext(Dispatchers.IO) { ImageIO.write(image, "JPG", saveFile) }

                if(createdData[it.id] != null) createdData[it.id]!!.add(saveFile)
                else createdData[it.id] = mutableListOf(saveFile)

                println("${name}, ${backImageFile.name}...")
            } }.awaitAll()
        }

        for ((trump, files) in createdData) {
            val validSize = ceil(files.size * validRate).toInt()
            val moveFiles = files.shuffled().take(validSize)

            validData[trump] = moveFiles.size

            for ((i, file) in moveFiles.withIndex()) {
                val saveFile = File(getDir(validDir, trump.value), file.name)

                file.copyTo(saveFile)
                file.delete()

                println("Valid data moving... [${trump.value}:$i] ${saveFile.absolutePath}")
            }
        }
    }

    val allSize = createdData.values.flatten().size
    val validSize = validData.values.sum()
    val trainSize = allSize - validSize

    println("[FINISH] Created $allSize data. (train: $trainSize, valid: $validSize) [DIR: ${saveDir.absolutePath}]")
}

fun composeFromJson(dataLoader: DataLoader, imageComposer: ImageComposer) {
    val originalTrumps = dataLoader.getOriginalTrumps("/trump/").shuffled()
    val imageUrls = dataLoader.getImageUrls("/urls/").shuffled()

    println("Trumps: ${originalTrumps.size}, Background: ${imageUrls.size}")

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

                val trumps = disposableTrumps.take(10)
                disposableTrumps.removeAll(trumps)

                for (trump in trumps) {
                    val image = imageComposer.compose(trump, data) ?: continue
                    val name = "${trump.id.value}-DM-${createdData[trump.id] ?: 0}"
                    val saveFile = File(getDir(if((createdData[trump.id] ?: 0) < 10) validDir else trainDir, trump.id.value), "$name.jpg")

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