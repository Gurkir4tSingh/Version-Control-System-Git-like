@echo off
REM ─────────────────────────────────────────────────────────────
REM  GitLite VCS — Build & Run (Windows)
REM  Requires: Java 17+
REM ─────────────────────────────────────────────────────────────

set PROJECT_DIR=%~dp0
set SRC_DIR=%PROJECT_DIR%src\main\java
set OUT_DIR=%PROJECT_DIR%out
set MAIN_CLASS=gitlite.Main

echo.
echo   GitLite VCS -- Build and Run
echo.

where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo   ERROR: javac not found. Install JDK 17+ from https://adoptium.net
    pause & exit /b 1
)

echo   Compiling...
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
dir /s /b "%SRC_DIR%\*.java" > "%TEMP%\sources.txt"
javac --release 17 -d "%OUT_DIR%" @"%TEMP%\sources.txt"
if %errorlevel% neq 0 ( echo   Compilation FAILED. & pause & exit /b 1 )
echo   Compilation successful.
echo.

if "%1"=="demo" (
    echo   Running automated demo...
    java -cp "%OUT_DIR%" %MAIN_CLASS% demo
) else (
    echo   Starting interactive shell...
    java -cp "%OUT_DIR%" %MAIN_CLASS%
)
pause
