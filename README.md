# 导了么 Android

原生 Android 记录应用，作者：**Suger**。

- GitHub：<https://github.com/SugerMus/dao-leme>
- applicationId：`cool.dao.leme`
- 当前版本：`2.3.1`（versionCode `8`）
- 最低 Android 版本：API 26

## 功能

- 首页记录“导了 / 没导”，支持类型、备注和历史日期补记。
- 记录页支持左右滑动切换月份，12 月可连续滑到次年 1 月；点击年月可选择指定年月。
- 分析页支持周、月、年、总统计，以及近六周、六月、六年的双色环对比。
- 双色环中心只显示“导了”次数；对比区用大号趋势卡显示差值，导了减少或没导增加时以红色警示。
- 设置页提供提醒、主题、JSON 备份迁移和关于页；关于页展示作者、版本、北京时间编译时间和 GitHub 链接。
- 使用椭圆半透明底部导航栏、状态栏安全区适配和卡片翻动页面动画。

## 构建

需要 JDK、Android SDK（compileSdk 35）和可用的 Gradle 9.5.0 wrapper。

调试构建：

```powershell
.\gradlew.bat assembleDebug
```

Release 构建需要 release keystore。不要把 keystore、密码或 `local.properties` 提交到仓库；使用环境变量传入签名路径和密码：

```powershell
$env:DAO_RELEASE_STORE = 'D:\path\to\dao-leme-release.p12'
$secure = Read-Host 'Release keystore password' -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:DAO_RELEASE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    .\gradlew.bat clean assembleRelease --offline --no-daemon --console=plain `
      -x lintVitalRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    Remove-Item Env:DAO_RELEASE_STORE,Env:DAO_RELEASE_PASSWORD -ErrorAction SilentlyContinue
}
```

Release APK 输出在 `app/build/outputs/apk/release/app-release.apk`。正式构建需复用项目 release 签名，才能覆盖安装同签名旧版。

## 测试

```powershell
.\tests\run_ui_regressions.ps1
```

统计和月份边界回归也可按项目交付说明中的命令运行。当前工程不使用 WebView，不包含网页资源。

## 数据与隐私

记录仅保存在设备本地。通过设置中的 JSON 备份导出和导入数据。应用不要求账号，也不上传记录。
