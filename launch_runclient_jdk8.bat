@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=%~dp0tools\jdk8u492-b09"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call gradlew.bat runClient
