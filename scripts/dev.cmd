@echo off
setlocal
set "PROJECT_ROOT=%~dp0.."
set "GOAL=spring-boot:run"
if /I "%~1"=="test" set "GOAL=test"
if /I "%~1"=="package" set "GOAL=package"
if not "%~1"=="" if /I not "%~1"=="run" if /I not "%~1"=="test" if /I not "%~1"=="package" (
  echo Uso: scripts\dev.cmd [run^|test^|package] [porta]
  exit /b 1
)

set "JAVA_HOME=%USERPROFILE%\openlogic-openjdk-17.0.9+9-windows-x64"
if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo JDK non trovata in "%JAVA_HOME%".
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo JDK: %JAVA_HOME%

set "MAVEN_CMD="
for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do set "MAVEN_CMD=%%M"
if defined MAVEN_CMD goto mavenReady
for /d %%M in ("%USERPROFILE%\.maven\maven-*") do (
  if exist "%%~fM\bin\mvn.cmd" set "MAVEN_CMD=%%~fM\bin\mvn.cmd"
)
if not defined MAVEN_CMD (
  echo Installa Maven 3.9 e aggiungi la sua cartella bin al PATH.
  exit /b 1
)
:mavenReady

if not "%~2"=="" set "PORT=%~2"
pushd "%PROJECT_ROOT%"
call "%MAVEN_CMD%" -f backend/pom.xml %GOAL% -B -ntp
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%