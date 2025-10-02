package com.poc.facedetekt

import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
  private val onFaceDetected: (Int, Int, List<Rect>) -> Unit
) : ImageAnalysis.Analyzer {

  private val detector = FaceDetection.getClient(
    FaceDetectorOptions.Builder()
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
      .enableTracking()
      .build()
  )

  @OptIn(ExperimentalGetImage::class)
  override fun analyze(imageProxy: ImageProxy) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
      val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
      detector.process(image)
        .addOnSuccessListener { faces ->
          val rects = faces.map { face -> face.boundingBox }
          onFaceDetected(imageProxy.width, imageProxy.height, rects)
        }
        .addOnFailureListener { e ->
          Log.e("Face", "Detection error: $e")
        }
        .addOnCompleteListener {
          imageProxy.close()
        }
    } else {
      imageProxy.close()
    }
  }
}
