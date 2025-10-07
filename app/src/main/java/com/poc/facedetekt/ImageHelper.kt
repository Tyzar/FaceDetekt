package com.poc.facedetekt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

fun ImageProxy.toJpegBytes(): ByteArray {
  val yBuffer = planes[0].buffer
  val uBuffer = planes[1].buffer
  val vBuffer = planes[2].buffer

  val ySize = yBuffer.remaining()
  val uSize = uBuffer.remaining()
  val vSize = vBuffer.remaining()

  val nv21 = ByteArray(ySize + uSize + vSize)
  yBuffer.get(nv21, 0, ySize)
  vBuffer.get(nv21, ySize, vSize)
  uBuffer.get(nv21, ySize + vSize, uSize)

  val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
  val out = ByteArrayOutputStream()
  yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
  return out.toByteArray()
}

suspend fun Image.toBitmap(): Bitmap? = withContext(Dispatchers.Default) {
  // Memeriksa format yang didukung
  if (format == ImageFormat.JPEG) {
    Log.e("BMP", "Start converting to bitmap...")
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  }

  if (format != ImageFormat.YUV_420_888) {
    Log.e("BMP", "Unsupported image format: ${format}. Only JPEG and YUV_420_888 is supported.")
    return@withContext null
  }

  Log.e("BMP", "Start converting to bitmap...")
  // Mendapatkan buffer YUV planes
  val planes = planes
  val yBuffer = planes[0].buffer
  val uBuffer = planes[1].buffer
  val vBuffer = planes[2].buffer

  val ySize = yBuffer.remaining()
  val uSize = uBuffer.remaining()
  val vSize = vBuffer.remaining()

  // Membuat array untuk menampung data NV21
  val nv21 = ByteArray(ySize + uSize + vSize)

  // Mengisi array NV21 (NV21 adalah YYYY...VV...UU...)
  yBuffer.get(nv21, 0, ySize)
  // Dalam format YUV_420_888, V dan U sering dibalik dalam buffer dibandingkan NV21 standar.
  // Metode ini mengasumsikan YUV_420_888 dan mengaturnya ke tata letak NV21 yang benar.
  vBuffer.get(nv21, ySize, vSize)
  uBuffer.get(nv21, ySize + vSize, uSize)

  // Membuat YuvImage
  val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)

  val out = ByteArrayOutputStream()
  // Mengkompres YuvImage ke JPEG
  yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)

  val imageBytes = out.toByteArray()

  // Mendecode JPEG menjadi Bitmap
  return@withContext BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun cropBitmapDirect(sourceBitmap: Bitmap, mlKitRect: Rect): Bitmap? {
  // 1. Tentukan batas awal (left, top) dan batas akhir (right, bottom)
  val originalLeft = mlKitRect.left
  val originalTop = mlKitRect.top
  val originalRight = mlKitRect.right
  val originalBottom = mlKitRect.bottom

  // 2. Terapkan Clamping (Pembatasan Batas)
  // Tujuan: Memastikan tidak ada nilai negatif atau melebihi dimensi Bitmap.

  // a. Tentukan koordinat awal (start/left/top)
  // Gunakan coerceAtLeast(0) untuk memastikan nilai tidak negatif.
  // Gunakan coerceAtMost() pada pasangan koordinat untuk memastikan start selalu yang terkecil.
  val startX = originalLeft.coerceAtMost(originalRight).coerceAtLeast(0)
  val startY = originalTop.coerceAtMost(originalBottom).coerceAtLeast(0)

  // b. Tentukan koordinat akhir (end/right/bottom)
  // Gunakan coerceAtMost(dimensi) untuk memastikan nilai tidak melebihi batas.
  // Gunakan coerceAtLeast() pada pasangan koordinat untuk memastikan end selalu yang terbesar.
  val endX = originalLeft.coerceAtLeast(originalRight).coerceAtMost(sourceBitmap.width)
  val endY = originalTop.coerceAtLeast(originalBottom).coerceAtMost(sourceBitmap.height)

  // 3. Hitung Lebar dan Tinggi setelah Clamping
  val cropWidth = endX - startX
  val cropHeight = endY - startY

  // 4. Cek validitas akhir (mencegah lebar/tinggi 0 atau negatif)
  if (cropWidth <= 0 || cropHeight <= 0) {
    Log.w("Crop", "Crop area is invalid: $cropWidth x $cropHeight.")
    return null
  }

  // 5. Buat Bitmap hasil potong
  return try {
    Bitmap.createBitmap(
      sourceBitmap,
      startX,
      startY,
      cropWidth,
      cropHeight
    )
  } catch (e: Exception) {
    // Menangkap error umum (misalnya IllegalArgumentException jika ada masalah sisa clamping)
    Log.e("Crop", "Error creating Bitmap: ${e.message}", e)
    null
  }
}

fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
  if (degrees == 0f || degrees == 360f || degrees == -360f) return source

  val matrix = Matrix()
  matrix.postRotate(degrees)

  val rotatedBitmap = Bitmap.createBitmap(
    source,
    0,
    0,
    source.width,
    source.height,
    matrix,
    true
  )
  return rotatedBitmap
}

