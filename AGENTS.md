# Repository Guidelines

## Project Structure & Module Organization
- Android app lives in `app/`; Gradle scripts at the root (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`).
- Source code is in `app/src/main/java/com/example/connectmate/` with feature screens grouped by responsibility (activities/fragments in the root package, shared helpers in `utils/`, simple POJOs in `models/`).
- UI resources stay under `app/src/main/res/` (layouts, drawables, colors, menus). Keep new assets scoped to their existing subfolders.
- Design hand-offs and reference assets reside in `app/figma-to-android-studio-v1/`; leave generated files there so they are easy to regenerate.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` – compile the debug APK with view binding and debug suffix enabled.
- `./gradlew installDebug` – deploy the debug build to a connected/emulated device.
- `./gradlew test` – run JVM unit tests in `src/test/java`; useful for model and utility logic.
- `./gradlew connectedDebugAndroidTest` – execute Espresso instrumentation suites from `src/androidTest/java` on a device.
- `./gradlew lint` – run Android lint (fails only on local because CI disables abort-on-error).

## Coding Style & Naming Conventions
- Java 11 source/target; prefer Android Studio’s default formatting (4-space indent, braces on same line).
- Class names are PascalCase (`ActivityDetailActivity`), methods and variables camelCase, constants ALL_CAPS.
- Resource IDs and file names use lowercase underscore (`activity_main.xml`, `color/primary_blue`).
- Enable and use view binding (`ActivityDetailActivityBinding`) instead of `findViewById`; avoid Kotlin-specific APIs unless the module switches languages.

## Testing Guidelines
- JUnit4 powers local tests, Espresso covers UI flows; place shared fakes under `src/test/java/com/example/connectmate`.
- Name test classes `<Subject>Test` and instrumentation suites `<Subject>InstrumentedTest` to mirror production packages.
- Gatekeeping rule: new logic in `utils/` or `models/` ships with unit coverage; surface bugs should include failing tests before fixes.

## Commit & Pull Request Guidelines
- Recent history favors sentence-case summaries describing the change set (e.g., `Fixed nav bar error, created activity details page, debugging, etc.`). Follow that tone and keep bodies concise bullet lists.
- Reference tracking tickets or issue IDs in the body; include before/after screenshots for UI work.
- PRs should state the user impact, test evidence (`./gradlew test`, device names), and call out any new configuration keys.

## Configuration & Secrets
- API keys (`KAKAO_APP_KEY`, `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`) load from `local.properties` via `buildConfigField`. Never commit actual secrets—document placeholder keys instead.
- When adding third-party SDKs, update `google-services.json` through Firebase and note any required manifest placeholders in the PR description.
