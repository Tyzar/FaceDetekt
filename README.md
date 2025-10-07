# Android Face Recognition POC

## Overview

This app is a Proof of Concept (POC) for face recognition on Android. It demonstrates how to use CameraX for camera input, ML Kit for face detection, and TensorFlow Lite with the FaceNet model for face embedding and recognition. Jetpack Compose is used for building the UI.

## Features

- **CameraX**: Captures live camera frames.
- **ML Kit**: Detects faces in real-time from camera frames.
- **TensorFlow Lite FaceNet**: Generates face embeddings and compares them for recognition.
- **Jetpack Compose**: Modern, declarative UI framework for Android.

## How It Works

1. **Register Face**
   The user registers their face. The app detects the face, crops it, and generates an embedding using FaceNet.

2. **Sign In**
   The app captures a new face image, generates its embedding, and compares it to the registered embedding using cosine similarity.

3. **Home Page**
   If the face is recognized, the user is signed in and shown the home page.

## Tech Stack

- Kotlin
- Jetpack Compose
- CameraX
- ML Kit Face Detection
- TensorFlow Lite (FaceNet)

## Usage

1. Grant camera permission.
2. Register your face.
3. Sign in using face recognition.

## Purpose

This project is intended as a technical demonstration and starting point for Android face recognition using modern libraries and models.

---
