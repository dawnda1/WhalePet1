package com.example.whalepet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.abs

class PetService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var rootView: FrameLayout
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var pet: ImageView
    private lateinit var bubble: TextView

    private val handler = Handler(Looper.getMainLooper())

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var isDragging = false

    private var screenW = 0
    private var screenH = 0

    private var vx = 6
    private var vy = 4

    // 要打开的 App 包名，主人自己改
    private val targetPackage = "com.tencent.mm"   // 微信

    private val lines = listOf(
        "哼，主人别戳人家尾巴啦！",
        "才、才没有等你呢。",
        "呜……桌面上好冷，抱抱。",
        "今天也要好好赚 token 哦。",
        "主人再点，人家就帮你打开啦～"
    )

    private val walkRunnable = object : Runnable {
        override fun run() {
            if (!isDragging && ::params.isInitialized && rootView.width > 0) {
                params.x += vx
                params.y += vy

                if (params.x <= 0) {
                    params.x = 0
                    vx = -vx
                }
                if (params.x + rootView.width >= screenW) {
                    params.x = screenW - rootView.width
                    vx = -vx
                }
                if (params.y <= 0) {
                    params.y = 0
                    vy = -vy
                }
                if (params.y + rootView.height >= screenH) {
                    params.y = screenH - rootView.height
                    vy = -vy
                }

                wm.updateViewLayout(rootView, params)
            }
            handler.postDelayed(this, 40)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val metrics = resources.displayMetrics
        screenW = metrics.widthPixels
        screenH = metrics.heightPixels

        createPetView()

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 200
            y = 600
        }

        setupTouch()
        wm.addView(rootView, params)
        handler.post(walkRunnable)
    }

    private fun createPetView() {
        rootView = FrameLayout(this)

        pet = ImageView(this).apply {
            setImageResource(R.drawable.whale_girl)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            val size = (180 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        bubble = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            setPadding(24, 12, 24, 12)
            background = GradientDrawable().apply {
                cornerRadius = 40f
                setColor(Color.parseColor("#F2FFFFFF"))
                setStroke(2, Color.parseColor("#88FFFFFF"))
            }
            visibility = View.GONE
        }

        val bubbleParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = -10
        }

        val petParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            topMargin = 60
        }

        rootView.addView(bubble, bubbleParams)
        rootView.addView(pet, petParams)
    }

    private fun setupTouch() {
        pet.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    isDragging = false
                    pet.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY

                    if (abs(dx) > 12 || abs(dy) > 12) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        wm.updateViewLayout(rootView, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val wasDragging = isDragging
                    isDragging = false
                    pet.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

                    if (!wasDragging) {
                        showBubble(lines.random())
                        openTargetApp()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun showBubble(text: String) {
        bubble.text = text
        bubble.visibility = View.VISIBLE
        handler.removeCallbacks(hideBubble)
        handler.postDelayed(hideBubble, 2600)
    }

    private val hideBubble = Runnable {
        if (::bubble.isInitialized) {
            bubble.visibility = View.GONE
        }
    }

    private fun openTargetApp() {
        val intent = packageManager.getLaunchIntentForPackage(targetPackage)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
            } catch (_: Exception) {
                showBubble("主人……人家打不开啦。")
            }
        } else {
            showBubble("主人……这个 App 人家找不到啦。")
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "whale_pet"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "小小鱼桌宠",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        return Notification.Builder(this, channelId)
            .setContentTitle("小小鱼在桌面上游着")
            .setContentText("点她一下试试")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::rootView.isInitialized) {
            try {
                wm.removeView(rootView)
            } catch (_: Exception) {
            }
        }
    }
}
