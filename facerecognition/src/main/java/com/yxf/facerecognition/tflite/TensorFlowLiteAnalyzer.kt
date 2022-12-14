package com.yxf.facerecognition.tflite

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TensorFlowLiteAnalyzer(private val context: Context) {


    companion object {
        const val TF_OD_API_INPUT_SIZE = 112
        private const val TF_OD_API_IS_QUANTIZED = false
        private const val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"

        private const val NUMBER_BYTES_PER_CHANNEL = 4

        // Float model
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f

        private const val OUTPUT_SIZE = 192

        // Number of threads in the java app
        private const val NUM_THREADS = 4

        private const val MODEL_FILE_NAME = TF_OD_API_MODEL_FILE

        private val TAG = "FR." + "TensorFlowLiteAnalyzer"

    }


    private val assets by lazy { context.assets }

    private val colorArray: IntArray = IntArray(TF_OD_API_INPUT_SIZE * TF_OD_API_INPUT_SIZE)


    private val imgData by lazy {
        ByteBuffer.allocateDirect(1 * TF_OD_API_INPUT_SIZE * TF_OD_API_INPUT_SIZE * 3 * NUMBER_BYTES_PER_CHANNEL).also {
            it.order(ByteOrder.nativeOrder())
        }
    }

    private val tfLite by lazy {
        Interpreter(loadModelFile(), null)
    }

    private val tfLiteFolderPath by lazy {
        val path = "${context.filesDir}/tflite"
        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }
        return@lazy path
    }

    private val modelFile by lazy {

        val filePath = "${tfLiteFolderPath}/$MODEL_FILE_NAME"
        val file = File(filePath)
        if (file.exists()) {
            return@lazy file
        } else {
            extractModelFile(file)
            return@lazy file
        }
    }

    private fun extractModelFile(file: File) {
        try {
            context.assets.open(MODEL_FILE_NAME).use { ins ->
                file.outputStream().use { os ->
                    val buffer = ByteArray(1024 * 100)
                    var len = 0
                    while (ins.read(buffer).also { len = it } != -1) {
                        os.write(buffer, 0, len)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "extract model file failed", e)
            file.delete()
            throw e
        }
    }


    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val inputStream = FileInputStream(modelFile)
        val fileChannel = inputStream.channel
        val startOffset = 0L
        val declaredLength = modelFile.length()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun analyzeFaceImage(bitmap: Bitmap): FloatArray {
        val inputSize = TF_OD_API_INPUT_SIZE
        bitmap.getPixels(colorArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        imgData.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue: Int = colorArray.get(i * inputSize + j)
                imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }

        val inputArray = arrayOf(imgData)
        val outputMap = HashMap<Int, Any>()
        val embeedings = Array(1) { FloatArray(OUTPUT_SIZE) }
        outputMap.put(0, embeedings)
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap)
        return embeedings[0]
    }


}