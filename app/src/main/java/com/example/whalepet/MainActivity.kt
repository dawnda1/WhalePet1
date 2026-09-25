package com.example.whalepet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val NOTI_REQ = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "🐋 小小鱼桌宠"
            textSize = 28f
            gravity = Gravity.CENTER
        }

        val desc = TextView(this).apply {
            text = "1. 给悬浮窗权限\n2. 给通知权限\n3. 点启动，然后回桌面"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }

        val start = Button(this).apply {
            text = "启动桌宠"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                    Toast.makeText(
                        this@MainActivity,
                        "先开悬浮窗权限，再点一次启动",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    requestNotiIfNeeded()
                    startForegroundService(Intent(this@MainActivity, PetService::class.java))
                    Toast.makeText(
                        this@MainActivity,
                        "小小鱼游出去了，回桌面看",
                        Toast.LENGTH_SHORT
                    ).show()
                    moveTaskToBack(true)
                }
            }
        }

        val stop = Button(this).apply {
            text = "收回小小鱼"
            setOnClickListener {
                stopService(Intent(this@MainActivity, PetService::class.java))
                Toast.makeText(
                    this@MainActivity,
                    "人家回海里了……",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        root.addView(title)
        root.addView(desc)
        root.addView(
            start,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            stop,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
    }

    private fun requestNotiIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTI_REQ
                )
            }
        }
    }
}
