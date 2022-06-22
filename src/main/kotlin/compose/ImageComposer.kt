package compose

import data.DataLoader
import data.ImageUrlData
import data.TrumpData
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.Size
import sun.awt.image.BufferedImageDevice
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random


class ImageComposer(private val dataLoader: DataLoader) {

    private val random = Random(System.currentTimeMillis())

    suspend fun compose(trump: TrumpData, imageUrlData: ImageUrlData, setting: ComposeSetting = ComposeSetting()): BufferedImage? {
        val trumpImage = withContext(Dispatchers.IO) { ImageIO.read(trump.file.inputStream()) }
        val backImage = dataLoader.getImage(imageUrlData) ?: return null

        return compose(trumpImage, backImage, setting)
    }

    suspend fun compose(trump: TrumpData, backImageFile: File, setting: ComposeSetting = ComposeSetting()): BufferedImage {
        val trumpImage = withContext(Dispatchers.IO) { ImageIO.read(trump.file.inputStream()) }
        val backImage = dataLoader.getImage(backImageFile)

        return compose(trumpImage, backImage, setting)
    }

    private fun compose(trumpImage: BufferedImage, backImage: BufferedImage, setting: ComposeSetting = ComposeSetting()): BufferedImage {
        val trumpSize = getScaleSize(backImage, trumpImage, setting.trumpSize)
        var resultImage = trumpImage.resize(trumpSize.width.toInt(), trumpSize.height.toInt())

        val angle = setting.rotate
        resultImage = resultImage.rotateImage(Math.toRadians(angle))

        val locateX = backImage.width * setting.locateX
        val locateY = backImage.height * setting.locateY

        return backImage.overwrite(resultImage, locateX.toInt(), locateY.toInt())
    }

    private fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
        return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also {
            val graphics = it.createGraphics()
            graphics.drawImage(this, 0, 0, width, height, null)
            graphics.dispose()
        }
    }

    private fun BufferedImage.rotateImage(radian: Double): BufferedImage {
        val sin = abs(sin(radian))
        val cos = abs(cos(radian))

        val nWidth = floor(width.toDouble() * cos + height.toDouble() * sin).toInt()
        val nHeight = floor(height.toDouble() * cos + width.toDouble() * sin).toInt()

        val rotatedImage = BufferedImage(nWidth, nHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = rotatedImage.createGraphics()

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.translate((nWidth - width) / 2, (nHeight - height) / 2) // rotation around the center point
        graphics.rotate(radian, (width / 2).toDouble(), (height / 2).toDouble())
        graphics.drawImage(this, 0, 0, null)
        graphics.dispose()

        return rotatedImage
    }

    private fun BufferedImage.overwrite(image: BufferedImage, _x: Int, _y: Int, isCenterBase: Boolean = true): BufferedImage {
        val x = if(isCenterBase) (_x - (image.width / 2.0)).toInt() else _x
        val y = if(isCenterBase) (_y - (image.height / 2.0)).toInt() else _y

        return BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB).also {
            val graphics = it.createGraphics()
            graphics.drawImage(this, 0, 0, null)
            graphics.drawImage(image, x, y, null)
            graphics.dispose()
        }
    }

    private fun getScaleSize(parentImage: BufferedImage, childImage: BufferedImage, ratio: Double): Size {
        val height = parentImage.height * ratio
        val width = height * (childImage.width.toDouble() / childImage.height)

        return Size(width, height)
    }
    data class ComposeSetting(
        val trumpSize: Double = (0.3..0.8).random(),
        val locateX: Double = (0.2..0.8).random(),
        val locateY: Double = (0.2..0.8).random(),
        val rotate: Double = (0.0..360.0).random()
    )
}

fun ClosedFloatingPointRange<Double>.random(): Double {
    return Random.nextDouble(this.start, this.endInclusive)
}
