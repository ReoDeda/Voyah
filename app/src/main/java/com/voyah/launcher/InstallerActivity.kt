package com.voyah.launcher

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.snackbar.Snackbar
import com.voyah.launcher.databinding.ActivityInstallerBinding
import java.io.File
import java.util.*

import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class InstallerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallerBinding
    private var currentFile: String = ""
    private val alisaPackageCandidates = listOf(
        "ru.yandex.searchplugin",
        "ru.yandex.start",
        "ru.yandex.searchapp",
        "ru.yandex.search",
        "com.yandex.launcher",
        "com.yandex.browser",
        "ru.yandex.browser",
        "com.yandex.aliceapp",
        "com.yandex.aliceapp.AliceApplication"
    )
    private val weatherPackageCandidates = listOf(
        "ru.yandex.weather",
        "ru.yandex.weather.plugin"
    )
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hideRunnable = object : Runnable {
        override fun run() {
            hideSystemUI()
            hideHandler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        binding = ActivityInstallerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnInstallNav.setOnClickListener {
            val installedPkg = resolveInstalledPackage(
                candidates = listOf("ru.yandex.yandexnavi"),
                labelKeywords = listOf("yandex navigator", "яндекс навигатор"),
                requireLaunchable = false
            )
            if (installedPkg != null) {
                uninstallApp(installedPkg)
            } else {
                currentFile = "yandexnavi.apk"
                startDirectDownload("https://drive.usercontent.google.com/download?id=1xgEXF857kisWLlM5DSb29fkwK78Wzbb6&export=download&authuser=0&confirm=t&uuid=55bc22a6-22e6-4dd7-a83a-a74d1ac1602d&at=ALBwUgnmJbd_od-CPW_y_ZBqO10s%3A1779114175597", currentFile)
            }
        }

        binding.btnInstallMusic.setOnClickListener {
            val installedPkg = resolveInstalledPackage(
                candidates = listOf("ru.yandex.music"),
                labelKeywords = listOf("yandex music", "яндекс музыка"),
                requireLaunchable = false
            )
            if (installedPkg != null) {
                uninstallApp(installedPkg)
            } else {
                currentFile = "muzykaya.apk"
                startDirectDownload("https://downloader.disk.yandex.ru/disk/3469b3b4af46848b2213b57375d4f0edebf873ad880cd0f0d317514ed4282792/6a0ba2c7/RNxA3PaDDR6KcGS6Jzfz4em1aidE_YIzSuyO8EP14V59XEwGTGYjUI4JsfrwT4LOqlVexvZBEfhqrzyH5HZFYw%3D%3D?uid=0&filename=yandexmusic.apk&disposition=attachment&hash=kO32XGENt1CaaxwhYm40hh8Nj0tRCb8qG%2BvFx8XYuzBbEE8HSK2rM8L7liFRhvHjq/J6bpmRyOJonT3VoXnDag%3D%3D&limit=0&content_type=application%2Fvnd.android.package-archive&owner_uid=320264845&fsize=63173320&hid=73233add0bf00e6b1a8ae2fd0e82244e&media_type=unknown&tknv=v3&is_direct_zip_experiment=1", currentFile)
            }
        }

        binding.btnInstallWeather.setOnClickListener {
            val installedPkg = resolveInstalledPackage(
                candidates = weatherPackageCandidates,
                labelKeywords = listOf("yandex weather", "яндекс погода", "погода", "weather"),
                requireLaunchable = false
            )
            if (installedPkg != null) {
                uninstallApp(installedPkg)
            } else {
                currentFile = "pogodayandex.apk"
                startDirectDownload("https://downloader.disk.yandex.ru/disk/34a6150a9c9f299f8619e1eb165ef77c89b84e8395981a61038ab120f71929c9/6a0b9b96/yadwu_FcPqcVmwDFrEjJCWnnDmYtcWjwldXttlPupYNUJz3AOR5vaClRVBfbe88xXkWQFNplTRXiO5ptSI5HQQ%3D%3D?uid=0&filename=pogodayandex.apk&disposition=attachment&hash=jNfRpQX5IIinjuF5o1I15okCTn7oVuFykqaY5rFcoT680OMQ7DRGuinz%2BAWq9tBiq/J6bpmRyOJonT3VoXnDag%3D%3D&limit=0&content_type=application%2Fvnd.android.package-archive&owner_uid=320264845&fsize=46950242&hid=6efe411145663df3eeff48d24814c00b&media_type=unknown&tknv=v3&is_direct_zip_experiment=1", currentFile)
            }
        }

        binding.btnInstallYoutube.setOnClickListener {
            val pkg = "com.google.android.youtube"
            if (isAppInstalled(pkg)) {
                uninstallApp(pkg)
            } else {
                Toast.makeText(this, "Ссылка для YouTube пока не настроена", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnInstallAlisa.setOnClickListener {
            val exclude = setOf(
                "ru.yandex.yandexnavi",
                "ru.yandex.weather",
                "ru.yandex.weather.plugin",
                "ru.yandex.music"
            )
            val installedPkg = resolveInstalledPackage(
                candidates = alisaPackageCandidates,
                labelKeywords = listOf("yandex start", "яндекс старт", "алиса", "alisa", "yandex", "яндекс", "браузер", "browser"),
                excludePackages = exclude,
                requireLaunchable = false
            )
            if (installedPkg != null) {
                uninstallApp(installedPkg)
            } else {
                currentFile = "yandex_alisa.apk"
                startDirectDownload("https://downloader.disk.yandex.ru/disk/6e1ce0e910aecd3b28c4fce54ff7c6978a8d90cea1ae46f878ba64d932650a52/6a0b8349/RNxA3PaDDR6KcGS6Jzfz4esmjY75oNHx-Leb3S-6w8DIEHK0HCQzu_UTxJ90QrcxqFHUDf64S_OdGPDghAy5Ig%3D%3D?uid=0&filename=alisa.apk&disposition=attachment&hash=llxzzFyN2jNcUeXDSsgJnNvfPkW7fVJigoJ0RrSupNmospZNJlJI3gGJ/Javawe3q/J6bpmRyOJonT3VoXnDag%3D%3D&limit=0&content_type=application%2Fvnd.android.package-archive&owner_uid=320264845&fsize=133554377&hid=a10d5f9e6377265950b09161f3f4b586&media_type=unknown&tknv=v3&is_direct_zip_experiment=1", currentFile)
            }
        }
        
        updateButtonsState()
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
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

    private fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Удаление недоступно на этом устройстве: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateButtonsState() {
        val exclude = setOf(
            "ru.yandex.yandexnavi",
            "ru.yandex.weather",
            "ru.yandex.weather.plugin",
            "ru.yandex.music"
        )
        binding.btnInstallNav.text =
            if (resolveInstalledPackage(listOf("ru.yandex.yandexnavi"), listOf("yandex navigator", "яндекс навигатор"), requireLaunchable = false) != null) "Удалить" else "Установить"
        binding.btnInstallMusic.text =
            if (resolveInstalledPackage(listOf("ru.yandex.music"), listOf("yandex music", "яндекс музыка"), requireLaunchable = false) != null) "Удалить" else "Установить"
        binding.btnInstallWeather.text =
            if (
                resolveInstalledPackage(
                    candidates = weatherPackageCandidates,
                    labelKeywords = listOf("yandex weather", "яндекс погода", "погода", "weather"),
                    requireLaunchable = false
                ) != null
            ) "Удалить" else "Установить"
        binding.btnInstallYoutube.text = if (isAppInstalled("com.google.android.youtube")) "Удалить" else "Установить"
        binding.btnInstallAlisa.text =
            if (
                resolveInstalledPackage(
                    candidates = alisaPackageCandidates,
                    labelKeywords = listOf("yandex start", "яндекс старт", "алиса", "alisa", "yandex", "яндекс", "браузер", "browser"),
                    excludePackages = exclude,
                    requireLaunchable = false
                ) != null
            ) "Удалить" else "Установить"
    }

    override fun onResume() {
        super.onResume()
        updateButtonsState()
        hideSystemUI()
        hideHandler.post(hideRunnable)
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
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    private fun startDirectDownload(url: String, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse("package:$packageName")))
                showModernToast("Разрешите установку и вернитесь", false)
                return
            }
        }

        val progressBar = when (fileName) {
            "yandexnavi.apk" -> binding.progressNav
            "yandexweatherplugin.apk" -> binding.progressWeather
            "pogodayandex.apk" -> binding.progressWeather
            "yandex_alisa.apk" -> binding.progressAlisa
            else -> binding.progressMusic
        }
        val statusText = when (fileName) {
            "yandexnavi.apk" -> binding.statusNav
            "yandexweatherplugin.apk" -> binding.statusWeather
            "pogodayandex.apk" -> binding.statusWeather
            "yandex_alisa.apk" -> binding.statusAlisa
            else -> binding.statusMusic
        }
        val button = when (fileName) {
            "yandexnavi.apk" -> binding.btnInstallNav
            "yandexweatherplugin.apk" -> binding.btnInstallWeather
            "pogodayandex.apk" -> binding.btnInstallWeather
            "yandex_alisa.apk" -> binding.btnInstallAlisa
            else -> binding.btnInstallMusic
        }

        progressBar.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE
        progressBar.progress = 0
        statusText.text = "Начинаю загрузку..."
        button.isEnabled = false

        thread {
            try {
                var currentUrl = url
                var connection: HttpURLConnection
                var responseCode: Int
                var redirects = 0
                val maxRedirects = 5

                // Цикл для обработки редиректов (важно для Google Drive)
                do {
                    val urlObj = URL(currentUrl)
                    connection = urlObj.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connect()
                    responseCode = connection.responseCode

                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                        responseCode == 307 || responseCode == 308) {
                        currentUrl = connection.getHeaderField("Location")
                        redirects++
                    } else {
                        break
                    }
                } while (redirects < maxRedirects)

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread { 
                        statusText.text = "Ошибка сервера: ${connection.responseCode}"
                        button.isEnabled = true
                    }
                    return@thread
                }

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val outputFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                val output = FileOutputStream(outputFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        runOnUiThread {
                            progressBar.progress = progress
                            statusText.text = "Загружено: $progress%"
                        }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                runOnUiThread {
                    statusText.text = "Загрузка завершена. Запуск установки..."
                    installApk(fileName)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    val userFriendlyError = when {
                        e is java.net.UnknownHostException -> "Нет интернета в эмуляторе (DNS ошибка)"
                        e is java.net.SocketTimeoutException -> "Время ожидания истекло (медленная сеть)"
                        else -> "Ошибка: ${e.message}"
                    }
                    statusText.text = userFriendlyError
                    button.isEnabled = true
                    
                    // Предлагаем открыть в браузере как запасной вариант
                    AlertDialog.Builder(this@InstallerActivity)
                        .setTitle("Проблема с сетью")
                        .setMessage("Эмулятор не видит интернет. Попробовать открыть ссылку в браузере?")
                        .setPositiveButton("Да") { _: DialogInterface, _: Int ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        }
                        .setNegativeButton("Нет", null)
                        .show()
                }
            }
        }
    }

    private fun installApk(fileName: String) {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
            
            val statusText = when (fileName) {
                "yandexnavi.apk" -> binding.statusNav
                "yandexweatherplugin.apk" -> binding.statusWeather
                "pogodayandex.apk" -> binding.statusWeather
                "yandex_alisa.apk" -> binding.statusAlisa
                else -> binding.statusMusic
            }
            val progressBar = when (fileName) {
                "yandexnavi.apk" -> binding.progressNav
                "yandexweatherplugin.apk" -> binding.progressWeather
                "pogodayandex.apk" -> binding.progressWeather
                "yandex_alisa.apk" -> binding.progressAlisa
                else -> binding.progressMusic
            }
            val button = when (fileName) {
                "yandexnavi.apk" -> binding.btnInstallNav
                "yandexweatherplugin.apk" -> binding.btnInstallWeather
                "pogodayandex.apk" -> binding.btnInstallWeather
                "yandex_alisa.apk" -> binding.btnInstallAlisa
                else -> binding.btnInstallMusic
            }

            statusText.text = "Установка запущена..."

            binding.root.postDelayed({
                if (file.exists()) {
                    file.delete()
                    showModernToast("Временный файл $fileName удален", true)
                    statusText.text = "Готово. Файл очищен."
                    button.isEnabled = true
                    progressBar.visibility = View.GONE
                }
            }, 15000) 
        }
    }

    private fun showModernToast(message: String, isSuccess: Boolean) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (isSuccess) {
            snackbar.setBackgroundTint(resources.getColor(android.R.color.holo_green_dark, null))
        }
        snackbar.show()
    }
}
