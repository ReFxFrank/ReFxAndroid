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
    realtime/  ConsoleSocket (Socket.IO live console, dual-bearer handshake, FIFO cap)
    storage/   SecureTokenStore (EncryptedSharedPreferences), AppPreferences (DataStore)
    session/   SessionManager (single source of auth truth)
    ui/        LoadState, AsyncState, ScreenScaffold, WebLink (https-only Custom Tabs)
  data/
    model/     Account, Auth, billing, support, servers, admin; enums (permissive UNKNOWN
               fallback), Money-backed types, state→color maps
    api/        Retrofit service interfaces (Auth, Account, Servers, Billing, Support,
               Upgrade, Staff, …)
    repo/      repositories per domain
  feature/     auth, home, servers (+ console/files/backups/databases/schedules/sub-users/
               settings/switch-game/upgrade), billing, support, account, staff (VM + screens)
  app/         Application, AppContainer (manual DI), MainActivity, nav graph, push (FCM)
docs/          Play data-safety, store-listing copy, compliance notes
```

## Build & test

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # contract decoding tests
./gradlew lintDebug
```

The Android SDK location is read from `local.properties` (`sdk.dir=…`), which is
gitignored. CI provisions the SDK automatically.

## Status — all milestones (1–6) complete

- **M1 Foundation:** Gradle (version catalog, wrapper) · ReFx Glassy design system ·
  `LoadState`/`AsyncState` · networking client (envelope unwrap, 401 refresh-once,
  tolerant dates, `Money`, `Page`, permissive enums) · EncryptedSharedPreferences
  tokens · DataStore origins · **Login → 2FA → authenticated shell** with role-aware
  bottom nav (Home / Servers / Support / Staff / Account) · CI + release workflow.
- **M2 Servers + live console:** servers list (search, paging), server detail,
  Socket.IO live console (dual-bearer handshake, 2000-line FIFO, refresh-on-unauth),
  power controls.
- **M3 Account / billing / support + push:** billing (credit, subscriptions,
  invoices, payment methods), support tickets (list, thread, create), account
  sub-screens, FCM push end-to-end (register on sign-in, router, cold-launch
  deep-link).
- **M4 Remaining server sections:** files, backups, databases, schedules,
  sub-users, settings (reinstall), switch-game, upgrade/resize. Game-conditional
  sections (mods/modpacks/workshop/voice) link out to the web panel.
- **M5 Staff / admin:** staff overview, admin servers (+create), users (+detail,
  store-credit grants), nodes (+bootstrap-token reveal), support queue, alerts,
  audit log, config reads.
- **M6 Compliance + polish:** Play §8 purchasing gate across every purchase entry
  point · accessibility pass (selectable semantics, ≥48dp touch targets) · Play
  data-safety / store-listing / compliance docs under `docs/`.

Each milestone shipped behind a green build (compile + unit tests + lint) and an
adversarial multi-agent verification pass.

### Notes / things to reconcile with the human

- **Reference repos** (`ReFxFrank/ReFxHostingApp`, `ReFxFrank/ReFxHosting`) were not
  accessible from this environment. The parity spec (`ReFxParitySpec.md`) reconciled
  enums (UPPERCASE raws), pagination shape, exact hex, and the MFA flow; remaining
  approximations are centralized for 1:1 reconciliation (see `DesignTokens`,
  `Enums.kt`, the `NOTE:` comments).
- **Game-conditional & admin-config screens** without DTOs in the spec
  (mods/modpacks/workshop/voice; products/templates/billing-admin) are intentional
  web link-outs rather than guessed native screens.
- **Firebase:** `app/google-services.json` is gitignored config; the FCM plugin is
  applied only when that file is present (debug builds compile without it).
- **Play compliance (§8):** `purchasingEnabled` is on for debug, **off** for release
  builds via `BuildConfig.PURCHASING_ENABLED`. Gate coverage and the data-safety
  answers live in `docs/compliance.md` and `docs/play-data-safety.md`.
- **Secrets** (keystore, Play service account, Firebase) live in CI secrets only.
