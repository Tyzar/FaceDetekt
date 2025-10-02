package com.poc.facedetekt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

class FaceOverlayView(context: Context) : View(context) {
  private val paint = Paint().apply {
    color = Color.GREEN
    style = Paint.Style.STROKE
    strokeWidth = 5f
  }

  private val mFaceBounds = mutableListOf<RectF>()
  val faceBounds get() = mFaceBounds

  private var imageWidth = 1
  private var imageHeight = 1
  private var isFrontCamera = false

  fun updateFaces(
    faces: List<Rect>,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean
  ) {
    this.imageWidth = imageWidth
    this.imageHeight = imageHeight
    this.isFrontCamera = isFrontCamera

    // Resize list jika perlu
    if (mFaceBounds.size < faces.size) {
      repeat(faces.size - mFaceBounds.size) {
        mFaceBounds.add(RectF())
      }
    }

    // Update value (tanpa alloc baru)
    faces.forEachIndexed { i, rect ->
      mFaceBounds[i].set(rect)
    }

    // Trim kalau jumlah wajah berkurang
    if (mFaceBounds.size > faces.size) {
      mFaceBounds.subList(faces.size, mFaceBounds.size).clear()
    }

    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (mFaceBounds.isEmpty()) return

    val scaleX = width.toFloat() / imageHeight.toFloat()
    val scaleY = height.toFloat() / imageWidth.toFloat()

    mFaceBounds.forEach { rect ->
      val left = rect.left * scaleX
      val top = rect.top * scaleY
      val right = rect.right * scaleX
      val bottom = rect.bottom * scaleY

      if (isFrontCamera) {
        canvas.drawRect(
          width - right, top,
          width - left, bottom,
          paint
        )
      } else {
        canvas.drawRect(left, top, right, bottom, paint)
      }
    }
  }
}


