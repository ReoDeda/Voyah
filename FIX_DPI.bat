@echo off
chcp 65001 >nul
color 0C
title 🔧 Исправление DPI эмулятора Voyah

cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║            🔧 ИСПРАВЛЕНИЕ DPI ЭМУЛЯТОРА                        ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo ⚠️  ПРОБЛЕМА: Android Studio выставляет 480 DPI
echo ✅  РЕШЕНИЕ: Нужно изменить на 160 DPI для Voyah Passion
echo.
echo ────────────────────────────────────────────────────────────────
echo.
echo 📱 Voyah Passion - Центральный экран:
echo    • Размер: 12.3 дюйма
echo    • Разрешение: 1920 x 720
echo    • Правильный DPI: 160
echo.
echo ────────────────────────────────────────────────────────────────
echo.
echo 🔧 СПОСОБЫ ИСПРАВЛЕНИЯ:
echo.
echo 1️⃣  ЧЕРЕЗ ADB (если эмулятор запущен):
echo    1. Запустите эмулятор
echo    2. Откройте командную строку
echo    3. Выполните команды:
echo.
echo       adb shell wm density 160
echo       adb reboot
echo.
echo ────────────────────────────────────────────────────────────────
echo.
echo 2️⃣  ЧЕРЕЗ CONFIG.INI (вручную):
echo    1. Закройте Android Studio полностью
echo    2. Откройте папку:
echo       C:\Users\ВАШ_ИМЯ\.android\avd\
echo    3. Найдите папку вашего эмулятора (например: Voyah_Passion.avd)
echo    4. Откройте файл config.ini в блокноте
echo    5. Найдите строку: hw.lcd.density = 480
echo    6. Замените на: hw.lcd.density = 160
echo    7. Сохраните и закройте
echo    8. Запустите эмулятор
echo.
echo ────────────────────────────────────────────────────────────────
echo.
echo 3️⃣  ЧЕРЕЗ AVD MANAGER (GUI):
echo    1. Android Studio → Tools → Device Manager
echo    2. Найдите свой эмулятор
echo    3. Нажмите "Edit" (значок карандаша)
echo    4. Нажмите "Show Advanced Settings"
echo    5. Найдите "Custom screen density"
echo    6. Поставьте галочку и введите: 160
echo    7. Нажмите "Finish"
echo.
echo ────────────────────────────────────────────────────────────────
echo.
echo 💡 ХОТИТЕ ИСПРАВИТЬ ЧЕРЕЗ ADB СЕЙЧАС?
echo.
set /p adbChoice="Эмулятор запущен? (y/n): "

if /i "%adbChoice%"=="y" (
    echo.
    echo 🔄 Исправляю DPI через ADB...
    echo.
    
    adb shell wm density 160
    
    if %ERRORLEVEL% == 0 (
        echo ✅ DPI изменён на 160
        echo.
        set /p rebootChoice="Перезагрузить эмулятор? (y/n): "
        if /i "!rebootChoice!"=="y" (
            echo 🔄 Перезагружаю...
            adb reboot
            echo ✅ Готово! Подождите загрузки эмулятора.
        )
    ) else (
        echo ❌ ОШИБКА! Убедитесь что:
        echo    1. Эмулятор запущен
        echo    2. ADB установлен (входит в Android SDK)
        echo    3. ADB добавлен в PATH
        echo.
        echo 📂 Путь к ADB обычно:
        echo    C:\Users\ВАШ_ИМЯ\AppData\Local\Android\Sdk\platform-tools\
    )
) else (
    echo.
    echo 💡 Используйте способ 2 или 3 из списка выше.
    echo.
    echo 📄 Подробная инструкция в файле:
    echo    AVD_CONFIG_VOYAH_PASSION.txt
)

echo.
echo ────────────────────────────────────────────────────────────────
echo.
echo 🔍 ПРОВЕРКА DPI:
echo.
echo После исправления проверьте:
echo    adb shell wm density
echo.
echo Должно показать: Physical density: 160
echo.
echo ────────────────────────────────────────────────────────────────
pause
