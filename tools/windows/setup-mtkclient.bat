@echo off
rem One-time: downloads mtkclient (the tool that talks to the player over USB) and sets it up.
rem Needs Python 3 from python.org with "Add python.exe to PATH" ticked during install.
setlocal
set "HERE=%~dp0"
rem The exact mtkclient version every Q5 flash so far was done with.
set "COMMIT=a1e1b4d8c68ea6d711e789678f98a256ce848eab"

where python >nul 2>&1 || (
    echo  Python isn't installed, or wasn't added to PATH.
    echo  Install it from https://www.python.org/downloads/ and tick
    echo  "Add python.exe to PATH" on the first screen. Then run this again.
    pause & exit /b 1
)
if exist "%HERE%mtkclient\.venv\Scripts\python.exe" (
    echo  mtkclient is already set up. Nothing to do.
    pause & exit /b 0
)
echo  Downloading mtkclient...
curl -fL -o "%HERE%mtkclient.zip" https://github.com/bkerler/mtkclient/archive/%COMMIT%.zip || (
    echo  Download failed. Check the internet connection and try again.
    pause & exit /b 1
)
tar -xf "%HERE%mtkclient.zip" -C "%HERE%." || (echo  Could not unpack mtkclient.zip & pause & exit /b 1)
if exist "%HERE%mtkclient" rmdir /s /q "%HERE%mtkclient"
ren "%HERE%mtkclient-%COMMIT%" mtkclient
del "%HERE%mtkclient.zip"

echo  Setting up Python packages...
python -m venv "%HERE%mtkclient\.venv" || (echo  Could not create the Python environment & pause & exit /b 1)
"%HERE%mtkclient\.venv\Scripts\python.exe" -m pip install -q pyusb==1.3.1 pycryptodome==4.6.0 pycryptodomex==3.24.2 pyserial==3.5 colorama==0.4.6 || (
    echo  Installing packages failed. Check the internet connection and try again.
    pause & exit /b 1
)
echo.
echo  Done. Next: install the USB driver with Zadig (see the guide), then run backup.bat.
pause
