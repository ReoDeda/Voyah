package com.voyah.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.voyah.launcher.databinding.ActivityMainBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.concurrent.thread
import android.Manifest
import androidx.core.app.ActivityCompat
import android.os.Build
import android.media.session.MediaSessionManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.media.MediaMetadata
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.IntentFilter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager
    private var isTripModeActive = false

    private val alisaPackageCandidates = listOf(
        "ru.yandex.searchplugin",
        "ru.yandex.start",
        "ru.yandex.searchapp",
        "ru.yandex.search",
        "com.yandex.launcher",
        "com.yandex.browser",
        "ru.yandex.browser",
        "com.yandex.aliceapp"
    )
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hideRunnable = object : Runnable {
        override fun run() {
            hideSystemUI()
            hideHandler.postDelayed(this, 2000)
        }
    }

    private var activeMediaController: MediaController? = null

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateMediaWidget()
        }
    }

    private fun refreshWeather() {
        thread {
            try {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=55.75&longitude=37.62&current_weather=true")
                val connection = url.openConnection() as HttpURLConnection
                val response = connection.inputStream.bufferedReader().readText()
                val jsonObject = JSONObject(response)
                
                val current = jsonObject.getJSONObject("current_weather")
                val temp = current.getDouble("temperature").toInt()
                val wind = current.getDouble("windspeed")
                val code = current.getInt("weathercode")
                
                val desc = when(code) {
                    0 -> "Ясно"
                    1, 2, 3 -> "Облачно"
                    45, 48 -> "Туман"
                    51, 53, 55, 61, 63, 65 -> "Дождь"
                    71, 73, 75, 77, 85, 86 -> "Снег"
                    95, 96, 99 -> "Гроза"
                    else -> "Осадки"
                }
                
                runOnUiThread {
                    binding.topTemp.text = "$temp°C"
                    binding.tvWeatherTempLarge.text = "$temp°C"
                    binding.tvWeatherDesc.text = desc
                    binding.tvWeatherWind.text = "Ветер: $wind км/ч"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveInstalledPackage(
        candidates: List<String>,
        labelKeywords: List<String>,
        packagePrefix: String? = null,
        excludePackages: Set<String> = emptySet(),
        requireLaunchable: Boolean = false
    ): String? {
        for (pkg in candidates) {
            try {
                packageManager.getPackageInfo(pkg, 0)
                if (excludePackages.contains(pkg)) continue
                if (packagePrefix != null && !pkg.startsWith(packagePrefix)) continue
                if (requireLaunchable && packageManager.getLaunchIntentForPackage(pkg) == null) continue
                return pkg
            } catch (_: Exception) {
            }
        }

        val keywordsLower = labelKeywords.map { it.lowercase() }
        return try {
            val apps = packageManager.getInstalledApplications(0)
            apps.firstOrNull { appInfo ->
                val pkg = appInfo.packageName ?: return@firstOrNull false
                if (excludePackages.contains(pkg)) return@firstOrNull false
                if (packagePrefix != null && !pkg.startsWith(packagePrefix)) return@firstOrNull false
                if (requireLaunchable && packageManager.getLaunchIntentForPackage(pkg) == null) return@firstOrNull false
                val label = appInfo.loadLabel(packageManager)?.toString()?.lowercase() ?: return@firstOrNull false
                keywordsLower.any { label.contains(it) }
            }?.packageName
        } catch (_: Exception) {
            null
        }
    }

    private fun loadAppIcons() {
        val exclude = setOf(
            "ru.yandex.yandexnavi",
            "ru.yandex.weather",
            "ru.yandex.weather.plugin",
            "ru.yandex.music"
        )
        val apps = listOf(
            Triple(
                listOf("ru.yandex.yandexnavi"),
                listOf("yandex navigator", "яндекс навигатор"),
                Pair(binding.btnYandexNav, binding.imgNavNative)
            ),
            Triple(
                listOf("ru.yandex.weather", "ru.yandex.weather.plugin"),
                listOf("yandex weather", "яндекс погода", "погода", "weather"),
                Pair(binding.btnWeatherMain, binding.imgWeatherNative)
            ),
            Triple(
                listOf("ru.yandex.music"),
                listOf("yandex music", "яндекс музыка"),
                Pair(binding.btnYandexMusic, binding.imgMusicNative)
            ),
            Triple(
                alisaPackageCandidates,
                listOf("yandex start", "яндекс старт", "алиса", "alisa", "yandex", "яндекс", "браузер", "browser"),
                Pair(binding.btnAlisa, binding.imgAlisaNative)
            ),
            Triple(
                listOf("ru.ivi.client"),
                listOf("ivi", "иви"),
                Pair(binding.btnIvi, binding.imgIviNative)
            )
        )

        apps.forEach { (packages, keywords, views) ->
            val (container, iconView) = views
            val isAlisa = container == binding.btnAlisa
            val isWeather = container == binding.btnWeatherMain
            val installedPackage = if (isAlisa) {
                resolveInstalledPackage(
                    candidates = packages,
                    labelKeywords = keywords,
                    excludePackages = exclude,
                    requireLaunchable = false
                )
            } else if (isWeather) {
                resolveInstalledPackage(
                    candidates = packages,
                    labelKeywords = keywords,
                    requireLaunchable = false
                )
            } else {
                resolveInstalledPackage(
                    candidates = packages,
                    labelKeywords = keywords,
                    requireLaunchable = true
                )
            }
            if (installedPackage == null) {
                container.visibility = View.GONE
                return@forEach
            }

            try {
                val icon = packageManager.getApplicationIcon(installedPackage)
                iconView.setImageDrawable(icon)
                container.visibility = View.VISIBLE
                container.setOnClickListener { launchApp(installedPackage) }
            } catch (_: Exception) {
                container.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        hideSystemUI()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)

        applySidebarSettings()
        loadAppIcons()
        setupAppButtons()
        setupSidebarMenu()
        setupEmergencyControls()
        checkOverlayPermission()

        setupDraggableResizable(binding.nativeWeatherWidget, "weather")
        setupDraggableResizable(binding.nativeClockWidget, "clock")
        setupDraggableResizable(binding.nativeMusicWidget, "music")

        refreshWeather()
        setupMediaWidget()

        // Запускаем sidebar только если разрешение оверлея есть
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                val intent = Intent(this, OverlayService::class.java)
                intent.action = "SHOW_SIDEBAR"
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupWeather() {
        // Weather logic removed to match reference image UI
    }

    private fun setupMediaWidget() {
        binding.btnMusicPlayPause.setOnClickListener {
            val state = activeMediaController?.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                activeMediaController?.transportControls?.pause()
            } else {
                activeMediaController?.transportControls?.play()
            }
        }
        binding.btnMusicNext.setOnClickListener {
            activeMediaController?.transportControls?.skipToNext()
        }
        binding.btnMusicPrev.setOnClickListener {
            activeMediaController?.transportControls?.skipToPrevious()
        }

        val filter = IntentFilter().apply {
            addAction("com.voyah.launcher.MEDIA_LISTENER_CONNECTED")
            addAction("com.voyah.launcher.MEDIA_UPDATED")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mediaReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(mediaReceiver, filter)
        }
        
        // Request Notification Access if needed
        if (!isNotificationServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("Виджет музыки")
                .setMessage("Чтобы виджет музыки работал, разрешите лаунчеру доступ к уведомлениям.")
                .setPositiveButton("Разрешить") { _, _ ->
                    startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
                .setNegativeButton("Позже", null)
                .show()
        } else {
            updateMediaWidget()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    return true
                }
            }
        }
        return false
    }

    private val sessionCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaUI()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaUI()
        }
    }

    private fun updateMediaWidget() {
        if (!isNotificationServiceEnabled()) return

        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, MediaListenerService::class.java)
            val controllers = mediaSessionManager.getActiveSessions(componentName)

            activeMediaController?.unregisterCallback(sessionCallback)
            
            // Ищем активный плеер (кто сейчас играет музыку или у кого есть метаданные)
            val activeController = controllers.firstOrNull { 
                it.playbackState?.state == PlaybackState.STATE_PLAYING 
            } ?: controllers.firstOrNull { 
                it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null 
            } ?: controllers.firstOrNull()

            if (activeController != null) {
                activeMediaController = activeController
                activeMediaController?.registerCallback(sessionCallback)
                binding.nativeMusicWidget.visibility = View.VISIBLE
                updateMediaUI()
            } else {
                activeMediaController = null
                binding.nativeMusicWidget.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMediaUI() {
        runOnUiThread {
            val controller = activeMediaController
            if (controller == null) {
                binding.nativeMusicWidget.visibility = View.GONE
                return@runOnUiThread
            }
            
            val metadata = controller.metadata
            val state = controller.playbackState

            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            
            // Если названия трека нет, значит это пустая сессия (например системные звуки)
            if (title.isNullOrEmpty()) {
                binding.nativeMusicWidget.visibility = View.GONE
                return@runOnUiThread
            }

            binding.nativeMusicWidget.visibility = View.VISIBLE
            binding.musicTrackTitle.text = title
            binding.musicTrackArtist.text = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Неизвестный исполнитель"
            
            val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            if (bitmap != null) {
                binding.musicAlbumArt.setImageBitmap(bitmap)
            } else {
                binding.musicAlbumArt.setImageResource(android.R.drawable.ic_media_play)
            }

            if (state?.state == PlaybackState.STATE_PLAYING) {
                binding.btnMusicPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                binding.btnMusicPlayPause.setImageResource(android.R.drawable.ic_media_play)
            }
        }
    }

    private fun setupSidebarMenu() {
        // 1. Машина - информация об устройстве
        binding.btnMainCar.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Информация об устройстве", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Домик - главный экран (просто остаемся здесь)
        binding.btnMainHome.setOnClickListener {
            // Уже на главном экране
            hideSystemUI()
        }

        // 3. Стрелка - навигация
        binding.btnMainNav.setOnClickListener {
            val pkg = resolveInstalledPackage(
                candidates = listOf("ru.yandex.yandexnavi"),
                labelKeywords = listOf("yandex navigator", "яндекс навигатор")
            )
            if (pkg != null) launchApp(pkg) else Toast.makeText(this, "Установите Яндекс.Навигатор", Toast.LENGTH_LONG).show()
        }

        // 4. Нота - музыка
        binding.btnMainMusic.setOnClickListener {
            val pkg = resolveInstalledPackage(
                candidates = listOf("ru.yandex.music"),
                labelKeywords = listOf("yandex music", "яндекс музыка")
            )
            if (pkg != null) launchApp(pkg) else Toast.makeText(this, "Установите Яндекс.Музыку", Toast.LENGTH_LONG).show()
        }

        // 5. Телефон - звонки
        binding.btnMainPhone.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Телефон недоступен", Toast.LENGTH_SHORT).show()
            }
        }

        // 6. Квадратики - все приложения / настройки
        binding.btnMainSettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Настройки недоступны", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupEmergencyControls() {
        binding.btnMinimize.setOnClickListener {
            moveTaskToBack(true)
        }

        binding.btnExit.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        binding.btnInstaller.setOnClickListener {
            startActivity(Intent(this, InstallerActivity::class.java))
        }

        // Новая 4-я кнопка - Настройки
        binding.btnSettingsMain.setOnClickListener {
            showSettingsDialog()
        }
    }

    /**
     * Применяет сохранённые настройки sidebar к layout.
     */
    private fun applySidebarSettings() {
        val density = resources.displayMetrics.density
        val width = (settingsManager.sidebarWidth * density).toInt()
        val offsetX = (settingsManager.sidebarOffsetX * density).toInt()
        val offsetY = (settingsManager.sidebarOffsetY * density).toInt()

        // Меняем ширину sidebar_spacer
        val params = binding.sidebarSpacer.layoutParams
        params.width = width
        binding.sidebarSpacer.layoutParams = params

        // Применяем сдвиг
        binding.sidebarSpacer.translationX = offsetX.toFloat()
        binding.sidebarSpacer.translationY = offsetY.toFloat()
    }

    /**
     * Показывает диалог настроек с управлением sidebar.
     */
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        val seekbarSize = dialogView.findViewById<android.widget.SeekBar>(R.id.seekbar_sidebar_size)
        val tvSizeValue = dialogView.findViewById<TextView>(R.id.tv_sidebar_size_value)
        val seekbarX = dialogView.findViewById<android.widget.SeekBar>(R.id.seekbar_sidebar_x)
        val tvXValue = dialogView.findViewById<TextView>(R.id.tv_sidebar_x_value)
        val seekbarY = dialogView.findViewById<android.widget.SeekBar>(R.id.seekbar_sidebar_y)
        val tvYValue = dialogView.findViewById<TextView>(R.id.tv_sidebar_y_value)
        val checkboxAutoHide = dialogView.findViewById<android.widget.CheckBox>(R.id.checkbox_auto_hide)
        val checkboxNeverHide = dialogView.findViewById<android.widget.CheckBox>(R.id.checkbox_never_hide)
        val seekbarHideDelay = dialogView.findViewById<android.widget.SeekBar>(R.id.seekbar_hide_delay)
        val tvHideDelayValue = dialogView.findViewById<TextView>(R.id.tv_hide_delay_value)
        val layoutHideDelay = dialogView.findViewById<View>(R.id.layout_hide_delay)
        val btnReset = dialogView.findViewById<android.widget.Button>(R.id.btn_reset_sidebar)

        // Загружаем текущие значения
        seekbarSize.progress = settingsManager.sidebarWidth
        tvSizeValue.text = "${settingsManager.sidebarWidth} dp"

        seekbarX.progress = settingsManager.sidebarOffsetX
        tvXValue.text = "${settingsManager.sidebarOffsetX} dp"

        seekbarY.progress = settingsManager.sidebarOffsetY
        tvYValue.text = "${settingsManager.sidebarOffsetY} dp"

        checkboxAutoHide.isChecked = settingsManager.sidebarAutoHide
        checkboxNeverHide.isChecked = settingsManager.sidebarNeverHide

        seekbarHideDelay.progress = settingsManager.sidebarHideDelay
        tvHideDelayValue.text = "${settingsManager.sidebarHideDelay} сек"

        updateHideDelayVisibility(checkboxAutoHide, checkboxNeverHide, seekbarHideDelay, layoutHideDelay)

        // Обработчики SeekBar
        seekbarSize.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvSizeValue.text = "$progress dp"
                settingsManager.sidebarWidth = progress
                applySidebarSettings()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekbarX.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvXValue.text = "$progress dp"
                settingsManager.sidebarOffsetX = progress
                applySidebarSettings()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekbarY.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvYValue.text = "$progress dp"
                settingsManager.sidebarOffsetY = progress
                applySidebarSettings()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekbarHideDelay.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvHideDelayValue.text = "$progress сек"
                settingsManager.sidebarHideDelay = progress
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Чекбоксы (взаимоисключающие)
        checkboxAutoHide.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.sidebarAutoHide = isChecked
            if (isChecked) {
                checkboxNeverHide.isChecked = false
                settingsManager.sidebarNeverHide = false
            }
            updateHideDelayVisibility(checkboxAutoHide, checkboxNeverHide, seekbarHideDelay, layoutHideDelay)
        }

        checkboxNeverHide.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.sidebarNeverHide = isChecked
            if (isChecked) {
                checkboxAutoHide.isChecked = false
                settingsManager.sidebarAutoHide = false
            }
            updateHideDelayVisibility(checkboxAutoHide, checkboxNeverHide, seekbarHideDelay, layoutHideDelay)
        }

        // Кнопка сброса
        btnReset.setOnClickListener {
            settingsManager.resetSidebarSettings()
            seekbarSize.progress = settingsManager.sidebarWidth
            seekbarX.progress = settingsManager.sidebarOffsetX
            seekbarY.progress = settingsManager.sidebarOffsetY
            checkboxAutoHide.isChecked = settingsManager.sidebarAutoHide
            checkboxNeverHide.isChecked = settingsManager.sidebarNeverHide
            seekbarHideDelay.progress = settingsManager.sidebarHideDelay
            applySidebarSettings()
            Toast.makeText(this, "Настройки меню сброшены", Toast.LENGTH_SHORT).show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Готово") { dialog, _ ->
                dialog.dismiss()
                hideSystemUI()
            }
            .setOnDismissListener {
                hideSystemUI()
                // Перезапускаем OverlayService с новыми настройками
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                        stopService(Intent(this, OverlayService::class.java))
                        val intent = Intent(this, OverlayService::class.java)
                        intent.action = "SHOW_SIDEBAR"
                        startService(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .show()
    }

    private fun updateHideDelayVisibility(
        autoHide: android.widget.CheckBox,
        neverHide: android.widget.CheckBox,
        delaySeekbar: android.widget.SeekBar,
        delayLayout: View
    ) {
        val showDelay = autoHide.isChecked && !neverHide.isChecked
        delayLayout.visibility = if (showDelay) View.VISIBLE else View.GONE
        delaySeekbar.visibility = if (showDelay) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        hideHandler.post(hideRunnable)
        loadAppIcons() // Обновляем иконки (Алиса появится после установки)
        refreshWeather()
        updateMediaWidget()
    }

    override fun onPause() {
        super.onPause()
        hideHandler.removeCallbacks(hideRunnable)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder(this)
                    .setTitle("Настройка лаунчера")
                    .setMessage("Для работы режима разделения экрана необходимо разрешить отображение поверх других окон.")
                    .setCancelable(false)
                    .setPositiveButton("Настроить") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, 123)
                    }
                    .show()
            } else {
                startService(Intent(this, OverlayService::class.java))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 123) {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverlayService::class.java))
            } else {
                Toast.makeText(this, "Разрешение на отображение поверх окон не предоставлено", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun launchApp(packageName: String) {
        if (alisaPackageCandidates.contains(packageName)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
                return
            }
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            val displayMetrics = resources.displayMetrics
            val sidebarWidthPx = (120 * displayMetrics.density).toInt()
            val screenWidthPx = displayMetrics.widthPixels
            val screenHeightPx = displayMetrics.heightPixels
            
            val options = android.app.ActivityOptions.makeBasic()
            
            if (isTripModeActive) {
                // В режиме "За рулем" открываем всё в ПРАВОЙ части экрана (LAUNCH_ADJACENT)
                intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Правая половина экрана (от середины до конца)
                    options.launchBounds = android.graphics.Rect(screenWidthPx / 2, 0, screenWidthPx, screenHeightPx)
                }
            } else {
                // Обычный режим - открываем от сайдбара
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    options.launchBounds = android.graphics.Rect(sidebarWidthPx, 0, screenWidthPx, screenHeightPx)
                }
            }
            
            startActivity(intent, options.toBundle())
        } else {
            if (isPackageInstalled(packageName)) {
                Toast.makeText(this, "Нет ярлыка запуска для приложения", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Приложение не установлено", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun launchTripMode() {
        val navPackage = "ru.yandex.yandexnavi"
        val navIntent = packageManager.getLaunchIntentForPackage(navPackage)

        if (navIntent != null) {
            isTripModeActive = true
            hideSystemUI()

            // 1. Открываем Навигатор в ЛЕВОЙ части
            navIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            val displayMetrics = resources.displayMetrics
            val screenWidthPx = displayMetrics.widthPixels
            val screenHeightPx = displayMetrics.heightPixels
            
            val navOptions = android.app.ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Левая половина (с учетом сайдбара)
                navOptions.launchBounds = android.graphics.Rect(120, 0, screenWidthPx / 2, screenHeightPx)
            }
            startActivity(navIntent, navOptions.toBundle())

            // 2. Возвращаем Лаунчер в ПРАВУЮ часть через небольшую паузу
            binding.root.postDelayed({
                val selfIntent = Intent(this, MainActivity::class.java)
                selfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                val selfOptions = android.app.ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    selfOptions.launchBounds = android.graphics.Rect(screenWidthPx / 2, 0, screenWidthPx, screenHeightPx)
                }
                startActivity(selfIntent, selfOptions.toBundle())
                hideSystemUI()
            }, 500)

            Toast.makeText(this, "Режим «За рулем» активен: Навигатор слева", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Установите Яндекс.Навигатор", Toast.LENGTH_LONG).show()
        }
    }


    private fun setupDraggableResizable(view: View, prefKey: String) {
        val prefs = getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
        
        // Загружаем сохраненные позиции
        val savedX = prefs.getFloat("${prefKey}_x", -1f)
        val savedY = prefs.getFloat("${prefKey}_y", -1f)
        val savedW = prefs.getInt("${prefKey}_w", -1)
        val savedH = prefs.getInt("${prefKey}_h", -1)

        view.post {
            if (savedX != -1f) view.x = savedX
            if (savedY != -1f) view.y = savedY
            if (savedW != -1) {
                val lp = view.layoutParams
                lp.width = savedW
                lp.height = savedH
                view.layoutParams = lp
            }
        }

        var isEditMode = false
        var dX = 0f
        var dY = 0f
        var startWidth = 0
        var startHeight = 0
        var isResizing = false

        view.setOnLongClickListener {
            isEditMode = !isEditMode
            
            if (isEditMode) {
                view.setBackgroundResource(R.drawable.glass_card_bg)
                view.alpha = 0.8f
            } else {
                view.setBackgroundResource(android.R.color.transparent)
                view.alpha = 1.0f
            }
            true
        }

        view.setOnTouchListener { v, event ->
            if (!isEditMode) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Если тапнули в правый нижний угол (область ресайза)
                    if (event.x >= v.width - 100 && event.y >= v.height - 100) {
                        isResizing = true
                        startWidth = v.width
                        startHeight = v.height
                        dX = event.rawX
                        dY = event.rawY
                    } else {
                        isResizing = false
                        dX = v.x - event.rawX
                        dY = v.y - event.rawY
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isResizing) {
                        val newWidth = maxOf(250, startWidth + (event.rawX - dX).toInt())
                        val newHeight = maxOf(150, startHeight + (event.rawY - dY).toInt())
                        val lp = v.layoutParams
                        lp.width = newWidth
                        lp.height = newHeight
                        v.layoutParams = lp
                    } else {
                        v.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    prefs.edit()
                        .putFloat("${prefKey}_x", v.x)
                        .putFloat("${prefKey}_y", v.y)
                        .putInt("${prefKey}_w", v.layoutParams.width)
                        .putInt("${prefKey}_h", v.layoutParams.height)
                        .apply()
                }
            }
            true // Перехватываем касание в режиме редактирования
        }
    }

    private fun setupAppButtons() {
        binding.btnYandexNav.setOnClickListener {
            val pkg = resolveInstalledPackage(
                candidates = listOf("ru.yandex.yandexnavi"),
                labelKeywords = listOf("yandex navigator", "яндекс навигатор")
            )
            if (pkg != null) launchApp(pkg) else Toast.makeText(this, "Установите Яндекс.Навигатор", Toast.LENGTH_LONG).show()
        }

        binding.nativeWeatherWidget.setOnClickListener {
            // Открываем встроенный браузер с Яндекс.Погодой
            val webView = WebView(this@MainActivity).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                loadUrl("https://yandex.ru/pogoda/")
            }
            
            val dialog = AlertDialog.Builder(this@MainActivity, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                .setView(webView)
                .setPositiveButton("Закрыть") { d, _ -> d.dismiss() }
                .create()
            
            dialog.show()
        }

        binding.nativeClockWidget.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Настройки времени недоступны", Toast.LENGTH_SHORT).show()
            }
        }

        // Если нужны были клики по карточкам (Music, Nav), нужно их вернуть в layout
        // binding.cardMusic.setOnClickListener { ... }
        // binding.cardNav.setOnClickListener { ... }



        binding.btnYandexMusic.setOnClickListener {
            val pkg = resolveInstalledPackage(
                candidates = listOf("ru.yandex.music"),
                labelKeywords = listOf("yandex music", "яндекс музыка")
            )
            if (pkg != null) launchApp(pkg) else Toast.makeText(this, "Установите Яндекс.Музыку", Toast.LENGTH_LONG).show()
        }

        binding.btnAlisa.setOnClickListener {
            val pkg = resolveInstalledPackage(
                candidates = alisaPackageCandidates,
                labelKeywords = listOf("yandex start", "яндекс старт", "алиса", "alisa")
            )
            if (pkg != null) launchApp(pkg) else Toast.makeText(this, "Установите Алису", Toast.LENGTH_LONG).show()
        }

        binding.btnIvi.setOnClickListener {
            val pkg = resolveInstalledPackage(
                candidates = listOf("ru.ivi.client"),
                labelKeywords = listOf("ivi", "иви")
            )
            if (pkg != null) launchApp(pkg) else Toast.makeText(this, "Установите Иви", Toast.LENGTH_LONG).show()
        }

        binding.btnTripMode.setOnClickListener {
            launchTripMode()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeMediaController?.unregisterCallback(sessionCallback)
        try {
            unregisterReceiver(mediaReceiver)
        } catch (e: Exception) {}
    }

    override fun onBackPressed() {
        // Do nothing
    }
}
