package com.tomato3d.app

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class TomatoGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: TomatoRenderer
    private var previousX = 0f
    private var previousY = 0f

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        renderer = TomatoRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousX = x
                previousY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - previousX
                val dy = y - previousY
                renderer.rotationY += dx * 0.5f
                renderer.rotationX += dy * 0.5f
                previousX = x
                previousY = y
            }
        }
        return true
    }
}
