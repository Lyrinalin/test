@echo off
setlocal enabledelayedexpansion

set "SERVER_PLUGINS_DIR=C:\path\to\your\server\plugins"

cd /d "%~dp0\.."

where gradle >nul 2>&1
if errorlevel 1 (
  echo Gradle not found in PATH. Please install Gradle 8.x or add it to PATH.
  exit /b 1
)

call gradle clean build
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

for /f %%f in ('dir /b /o:-d build\\libs\\MeteorBow-*.jar 2^>nul') do (
  set "JAR_PATH=build\\libs\\%%f"
  goto :foundJar
)

:foundJar
if not defined JAR_PATH (
  echo JAR not found in build\\libs.
  exit /b 1
)

if not exist "%SERVER_PLUGINS_DIR%" (
  echo Plugins directory not found: %SERVER_PLUGINS_DIR%
  exit /b 1
)

copy /y "%JAR_PATH%" "%SERVER_PLUGINS_DIR%" >nul

echo Copied %JAR_PATH% to %SERVER_PLUGINS_DIR%
endlocal
