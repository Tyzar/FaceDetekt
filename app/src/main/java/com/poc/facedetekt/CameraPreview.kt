package com.poc.facedetekt

import android.graphics.Bitmap
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreview(modifier: Modifier = Modifier, onCaptureFace: (Bitmap) -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
      modifier = modifier,
      factory = { ctx ->
        FrameLayout(ctx).apply {
          val previewView = PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            layoutParams = FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT
            )
          }

          val overlay = FaceOverlayView(context).apply {
            layoutParams = previewView.layoutParams
          }
          addView(previewView)
          addView(overlay)

          val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
          cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
              it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()
              .also {
                it.setAnalyzer(
                  ContextCompat.getMainExecutor(context),
                  FaceAnalyzer(
                    onFaceDetected = { imgWidth, imgHeight, faceRects ->
                      overlay.updateFaces(
                        faceRects,
                        imgWidth,
                        imgHeight,
                        isFrontCamera = true
                      )
                    }
                  )
                )
              }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
              cameraProvider.unbindAll()
              cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }, ContextCompat.getMainExecutor(context))
        }
      }
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(color = MaterialTheme.colorScheme.surface)
        .padding(16.dp)
    ) {
      Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
          //TODO: capture face and create cropped bitmap

        }) {
        Text("Capture")
      }
    }
  }
}