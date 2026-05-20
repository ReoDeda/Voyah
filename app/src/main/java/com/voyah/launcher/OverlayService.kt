package com.voyah.launcher

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.voyah.launcher.databinding.OverlaySidebarBinding

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var sidebarView: View
    private lateinit var binding: OverlaySidebarBinding
    private lateinit var params: WindowManager.LayoutParams
    private var isHidden = false
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hideSidebar() }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        
        binding = OverlaySidebarBinding.inflate(inflater)
        sidebarView = binding.root
        binding.sidebarHandle.visibility = View.GONE

        setupSidebarActions()
        setupTouchListener()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            (150 * resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        windowManager.addView(sidebarView, params)
        
        // Запускаем таймер сразу после создания
        resetHideTimer()
        
        // Убираем системные панели для самого сайдбара, чтобы он не прыгал и был во всю высоту
        sidebarView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Если свайпы/двойные тапы не сработали, но человек просто тапнул 1 раз:
                if (isHidden) {
                    showSidebar()
                }
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
                // Если тянем вправо (distanceX < 0)
                if (isHidden && distanceX < -2) {
                    showSidebar()
                }
                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Быстрый свайп вправо
                if (isHidden && velocityX > 100) {
                    showSidebar()
                }
                return true
            }
        })

        // Слушатель для невидимой зоны (30dp)
        binding.touchArea.setOnTouchListener { _, event ->
            // Перехватываем свайпы и двойные тапы через GestureDetector
            val handled = gestureDetector.onTouchEvent(event)
            
            // Если панель скрыта, мы перехватываем все касания в этой зоне,
            // чтобы они доходили до GestureDetector
            if (isHidden) {
                true 
            } else {
                handled
            }
        }

        // Тап по самой панели сбрасывает таймер
        sidebarView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                resetHideTimer()
            }
            false
        }
    }

    private fun showSidebar() {
        if (!isHidden) return
        isHidden = false
        
        binding.sidebarHandle.visibility = View.GONE
        
        val targetX = 0
        animateSidebar(targetX)
        resetHideTimer()
    }

    private fun hideSidebar() {
        if (isHidden) return
        isHidden = true
        
        binding.sidebarHandle.visibility = View.VISIBLE
        
        // Сдвигаем на 120dp влево, чтобы видимая часть (120dp) ушла за экран, 
        // а прозрачная зона (30dp) осталась для тапа
        val targetX = -(120 * resources.displayMetrics.density).toInt()
        animateSidebar(targetX)
    }

    private fun animateSidebar(targetX: Int) {
        val startX = params.x
        val animator = android.animation.ValueAnimator.ofInt(startX, targetX)
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            windowManager.updateViewLayout(sidebarView, params)
        }
        animator.duration = 300
        animator.start()
    }

    private fun resetHideTimer() {
        hideHandler.removeCallbacks(autoHideRunnable)
        hideHandler.postDelayed(autoHideRunnable, 10000) // 10 секунд
    }

    private fun setupSidebarActions() {
        binding.btnHomeSidebar.setOnClickListener {
            launchMainActivity()
            showSidebar() // Показываем при возврате домой
        }
        binding.logoVoyah.setOnClickListener {
            launchMainActivity()
            showSidebar()
        }
        binding.btnCarSidebar.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            resetHideTimer()
        }
        binding.btnNavSidebar.setOnClickListener {
            launchApp("ru.yandex.yandexnavi")
            hideSidebar() // Прячем при открытии приложения
        }
        binding.btnMusicSidebar.setOnClickListener {
            launchApp("ru.yandex.music")
            hideSidebar()
        }
        binding.btnPhoneSidebar.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            hideSidebar()
        }
        binding.btnSettingsSidebar.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            hideSidebar()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "SHOW_SIDEBAR") {
            showSidebar()
        }
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
            val sidebarWidthPx = (120 * displayMetrics.density).toInt()
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
        if (::sidebarView.isInitialized) {
            windowManager.removeView(sidebarView)
        }
    }
}