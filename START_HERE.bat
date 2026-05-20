@echo off
chcp 65001 >nul
color 0B
title 🚗 Voyah Launcher - Главное меню

:MAIN_MENU
cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║                                                                ║
echo ║            🚗 VOYAH LAUNCHER - ГЛАВНОЕ МЕНЮ 🚗                 ║
echo ║                                                                ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo  📂 Проект: Voyah Android Launcher
echo  🌐 GitHub: https://github.com/ReoDeda/Voyah
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║                    📥 СКАЧАТЬ ИЗМЕНЕНИЯ                        ║
echo ╠════════════════════════════════════════════════════════════════╣
echo ║  1. ⚡ Быстрое обновление (скачать с GitHub)                   ║
echo ║  2. 📋 Полное меню обновлений (все опции)                      ║
echo ╠════════════════════════════════════════════════════════════════╣
echo ║                    📤 ОТПРАВИТЬ ИЗМЕНЕНИЯ                      ║
echo ╠════════════════════════════════════════════════════════════════╣
echo ║  3. ⚡ Быстрая отправка (залить на GitHub)                     ║
echo ║  4. 📋 Полное меню отправки (все опции)                        ║
echo ╠════════════════════════════════════════════════════════════════╣
echo ║                    🛠️  ДОПОЛНИТЕЛЬНО                           ║
echo ╠════════════════════════════════════════════════════════════════╣
echo ║  5. 📊 Показать статус проекта                                 ║
echo ║  6. 🌿 Показать все ветки                                      ║
echo ║  7. 📁 Открыть проект в проводнике                             ║
echo ║  8. 🌐 Открыть GitHub в браузере                               ║
echo ╠════════════════════════════════════════════════════════════════╣
echo ║  0. ❌ Выход                                                    ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
set /p mainChoice="➤ Выберите действие: "

if "%mainChoice%"=="1" goto QUICK_UPDATE
if "%mainChoice%"=="2" goto FULL_UPDATE
if "%mainChoice%"=="3" goto QUICK_PUSH
if "%mainChoice%"=="4" goto FULL_PUSH
if "%mainChoice%"=="5" goto SHOW_STATUS
if "%mainChoice%"=="6" goto SHOW_BRANCHES
if "%mainChoice%"=="7" goto OPEN_FOLDER
if "%mainChoice%"=="8" goto OPEN_GITHUB
if "%mainChoice%"=="0" goto EXIT
goto MAIN_MENU

:QUICK_UPDATE
cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║              ⚡ БЫСТРОЕ ОБНОВЛЕНИЕ (СКАЧИВАНИЕ)                ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
call quick_update.bat
pause
goto MAIN_MENU

:FULL_UPDATE
cls
call update_project.bat
goto MAIN_MENU

:QUICK_PUSH
cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║              ⚡ БЫСТРАЯ ОТПРАВКА (ЗАГРУЗКА)                    ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
call quick_push.bat
pause
goto MAIN_MENU

:FULL_PUSH
cls
call push_to_github.bat
goto MAIN_MENU

:SHOW_STATUS
cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║                    📊 СТАТУС ПРОЕКТА                           ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo 🌿 Текущая ветка:
git branch --show-current
echo.
echo ────────────────────────────────────────────────────────────────
echo 📝 Измененные файлы:
git status -s
echo.
echo ────────────────────────────────────────────────────────────────
echo 📋 Последние коммиты:
git log --oneline -5
echo.
pause
goto MAIN_MENU

:SHOW_BRANCHES
cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║                    🌿 ВСЕ ВЕТКИ                                ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo 📂 Локальные ветки:
git branch
echo.
echo ────────────────────────────────────────────────────────────────
echo 🌐 Удаленные ветки:
git fetch origin >nul 2>&1
git branch -r
echo.
pause
goto MAIN_MENU

:OPEN_FOLDER
start explorer "%CD%"
goto MAIN_MENU

:OPEN_GITHUB
start https://github.com/ReoDeda/Voyah
goto MAIN_MENU

:EXIT
cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║              👋 Удачной разработки Voyah! 🚗                   ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
timeout /t 2 >nul
exit
