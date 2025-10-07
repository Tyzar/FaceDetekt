package com.poc.facedetekt

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.io.FileInputStream
import java.nio.channels.FileChannel

class FaceNetEmbedder(context: Context, modelPath: String = "facenet.tflite") {

  private val interpreter: Interpreter
  private val modelInputSize: Int // Biasanya 160
  private val embeddingSize: Int  // Biasanya 128 atau 512

  init {
    // Memuat model TFLite dari folder assets
    val modelByteBuffer = loadModelFile(context, modelPath)
    val options = Interpreter.Options()
    // Opsi: Menghindari crash di beberapa perangkat.
    options.setNumThreads(4)

    interpreter = Interpreter(modelByteBuffer, options)

    // Mendapatkan ukuran input (misal: 160) dan output (misal: 128)
    val inputShape = interpreter.getInputTensor(0).shape()
    val outputShape = interpreter.getOutputTensor(0).shape()

    modelInputSize = inputShape[1] // Tinggi/Lebar Input
    embeddingSize = outputShape[1] // Ukuran Vektor Output

    Log.i(
      "FaceNetEmbedder",
      "Model loaded. Input: $modelInputSize x $modelInputSize. Output: $embeddingSize"
    )
  }

  /**
   * Memuat file TFLite ke ByteBuffer.
   */
  private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
    val fileDescriptor = context.assets.openFd(modelPath)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
  }

  /**
   * Pra-pemrosesan Bitmap dan menjalankan inferensi.
   * @param faceBitmap Bitmap wajah yang sudah di-crop.
   * @return Array float (embedding) atau null jika gagal.
   */
  suspend fun getFaceEmbedding(faceBitmap: Bitmap): FloatArray? = withContext(Dispatchers.Default) {
    if (faceBitmap.width == 0 || faceBitmap.height == 0) return@withContext null

    // 1. Pra-pemrosesan (Resize & Normalization)
    val imageProcessor = ImageProcessor.Builder()
      // Resize ke ukuran input model (misal 160x160)
      .add(ResizeOp(modelInputSize, modelInputSize, ResizeOp.ResizeMethod.BILINEAR))
      // Normalisasi: Sesuai kebutuhan model (seringkali [0, 255] -> [-1, 1])
      .add(NormalizeOp(127.5f, 127.5f))
      .build()

    var tImage = TensorImage(DataType.FLOAT32)
    tImage.load(faceBitmap)
    tImage = imageProcessor.process(tImage)

    // 2. Siapkan Output Buffer
    // Bentuk output: [1, embeddingSize]
    val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, embeddingSize), DataType.FLOAT32)

    // 3. Inferensi
    try {
      interpreter.run(tImage.buffer, outputBuffer.buffer.rewind())
      // Dapatkan hasil embedding sebagai FloatArray
      return@withContext outputBuffer.floatArray
    } catch (e: Exception) {
      Log.e("FaceNetEmbedder", "TFLite inference failed: ${e.message}")
      return@withContext null
    }
  }
}