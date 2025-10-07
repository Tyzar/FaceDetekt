package com.poc.facedetekt

const val thresholdSimilarity = 0.5f

//fungsi untuk menghitung cosine similarity antara dua float array kotlin
fun cosineSimilarity(emb1: FloatArray, emb2: FloatArray): Float {
  require(emb1.size == emb2.size) { "Embedding arrays must be of the same size" }

  var dotProduct = 0f
  var normA = 0f
  var normB = 0f

  for (i in emb1.indices) {
    dotProduct += emb1[i] * emb2[i]
    normA += emb1[i] * emb1[i]
    normB += emb2[i] * emb2[i]
  }

  return if (normA == 0f || normB == 0f) {
    0f
  } else {
    dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
  }
}