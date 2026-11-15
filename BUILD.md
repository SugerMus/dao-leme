# 导了么原生 Android 构建

此工程只包含 Java 原生 Android 界面、记录存储、统计、提醒和图标资源，不依赖 WebView 或网页资源。

## 构建正式 APK

准备好 Android SDK 和 PKCS12 release 密钥后，可以运行工作区的发布脚本，或在 Windows PowerShell 中用隐藏输入的密码构建：

```powershell
$env:DAO_RELEASE_STORE = "D:\path\to\dao-leme-release.p12"
$secure = Read-Host "Release keystore password" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:DAO_RELEASE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    .\gradlew.bat clean assembleRelease --offline --no-daemon --console=plain -x lintVitalRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    Remove-Item Env:DAO_RELEASE_STORE,Env:DAO_RELEASE_PASSWORD -ErrorAction SilentlyContinue
}
```

正式 APK 输出为 `app/build/outputs/apk/release/app-release.apk`。源码包不含密钥、签名密码、本机 SDK 路径或构建缓存。发布脚本先清理旧构建目录，避免遗留资源进入安装包。

此版本使用包名 `cool.dao.leme`、版本 `2.3.1`（versionCode 8），并复用本项目 release 密钥，可覆盖安装签名一致的旧版本。若设备上的旧版签名不同，需先从旧 App 导出 JSON、卸载旧版、安装新 APK 后导入备份。

## 主要功能

- 打卡、历史日历、日期详情和备注。
- 月份左右滑动与年月选择器。
- 周、月、年、总统计，包含状态与类型次数；周、月、年分别支持近六周、六月、六年的双色环对比，环心只突出显示导了次数。
- 设置子页面的系统返回层级、提醒、主题与 JSON 导入/导出。
- 设置包含关于页：作者 Suger、版本号、编译时间与 GitHub 项目链接；不展示内部构建 UUID。
- 状态栏和导航栏安全区适配、圆角半透明底部栏及页面过渡动画。
- 底部 dock 低分辨率采样并模糊背后的页面，再绘制透明叠层和清晰的导航文字。
