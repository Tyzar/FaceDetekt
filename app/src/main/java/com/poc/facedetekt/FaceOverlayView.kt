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

  private var mFaceBound: RectF? = null

  private var imageWidth = 1
  private var imageHeight = 1
  private var isFrontCamera = false

  fun updateFaces(
    faceRect: Rect,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean
  ) {
    this.imageWidth = imageWidth
    this.imageHeight = imageHeight
    this.isFrontCamera = isFrontCamera

    mFaceBound = RectF(faceRect)

    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (mFaceBound == null || mFaceBound!!.isEmpty) return

    val scaleX = width.toFloat() / imageHeight.toFloat()
    val scaleY = height.toFloat() / imageWidth.toFloat()

    if (mFaceBound != null) {
      val left = mFaceBound!!.left * scaleX
      val top = mFaceBound!!.top * scaleY
      val right = mFaceBound!!.right * scaleX
      val bottom = mFaceBound!!.bottom * scaleY

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


