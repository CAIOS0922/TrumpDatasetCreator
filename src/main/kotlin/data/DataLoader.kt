package data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

class DataLoader {

    private val formatter = Json { ignoreUnknownKeys = true }
    fun getOriginalTrumps(path: String): List<TrumpData> {
        val dirFile = getResourceFile(path)
        val trumpsFile = dirFile.listFiles()
        val resultList = mutableListOf<TrumpData>()

        for(trump in trumpsFile ?: emptyArray()) {
            val name = trump.name.dropLast(4)
            val id = Trump.values().find { it.value.uppercase() == name.uppercase() } ?: continue

            resultList.add(TrumpData(id, trump))
        }

        return resultList
    }

    fun getImageUrls(path: String): List<ImageUrlData> {
        val dirFile = getResourceFile(path)
        val jsonFiles = dirFile.listFiles()
        val resultList = mutableListOf<ImageUrlData>()

        for(file in jsonFiles ?: emptyArray()) {
            val json = file.readText()
            val serializer = ListSerializer(ImageUrlData.serializer())
            val list = formatter.decodeFromString(serializer, json)

            resultList.addAll(list.map { ImageUrlData(it.image, it.iusc) })
        }

        return resultList.distinctBy { it.image }
    }

    fun getImageFiles(path: String): List<File> {
        val dirFile = getResourceFile(path)
        return dirFile.listFiles()?.toList() ?: emptyList()
    }

    suspend fun getImage(imageUrl: ImageUrlData): BufferedImage? {
        try {
            if (imageUrl.image.take(4).lowercase() != "http") {
                println("ERROR: URL format is incorrect. [${imageUrl.image.take(20)}...]")
                return null
            }

            val response = HttpClient(CIO).get(Url(imageUrl.image))
            if (!response.status.isSuccess()) {
                println("ERROR: Can't download image file. [${response.request.url}]")
                return null
            }

            return withContext(Dispatchers.IO) { ImageIO.read(ByteArrayInputStream(response.readBytes())) }
        } catch (e: Throwable) {
            println("ERROR: getImage throw ${e.stackTrace[0]}")
            delay(100)
            return null
        }
    }

    suspend fun getImage(file: File): BufferedImage? {
        return withContext(Dispatchers.IO) { ImageIO.read(file.inputStream()) }
    }

    private fun getResourceFile(path: String): File {
        val url = this.javaClass.getResource(path)
        return File(url!!.toURI())
    }

    private fun String.deleteParameter(): String {
        val qIndex = this.indexOf("?")
        return if(qIndex != -1) this.substring(0, qIndex) else this
    }
}

data class TrumpData(
    val id: Trump,
    val file: File,
)

@kotlinx.serialization.Serializable
data class ImageUrlData(
    val image: String = "",
    val iusc: String = "",
    val oztcrd: String = "",
    val fxgdke: String = ""
)