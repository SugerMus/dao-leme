# Android Build Prerequisites

The Android SDK root is `C:\Users\Suger\AppData\Local\Android\Sdk`.

Install these SDK Manager packages before building:

- Android SDK Platform 35
- Android SDK Build-Tools 34.0.0 or newer
- Android SDK Platform-Tools

Then build from this directory with Gradle:

```powershell
$g = Get-ChildItem -LiteralPath 'C:\Users\Suger\.gradle\wrapper\dists\gradle-9.5.0-bin' -Recurse -Filter gradle.bat -File | Select-Object -First 1 -ExpandProperty FullName
& $g assembleDebug
```

Expected APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Delivered APK:

```text
D:\codex\WorkSpace\project003_dao_mobile_app\03_outputs\DAO_Leme.apk
```

## Native bridge

- Daily reminders use Android `AlarmManager` and are skipped after the current day has a record.
- Export uses Android's system document creator.
- Import uses Android's system document picker.
