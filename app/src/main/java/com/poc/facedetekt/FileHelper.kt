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