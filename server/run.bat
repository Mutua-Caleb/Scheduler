@echo off
REM Call Scheduler server launcher for Windows.
REM Double-click this file, or run it from cmd / PowerShell.

setlocal
cd /d "%~dp0"

if not exist .venv (
    echo Creating virtual environment...
    python -m venv .venv
    if errorlevel 1 (
        echo.
        echo Could not create the virtual environment.
        echo Make sure Python 3.10 or newer is installed and on your PATH.
        echo Download it from https://www.python.org/downloads/ ^(check "Add python.exe to PATH"^).
        pause
        exit /b 1
    )
)

call .venv\Scripts\activate.bat

python -m pip install --quiet --upgrade pip
python -m pip install --quiet -r requirements.txt
if errorlevel 1 (
    echo.
    echo Failed to install dependencies. Check your internet connection.
    pause
    exit /b 1
)

if "%SCHEDULER_HOST%"=="" set SCHEDULER_HOST=0.0.0.0
if "%SCHEDULER_PORT%"=="" set SCHEDULER_PORT=8765

echo.
echo Call Scheduler server starting on http://%SCHEDULER_HOST%:%SCHEDULER_PORT%
echo Open http://localhost:%SCHEDULER_PORT%/ in your browser.
echo Press Ctrl+C to stop.
echo.

python -m scheduler_server --host %SCHEDULER_HOST% --port %SCHEDULER_PORT%

endlocal
