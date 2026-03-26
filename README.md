# AutoTypeHID

AutoTypeHID is an Android app that turns a phone into a Bluetooth HID keyboard and automates typing scripts on paired host devices.

It is designed for reliable scripted input, practical device reconnect workflows, and configurable human-like typing behavior.

## Highlights

- Bluetooth HID keyboard emulation from Android
- Script library with create, edit, delete, and selection flows
- Typing control: start, pause/resume, stop
- Typing behavior tuning:
  - Speed multiplier
  - Typo probability
  - Word-gap delay
  - Random jitter
- Preset profiles (`NORMAL`, `FAST`, `SLOW`) plus `CUSTOM`
- Saved devices list with reconnect support
- Permission-aware startup flow
- Foreground typing service for long-running tasks
- Jetpack Compose UI with MVVM architecture

## Tech Stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: MVVM
- Navigation: Compose Navigation
- State: Kotlin Coroutines + StateFlow/SharedFlow
- Persistence:
  - DataStore (app settings)
  - Local known-device storage for reconnect UX
- Bluetooth:
  - Android Bluetooth APIs
  - Bluetooth HID Device profile integration
- Build: Gradle (Kotlin DSL)

## Project Structure

- `app/src/main/java/com/autotypehid/presentation/`
  - Compose screens, UI contracts, and ViewModels
- `app/src/main/java/com/autotypehid/domain/`
  - Models and use cases
- `app/src/main/java/com/autotypehid/data/`
  - Bluetooth, local storage, and repositories
- `app/src/main/java/com/autotypehid/core/`
  - App wiring and foreground typing service
- `app/src/main/res/`
  - Android resources (icons, themes, strings)

## Requirements

- Android Studio (recent stable version)
- JDK 17
- Android device with Bluetooth support (recommended for full testing)
- Minimum Android SDK as configured in `app/build.gradle.kts`

## Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync finish.
4. Connect a physical Android device (recommended) and enable USB debugging.

## Build and Run

From project root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

To run checks:

```bash
./gradlew :app:compileDebugKotlin
```

## Runtime Permissions

AutoTypeHID may request these depending on Android version and flow:

- Bluetooth permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`)
- Location permission for Bluetooth scanning requirements on Android
- Notification permission for foreground service visibility on newer Android versions

## How It Works

1. App starts and validates permission state.
2. User proceeds to Home dashboard.
3. User can connect/reconnect to a target device from saved devices or scan flow.
4. A script is selected from script management screens.
5. Typing session is controlled from the typing screen and executed via foreground service.

## Release Notes

- Optimized launcher icon assets and adaptive icon configuration
- Improved startup and dashboard UX flows
- Saved-device reconnect and settings persistence enhancements

## Contributing

Contributions are welcome.

- Open an issue describing the bug or enhancement.
- Keep changes focused and test on a physical device when Bluetooth behavior is involved.
- Include build/test notes in pull requests.

## License

Add your preferred license file (for example, MIT, Apache-2.0) and update this section accordingly.
