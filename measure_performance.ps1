$adb = "C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$package = "com.hativ2"
$mainActivity = "com.hativ2/.MainActivity"
$outputFile = "performance_results.txt"

echo "Starting Performance Tests..." > $outputFile

# 1. Measure UI Jank (Add Expense Flow)
echo "`n--- UI Jank (Add Expense Flow) ---" >> $outputFile
echo "Resetting gfxinfo..."
& $adb shell dumpsys gfxinfo $package reset

echo "Running UI Test..."
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.hativ2.ui.screens.AddExpenseScreenTest

echo "Capturing gfxinfo..."
& $adb shell dumpsys gfxinfo $package >> $outputFile

# 2. Measure Startup Time
echo "`n--- Startup Time ---" >> $outputFile
echo "Force stopping app..."
& $adb shell am force-stop $package

for ($i=1; $i -le 3; $i++) {
    echo "Run $i..."
    & $adb shell am start -W -n $mainActivity | Select-String "TotalTime" >> $outputFile
    & $adb shell am force-stop $package
    Start-Sleep -Seconds 2
}

echo "Done. Results saved to $outputFile"
Get-Content $outputFile
