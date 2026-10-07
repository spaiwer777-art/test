@echo off
chcp 65001 > nul
:: ============================================================
::  roblox-qwerty.bat - Zapret profile for Roblox, ISP QWERTY (Moscow)
::
::  QWERTY is the former Central Telegraph network, now part of the
::  Rostelecom group, so it is filtered by the same TSPU (DPI) boxes.
::  Strategies below are the ones that passed the Roblox targets in
::  "utils\test results" on this connection.
::
::  Put this file into the zapret-discord-youtube folder (next to bin\).
::  Run it INSTEAD of general*.bat (only one winws.exe may run).
::
::  Usage:  roblox-qwerty.bat        - strategy 1 (default)
::          roblox-qwerty.bat 2      - strategy 2, also 3 or 4
::  Try the next number if Roblox does not log in or textures do not load.
:: ============================================================

cd /d "%~dp0"

:: ---- admin rights ----
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Requesting administrator rights...
    if "%~1"=="" (
        powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    ) else (
        powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -ArgumentList '%~1' -Verb RunAs"
    )
    exit /b
)

set "BIN=%~dp0bin\"
if not exist "%BIN%winws.exe" (
    echo ERROR: bin\winws.exe not found. Put this file next to the bin folder.
    pause
    exit /b 1
)

:: ---- only ONE winws must run ----
taskkill /F /IM winws.exe >nul 2>&1

:: ---- Roblox domains (HTTPS: site, login, API, assets, textures) ----
set "RBX_DOMAINS=roblox.com,rbxcdn.com,rbxcdn.net,rbxtrk.com,rblx.com,robloxlabs.com,roblox.cn,arkoselabs.com,arkoselabs.cn,funcaptcha.com"

:: ---- Roblox game servers, AS22697 (UDP, error 279 / endless loading) ----
:: Not present in lists\ipset-all.txt, so the general*.bat do not touch them.
set "RBX_IPS=128.116.0.0/17,103.140.28.0/23,103.142.220.0/23,141.193.3.0/24,205.201.62.0/24,23.173.192.0/24,204.9.184.0/24,204.13.168.0/22,204.13.172.0/23,204.13.174.0/24,209.206.40.0/21"

:: ---- strategy (argument 1-4, default 1) ----
set "VAR=%~1"
if not "%VAR%"=="2" if not "%VAR%"=="3" if not "%VAR%"=="4" set "VAR=1"
goto :s%VAR%

:: 1 - from general (EXP): best result on QWERTY (Roblox all OK, 0 errors)
:s1
set "TCPS=--dpi-desync=fake,multisplit --dpi-desync-split-seqovl=480 --dpi-desync-split-pos=1 --dpi-desync-fooling=ts --dpi-desync-repeats=4 --dpi-desync-split-seqovl-pattern="%BIN%stun2.bin" --dpi-desync-fake-tls="%BIN%tls_clienthello_max_ru.bin" --dpi-desync-fake-http="%BIN%tls_clienthello_max_ru.bin""
goto :go
:: 2 - from general (ALT11): longer overlap, more repeats
:s2
set "TCPS=--dpi-desync=fake,multisplit --dpi-desync-split-seqovl=664 --dpi-desync-split-pos=1 --dpi-desync-fooling=ts --dpi-desync-repeats=8 --dpi-desync-split-seqovl-pattern="%BIN%tls_clienthello_max_ru.bin" --dpi-desync-fake-tls="%BIN%stun2.bin" --dpi-desync-fake-tls="%BIN%tls_clienthello_max_ru.bin" --dpi-desync-fake-http="%BIN%tls_clienthello_max_ru.bin""
goto :go
:: 3 - hostfakesplit: different technique, if rbxcdn.com (textures) still fails
:s3
set "TCPS=--dpi-desync=hostfakesplit --dpi-desync-repeats=4 --dpi-desync-fooling=ts,md5sig --dpi-desync-hostfakesplit-mod=host=ozon.ru"
goto :go
:: 4 - fake TLS with google SNI + big overlap
:s4
set "TCPS=--dpi-desync=fake,multisplit --dpi-desync-split-seqovl=681 --dpi-desync-split-pos=1 --dpi-desync-fooling=ts --dpi-desync-repeats=8 --dpi-desync-split-seqovl-pattern="%BIN%tls_clienthello_www_google_com.bin" --dpi-desync-fake-tls-mod=rnd,dupsid,sni=www.google.com"
goto :go

:go
cd /d "%BIN%"

:: Profile 1: Roblox HTTPS by domain name (SNI).
:: Profile 2: Roblox game UDP by IP, only the first packets of each flow
::            get a fake (cutoff), so in-game ping is not affected.
start "zapret: roblox-qwerty %VAR%" /min "%BIN%winws.exe" --wf-tcp=80,443 --wf-udp=49152-65535 ^
--filter-tcp=80,443 --hostlist-domains=%RBX_DOMAINS% %TCPS% --new ^
--filter-udp=49152-65535 --ipset-ip=%RBX_IPS% --dpi-desync=fake --dpi-desync-any-protocol=1 --dpi-desync-repeats=6 --dpi-desync-fake-unknown-udp="%BIN%ACTIVE_GAME_UDP.bin" --dpi-desync-cutoff=n3

echo:
echo Zapret started for Roblox (QWERTY), strategy %VAR%.
echo Keep the "zapret: roblox-qwerty" window open, then start Roblox.
echo If something does not load, run: roblox-qwerty.bat 2  (or 3, 4)
timeout /t 4 /nobreak >nul
