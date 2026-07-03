@echo off
setlocal

set MAVEN_VERSION=3.9.9
set WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%MAVEN_ZIP%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%MAVEN_ZIP%' '%WRAPPER_DIR%'"
  if errorlevel 1 exit /b 1
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
