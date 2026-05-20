package com.voyah.launcher

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import com.voyah.launcher.databinding.OverlaySidebarBinding

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var sidebarView: View
    private lateinit var binding: OverlaySidebarBinding
    private lateinit var params: WindowManager.LayoutParams
    private var isHidden = false
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hideSidebar() }
    
    // Track active button
    private enum class ActiveButton {
        HOME, CAR, NAV, MUSIC, PHONE, SETTINGS
    }
    private var currentActive = ActiveButton.CAR

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
            (160 * resources.displayMetrics.density).toInt(),
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
        
        // Start with sidebar visible
        resetHideTimer()
        
        // Fullscreen immersive mode
        sidebarView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // Animate entrance
        animateEntrance()
    }

    private fun animateEntrance() {
        binding.sidebarPanel.alpha = 0f
        binding.sidebarPanel.scaleX = 0.8f
        binding.sidebarPanel.scaleY = 0.8f
        
        binding.sidebarPanel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
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
                if (isHidden && distanceX < -3) {
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
                if (isHidden && velocityX > 150) {
                    showSidebar()
                }
                return true
            }
        })

        binding.touchArea.setOnTouchListener { _, event ->
            val handled = gestureDetector.onTouchEvent(event)
            if (isHidden) true else handled
        }

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
        animateSidebarSlide(targetX)
        
        // Animate panel appearance
        binding.sidebarPanel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator(0.8f))
            .start()
        
        resetHideTimer()
    }

    private fun hideSidebar() {
        if (isHidden) return
        isHidden = true
        
        // Animate panel disappearance
        binding.sidebarPanel.animate()
            .alpha(0.7f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.sidebarHandle.visibility = View.VISIBLE
                animateHandleAppearance()
            }
            .start()
        
        val targetX = -(130 * resources.displayMetrics.density).toInt()
        animateSidebarSlide(targetX)
    }

    private fun animateHandleAppearance() {
        binding.sidebarHandle.alpha = 0f
        binding.sidebarHandle.scaleX = 0.5f
        binding.sidebarHandle.animate()
            .alpha(0.8f)
            .scaleX(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun animateSidebarSlide(targetX: Int) {
        val startX = params.x
        val animator = ValueAnimator.ofInt(startX, targetX)
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            windowManager.updateViewLayout(sidebarView, params)
        }
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun resetHideTimer() {
        hideHandler.removeCallbacks(autoHideRunnable)
        hideHandler.postDelayed(autoHideRunnable, 12000) // 12 seconds
    }

    private fun setActiveButton(button: ActiveButton) {
        if (currentActive == button) return
        
        // Hide all indicators
        binding.indicatorCar.visibility = View.GONE
        binding.indicatorNav.visibility = View.GONE
        binding.indicatorMusic.visibility = View.GONE
        binding.indicatorPhone.visibility = View.GONE
        binding.indicatorSettings.visibility = View.GONE
        
        // Reset all buttons to inactive background
        binding.btnHomeSidebar.setBackgroundResource(R.drawable.sidebar_item_inactive)
        binding.btnCarSidebar.setBackgroundResource(R.drawable.sidebar_item_inactive)
        binding.btnNavSidebar.setBackgroundResource(R.drawable.sidebar_item_inactive)
        binding.btnMusicSidebar.setBackgroundResource(R.drawable.sidebar_item_inactive)
        binding.btnPhoneSidebar.setBackgroundResource(R.drawable.sidebar_item_inactive)
        binding.btnSettingsSidebar.setBackgroundResource(R.drawable.sidebar_item_inactive)
        
        // Set new active button
        currentActive = button
        
        when (button) {
            ActiveButton.HOME -> {
                binding.btnHomeSidebar.setBackgroundResource(R.drawable.sidebar_item_active)
            }
            ActiveButton.CAR -> {
                binding.btnCarSidebar.setBackgroundResource(R.drawable.sidebar_item_active)
                animateIndicator(binding.indicatorCar)
            }
            ActiveButton.NAV -> {
                binding.btnNavSidebar.setBackgroundResource(R.drawable.sidebar_item_active)
                animateIndicator(binding.indicatorNav)
            }
            ActiveButton.MUSIC -> {
                binding.btnMusicSidebar.setBackgroundResource(R.drawable.sidebar_item_active)
                animateIndicator(binding.indicatorMusic)
            }
            ActiveButton.PHONE -> {
                binding.btnPhoneSidebar.setBackgroundResource(R.drawable.sidebar_item_active)
                animateIndicator(binding.indicatorPhone)
            }
            ActiveButton.SETTINGS -> {
                binding.btnSettingsSidebar.setBackgroundResource(R.drawable.sidebar_item_active)
                animateIndicator(binding.indicatorSettings)
            }
        }
    }

    private fun animateIndicator(indicator: View) {
        indicator.visibility = View.VISIBLE
        indicator.alpha = 0f
        indicator.scaleY = 0.5f
        
        indicator.animate()
            .alpha(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }

    private fun animateButtonClick(button: View) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.92f),
                ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.92f)
            )
            duration = 100
        }
        
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(button, "scaleX", 0.92f, 1f),
                ObjectAnimator.ofFloat(button, "scaleY", 0.92f, 1f)
            )
            duration = 150
            interpolator = OvershootInterpolator()
        }
        
        scaleDown.doOnEnd {
            scaleUp.start()
        }
        scaleDown.start()
    }

    private fun setupSidebarActions() {
        binding.btnHomeSidebar.setOnClickListener {
            animateButtonClick(it)
            setActiveButton(ActiveButton.HOME)
            launchMainActivity()
            showSidebar()
        }
        
        binding.logoContainer.setOnClickListener {
            animateButtonClick(it)
            setActiveButton(ActiveButton.HOME)
            launchMainActivity()
            showSidebar()
        }
        
        binding.btnCarSidebar.setOnClickListener {
            animateButtonClick(it)
            setActiveButton(ActiveButton.CAR)
            val intent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            resetHideTimer()
        }
        
        binding.btnNavSidebar.setOnClickListener {
            animateButtonClick(it)
            setActiveButton(ActiveButton.NAV)
            launchApp("ru.yandex.yandexnavi")
            hideSidebar()
        }
        
        binding.btnMusicSidebar.setOnClickListener {
            animateButtonClick(it)
            setActiveButton(ActiveButton.MUSIC)
            launchApp("ru.yandex.music")
            hideSidebar()
        }
        
        binding.btnPhoneSidebar.setOnClickListener {
            animateButtonClick(it)
            setActiveButton(ActiveButton.PHONE)
            val intent = Intent(Intent.ACTION_DIAL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            hideSidebar()
        }
        
        binding.btnSettingsSidebar.setOnClickListener {
            animateButtonClick(it)
            setActiveButton(ActiveButton.SETTINGS)
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
            val sidebarWidthPx = (130 * displayMetrics.density).toInt()
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
