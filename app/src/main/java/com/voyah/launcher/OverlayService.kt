package com.voyah.launcher

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import com.voyah.launcher.databinding.OverlaySidebarBinding

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var sidebarView: View
    private lateinit var binding: OverlaySidebarBinding
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var settingsManager: SettingsManager
    private var isHidden = false
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hideSidebar() }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        try {
            settingsManager = SettingsManager(this)
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            
            binding = OverlaySidebarBinding.inflate(inflater)
            sidebarView = binding.root
            
            setupSidebarActions()
            setupTouchListener()

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            // Используем настройки пользователя
            val widthDp = settingsManager.sidebarWidth
            val offsetXDp = settingsManager.sidebarOffsetX
            val offsetYDp = settingsManager.sidebarOffsetY
            val density = resources.displayMetrics.density

            params = WindowManager.LayoutParams(
                (widthDp * density).toInt(),
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.START
            params.x = (offsetXDp * density).toInt()
            params.y = (offsetYDp * density).toInt()

            windowManager.addView(sidebarView, params)

            // Запускаем таймер автоскрытия только если нужно
            if (settingsManager.sidebarAutoHide && !settingsManager.sidebarNeverHide) {
                resetHideTimer()
            }
            
            sidebarView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Если не удалось создать оверлей (например, нет разрешения) - 
            // просто останавливаем сервис, не крашим лаунчер
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isHidden) showSidebar()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isHidden) showSidebar()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (isHidden && distanceX < -3) showSidebar()
                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (isHidden && velocityX > 150) showSidebar()
                return true
            }
        })

        binding.touchArea.setOnTouchListener { _, event ->
            val handled = gestureDetector.onTouchEvent(event)
            if (isHidden) true else handled
        }

        sidebarView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) resetHideTimer()
            false
        }
    }

    private fun showSidebar() {
        if (!::binding.isInitialized || !isHidden) return
        isHidden = false
        binding.sidebarHandle.visibility = View.GONE
        // Возвращаемся к настроенной X позиции
        val targetX = if (::settingsManager.isInitialized) {
            (settingsManager.sidebarOffsetX * resources.displayMetrics.density).toInt()
        } else 0
        animateSidebar(targetX)
        resetHideTimer()
    }

    private fun hideSidebar() {
        if (!::binding.isInitialized || isHidden) return
        // Если "никогда не скрывать" - не скрываем
        if (::settingsManager.isInitialized && settingsManager.sidebarNeverHide) return
        isHidden = true
        binding.sidebarHandle.visibility = View.VISIBLE
        // Сдвигаем меню за пределы экрана с учётом настроенной ширины
        val widthDp = if (::settingsManager.isInitialized) settingsManager.sidebarWidth else 120
        val targetX = -((widthDp - 20) * resources.displayMetrics.density).toInt()
        animateSidebar(targetX)
    }

    private fun animateSidebar(targetX: Int) {
        if (!::sidebarView.isInitialized || !::params.isInitialized) return
        try {
            val startX = params.x
            val animator = android.animation.ValueAnimator.ofInt(startX, targetX)
            animator.addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                try {
                    windowManager.updateViewLayout(sidebarView, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            animator.duration = 250
            animator.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resetHideTimer() {
        hideHandler.removeCallbacks(autoHideRunnable)
        // Если "никогда не скрывать" - не запускаем таймер
        if (::settingsManager.isInitialized && settingsManager.sidebarNeverHide) return
        if (::settingsManager.isInitialized && !settingsManager.sidebarAutoHide) return
        // Используем настроенное время задержки
        val delaySec = if (::settingsManager.isInitialized) settingsManager.sidebarHideDelay else 10
        hideHandler.postDelayed(autoHideRunnable, delaySec * 1000L)
    }

    private fun setupSidebarActions() {
        // 1. Машина - раздел автомобиля
        binding.btnCarSidebar.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            resetHideTimer()
        }

        // 2. Домик - главный экран
        binding.btnHomeSidebar.setOnClickListener {
            launchMainActivity()
            showSidebar()
        }

        // 3. Стрелка - навигация
        binding.btnNavSidebar.setOnClickListener {
            launchApp("ru.yandex.yandexnavi")
            hideSidebar()
        }

        // 4. Нота - музыка
        binding.btnMusicSidebar.setOnClickListener {
            launchApp("ru.yandex.music")
            hideSidebar()
        }

        // 5. Телефон - звонки
        binding.btnPhoneSidebar.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            hideSidebar()
        }

        // 6. Квадратики - все приложения / настройки
        binding.btnSettingsSidebar.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            hideSidebar()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "SHOW_SIDEBAR") showSidebar()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            val displayMetrics = resources.displayMetrics
            val sidebarWidthPx = (110 * displayMetrics.density).toInt()
            val screenWidthPx = displayMetrics.widthPixels
            val screenHeightPx = displayMetrics.heightPixels
            
            val options = android.app.ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val bounds = android.graphics.Rect(sidebarWidthPx, 0, screenWidthPx, screenHeightPx)
                options.launchBounds = bounds
            }
            
            startActivity(intent, options.toBundle())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacks(autoHideRunnable)
        try {
            if (::sidebarView.isInitialized && sidebarView.isAttachedToWindow) {
                windowManager.removeView(sidebarView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
