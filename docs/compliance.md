# Compliance notes — ReFx Android

Operational notes for keeping the app compliant with Google Play policy. Read
alongside `play-data-safety.md` and `play-store-listing.md`.

## §8 — Purchases & Google Play Billing

ReFx sells digital goods (server plans, store credit, subscriptions). Google
Play policy requires Play Billing for in-app digital purchases and forbids
steering users to external payment from within the app. Until/unless the app
integrates Play Billing, the release build **hides every purchase entry point**.

This is enforced by a single build flag, `BuildConfig.PURCHASING_ENABLED`,
surfaced as `AppContainer.purchasingEnabled`:

- **debug build:** `PURCHASING_ENABLED = true` — purchase UI visible for
  internal testing.
- **release build:** `PURCHASING_ENABLED = false` — purchase UI hidden.

### Entry points gated by the flag
| Surface | File | What's hidden when off |
|---|---|---|
| New server | `feature/servers/ServersListScreen.kt` | "+" FAB |
| Plan upgrade/resize | `feature/servers/ServerSection.kt` (`applicableFor`) + `ServerDetailScreen.kt` | "Upgrade" manage section |
| Pay invoice | `feature/billing/InvoiceDetailScreen.kt` | "Pay on web" button |
| Add payment card | `feature/billing/BillingScreen.kt` | "Add card on web" button |

`ServerSection.isPurchase` marks purchase-initiating sections; `applicableFor(server,
purchasingEnabled)` drops them when purchasing is off. Covered by
`ServerSectionTest`.

### Explicitly NOT gated (account management, allowed)
- Cancel / resume an existing subscription (no new charge, no external surface)
- Viewing invoices, credit balance, and the payment-methods list
- All non-billing server management (console, files, backups, etc.)

> When adding any new flow that creates a paid resource or opens an external
> payment surface, gate it behind `container.purchasingEnabled` and add it to
> the table above.

## Privacy & data handling
- No analytics, attribution, advertising, or crash-reporting SDKs are
  integrated. The only Google dependency is **Firebase Cloud Messaging** for
  push delivery.
- Auth tokens live only in `EncryptedSharedPreferences` (Android Keystore).
- All network traffic is HTTPS; cleartext is not permitted.
- External links (web checkout, panels, legal pages) open via Custom Tabs and
  are **https-only** (`core/ui/WebLink` rejects other schemes). The in-app
  Privacy Policy / Terms link-outs (AccountScreen) resolve to
  https://refx.gg/privacy and https://refx.gg/terms via `ApiConfig.webUrl(...)`;
  support is https://refx.gg/support.

## Permissions hygiene
Declare only permissions the app exercises. Current set: `INTERNET`,
`ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`. Re-audit on every feature that
touches a new capability; remove any permission that loses its last caller.

## Secrets
Keystore, Play service-account JSON, and `google-services.json` are **never**
committed (all gitignored). They are provided to CI via secrets. Debug builds
compile without `google-services.json` (FCM no-ops).

## Release pipeline
`/.github/workflows/release.yml` builds a signed AAB and uploads to the Play
internal track. Signing config is populated from CI secrets
(`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
`RELEASE_KEY_PASSWORD`); without them the build falls back to debug signing for
local verification only.
