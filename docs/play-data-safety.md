# Google Play — Data safety form

Source-of-truth answers for the **Data safety** section of the Play Console
listing. Derived from what the app actually does in code (not aspirational).
Re-verify against the backend privacy policy before each submission.

> **Scope note.** This describes the **ReFx Android client only**. The backend
> (api.refx.gg) and the web checkout (refx.gg) have their own data handling;
> link the published privacy policy (https://refx.gg/privacy) in the Play Console.
> Support: https://refx.gg/support · Terms: https://refx.gg/terms.

## Summary of what the app handles

| Data type | Collected¹ | Shared² | Why | On-device only |
|---|---|---|---|---|
| Email address | Yes | No | Account sign-in & identification | — |
| Name (first/last) | Yes | No | Display name on the account | — |
| Password | Yes (transient) | No | Authentication. Sent over TLS, **never stored on device** | — |
| 2FA / TOTP codes | Yes (transient) | No | Two-factor authentication | — |
| FCM registration token | Yes | Yes (Google FCM)³ | Deliver push notifications | — |
| Purchase history (invoices, credit balance, subscriptions) | No⁴ | No | Displayed read-only from the account | — |
| Auth tokens (access/refresh) | No | No | Session — stored **encrypted on device only** | Yes |
| App preferences (API/web origin, last route) | No | No | App configuration | Yes |

¹ *Collected* = transmitted off the device by the app.
² *Shared* = transferred to a third party.
³ The FCM token is handed to Google's Firebase Cloud Messaging purely as the
  delivery address for notifications; no other user data is sent to Google by
  this app.
⁴ The app **displays** purchase history fetched from the user's own account but
  does not itself collect or transmit it anywhere new, and it does **not**
  collect any payment-card / financial-instrument data — card entry happens on
  the external web checkout, never in-app (see Play §8 note in `compliance.md`).

## Play Console answers (field-by-field)

**Does your app collect or share any of the required user data types?** → **Yes**

### Personal info
- **Name** — Collected. Not shared. Purpose: *Account management*. Required: not
  required to use the app, but present for signed-in accounts. Not used for ads.
- **Email address** — Collected. Not shared. Purpose: *Account management*,
  *Authentication*. Required.
- **User IDs** — Collected (account id, used in API calls). Not shared. Purpose:
  *Account management*, *App functionality*.

### App activity
- None for analytics/advertising. The app integrates **no** analytics,
  attribution, or advertising SDKs.

### App info and performance
- **Crash logs** — Not collected. (No crash-reporting SDK is integrated.)
- **Diagnostics** — Not collected.

### Device or other IDs
- **Device or other IDs** — Collected (the FCM registration token, which is a
  per-install messaging identifier). Shared with Google (Firebase Cloud
  Messaging) as the notification delivery channel. Purpose: *App functionality*
  (push notifications).

### Financial info
- **Purchase history** — *Accessed for display only.* If the Play Console
  requires it to be declared because purchase data is shown in-app, mark it
  Collected = No, Shared = No, and note it is read from the user's own account.
- **Payment info** — **Not collected.** No card or payment-instrument data is
  entered or handled in the app.

## Security & retention practices (Play Console)

- ☑ **Data is encrypted in transit** — all API traffic is HTTPS/TLS; cleartext
  traffic is disabled (no `usesCleartextTraffic`).
- ☑ **Users can request that data be deleted** — account deletion is available
  via the account/web flow; the deletion path is documented at
  https://refx.gg/privacy.
- ☑ **Tokens encrypted at rest** — session tokens are stored with
  `EncryptedSharedPreferences` (Android Keystore-backed). *(Play's "encrypted in
  transit" toggle covers network; on-device encryption is described here for
  completeness.)*
- ☐ **Data is NOT sold.**
- **Account required:** the app's primary function requires a ReFx account.

## Permissions declared (and why)

| Permission | Purpose |
|---|---|
| `INTERNET` | API calls to the ReFx backend |
| `ACCESS_NETWORK_STATE` | Detect connectivity for ret/refresh behaviour |
| `POST_NOTIFICATIONS` | Show push notifications (Android 13+) |

> Keep this table in sync with `AndroidManifest.xml`. Do **not** declare
> permissions the app does not exercise — Play rejects unused sensitive
> permissions and they widen the disclosed footprint.
