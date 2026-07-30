@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@REM Begin all assignments locally
@setlocal

@REM Set the current directory to the location of this script
@set "MVNW_DIR=%~dp0"

@REM Check JAVA_HOME
@if not "%JAVA_HOME%"=="" goto javaHomeSet
@echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. >&2
@echo. >&2
@echo Please set the JAVA_HOME variable in your environment to match the >&2
@echo location of your Java installation. >&2
@goto error

:javaHomeSet
@if exist "%JAVA_HOME%\bin\java.exe" goto javaFound
@echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
@echo. >&2
@echo Please set the JAVA_HOME variable in your environment to match the >&2
@echo location of your Java installation. >&2
@goto error

:javaFound
@set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

@REM Determine Maven home directory
@set "MAVEN_PROJECTBASEDIR=%MVNW_DIR%"

@REM Download maven wrapper jar if not present
@set "WRAPPER_JAR=%MVNW_DIR%\.mvn\wrapper\maven-wrapper.jar"

@REM Properties file
@set "WRAPPER_PROPERTIES=%MVNW_DIR%\.mvn\wrapper\maven-wrapper.properties"

@REM Read properties
@for /f "usebackq tokens=1,* delims==" %%a in ("%WRAPPER_PROPERTIES%") do @(
    @if "%%a"=="distributionUrl" @set "DISTRIBUTION_URL=%%b"
    @if "%%a"=="wrapperUrl" @set "WRAPPER_URL=%%b"
    @if "%%a"=="wrapperVersion" @set "WRAPPER_VERSION=%%b"
)

@REM Default wrapper URL
@if "%WRAPPER_URL%"=="" @set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/%WRAPPER_VERSION%/maven-wrapper-%WRAPPER_VERSION%.jar"

@REM Maven home for downloaded distribution
@set "MAVEN_HOME_DIR=%USERPROFILE%\.m2\wrapper\dists"

@REM Create hash of distribution URL for unique directory
@set "DIST_NAME="
@for %%i in ("%DISTRIBUTION_URL%") do @set "DIST_NAME=%%~ni"
@set "MAVEN_HOME=%MAVEN_HOME_DIR%\%DIST_NAME%"

@REM Check if Maven is already downloaded
@if exist "%MAVEN_HOME%\bin\mvn.cmd" goto mavenReady

@echo Downloading Maven from %DISTRIBUTION_URL%...

@REM Create directories
@if not exist "%MAVEN_HOME_DIR%" @mkdir "%MAVEN_HOME_DIR%"

@REM Download distribution
@set "DIST_ZIP=%MAVEN_HOME_DIR%\%DIST_NAME%.zip"

@REM Try PowerShell download
@powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DIST_ZIP%' }" >nul 2>&1
@if errorlevel 1 (
    @echo ERROR: Failed to download Maven distribution. >&2
    @goto error
)

@REM Extract distribution
@echo Extracting Maven...
@powershell -Command "& { Expand-Archive -Path '%DIST_ZIP%' -DestinationPath '%MAVEN_HOME_DIR%' -Force }" >nul 2>&1
@if errorlevel 1 (
    @echo ERROR: Failed to extract Maven distribution. >&2
    @goto error
)

@REM Rename extracted folder
@set "EXTRACTED_DIR="
@for /d %%d in ("%MAVEN_HOME_DIR%\apache-maven-*") do @set "EXTRACTED_DIR=%%d"
@if not "%EXTRACTED_DIR%"=="" (
    @if not exist "%MAVEN_HOME%" @rename "%EXTRACTED_DIR%" "%DIST_NAME%"
)

@REM Clean up zip
@del "%DIST_ZIP%" >nul 2>&1

@REM Verify
@if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    @echo ERROR: Maven extraction failed. >&2
    @goto error
)

@echo Maven downloaded successfully.

:mavenReady
@REM Run Maven
@set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

@REM Pass along all arguments
"%MAVEN_CMD%" %* -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%"
@if errorlevel 1 goto error
@goto end

:error
@set ERROR_CODE=1

:end
@endlocal & @set ERROR_CODE=%ERROR_CODE%
@if not "%ERROR_CODE%"=="0" @exit /b %ERROR_CODE%
