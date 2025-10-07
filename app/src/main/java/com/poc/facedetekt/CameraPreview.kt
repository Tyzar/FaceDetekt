package com.poc.facedetekt

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun CameraPreview(modifier: Modifier = Modifier, onCaptureFace: (Bitmap) -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()

  val previewView = remember {
    PreviewView(context).apply {
      scaleType = PreviewView.ScaleType.FILL_CENTER
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    }
  }

  val overlay = remember {
    FaceOverlayView(context).apply {
      layoutParams = previewView.layoutParams
    }
  }

  val faceAnalyzer = remember {
    FaceAnalyzer(
      onFaceDetected = { imgWidth, imgHeight, faceRect ->
        overlay.updateFaces(
          faceRect,
          imgWidth,
          imgHeight,
          isFrontCamera = true
        )
      }
    )
  }

  val imageCapture = remember {
    ImageCapture.Builder()
      .setResolutionSelector(
        ResolutionSelector.Builder()
          .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
          .build()
      )
      .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
      .build()
  }

  DisposableEffect(Unit) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    val cameraProvider = cameraProviderFuture.get()
    val preview = Preview.Builder().build().also {
      it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val imageAnalyzer = ImageAnalysis.Builder()
      .setResolutionSelector(
        ResolutionSelector.Builder()
          .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY).build()
      )
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      .build()
      .also {
        it.setAnalyzer(
          ContextCompat.getMainExecutor(context),
          faceAnalyzer
        )
      }

    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    try {
      cameraProvider.unbindAll()
      cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        imageAnalyzer,
        imageCapture
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }

    onDispose {
      cameraProvider.unbindAll()
    }
  }

  Column(modifier = modifier.fillMaxSize()) {
    AndroidView(
      modifier = Modifier.weight(1f),
      factory = { ctx ->
        FrameLayout(ctx).apply {
          addView(previewView)
          addView(overlay)
        }
      }
    )
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(color = MaterialTheme.colorScheme.surface)
        .padding(16.dp)
    ) {
      Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
          val faceData = faceAnalyzer.faceData.get() ?: return@Button

          Log.e("BMP", "Has face data...")
          imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
              @OptIn(ExperimentalGetImage::class)
              override fun onCaptureSuccess(imageProxy: ImageProxy) {
                coroutineScope.launch {
                  Log.e("BMP", "Capture success!!")
                  val fullBitmap =
                    imageProxy.image?.toBitmap() ?: return@launch

                  val fullBitmapWidth = fullBitmap.width
                  val fullBitmapHeight = fullBitmap.height

                  //rotasi fullBitmap dengan nilai faceData.rotation
                  val rotatedBitmap = rotateBitmap(fullBitmap, faceData.rotation.toFloat())
                  if (rotatedBitmap !== fullBitmap) {
                    fullBitmap.recycle()
                  }

                  val scaleX = fullBitmapWidth.toFloat() / faceData.imgWidth
                  val scaleY = fullBitmapHeight.toFloat() / faceData.imgHeight
                  val faceRect = faceData.mlKitRect

                  val mappedRect = Rect(
                    (faceRect.left * scaleX).toInt(),
                    (faceRect.top * scaleY).toInt(),
                    (faceRect.right * scaleX).toInt(),
                    (faceRect.bottom * scaleY).toInt()
                  )
                  Log.d("BMP", "Face Rect Bitmap: ${faceRect.width()}x${faceRect.height()}")
                  Log.d("BMP", "Scaled Rect Bitmap: ${mappedRect.width()}x${mappedRect.height()}")

// Add padding if needed
                  mappedRect.inset(-100, -100)

// Crop using mappedRect
                  val faceBitmap = cropBitmapDirect(rotatedBitmap, mappedRect, true) ?: run {
                    Log.e("BMP", "Failed to crop face bitmap.")
                    rotatedBitmap.recycle()
                    imageProxy.close()
                    return@launch
                  }

                  Log.e("BMP", "Face bitmap cropped: ${faceBitmap.width}x${faceBitmap.height}")
                  onCaptureFace(faceBitmap)

                  rotatedBitmap.recycle()
                  imageProxy.close()
                }
              }

              override fun onError(exception: ImageCaptureException) {
                Log.e("BMP", "Photo capture failed: ${exception.message}", exception)
              }
            })
        }) {
        Text("Capture")
      }
    }
  }
}