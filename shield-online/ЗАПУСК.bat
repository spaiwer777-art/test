@echo off
chcp 65001 >nul
rem Протокол «Щит» — запуск сервера игры на этом компьютере (Windows).
rem Двойной клик по файлу. Окно не закрывайте, пока идёт игра.
title Протокол «Щит» — сервер игры
cd /d "%~dp0"

rem ======== Настройки ========
rem Порт сервера: адрес игры будет http://<адрес этого компьютера>:ПОРТ/
set "PORT=8080"
rem Ключ ведущего хранится в файле host-key.txt рядом с этим файлом.
rem Чтобы сменить ключ, удалите host-key.txt и запустите ЗАПУСК.bat снова.
rem ===========================

where node >nul 2>nul
if not errorlevel 1 goto have_node
echo.
echo   Не найден Node.js — без него сервер не запустится.
echo   Сейчас откроется сайт nodejs.org: скачайте версию LTS и установите её
echo   с настройками по умолчанию. Потом снова запустите ЗАПУСК.bat.
echo.
start "" https://nodejs.org/
pause
exit /b 1

:have_node
if exist "host-key.txt" goto read_key
echo.
echo   Первый запуск. Придумайте ключ ведущего: его вводят на терминале защиты,
echo   в конструкторе игры и на странице QR-кодов. Игрокам он не нужен.
echo   Только латинские буквы, цифры, дефис и подчёркивание, не короче 6 символов.
echo   Например: shield-6b-tigr
call :ask_key

:read_key
set "HOST_KEY="
set /p HOST_KEY=<"host-key.txt"
if defined HOST_KEY goto run
echo.
echo   Файл host-key.txt пустой — задайте ключ заново.
call :ask_key
goto read_key

:run
echo.
echo   Ключ ведущего записан в файле host-key.txt
echo   Не закрывайте это окно, пока идёт игра. Остановить сервер — закрыть окно.
echo   Если Windows спросит про доступ к сети — отметьте «Частные сети» и разрешите.
node server.js
if errorlevel 2 goto port_busy
echo.
echo   Сервер остановился. Перезапуск через 5 секунд...
echo   Чтобы остановить сервер совсем, закройте это окно.
timeout /t 5 /nobreak >nul
goto run

:port_busy
echo.
echo   Порт %PORT% уже занят. Скорее всего, сервер уже запущен в другом окне.
echo   Закройте лишнее окно или поменяйте PORT в начале файла ЗАПУСК.bat.
echo.
pause
exit /b 2

rem ---------- ввод ключа ведущего ----------
:ask_key
setlocal EnableDelayedExpansion
:ask_key_again
echo.
set "NEWKEY="
set /p "NEWKEY=  Ключ ведущего: "
if not defined NEWKEY goto ask_key_again
if "!NEWKEY:~5!"=="" (
  echo   Слишком короткий ключ — нужно не меньше 6 символов.
  goto ask_key_again
)
>"host-key.tmp" echo(!NEWKEY!
findstr /r /x "[0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-]*" "host-key.tmp" >nul
if errorlevel 1 (
  del "host-key.tmp" >nul 2>nul
  echo   Можно только латинские буквы, цифры, дефис и подчёркивание, без пробелов.
  goto ask_key_again
)
move /y "host-key.tmp" "host-key.txt" >nul
echo   Ключ сохранён в host-key.txt
endlocal
exit /b 0
