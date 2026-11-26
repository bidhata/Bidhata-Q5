@echo off
rem Saves the part of the player's memory that the install changes, plus the parts that are
rem unique to YOUR player (Wi-Fi/Bluetooth calibration). restore-stock.bat puts it back.
setlocal
call "%~dp0_common.bat" || (pause & exit /b 1)
set "OUT=%HERE%backup"
if exist "%OUT%\q5-backup.bin" (
    echo.
    echo  There is already a backup in %OUT%
    echo  Move that folder somewhere safe first. It may be your only copy of stock.
    pause & exit /b 1
)
mkdir "%OUT%" 2>nul
echo.
echo  BACKUP: reads about 1.6 GB from the player. Takes 10-15 minutes.
echo  Nothing on the player is changed.
call "%HERE%_common.bat" :connect

rem 0x800000..0x62000000 holds the partition table, calibration, bootloader, boot, recovery,
rem system and the start of cache/data, in both the stock and the Bidhata layout.
echo  Working... this window stays quiet for 10-15 minutes. That is normal.
"%PY%" -u mtk.py multi "ro 0x800000 0x61800000 %OUT%\q5-backup.bin;ro 0x880000 0x200 %OUT%\ebr1.bin;ro 0xC00000 0x500000 %OUT%\nvram.bin;ro 0x1100000 0xA00000 %OUT%\protect_f.bin;ro 0x1B00000 0xA00000 %OUT%\protect_s.bin;ro 0x2520000 0x40000 %OUT%\lk-head.bin;ro 0x0 0x400000 %OUT%\preloader.bin --parttype boot1" > "%OUT%\backup.log" 2>&1

for %%F in ("%OUT%\q5-backup.bin") do set "SZ=%%~zF"
if not "%SZ%"=="1635778560" (
    echo.
    echo  The backup is incomplete. Details: %OUT%\backup.log
    echo  Delete the backup folder and run backup.bat again.
    pause & exit /b 1
)
call "%HERE%_common.bat" :sha256 E "%OUT%\ebr1.bin"
set "LAYOUT=unknown"
if "%E%"=="90f5a95a907f63e57ec2a60598fbec39aa06308f730a8e72def39838a67582da" set "LAYOUT=stock"
if "%E%"=="d3362e430dd0209376f3978633ff0ea2cc1402885341778f3d4aa8ca0365c8a2" set "LAYOUT=bidhata"
> "%OUT%\layout.txt" echo %LAYOUT%
rem The bootloader (lk) Bidhata has been run on is the stock 4.4 one. Others (T-ROM 5.0, other
rem firmware versions) are untested, so install.bat stops for those.
call "%HERE%_common.bat" :sha256 L "%OUT%\lk-head.bin"
set "LK=other"
if "%L%"=="0993ac6257fe3d3726b2733cef81f100377e0dbd30679f56c208f8411c549528" set "LK=stock"
> "%OUT%\bootloader.txt" echo %LK%
(for %%F in ("%OUT%\*.bin") do (
    call "%HERE%_common.bat" :sha256 H "%%F"
    call echo %%H%%  %%~nxF
)) > "%OUT%\SHA256SUMS.txt"
echo.
echo  Backup done. Partition layout: %LAYOUT%.  Bootloader: %LK%.
if "%LK%"=="other" (
    echo.
    echo  NOTE: this player has a bootloader Bidhata-Q5 hasn't been tested with
    echo  ^(for example from T-ROM 5.0^). install.bat will stop. Please open an Issue
    echo  on GitHub so we can work out a safe way for your player.
)
echo  Copy the whole "backup" folder somewhere safe too (a USB stick, cloud drive).
echo  Unplug the cable; the player starts normally ^(or press RESET^).
pause
