package com.voyah.launcher

import android.content.Context
import android.content.SharedPreferences

/**
 * Менеджер настроек лаунчера.
 * Сохраняет/загружает все пользовательские настройки.
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ===== Настройки бокового меню =====

    /** Ширина sidebar в dp (60-200) */
    var sidebarWidth: Int
        get() = prefs.getInt(KEY_SIDEBAR_WIDTH, DEFAULT_SIDEBAR_WIDTH)
        set(value) = prefs.edit().putInt(KEY_SIDEBAR_WIDTH, value).apply()

    /** Сдвиг по X в dp (-50 до 100) */
    var sidebarOffsetX: Int
        get() = prefs.getInt(KEY_SIDEBAR_OFFSET_X, 0)
        set(value) = prefs.edit().putInt(KEY_SIDEBAR_OFFSET_X, value).apply()

    /** Сдвиг по Y в dp (-200 до 200) */
    var sidebarOffsetY: Int
        get() = prefs.getInt(KEY_SIDEBAR_OFFSET_Y, 0)
        set(value) = prefs.edit().putInt(KEY_SIDEBAR_OFFSET_Y, value).apply()

    /** Автоматически скрывать меню */
    var sidebarAutoHide: Boolean
        get() = prefs.getBoolean(KEY_SIDEBAR_AUTO_HIDE, true)
        set(value) = prefs.edit().putBoolean(KEY_SIDEBAR_AUTO_HIDE, value).apply()

    /** Время до автоскрытия в секундах (3-60) */
    var sidebarHideDelay: Int
        get() = prefs.getInt(KEY_SIDEBAR_HIDE_DELAY, 10)
        set(value) = prefs.edit().putInt(KEY_SIDEBAR_HIDE_DELAY, value).apply()

    /** Никогда не скрывать меню */
    var sidebarNeverHide: Boolean
        get() = prefs.getBoolean(KEY_SIDEBAR_NEVER_HIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_SIDEBAR_NEVER_HIDE, value).apply()

    /** Сбросить настройки sidebar к дефолтным значениям */
    fun resetSidebarSettings() {
        prefs.edit()
            .remove(KEY_SIDEBAR_WIDTH)
            .remove(KEY_SIDEBAR_OFFSET_X)
            .remove(KEY_SIDEBAR_OFFSET_Y)
            .remove(KEY_SIDEBAR_AUTO_HIDE)
            .remove(KEY_SIDEBAR_HIDE_DELAY)
            .remove(KEY_SIDEBAR_NEVER_HIDE)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "VoyahLauncherSettings"

        // Ключи настроек sidebar
        private const val KEY_SIDEBAR_WIDTH = "sidebar_width"
        private const val KEY_SIDEBAR_OFFSET_X = "sidebar_offset_x"
        private const val KEY_SIDEBAR_OFFSET_Y = "sidebar_offset_y"
        private const val KEY_SIDEBAR_AUTO_HIDE = "sidebar_auto_hide"
        private const val KEY_SIDEBAR_HIDE_DELAY = "sidebar_hide_delay"
        private const val KEY_SIDEBAR_NEVER_HIDE = "sidebar_never_hide"

        // Значения по умолчанию
        const val DEFAULT_SIDEBAR_WIDTH = 120
    }
}
