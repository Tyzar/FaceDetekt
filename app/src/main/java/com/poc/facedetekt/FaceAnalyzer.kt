package com.poc.facedetekt

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicReference

data class FaceData(
  val mlKitRect: Rect,
  val rotation: Int,
  val imgWidth: Int,
  val imgHeight: Int
)

class FaceAnalyzer(
  private val onFaceDetected: (Int, Int, Rect) -> Unit
) : ImageAnalysis.Analyzer {
  private val detector = FaceDetection.getClient(
    FaceDetectorOptions.Builder()
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
      .build()
  )

  private val lastFaceData: AtomicReference<FaceData?> = AtomicReference(null)
  val faceData get() = lastFaceData

  @OptIn(ExperimentalGetImage::class)
  override fun analyze(image: ImageProxy) {
    val mediaImage = image.image ?: return image.close()
    val inputRotation = image.imageInfo.rotationDegrees
    val inputImage = InputImage.fromMediaImage(mediaImage, inputRotation)

    detector.process(inputImage)
      .addOnSuccessListener { faces ->
        val faceRect = faces.firstOrNull()?.boundingBox
        if (faceRect != null) {
          lastFaceData.set(
            FaceData(
              mlKitRect = faceRect,
              rotation = inputRotation,
              imgWidth = image.width,
              imgHeight = image.height,
            )
          )
          onFaceDetected(image.width, image.height, lastFaceData.get()!!.mlKitRect)
        }
        image.close()
      }
      .addOnFailureListener {
        image.close()
      }
  }
}
