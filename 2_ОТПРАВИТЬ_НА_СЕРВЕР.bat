@echo off
chcp 65001 >nul
color 0E
title ⚡ Voyah - Быстрая отправка

cls
echo.
echo ╔════════════════════════════════════════════════════╗
echo ║      ⚡ БЫСТРАЯ ОТПРАВКА ИЗМЕНЕНИЙ НА GITHUB       ║
echo ╚════════════════════════════════════════════════════╝
echo.

:: Проверяем наличие изменений
git diff-index --quiet HEAD --
if %ERRORLEVEL% == 0 (
    echo ℹ️  Нет изменений для отправки.
    timeout /t 3
    exit
)

echo 📝 Измененные файлы:
git status -s
echo.

set /p comment="➤ Описание изменений (Enter = автоматическое): "

if "%comment%"=="" (
    :: Генерируем автоматическое описание с датой и временем
    for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
    set date_str=!datetime:~0,4!-!datetime:~4,2!-!datetime:~6,2!
    set time_str=!datetime:~8,2!:!datetime:~10,2!
    set comment=Обновление от !date_str! !time_str!
)

echo.
echo 🔄 Отправляю изменения...
echo.

git add .
git commit -m "%comment%"
git push origin main

echo.
if %ERRORLEVEL% == 0 (
    echo ✅ Готово! Изменения на GitHub.
    echo 📋 %comment%
) else (
    echo ❌ Ошибка! Используйте push_to_github.bat для деталей.
)
echo.
timeout /t 3
