@echo off
setlocal enabledelayedexpansion

title Smart TV Remote - Git Setup & Push Tool

echo =======================================================
echo     Smart TV Remote - Git Setup ^& GitHub Push
echo =======================================================
echo.

:: 1. Check if Git is installed
where git >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Git is not detected in your PATH.
    echo Please install Git from https://git-scm.com/ and try again.
    pause
    exit /b 1
)

:: 2. Initialize repository if needed
if not exist ".git" (
    echo [*] Initializing Git repository...
    git init
    git branch -M main
) else (
    echo [*] Git repository detected.
    git branch -M main
)

:: 3. Force-add gradle wrapper jar
if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [*] Adding gradle-wrapper.jar...
    git add -f gradle/wrapper/gradle-wrapper.jar
)

:: 4. Stage all project files
echo [*] Staging files...
git add .

:: 5. Commit
echo [*] Committing changes...
git commit -m "Fix build: update Gradle wrapper, theme syntax, and Compose clickables"

:: 6. Configure remote origin
git remote get-url origin >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [*] Setting remote origin to https://github.com/Nikhilgowtham1411/remote.git
    git remote add origin https://github.com/Nikhilgowtham1411/remote.git
)

:: 7. Push to GitHub
echo.
echo [*] Pushing to GitHub (origin main)...
git push -u origin main

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo -----------------------------------------------------------------
    echo [NOTE] If you get "403 Forbidden" or "denied to another user":
    echo Windows Credential Manager is sending an old GitHub account.
    echo.
    echo To fix this:
    echo 1. Open Windows Start Menu, type "Credential Manager" and open it.
    echo 2. Click "Windows Credentials".
    echo 3. Under "Generic Credentials", find "git:https://github.com" and Remove it.
    echo 4. Run this script again - GitHub will prompt you to sign in with Nikhilgowtham1411.
    echo -----------------------------------------------------------------
) else (
    echo.
    echo =======================================================
    echo [SUCCESS] Pushed successfully!
    echo Check your Actions tab:
    echo https://github.com/Nikhilgowtham1411/remote/actions
    echo =======================================================
)

echo.
pause
