# ReFxAndroid

Native Android client for **ReFx** — a game-server hosting platform. Built for 1:1
feature and visual parity with the SwiftUI iOS app, against the same backend.

- **Stack:** Kotlin · Jetpack Compose · Material 3 · MVVM (`ViewModel` + `StateFlow`)
- **Networking:** Retrofit + OkHttp + kotlinx.serialization (envelope auto-unwrap,
  401 refresh-once-retry, permissive enum decoding, tolerant ISO-8601 dates)
- **Min SDK 26**, target/compile SDK 35 · phone + tablet
- **Secure storage:** EncryptedSharedPreferences (tokens) · DataStore (origins/prefs)

## Module layout

```
app/src/main/java/gg/refx/android/
  core/
    design/    ReFx Glassy theme, color tokens, components (GlassCard, StatePill, buttons, skeleton)
    network/   ApiClient, envelope unwrap, auth/refresh, Money, Page, permissive enums, dates
    storage/   SecureTokenStore (EncryptedSharedPreferences), AppPreferences (DataStore)
    session/   SessionManager (single source of auth truth)
    ui/        LoadState, AsyncState, ScreenScaffold, WebLink (https-only Custom Tabs)
  data/
    model/     Account, Auth, enums (permissive .unknown fallback), Money-backed types
    api/       Retrofit service interfaces (AuthApi, AccountApi)
    repo/      AuthRepository
  feature/     auth, home, servers, support, account, staff (VM + screens)
  app/         Application, AppContainer (manual DI), MainActivity, nav graph, push
```

## Build & test

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # contract decoding tests
./gradlew lintDebug
```

The Android SDK location is read from `local.properties` (`sdk.dir=…`), which is
gitignored. CI provisions the SDK automatically.

## Status — Milestone 1 (Foundation) complete

Done: project + Gradle (version catalog, wrapper) · ReFx Glassy design system ·
`LoadState`/`AsyncState` · networking client (envelope, auth/refresh, dates,
errors, `Money`, `Page`, permissive enums) · EncryptedSharedPreferences token
store · DataStore origins · **Login → 2FA → authenticated shell** with role-aware
bottom nav (Home / Servers / Support / Staff / Account) · About & legal link-outs ·
sign out · contract unit tests · CI (assemble + test + lint) and a release
workflow (signed AAB → Play internal).

Remaining milestones (per `AndroidPortPlan.md`): servers + live console (2) ·
account/billing/support + push (3) · remaining server sections (4) · staff/admin
(5) · compliance + polish (6).

### Notes / things to reconcile with the human

- **Reference repos** (`ReFxFrank/ReFxHostingApp`, `ReFxFrank/ReFxHosting`) were not
  accessible from this environment. Exact color hex values, enum raw strings, and a
  few request/response field names are faithful approximations centralized for easy
  1:1 reconciliation (see `DesignTokens`, `Enums.kt`, the `NOTE:` comments).
- **Firebase:** `app/google-services.json` is gitignored config; the FCM plugin is
  applied only when that file is present (debug builds compile without it).
- **Play compliance (§8):** `purchasingEnabled` is on for debug, **off** for release
  builds via `BuildConfig.PURCHASING_ENABLED`.
- **Secrets** (keystore, Play service account, Firebase) live in CI secrets only.
