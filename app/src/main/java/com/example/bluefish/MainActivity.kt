package com.example.bluefish

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout

class MainActivity : Activity() {
    private val reqOverlay = 1001
    private lateinit var btn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER

        btn = Button(this)
        btn.text = "召唤小鱼"
        btn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), reqOverlay)
            } else {
                toggleFish()
            }
        }
        layout.addView(btn)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        btn.text = if (FishService.isRunning) "收回小鱼" else "召唤小鱼"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqOverlay && Settings.canDrawOverlays(this)) {
            toggleFish()
        }
    }

    private fun toggleFish() {
        if (FishService.isRunning) {
            stopService(Intent(this, FishService::class.java))
        } else {
            startService(Intent(this, FishService::class.java))
        }
        btn.postDelayed({ onResume() }, 300)
    }
}
