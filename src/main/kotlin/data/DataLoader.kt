package data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

class DataLoader {
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

            resultList.addAll(Json.decodeFromString(serializer, json))
        }

        return resultList
    }

    suspend fun getImage(imageUrl: ImageUrlData): Mat? {
        val response = HttpClient(CIO).get(Url(imageUrl.image.deleteParameter()))
        if(!response.status.isSuccess()) {
            println("ERROR: Can't download image file. [${response.request.url}]")
            return null
        }

        return Imgcodecs.imdecode(MatOfByte(*response.readBytes()), Imgcodecs.IMREAD_UNCHANGED)
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
)