# BooksOpdsApp

BooksOpdsApp 是一個以 Kotlin 與 Jetpack Compose 開發的 Android OPDS 閱讀器，用來瀏覽 OPDS 書庫、搜尋書籍、查看書籍資訊，並透過 Android DownloadManager 下載可讀檔案。

## 目前版本

- Version Name: `1.0.9`
- Version Code: `4`
- Application ID: `com.example.booksopdsapp`
- Min SDK: `24`
- Target SDK: `34`
- Compile SDK: `34`

## 功能

- 讀取 OPDS Feed，支援 HTTP Basic 驗證。
- 瀏覽 OPDS 子分類，並保留返回上一層的導覽狀態。
- 支援 OPDS `next` / `previous` / `prev` 分頁導覽。
- 支援 OPDS 搜尋連結。
- 顯示書籍詳情，包含封面、書名、作者、簡介與可下載格式。
- 透過 Android DownloadManager 下載書籍檔案。
- 依 URL、MIME type 與 OPDS link 推測下載副檔名。
- 盡量使用伺服器回傳的 `Content-Disposition` 檔名。
- 避免重複加入相同下載任務。
- 支援 OPDS 記憶清單，並加密保存密碼。
- 支援 OPDS 設定檔匯入與匯出。
- 支援日夜模式切換並記憶偏好。
- Debug URL 彈窗預設關閉。

## 使用方式

1. 開啟 App。
2. 輸入 OPDS Feed URL。
3. 如 OPDS 需要驗證，輸入帳號與密碼。
4. 點選讀取按鈕載入書庫。
5. 點選分類可進入子分類；點選書籍可查看詳情與下載格式。
6. 可勾選「記憶」保存 OPDS 設定，之後從記憶清單快速載入。

## 開發環境

- Android Studio
- JDK 17 或 Android Studio 內建 JBR
- Android Gradle Plugin `8.5.2`
- Kotlin `1.9.24`
- Gradle Wrapper `8.7`

## 專案結構

- `app/src/main/java/com/example/booksopdsapp/MainActivity.kt`: Compose 主畫面與操作流程。
- `app/src/main/java/com/example/booksopdsapp/OpdsViewModel.kt`: 畫面狀態、導覽、搜尋與下載連結解析。
- `app/src/main/java/com/example/booksopdsapp/OpdsNavigator.kt`: OPDS link 與 URL 解析。
- `app/src/main/java/com/example/booksopdsapp/OpdsRepository.kt`: 網路讀取。
- `app/src/main/java/com/example/booksopdsapp/OpdsParser.kt`: OPDS XML 解析。
- `app/src/main/java/com/example/booksopdsapp/OpdsDialogs.kt`: 搜尋、錯誤、記憶清單、書籍詳情等對話框。
- `app/src/main/java/com/example/booksopdsapp/OpdsComponents.kt`: 書籍卡片與可重用 UI 元件。
- `app/src/main/java/com/example/booksopdsapp/ProfileStorage.kt`: OPDS 設定檔儲存、匯入、匯出與密碼加密。
- `app/src/main/java/com/example/booksopdsapp/DownloadUtils.kt`: 書籍下載、檔名解析、MIME type 與重複下載處理。

## Debug 建置

```powershell
.\gradlew.bat assembleDebug
```

輸出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release 建置

```powershell
.\gradlew.bat assembleRelease
```

未簽章 APK 輸出位置：

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

若要產生正式可安裝版本，請使用 Android Studio 的 `Build > Generate Signed Bundle / APK...` 進行簽章。

## 注意事項

- App 目前允許 HTTP 明文流量，以支援區網 OPDS 伺服器。
- 若要公開發佈，建議使用 HTTPS 並收斂 network security 設定。
- 版本更新時，`versionName` 增加後，`versionCode` 也應同步加 1。
