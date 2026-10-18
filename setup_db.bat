@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "MYSQL_BIN=%MYSQL_BIN%"
if "%MYSQL_BIN%"=="" set "MYSQL_BIN=mysql"
set "MYSQL_USER=%MYSQL_USER%"
if "%MYSQL_USER%"=="" set "MYSQL_USER=root"
set "MYSQL_PASSWORD=%MYSQL_PASSWORD%"
if "%MYSQL_PASSWORD%"=="" set "MYSQL_PASSWORD=2805"
if not exist "lib" mkdir "lib"
if not exist "lib\mysql-connector-j-9.0.0.jar" (
  echo Downloading MySQL Connector/J...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$u='https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.0.0/mysql-connector-j-9.0.0.jar'; Invoke-WebRequest -Uri $u -OutFile 'lib\mysql-connector-j-9.0.0.jar'"
  if errorlevel 1 (echo Connector download failed.& exit /b 1)
)
echo Importing database/atm_schema.sql...
if "%MYSQL_PASSWORD%"=="" (
  "%MYSQL_BIN%" -u "%MYSQL_USER%" < database\atm_schema.sql
) else (
  "%MYSQL_BIN%" -u "%MYSQL_USER%" --password="%MYSQL_PASSWORD%" < database\atm_schema.sql
)
if errorlevel 1 (echo Database setup failed. Check MySQL service, user, password, and MYSQL_BIN.& exit /b 1)
echo Database setup completed.
endlocal
