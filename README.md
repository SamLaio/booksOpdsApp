# BooksOpdsApp

使用 Kotlin + Jetpack Compose 開發的 Android OPDS 閱讀器。

## 功能

- 讀取 OPDS Feed（支援 HTTP Basic 驗證）
- 瀏覽 OPDS 子目錄
- 分頁導覽（`prev` / `next`）
- OPDS 搜尋（`rel=search`）
- 書籍詳情視窗（封面、書名/作者、簡介）
- 透過 Android `DownloadManager` 下載書籍檔案
- OPDS 記憶清單（密碼加密儲存）
- 日夜模式切換（可記憶）

## 技術棧

- Kotlin
- Jetpack Compose（Material 3）
- Android ViewModel + StateFlow
- OkHttp
- Coil

## 環境需求

- Android Studio（建議）
- JDK 17 以上（或 Android Studio 內建 JBR）
- Android SDK（minSdk 24、target/compile 34）

## 執行（Debug）

```powershell
cd D:\project\booksOpdsApp
.\gradlew.bat assembleDebug
```

Debug APK 輸出：

`app/build/outputs/apk/debug/app-debug.apk`

## 建置（Release）

未簽章 Release：

```powershell
cd D:\project\booksOpdsApp
.\gradlew.bat assembleRelease
```

輸出：

`app/build/outputs/apk/release/app-release-unsigned.apk`

已簽章 Release：

請使用 Android Studio：

`Build > Generate Signed Bundle / APK... > APK > release`

## 專案結構

- `app/src/main/java/com/example/booksopdsapp/MainActivity.kt` - 主畫面
- `app/src/main/java/com/example/booksopdsapp/OpdsViewModel.kt` - 畫面狀態與操作流程
- `app/src/main/java/com/example/booksopdsapp/OpdsNavigator.kt` - URL/連結解析
- `app/src/main/java/com/example/booksopdsapp/OpdsRepository.kt` - 網路讀取
- `app/src/main/java/com/example/booksopdsapp/OpdsParser.kt` - OPDS XML 解析
- `app/src/main/java/com/example/booksopdsapp/OpdsDialogs.kt` - 對話框元件
- `app/src/main/java/com/example/booksopdsapp/OpdsComponents.kt` - 可重用 UI 元件
- `app/src/main/java/com/example/booksopdsapp/ProfileStorage.kt` - 記錄與加密儲存
- `app/src/main/java/com/example/booksopdsapp/DownloadUtils.kt` - 下載工具

## 備註

- 目前 App 允許 HTTP 明文流量，以支援區網 OPDS 使用情境。
- 若要正式發佈，建議改用 HTTPS，並收斂 network security 設定。
