@echo off
chcp 65001 > nul
title BINGO - JUGADOR

REM Ir a la carpeta del .bat (también con rutas de red)
pushd "%~dp0"

set RUTA_COMPARTIDA=\\192.168.0.112\BingoCompartido
set JAVA_EXE=

echo Buscando Java moderno (21+)...

REM 1. PRIORIDAD: Java de Eclipse del usuario actual (JustJ - suele ser 21)
for /d %%D in ("%USERPROFILE%\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_*") do (
    if exist "%%D\jre\bin\java.exe" (
        set JAVA_EXE="%%D\jre\bin\java.exe"
        goto encontrado
    )
)

REM 2. Eclipse Adoptium / Temurin 21
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
    if exist "%%D\bin\java.exe" (
        set JAVA_EXE="%%D\bin\java.exe"
        goto encontrado
    )
)

REM 3. Java 21 en Program Files
if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set JAVA_EXE="C:\Program Files\Java\jdk-21\bin\java.exe"
    goto encontrado
)

REM 4. Java 17 (también vale)
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set JAVA_EXE="C:\Program Files\Java\jdk-17\bin\java.exe"
    goto encontrado
)
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do (
    if exist "%%D\bin\java.exe" (
        set JAVA_EXE="%%D\bin\java.exe"
        goto encontrado
    )
)

REM 5. Último recurso: java del PATH (puede ser antiguo)
where java >nul 2>&1
if %errorlevel%==0 (
    set JAVA_EXE=java
    goto encontrado
)

echo.
echo ERROR: No se ha encontrado un Java compatible (se necesita 17 o 21).
echo Instala Eclipse o Java 21 y vuelve a intentarlo.
echo.
popd
pause
exit /b 1

:encontrado
echo Java encontrado: %JAVA_EXE%
echo Conectando a: %RUTA_COMPARTIDA%
echo Directorio de trabajo: %CD%
echo.

%JAVA_EXE% -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp bin jugador.JugadorMain "%RUTA_COMPARTIDA%"

if errorlevel 1 (
    echo.
    echo ERROR: Fallo al ejecutar el juego.
)

popd
pause