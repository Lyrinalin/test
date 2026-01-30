@echo off
setlocal

set BUILD_DIR=build\meteorbow_datapack
set ZIP_PATH=build\meteorbow_datapack.zip

if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"

copy pack.mcmeta "%BUILD_DIR%" >nul
xcopy data "%BUILD_DIR%\data" /e /i /q >nul

if exist "%ZIP_PATH%" del /q "%ZIP_PATH%"

powershell -NoLogo -NoProfile -Command "Compress-Archive -Path '%BUILD_DIR%\pack.mcmeta','%BUILD_DIR%\data' -DestinationPath '%ZIP_PATH%' -Force"

endlocal
