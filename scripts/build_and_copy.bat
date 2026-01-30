@echo off
setlocal enabledelayedexpansion

set "SERVER_PLUGINS_DIR=C:\path\to\your\server\plugins"

cd /d "%~dp0\.."

call gradle clean build
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

for %%f in (build\libs\MeteorBow-*.jar) do set "JAR_PATH=%%f"

if not defined JAR_PATH (
  echo JAR not found in build\libs.
  exit /b 1
)

if not exist "%SERVER_PLUGINS_DIR%" (
  echo Plugins directory not found: %SERVER_PLUGINS_DIR%
  exit /b 1
)

copy /y "%JAR_PATH%" "%SERVER_PLUGINS_DIR%"

echo Copied %JAR_PATH% to %SERVER_PLUGINS_DIR%
endlocal
