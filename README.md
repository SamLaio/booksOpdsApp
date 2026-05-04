# BooksOpdsApp

Android OPDS Reader app (Kotlin + Jetpack Compose).

## Features

- Read OPDS feed (supports HTTP basic auth)
- Navigate OPDS sub-catalogs
- Pagination (`prev` / `next`)
- OPDS search (`rel=search`)
- Book detail dialog (cover, title/author, summary)
- Download book files via Android `DownloadManager`
- Saved OPDS profiles (with encrypted password storage)
- Light/Dark mode toggle (persisted)

## Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Android ViewModel + StateFlow
- OkHttp
- Coil

## Requirements

- Android Studio (recommended)
- JDK 17+ (or Android Studio embedded JBR)
- Android SDK (minSdk 24, target/compile 34)

## Run (Debug)

```powershell
cd D:\project\booksOpdsApp
.\gradlew.bat assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Build (Release)

Unsigned release:

```powershell
cd D:\project\booksOpdsApp
.\gradlew.bat assembleRelease
```

Output:

`app/build/outputs/apk/release/app-release-unsigned.apk`

Signed release:

Use Android Studio:

`Build > Generate Signed Bundle / APK... > APK > release`

## Project Structure

- `app/src/main/java/com/example/booksopdsapp/MainActivity.kt` - main screen
- `app/src/main/java/com/example/booksopdsapp/OpdsViewModel.kt` - UI state + actions
- `app/src/main/java/com/example/booksopdsapp/OpdsNavigator.kt` - URL/link resolution
- `app/src/main/java/com/example/booksopdsapp/OpdsRepository.kt` - network loading
- `app/src/main/java/com/example/booksopdsapp/OpdsParser.kt` - OPDS XML parsing
- `app/src/main/java/com/example/booksopdsapp/OpdsDialogs.kt` - dialog components
- `app/src/main/java/com/example/booksopdsapp/OpdsComponents.kt` - reusable UI components
- `app/src/main/java/com/example/booksopdsapp/ProfileStorage.kt` - profile persistence/encryption
- `app/src/main/java/com/example/booksopdsapp/DownloadUtils.kt` - download helper

## Notes

- App currently permits HTTP cleartext traffic for LAN OPDS use.
- If you plan to publish, prefer HTTPS and tighten network security config.
