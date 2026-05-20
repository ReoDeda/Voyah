@echo off
chcp 65001 >nul
color 0B
title ⚡ Voyah - Быстрое обновление

cls
echo.
echo ╔════════════════════════════════════════════════════╗
echo ║      ⚡ БЫСТРОЕ ОБНОВЛЕНИЕ ПРОЕКТА (MAIN)          ║
echo ╚════════════════════════════════════════════════════╝
echo.
echo 🔄 Скачиваю последнюю версию...
echo.

git pull origin main

echo.
if %ERRORLEVEL% == 0 (
    echo ✅ Готово! Проект обновлен.
) else (
    echo ❌ Ошибка! Используйте update_project.bat для детальной информации.
)
echo.
timeout /t 3
