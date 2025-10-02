package com.poc.facedetekt

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var faceDetected by remember { mutableStateOf(false) }

  var isCameraPermGranted by remember {
    mutableStateOf(
      ContextCompat
        .checkSelfPermission(
          context,
          Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      isCameraPermGranted = isGranted
    }

  Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
    TopAppBar(title = {
      Text("Face Detekt")
    })
  }) { contentPadding ->
    when (isCameraPermGranted) {
      false -> Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(contentPadding)
          .padding(16.dp), contentAlignment = Alignment.Center
      ) {
        Button(onClick = {
          cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }) {
          Text("Grant Permission")
        }
      }

      true -> when (faceDetected) {
        true -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("Face has been detected")
        }

        false -> CameraPreview(
          modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
          onCaptureFace = {

          }
        )
      }
    }
  }
}