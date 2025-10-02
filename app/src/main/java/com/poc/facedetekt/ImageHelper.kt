package com.poc.facedetekt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
  val yBuffer = imageProxy.planes[0].buffer
  val uBuffer = imageProxy.planes[1].buffer
  val vBuffer = imageProxy.planes[2].buffer

  val ySize = yBuffer.remaining()
  val uSize = uBuffer.remaining()
  val vSize = vBuffer.remaining()

  val nv21 = ByteArray(ySize + uSize + vSize)
  yBuffer.get(nv21, 0, ySize)
  vBuffer.get(nv21, ySize, vSize)
  uBuffer.get(nv21, ySize + vSize, uSize)

  val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
  val out = ByteArrayOutputStream()
  yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
  val imageBytes = out.toByteArray()
  return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun cropFace(bitmap: Bitmap, bounds: Rect): Bitmap {
  // Clamp nilai agar tidak keluar dari area gambar
  val padding = 20
  val x = (bounds.left - padding).coerceAtLeast(0)
  val y = (bounds.top - padding).coerceAtLeast(0)
  val right = (bounds.right + padding).coerceAtMost(bitmap.width)
  val bottom = (bounds.bottom + padding).coerceAtMost(bitmap.height)
  
  val width = right - x
  val height = bottom - y

  return Bitmap.createBitmap(bitmap, x, y, width, height)
}

