@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Instala Java 25 o superior antes de continuar.
  pause
  exit /b 1
)
echo.
echo BRUJULA - Finanzas personales
echo Abre http://localhost:8091 cuando aparezca Started BrujulaApplication.
echo Para detener la aplicacion, presiona Ctrl+C en esta ventana.
echo.
java -Duser.timezone=America/Lima -jar brujula.jar
pause
