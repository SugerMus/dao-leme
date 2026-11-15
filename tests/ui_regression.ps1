param(
    [string]$Source = (Join-Path $PSScriptRoot '../app/src/main/java/cool/dao/leme/MainActivity.java')
)
$ErrorActionPreference = 'Stop'
$sourceText = Get-Content -Raw -LiteralPath $Source
$ringSource = Get-Content -Raw -LiteralPath (Join-Path (Split-Path $Source) 'ComparisonRingView.java')
$analysis = [regex]::Match($sourceText, 'private LinearLayout analysisPage\(\) \{(?<body>.*?)\n    \}\r?\n\r?\n    private String periodLabel', [Text.RegularExpressions.RegexOptions]::Singleline)
if (-not $analysis.Success) { throw 'Could not locate analysisPage().' }
if ($analysis.Groups['body'].Value -notmatch 'page\.setOrientation\(LinearLayout\.VERTICAL\)') {
    throw 'analysisPage must use a vertical SwipePageLayout so statistics render below the tabs.'
}
if ($sourceText -notmatch 'private LinearLayout recordsPage\(\) \{\s*LinearLayout page=new SwipePageLayout\(true,\s*\(\) -> navigateCalendar\(-1\),\s*\(\) -> navigateCalendar\(1\)\);\s*page\.setOrientation\(LinearLayout\.VERTICAL\)') {
    throw 'Calendar swipe handling must cover the whole vertical records page.'
}
if ($sourceText -notmatch 'private final SwipeNavigation swipeNavigation = new SwipeNavigation\(') {
    throw 'SwipePageLayout must use the shared swipe state machine.'
}
if ($sourceText -match 'setUpdateListener\(animation\s*->\s*dock\.invalidate\(\)\)') {
    throw 'Page animation must not force expensive dock blur capture every frame.'
}
if ($sourceText -notmatch 'displayPage\(page, animate, direction\)' -or $sourceText -notmatch 'ObjectAnimator\.ofFloat\(page, View\.ROTATION_Y') {
    throw 'Page changes must use the card flip transition.'
}
if ($ringSource -match 'canvas\.drawText\("导了"') {
    throw 'Comparison rings must not repeat the “导了” label inside the ring.'
}
if ($ringSource -notmatch 'float textSize = 21 \* density' -or $ringSource -notmatch 'canvas\.drawText\(doneCount') {
    throw 'The done count must be the prominent ring center content.'
}
if ($sourceText -notmatch 'buildAboutPage\(' -or $sourceText -notmatch 'APP_AUTHOR' -or $sourceText -notmatch 'github\.com/SugerMus/dao-leme') {
    throw 'Settings must expose the author and GitHub project in an About page.'
}
if ($sourceText -notmatch 'BuildConfig\.VERSION_NAME' -or $sourceText -notmatch 'BuildConfig\.BUILD_TIME') {
    throw 'About page must read the packaged version and build time from BuildConfig.'
}
if ($sourceText -match 'buildAboutPage\(.*?UUID' -or $sourceText -match '构建 UUID') {
    throw 'About page must omit the internal build UUID from user-facing settings.'
}
if ($sourceText -notmatch 'comparisonDeltaCard\(' -or $sourceText -notmatch 'negativeSoft') {
    throw 'Monthly comparison deltas must use prominent warning cards.'
}
Write-Output 'UI layout and swipe wiring checks passed.'
