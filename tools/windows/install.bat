@echo off
rem First install of Bidhata-Q5 from a PC: works from stock firmware or any earlier Bidhata.
rem Writes the Bidhata partition table, TWRP, boot and system, and clears cache and data.
rem The player's bootloader (preloader, lk) is never touched.
setlocal
call "%~dp0_common.bat" || (pause & exit /b 1)
for %%F in (ebr1-q5.bin twrp-q5.img boot.img system.img zeros-1m.bin) do (
    if not exist "%HERE%%%F" (echo  Missing file: %%F. Unpack the whole package again. & pause & exit /b 1)
)
for %%F in ("%HERE%system.img") do set "SZ=%%~zF"
if not "%SZ%"=="1415577600" (echo  system.img has the wrong size; download the package again. & pause & exit /b 1)
if /i not "%~1"=="nobackup" (
    if not exist "%HERE%backup\q5-backup.bin" (
        echo.
        echo  No backup found. Run backup.bat first: it is your way back to stock.
        pause & exit /b 1
    )
    set /p LK=<"%HERE%backup\bootloader.txt"
)
if /i not "%~1"=="nobackup" if not "%LK%"=="stock" (
    echo.
    echo  This player's bootloader is one Bidhata-Q5 hasn't been tested with.
    echo  Stopping here to keep your player safe. Please open an Issue on GitHub.
    pause & exit /b 1
)
echo.
echo  ==================================================================
echo   INSTALL BIDHATA-Q5
echo.
echo   This ERASES the player's internal storage: music copied to the
echo   player itself, apps and settings. The microSD card is NOT touched.
echo   Copy anything you want to keep off the player first.
echo.
echo   Battery should be above 50%%. Takes about 25 minutes.
echo  ==================================================================
set /p OK=  Type YES and press Enter to continue: 
if /i not "%OK%"=="YES" (echo  Cancelled. Nothing was changed. & pause & exit /b 0)

set "CHK=%HERE%check"
if exist "%CHK%" rmdir /s /q "%CHK%"
mkdir "%CHK%"
call "%HERE%_common.bat" :hexsize TS "%HERE%twrp-q5.img"
call "%HERE%_common.bat" :hexsize BS "%HERE%boot.img"
call "%HERE%_common.bat" :connect

rem write, then read every piece back for checking
"%PY%" -u mtk.py multi "wo 0x880000 0x200 %HERE%ebr1-q5.bin;wo 0x3580000 0x1000000 %HERE%twrp-q5.img;wo 0x2580000 0x1000000 %HERE%boot.img;wo 0x5900000 0x54600000 %HERE%system.img;wo 0x59F00000 0x100000 %HERE%zeros-1m.bin;wo 0x61D00000 0x100000 %HERE%zeros-1m.bin;ro 0x880000 0x200 %CHK%\ebr1-q5.bin;ro 0x3580000 %TS% %CHK%\twrp-q5.img;ro 0x2580000 %BS% %CHK%\boot.img;ro 0x5900000 0x54600000 %CHK%\system.img" > "%HERE%install.log" 2>&1

set "BAD="
for %%F in (ebr1-q5.bin twrp-q5.img boot.img system.img) do (
    call "%HERE%_common.bat" :sha256 A "%HERE%%%F"
    call "%HERE%_common.bat" :sha256 B "%CHK%\%%F"
    call :same %%F
)
if defined BAD (
    echo.
    echo  These did not check out:%BAD%
    echo  Details: %HERE%install.log
    echo  Don't worry: the bootloader was not touched, so you can simply retry.
    echo  Run install.bat again ^(it works even if the player won't start^).
    pause & exit /b 1
)
rmdir /s /q "%CHK%"
echo.
echo  ==================================================================
echo   Installed and verified.
echo   Unplug the cable, then press RESET ^(or hold Power 10 seconds^) to start.
echo   The first start takes about 5 minutes. That's normal.
echo  ==================================================================
pause
exit /b 0

:same
if not "%A%"=="%B%" set "BAD=%BAD% %1"
if "%A%"=="" set "BAD=%BAD% %1"
exit /b 0
