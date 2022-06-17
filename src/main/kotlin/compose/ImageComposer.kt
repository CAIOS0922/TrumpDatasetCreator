package compose

import data.DataLoader
import data.ImageUrlData
import data.Trump
import data.TrumpData
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ImageComposer(private val dataLoader: DataLoader, private val setting: ComposeSetting = ComposeSetting()) {

    private val random = Random(System.currentTimeMillis())

    suspend fun compose(trump: TrumpData, imageUrlData: ImageUrlData): Mat? {
        val trumpMat = Imgcodecs.imread(trump.file.absolutePath)
        val imageMat = dataLoader.getImage(imageUrlData) ?: return null

        val trumpSize = getScaleSize(imageMat, trumpMat, setting.trumpSize.random())
        Imgproc.resize(trumpMat, trumpMat, trumpSize)

        val locateX = imageMat.size().width * setting.locateX.random()
        val locateY = imageMat.size().height * setting.locateY.random()
        val locate = getLocate(trumpMat, locateX, locateY, false)

        val roi = Rect(
            locate.x.toInt(),
            locate.y.toInt(),
            min(trumpMat.cols(), imageMat.cols() - locateX.toInt()),
            min(trumpMat.rows(), imageMat.rows() - locateY.toInt())
        )
        val settingMat = Mat(imageMat, roi)

        //settingMat.copyTo(imageMat, trumpMat)
        Core.add(imageMat, trumpMat, settingMat)

        Imgcodecs.imwrite("${System.getProperty("user.dir")}/trump.jpg", trumpMat)
        Imgcodecs.imwrite("${System.getProperty("user.dir")}/image.jpg", imageMat)
        Imgcodecs.imwrite("${System.getProperty("user.dir")}/setting.jpg", settingMat)

        return trumpMat
    }

    private fun getScaleSize(parentMat: Mat, childMat: Mat, ratio: Double): Size {
        val parentSize = parentMat.size()
        val childSize = childMat.size()
        val height = parentSize.height * ratio
        val width = height * (childSize.width / childSize.height)

        return Size(width, height)
    }

    private fun getLocate(childMat: Mat, x: Double, y: Double, isCenterBase: Boolean = true): Point {
        return if(!isCenterBase) Point(x, y) else Point(x - (childMat.size().width / 2), y - (childMat.size().height / 2))
    }

    private fun ClosedFloatingPointRange<Double>.random(): Double {
        return random.nextDouble(this.start, this.endInclusive)
    }
}

data class ComposeSetting(
    val trumpSize: ClosedFloatingPointRange<Double> = 0.3..0.8,
    val locateX: ClosedFloatingPointRange<Double> = 0.1..0.9,
    val locateY: ClosedFloatingPointRange<Double> = 0.1..0.9,
)