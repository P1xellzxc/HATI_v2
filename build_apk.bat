@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
echo Using JAVA_HOME: %JAVA_HOME%
call gradlew.bat assembleDebug > build_log.txt 2>&1
