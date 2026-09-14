# 导了么（DAO Leme）

一个只记录当天状态的 Android 小应用：今天“导了”还是“没导”，由你自己决定、自己记录。

## 功能

- 今日打卡：`导了` / `没导`
- 类型单选：干导、看片导、睡前导、晨导，也可以在设置中管理类型
- 月历历史：查看每日状态和月份汇总
- 备注：每条记录最多 100 个字
- 每日一句：在打卡页查看当天话语
- 提醒：可自定义时间，默认 23:45；当天已有记录时自动跳过
- 外观：跟随系统、明亮、暗色
- 本地备份：JSON 导入 / 导出
- 本地存储：无需登录，不上传、不云端同步
- 日期保护：今天之后的日期不能补记或修改

## 环境要求

- Android Studio 或 JDK 18+
- Android SDK Platform 35
- Android SDK Build-Tools 34.0.0 或更高
- Gradle 9.5.0
- Android API 26（Android 8.0）及以上设备

## 构建 Debug APK

在仓库根目录执行：

```powershell
gradle assembleDebug
```

输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 构建 Release APK

Release 签名密钥不包含在仓库中。请准备自己的 keystore，并通过构建参数传入：

```powershell
gradle assembleRelease `
  -PRELEASE_STORE_FILE="D:/secure/dao_leme_release.jks" `
  -PRELEASE_STORE_PASSWORD="你的keystore密码" `
  -PRELEASE_KEY_ALIAS="dao_leme_release" `
  -PRELEASE_KEY_PASSWORD="你的key密码"
```

不要把密码、`.jks` 文件或 `local.properties` 提交到 GitHub。丢失发布 keystore 后，无法覆盖更新已发布的 APK。

## 项目结构

```text
app/src/main/java/       Android 原生 WebView、提醒和文件选择桥接
app/src/main/assets/     移动端网页界面与本地业务逻辑
app/src/main/res/        Android 资源
```

## Release

Release APK 下载地址：

<https://github.com/SugerMus/dao-leme/releases/tag/v1.0.0>

当前版本：`1.0.0`（versionName `1.0`，versionCode `1`）。

## 联系作者

作者：suger  
QQ：2397100743（添加时请备注来意）
