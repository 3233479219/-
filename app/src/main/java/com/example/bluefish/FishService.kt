package com.example.bluefish

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

class FishService : Service() {
    private lateinit var wm: WindowManager
    private var fish: FishView? = null
    private lateinit var params: WindowManager.LayoutParams

    companion object {
        @Volatile
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (fish == null) {
            if (!Settings.canDrawOverlays(this)) {
                stopSelf()
                return START_NOT_STICKY
            }
            addFish()
        }
        return START_NOT_STICKY
    }

    private fun addFish() {
        val v = FishView(this)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        v.onMove = { dx, dy ->
            params.x += dx.toInt()
            params.y += dy.toInt()
            wm.updateViewLayout(v, params)
        }

        wm.addView(v, params)
        fish = v
        isRunning = true
    }

    override fun onDestroy() {
        fish?.let { wm.removeView(it) }
        fish = null
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
