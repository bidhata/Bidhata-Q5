@echo off
rem Puts the player back the way backup.bat found it (stock TIMMKOO firmware), with empty
rem cache and data. Only for backups made on the stock layout.
setlocal
set "LAYOUT=" & set "OK="
call "%~dp0_common.bat" || (pause & exit /b 1)
set "BK=%HERE%backup"
for %%F in ("%BK%\q5-backup.bin") do set "SZ=%%~zF"
if not "%SZ%"=="1635778560" (echo  No complete backup in %BK%. & pause & exit /b 1)
if exist "%BK%\layout.txt" set /p LAYOUT=<"%BK%\layout.txt"
if not "%LAYOUT%"=="stock" (
    echo  This backup was not made on stock firmware ^(layout: %LAYOUT%^).
    echo  restore-stock.bat only knows how to restore stock. Ask for help in the
    echo  project's Issues before trying anything else.
    pause & exit /b 1
)
for %%F in (stock-cache-empty.img stock-userdata-empty.img) do (
    if not exist "%HERE%%%F" (echo  Missing file: %%F. Unpack the whole package again. & pause & exit /b 1)
)
echo.
echo  ==================================================================
echo   RESTORE STOCK FIRMWARE from your backup.
echo   This erases Bidhata-Q5 and everything on the player's internal
echo   storage. The microSD card is not touched. About 15 minutes.
echo  ==================================================================
set /p OK=  Type YES and press Enter to continue: 
if /i not "%OK%"=="YES" (echo  Cancelled. Nothing was changed. & pause & exit /b 0)
call "%HERE%_common.bat" :connect

rem a read-back left over from an earlier run must not pass for this one
del "%BK%\ebr1-after-restore.bin" 2>nul
echo  Working... this window stays quiet for about 15 minutes. That is normal.
"%PY%" -u mtk.py multi "wo 0x800000 0x61800000 %BK%\q5-backup.bin;wo 0x38D00000 0x7E00000 %HERE%stock-cache-empty.img;wo 0x40B00000 0x1F300000 %HERE%stock-userdata-empty.img;ro 0x880000 0x200 %BK%\ebr1-after-restore.bin" > "%HERE%restore.log" 2>&1

call "%HERE%_common.bat" :sha256 A "%BK%\ebr1.bin"
call "%HERE%_common.bat" :sha256 B "%BK%\ebr1-after-restore.bin"
if "%B%"=="" set "B=missing"
if not "%A%"=="%B%" (
    echo  The restore did not check out. Details: %HERE%restore.log
    echo  Run restore-stock.bat again.
    pause & exit /b 1
)
echo.
echo  Restored. Unplug, then press RESET to start. Once it has started, do
echo  Settings ^> Backup ^& reset ^> Factory data reset, so the storage gets its full size back.
pause
