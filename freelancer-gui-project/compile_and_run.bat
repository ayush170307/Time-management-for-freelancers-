@echo off
REM Run this from the project folder (the one that contains "app" and "lib").
javac -cp "lib/*" app\*.java
if errorlevel 1 (
    echo.
    echo Compilation failed - see errors above.
    pause
    exit /b 1
)
java -cp ".;lib/*" app.MainApp
