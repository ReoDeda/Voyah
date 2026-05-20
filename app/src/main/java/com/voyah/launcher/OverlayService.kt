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
        
        setupSidebarActions()
        setupTouchListener()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            (130 * resources.displayMetrics.density).toInt(),
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
        resetHideTimer()
        
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
        if (!isHidden) return
        isHidden = false
        binding.sidebarHandle.visibility = View.GONE
        animateSidebar(0)
        resetHideTimer()
    }

    private fun hideSidebar() {
        if (isHidden) return
        isHidden = true
        binding.sidebarHandle.visibility = View.VISIBLE
        val targetX = -(108 * resources.displayMetrics.density).toInt()
        animateSidebar(targetX)
    }

    private fun animateSidebar(targetX: Int) {
        val startX = params.x
        val animator = android.animation.ValueAnimator.ofInt(startX, targetX)
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            windowManager.updateViewLayout(sidebarView, params)
        }
        animator.duration = 250
        animator.start()
    }

    private fun resetHideTimer() {
        hideHandler.removeCallbacks(autoHideRunnable)
        hideHandler.postDelayed(autoHideRunnable, 10000)
    }

    private fun setActiveButton(button: View) {
        // Сброс всех кнопок
        binding.btnHomeSidebar.setBackgroundResource(R.drawable.sidebar_button_frame)
        binding.btnNavSidebar.setBackgroundResource(R.drawable.sidebar_button_frame)
        binding.btnMusicSidebar.setBackgroundResource(R.drawable.sidebar_button_frame)
        binding.btnPhoneSidebar.setBackgroundResource(R.drawable.sidebar_button_frame)
        binding.btnSettingsSidebar.setBackgroundResource(R.drawable.sidebar_button_frame)
        
        // Активация выбранной кнопки
        button.setBackgroundResource(R.drawable.sidebar_button_active)
    }

    private fun setupSidebarActions() {
        binding.btnHomeSidebar.setOnClickListener {
            setActiveButton(it)
            launchMainActivity()
            showSidebar()
        }
        
        binding.logoContainer.setOnClickListener {
            launchMainActivity()
            showSidebar()
        }
        
        binding.btnNavSidebar.setOnClickListener {
            setActiveButton(it)
            launchApp("ru.yandex.yandexnavi")
            hideSidebar()
        }
        
        binding.btnMusicSidebar.setOnClickListener {
            setActiveButton(it)
            launchApp("ru.yandex.music")
            hideSidebar()
        }
        
        binding.btnPhoneSidebar.setOnClickListener {
            setActiveButton(it)
            val intent = Intent(Intent.ACTION_DIAL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            hideSidebar()
        }
        
        binding.btnSettingsSidebar.setOnClickListener {
            setActiveButton(it)
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
        if (::sidebarView.isInitialized) {
            windowManager.removeView(sidebarView)
        }
    }
}
