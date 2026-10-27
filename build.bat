@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
::  XRail build script (Windows)
::  Version rule: yy.MM.dd.<daily index>, auto-increment per build.
::  The version is synced into build.sh (VERSION=) and
::  plugin.yml (version:) automatically.
:: ============================================================

set "PLUGIN_NAME=XRail"

:: ---- compute version (PowerShell keeps the date locale-independent) ----
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yy.MM.dd"') do set "VERSION_DATE=%%i"
set "VERSION_FILE=.version"
set "BUILD_NUM=0"
if exist "%VERSION_FILE%" (
    for /f "tokens=1,2" %%a in (%VERSION_FILE%) do (
        if "%%a"=="%VERSION_DATE%" set "BUILD_NUM=%%b"
    )
)
set /a BUILD_NUM=%BUILD_NUM%+1
> "%VERSION_FILE%" echo %VERSION_DATE% %BUILD_NUM%
set "VERSION=%VERSION_DATE%.%BUILD_NUM%"

echo ========================================
echo  XRail Builder v%VERSION%
echo ========================================

:: ---- sync version into build.sh and plugin.yml ----
powershell -NoProfile -Command "$c = Get-Content 'build.sh' -Raw; $c = [regex]::Replace($c, '(?m)^VERSION=.*$', 'VERSION=%VERSION%'); Set-Content 'build.sh' -Value $c -Encoding Ascii -NoNewline"
powershell -NoProfile -Command "$c = Get-Content 'src/main/resources/plugin.yml' -Raw; $c = [regex]::Replace($c, '(?m)^version: .*$', 'version: %VERSION%'); Set-Content 'src/main/resources/plugin.yml' -Value $c -Encoding Ascii -NoNewline"

:: ---- toolchain ----
set "JDK_BIN=C:\Program Files\Java\latest\jdk-26\bin"
if not exist "%JDK_BIN%\javac.exe" set "JDK_BIN="
set "JAVAC=%JDK_BIN%\javac.exe"
if "%JAVAC%"=="\javac.exe" set "JAVAC=javac"
set "JAR=%JDK_BIN%\jar.exe"
if "%JAR%"=="\jar.exe" set "JAR=jar"

:: ---- dependency classpath (local maven repo, offline friendly) ----
set "M2=%USERPROFILE%\.m2\repository"
set "PAPER=%M2%\io\papermc\paper\paper-api\1.20.4-R0.1-SNAPSHOT\paper-api-1.20.4-R0.1-SNAPSHOT.jar"
if not exist "%PAPER%" (
    echo [ERROR] paper-api jar not found: %PAPER%
    echo         Set PAPER env or run a maven download once.
    exit /b 1
)
set "CP=%PAPER%"
set "CP=%CP%;%M2%\net\kyori\adventure-api\4.16.0\adventure-api-4.16.0.jar"
set "CP=%CP%;%M2%\net\kyori\adventure-key\4.16.0\adventure-key-4.16.0.jar"
set "CP=%CP%;%M2%\net\kyori\examination-api\1.3.0\examination-api-1.3.0.jar"
set "CP=%CP%;%M2%\net\kyori\option\1.0.0\option-1.0.0.jar"
set "CP=%CP%;%M2%\net\kyori\adventure-text-serializer-legacy\4.16.0\adventure-text-serializer-legacy-4.16.0.jar"
set "CP=%CP%;%M2%\org\jetbrains\annotations\24.1.0\annotations-24.1.0.jar"
set "CP=%CP%;%M2%\com\google\guava\guava\32.1.2-jre\guava-32.1.2-jre.jar"
set "CP=%CP%;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"
set "CP=%CP%;%M2%\net\md-5\bungeecord-chat\1.20-R0.2-deprecated+build.18\bungeecord-chat-1.20-R0.2-deprecated+build.18.jar"

:: ---- compile ----
if not exist "build\classes" mkdir "build\classes"
if not exist "dist" mkdir "dist"
dir /s /b "src\main\java\*.java" > "build\sources.txt"
"%JAVAC%" --release 17 -encoding UTF-8 -cp "%CP%" -d "build\classes" "@build\sources.txt"
if errorlevel 1 (
    echo [ERROR] Compile failed.
    exit /b 1
)

:: ---- package ----
if exist "build\%PLUGIN_NAME%-%VERSION%.jar" del "build\%PLUGIN_NAME%-%VERSION%.jar"
"%JAR%" --create --file "build\%PLUGIN_NAME%-%VERSION%.jar" -C "build\classes" . -C "src\main\resources" .
if errorlevel 1 (
    echo [ERROR] Package failed.
    exit /b 1
)

copy /Y "build\%PLUGIN_NAME%-%VERSION%.jar" "dist\" >nul
echo.
echo [OK] Built: dist\%PLUGIN_NAME%-%VERSION%.jar
endlocal
