$ErrorActionPreference = 'Stop'
$sourceRoot = Join-Path $PSScriptRoot '../app/src/main/java/cool/dao/leme'
$classes = Join-Path $env:TEMP ('dao-leme-ui-regression-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes | Out-Null
& javac -encoding UTF-8 -d $classes (Join-Path $sourceRoot 'SwipeNavigation.java') (Join-Path $sourceRoot 'CalendarNavigation.java') (Join-Path $PSScriptRoot 'CalendarSwipeRegression.java')
if ($LASTEXITCODE -ne 0) { throw 'Could not compile calendar swipe regression.' }
& java -cp $classes cool.dao.leme.CalendarSwipeRegression
if ($LASTEXITCODE -ne 0) { throw 'Calendar swipe regression failed.' }
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'ui_regression.ps1')
if ($LASTEXITCODE -ne 0) { throw 'UI layout regression failed.' }
