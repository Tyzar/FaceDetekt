package com.poc.facedetekt

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun createImageFile(context: Context, bitmap: Bitmap) = withContext(Dispatchers.IO) {
  var fos: FileOutputStream? = null

  try {
    val filesDir = context.filesDir
    val outFile = File(filesDir, "outface.jpg")

    val fos = FileOutputStream(outFile)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
  } catch (e: Exception) {
    e.printStackTrace()
  } finally {
    fos?.close()
  }
}

//fungsi simpan facenet float array ke file
suspend fun saveEmbeddingToFile(context: Context, embedding: FloatArray, fileName: String) =
  withContext(Dispatchers.IO) {
    try {
      val filesDir = context.filesDir
      val outFile = File(filesDir, fileName)
      outFile.outputStream().use { out ->
        embedding.forEach { value ->
          out.write(java.nio.ByteBuffer.allocate(4).putFloat(value).array())
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

// fungsi baca facenet float array dari file dengan binary read
suspend fun readEmbeddingFromFile(context: Context, fileName: String): FloatArray? =
  withContext(Dispatchers.IO) {
    try {
      val size = 128
      val filesDir = context.filesDir
      val inFile = File(filesDir, fileName)
      if (!inFile.exists()) return@withContext null

      val byteArray = inFile.readBytes()
      if (byteArray.size < size * 4) return@withContext null

      val floatArray = FloatArray(size)
      for (i in 0 until size) {
        floatArray[i] = java.nio.ByteBuffer.wrap(byteArray, i * 4, 4).float
      }
      return@withContext floatArray
    } catch (e: Exception) {
      e.printStackTrace()
      return@withContext null
    }
  }