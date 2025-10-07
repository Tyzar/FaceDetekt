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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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

fun cropBitmapDirect(
  sourceBitmap: Bitmap,
  scaledRect: Rect,
  isFrontCamera: Boolean = false
): Bitmap? {
  if (isFrontCamera) {
    // A. Mirroring Horizontal (X): Kunci untuk kamera depan
    val tempLeft = scaledRect.left
    scaledRect.left = sourceBitmap.width - scaledRect.right // Gunakan bitmapWidth
    scaledRect.right = sourceBitmap.width - tempLeft

    // B. JANGAN PERNAH MIRRORING VERTIKAL jika isFrontCamera = true
    // scaledRect.top dan scaledRect.bottom TIDAK BOLEH DIGANGGU di sini.
  }


  // 1. Tentukan Titik Awal (start/left/top)
  val startX = scaledRect.left.coerceAtMost(scaledRect.right).coerceAtLeast(0)
  val startY = scaledRect.top.coerceAtMost(scaledRect.bottom)
    .coerceAtLeast(0) // <-- Ambil yang terkecil dan >= 0

  // 2. Tentukan Titik Akhir (end/right/bottom)
  val endX = scaledRect.left.coerceAtLeast(scaledRect.right).coerceAtMost(sourceBitmap.width)
  val endY = scaledRect.top.coerceAtLeast(scaledRect.bottom)
    .coerceAtMost(sourceBitmap.height) // <-- Ambil yang terbesar dan <= height

  // 3. Hitung Dimensi
  val cropWidth = endX - startX
  val cropHeight = endY - startY

  // Diagnostik: Jika ini menghasilkan 175, berarti endY dan startY terlalu dekat
  Log.d(
    "FINAL_CROP",
    "Final Rect: L:$startX, T:$startY, R:$endX, B:$endY. Dimensions: $cropWidth x $cropHeight"
  )

  if (cropWidth <= 0 || cropHeight <= 0) {
    Log.e("Crop", "Crop area is invalid. Width:$cropWidth, Height:$cropHeight.")
    return null
  }

  return try {
    Bitmap.createBitmap(sourceBitmap, startX, startY, cropWidth, cropHeight)
  } catch (e: Exception) {
    Log.e("Crop", "Error cropping: ${e.message}", e)
    null
  }
}

/**
 * Memutar Bitmap sumber sesuai dengan sudut yang diberikan (biasanya 90, 180, atau 270 derajat).
 * * @param source Bitmap yang akan dirotasi (fullBitmap mentah dari ImageCapture).
 * @param degrees Sudut rotasi (diambil dari faceData.rotation).
 * @return Bitmap baru yang sudah dirotasi.
 */
fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
  // 1. Cek apakah rotasi diperlukan
  if (degrees == 0f || degrees % 360f == 0f) {
    Log.d("Rotate", "Rotation is 0. Returning source bitmap.")
    return source
  }

  // 2. Buat Matrix rotasi
  val matrix = Matrix()
  matrix.postRotate(degrees)

  Log.d(
    "Rotate",
    "Rotating bitmap by $degrees degrees. Original size: ${source.width}x${source.height}"
  )

  // 3. Buat Bitmap baru
  val rotatedBitmap = try {
    Bitmap.createBitmap(
      source,
      0,
      0,
      source.width,
      source.height,
      matrix,
      true // filter = true untuk kualitas yang lebih baik
    )
  } catch (e: Exception) {
    Log.e("Rotate", "Failed to create rotated bitmap: ${e.message}", e)
    return source // Kembalikan sumber jika gagal
  }

  // 4. (Opsional tapi Disarankan) Recycle Bitmap Sumber
  // Karena kita tidak ingin menyimpan dua salinan besar di memori.
  // Catatan: Pastikan source tidak digunakan lagi setelah pemanggilan ini.
  source.recycle()

  Log.d("Rotate", "Rotation successful. New size: ${rotatedBitmap.width}x${rotatedBitmap.height}")

  return rotatedBitmap
}

