package com.example.pdi_segmentation_project

import android.content.Context
import android.graphics.*
import ai.onnxruntime.*
import java.nio.FloatBuffer
import kotlin.math.roundToInt

class Segmenter(context: Context) {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    private val inputSize = 128

    init {
        val modelFile = copyAssetToInternalStorage(context, "unet_pet_segmentation.onnx")
        copyAssetToInternalStorage(context, "unet_pet_segmentation.onnx.data")

        session = env.createSession(modelFile.absolutePath)
    }
    private fun copyAssetToInternalStorage(context: Context, assetName: String): java.io.File {
        val file = java.io.File(context.filesDir, assetName)

        if (!file.exists()) {
            context.assets.open(assetName).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return file
    }
    fun segment(bitmap: Bitmap): Bitmap {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val input = FloatArray(1 * 3 * inputSize * inputSize)

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resized.getPixel(x, y)

                val r = Color.red(pixel) / 255.0f
                val g = Color.green(pixel) / 255.0f
                val b = Color.blue(pixel) / 255.0f

                val index = y * inputSize + x

                input[index] = r
                input[inputSize * inputSize + index] = g
                input[2 * inputSize * inputSize + index] = b
            }
        }

        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())

        val tensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(input),
            shape
        )

        val inputName = session.inputNames.iterator().next()

        val output = session.run(mapOf(inputName to tensor))
        val rawOutput = output[0].value as Array<Array<Array<FloatArray>>>

        val maskBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val value = rawOutput[0][0][y][x]

                val prob = sigmoid(value)

                if (prob > 0.5f) {
                    maskBitmap.setPixel(x, y, Color.argb(140, 255, 0, 0))
                } else {
                    maskBitmap.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }

        val finalMask = Bitmap.createScaledBitmap(maskBitmap, bitmap.width, bitmap.height, true)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawBitmap(finalMask, 0f, 0f, null)

        tensor.close()
        output.close()

        return result
    }

    private fun sigmoid(x: Float): Float {
        return (1.0f / (1.0f + kotlin.math.exp(-x))).toFloat()
    }
}