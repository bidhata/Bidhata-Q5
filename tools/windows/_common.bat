@echo off
rem Shared bits for the Bidhata-Q5 PC scripts. Not meant to be run on its own.
if not "%~1"=="" goto %~1

set "HERE=%~dp0"
if not "%HERE: =%"=="%HERE%" (
    echo.
    echo  This folder's path contains a space:  %HERE%
    echo  mtkclient can't handle that. Move the whole folder somewhere simple,
    echo  for example C:\Q5, and run the script again.
    exit /b 1
)
set "PY=%HERE%mtkclient\.venv\Scripts\python.exe"
if not exist "%PY%" (
    echo.
    echo  mtkclient isn't set up yet. Run setup-mtkclient.bat first.
    exit /b 1
)
cd /d "%HERE%mtkclient"
rem a leftover session file from an earlier run makes mtkclient talk to a dead session
if exist .state del .state
exit /b 0

:connect
echo.
echo  ------------------------------------------------------------------
echo   Get the player ready:
echo     - Plug the USB cable into the PC and the player.
echo     - Have a paperclip or SIM pin ready for the small RESET hole.
echo   It doesn't matter whether the player is on, off, or stuck on the logo.
echo  ------------------------------------------------------------------
pause
echo.
echo  NOW press the RESET hole once (a short press is enough).
echo  The player restarts and this window catches it in the first seconds.
echo  The screen may stay black; that's normal. Leave the cable alone until
echo  this window says it's finished. If nothing happens within a minute,
echo  press RESET once more.
echo.
exit /b 0

:hexsize
rem %2 = file size of %3 in hex (0x...), the form mtkclient wants
set "_sz=%~z3"
cmd /c exit /b %_sz%
set "%2=0x%=ExitCode%"
exit /b 0

:sha256
rem %2 = SHA-256 of file %3 (lower case, no spaces)
set "%2="
for /f "skip=1 delims=" %%H in ('certutil -hashfile "%~3" SHA256') do (
    if not defined %2 set "%2=%%H"
)
call set "%2=%%%2: =%%"
exit /b 0
