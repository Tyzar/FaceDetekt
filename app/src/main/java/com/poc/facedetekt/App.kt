package com.poc.facedetekt

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import kotlinx.coroutines.launch

enum class Page {
  Register, SignIn, Home
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
  val context = LocalContext.current

  var currentPage by remember { mutableStateOf(Page.Register) }

  val faceNetEmbedder = remember { FaceNetEmbedder(context) }

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
      Text(
        when (currentPage) {
          Page.Register -> "Register Face"
          Page.SignIn -> "Sign In"
          Page.Home -> "Home"
        }
      )
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

      true -> when (currentPage) {
        Page.Register -> RegisterFace(faceNetEmbedder = faceNetEmbedder, onReqSignIn = {
          currentPage = Page.SignIn
        })

        Page.SignIn -> SignIn(faceNetEmbedder = faceNetEmbedder, onSuccess = {
          Toast
            .makeText(context, "Sign In Success", Toast.LENGTH_SHORT)
            .show()
          currentPage = Page.Home
        })

        Page.Home -> HomePage()
      }
    }
  }
}

@Composable
fun RegisterFace(faceNetEmbedder: FaceNetEmbedder, onReqSignIn: () -> Unit) {
  val coroutineScope = rememberCoroutineScope()
  var faceRegistered by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  val context = LocalContext.current

  when (faceRegistered) {
    true -> Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text("Face has been registered")
      Spacer(Modifier.height(16.dp))
      Button(onClick = {
        onReqSignIn()
      }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text("Go to Sign In")
      }
    }

    false -> CameraPreview(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      onCaptureFace = {
        coroutineScope.launch {
          if (isLoading) return@launch

          isLoading = true
          val embedBitmap = faceNetEmbedder.getFaceEmbedding(it)
          if (embedBitmap != null) {
            //simpan embedding ke file
            saveEmbeddingToFile(
              context = context,
              embedding = embedBitmap,
              fileName = "face_data.dat"
            )
          }

          isLoading = false
          faceRegistered = true
        }
      }
    )
  }
}

@Composable
fun SignIn(faceNetEmbedder: FaceNetEmbedder, onSuccess: () -> Unit) {
  var faceRecognized by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  when (faceRecognized) {
    true -> Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), contentAlignment = Alignment.Center
    ) {
      Text("Face recognized, Sign In Success")
    }

    false -> CameraPreview(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      onCaptureFace = {
        coroutineScope.launch {
          if (isLoading) return@launch

          isLoading = true
          val embedBitmap = faceNetEmbedder.getFaceEmbedding(it)
          if (embedBitmap == null) {
            isLoading = false
            faceRecognized = false
            Toast.makeText(context, "Face not detected, try again", Toast.LENGTH_SHORT).show()
            return@launch
          }

          //baca embedding dari file
          val savedEmbedding =
            readEmbeddingFromFile(context = context, fileName = "face_data.dat")
          //jika null, berarti belum register
          if (savedEmbedding == null) {
            isLoading = false
            faceRecognized = false
            Toast.makeText(context, "No registered face, please register first", Toast.LENGTH_SHORT)
              .show()
            return@launch
          }

          //hitung cosine similarity
          val cosineSim = cosineSimilarity(savedEmbedding, embedBitmap)
          Log.d("Recognize", "Cosine Similarity: $cosineSim")
          if (cosineSim >= thresholdSimilarity) {
            faceRecognized = true
            onSuccess()
          } else {
            faceRecognized = false
            Toast.makeText(context, "Face not recognized, try again", Toast.LENGTH_SHORT).show()
          }

          isLoading = false
        }
      }
    )
  }
}

@Composable
fun HomePage() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp), contentAlignment = Alignment.Center
  ) {
    Text("Welcome to Home Page")
  }
}
