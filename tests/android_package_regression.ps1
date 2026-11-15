param(
    [string]$Apk = (Join-Path $PSScriptRoot '../app/build/outputs/apk/release/app-release.apk'),
    [string]$Sdk = (Join-Path $env:LOCALAPPDATA 'Android/Sdk'),
    [string]$ExpectedPackage = 'cool.dao.leme',
    [string]$ExpectedLabel = '导了么',
    [string]$ExpectedVersionCode = '8',
    [string]$ReferenceApk = ''
)
$ErrorActionPreference = 'Stop'
$tools = Get-ChildItem -LiteralPath (Join-Path $Sdk 'build-tools') -Directory | Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
$aapt = Join-Path $tools.FullName 'aapt.exe'
$apksigner = Join-Path $tools.FullName 'apksigner.bat'
$badging = (& $aapt dump badging $Apk | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'aapt could not read the APK.' }
if ($badging -notmatch "package: name='$([regex]::Escape($ExpectedPackage))'") { throw "Unexpected Android package ID (expected $ExpectedPackage)." }
if ($badging -notmatch "application: label='$([regex]::Escape($ExpectedLabel))' icon='[A-Za-z0-9_./-]+'"){ throw 'APK has no launcher icon resource or the launcher label is wrong.' }
if ($badging -notmatch 'application-icon-\d+:') { throw 'APK has no launcher icon resource.' }
$manifestVersion = $badging | Select-String "versionCode='$ExpectedVersionCode' versionName='2\.3\.1'"
if (-not $manifestVersion) { throw 'Unexpected APK version.' }
$entries = (& $aapt list $Apk | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'aapt could not list APK contents.' }
if ($entries -match '(?m)^assets/') { throw 'APK contains packaged web/assets files.' }
$signing = (& $apksigner verify --print-certs $Apk | Out-String)
$signerPattern = '(?m)^(?:V[123](?:\.0)? Signer: certificate|Signer #\d+ certificate) SHA-256 digest:\s*([0-9a-fA-F:]+)'
if ($LASTEXITCODE -ne 0 -or $signing -notmatch $signerPattern) { throw 'APK signature is invalid.' }
$signerLine = [regex]::Match($signing, $signerPattern).Groups[1].Value.Replace(':','').ToLowerInvariant()
if ($ReferenceApk) {
    $referenceSigning = (& $apksigner verify --print-certs $ReferenceApk | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'Could not verify reference APK signature.' }
    if ($referenceSigning -notmatch $signerPattern) { throw 'Reference APK has no readable signer certificate.' }
    $referenceSignerLine = [regex]::Match($referenceSigning, $signerPattern).Groups[1].Value.Replace(':','').ToLowerInvariant()
    if ($signerLine -ne $referenceSignerLine) {
        throw 'Update package signer does not match the reference APK.'
    }
}
Write-Output 'APK package, version, launcher icon, and APK signature checks passed.'
Write-Output ("Signer SHA-256: " + $signerLine)
