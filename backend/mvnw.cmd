@echo off
setlocal
set "MAVEN_VERSION=3.9.11"

rem Load backend/.env for local Windows runs. Deployment platforms inject env vars directly.
if exist ".env" (
  for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" if not "%%A:~0,1%%"=="#" set "%%A=%%B"
  )
)
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"

if exist "%MAVEN_BIN%" goto run

echo Maven %MAVEN_VERSION% is not installed. Downloading it...
set "TMP_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%.zip"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%TMP_ZIP%'"
if errorlevel 1 exit /b 1

if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%TMP_ZIP%' '%MAVEN_HOME%'"
if errorlevel 1 exit /b 1
del "%TMP_ZIP%" >nul 2>&1

:run
call "%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
