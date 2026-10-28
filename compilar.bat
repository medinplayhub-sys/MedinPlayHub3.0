@echo off
setlocal
set "DIRECTORIO_PROYECTO=%~dp0"
cd /d "%DIRECTORIO_PROYECTO%"

set "JAVA_HOME_INICIAL=%JAVA_HOME%"
set "JAVA_HOME="

if defined ANDROID_STUDIO_JDK call :usar_jdk "%ANDROID_STUDIO_JDK%"
if defined JAVA_HOME goto compilar
if defined STUDIO_JDK call :usar_jdk "%STUDIO_JDK%"
if defined JAVA_HOME goto compilar
if defined JAVA_HOME_INICIAL call :usar_jdk "%JAVA_HOME_INICIAL%"
if defined JAVA_HOME goto compilar

call :usar_jdk "%ProgramFiles%\Android\Android Studio\jbr"
if defined JAVA_HOME goto compilar
call :usar_jdk "%ProgramFiles(x86)%\Android\Android Studio\jbr"
if defined JAVA_HOME goto compilar
call :usar_jdk "%LOCALAPPDATA%\Programs\Android Studio\jbr"
if defined JAVA_HOME goto compilar
call :usar_jdk "%ProgramFiles%\Android\Android Studio\jre"
if defined JAVA_HOME goto compilar

for %%V in (25 24 23 22 21 20 19 18 17) do (
    for /d %%D in (
        "%ProgramFiles%\Java\jdk-%%V*"
        "%ProgramFiles%\Eclipse Adoptium\jdk-%%V*"
        "%ProgramFiles%\Microsoft\jdk-%%V*"
        "%LOCALAPPDATA%\Programs\Eclipse Adoptium\jdk-%%V*"
        "%LOCALAPPDATA%\Programs\Microsoft\jdk-%%V*"
    ) do (
        if exist "%%~D\bin\java.exe" if exist "%%~D\bin\javac.exe" (
            set "JAVA_HOME=%%~D"
            goto compilar
        )
    )
)

echo Error: no se encontro un JDK compatible en las ubicaciones habituales.
echo Instala Android Studio con su JDK incluido o configura ANDROID_STUDIO_JDK o JAVA_HOME.
exit /b 1

:compilar
if not exist "%DIRECTORIO_PROYECTO%gradlew.bat" (
    echo Error: no se encontro gradlew.bat en la raiz del proyecto.
    exit /b 1
)

echo Usando Java: "%JAVA_HOME%"
echo Compilando e instalando la aplicacion en el dispositivo conectado...
call "%DIRECTORIO_PROYECTO%gradlew.bat" :app:installDebug
set "CODIGO_SALIDA=%ERRORLEVEL%"
if "%CODIGO_SALIDA%"=="0" (
    echo Compilacion e instalacion completadas correctamente.
) else (
    echo Error: Gradle termino con el codigo %CODIGO_SALIDA%.
)
exit /b %CODIGO_SALIDA%

:usar_jdk
if exist "%~1\bin\java.exe" if exist "%~1\bin\javac.exe" set "JAVA_HOME=%~1"
exit /b 0
