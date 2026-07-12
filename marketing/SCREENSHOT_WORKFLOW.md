# Screenshot Workflow

## Build debug app
```powershell
.\gradlew.bat assembleDebug
```

## Start demo mode
The marketing demo receiver exists only in the debug source set:
```powershell
adb shell am broadcast -a com.fridgefinish.app.DEBUG_MARKETING_DEMO
```

## Reset demo data
```powershell
adb shell pm clear com.fridgefinish.app
adb shell am broadcast -a com.fridgefinish.app.DEBUG_MARKETING_DEMO
```

## Capture manually from emulator
```powershell
tools\capture_marketing_screenshots.ps1
```

## Generate styled screenshots and all marketing assets
```powershell
tools\generate_marketing_pack.ps1
```

## Validate dimensions
```powershell
python tools\validate_marketing_assets.py
```
