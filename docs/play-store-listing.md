# Google Play — Store listing copy

Draft listing text for the ReFx Android app. Keep within Play's character
limits (Title ≤ 30, Short description ≤ 80, Full description ≤ 4000).

## App title (≤ 30)
```
ReFx — Game Server Hosting
```
*(27 chars)*

## Short description (≤ 80)
```
Manage your ReFx game servers: live console, files, backups, billing & support.
```
*(79 chars)*

## Full description (≤ 4000)
```
ReFx puts your game-server control panel in your pocket. Sign in to the same
ReFx account you use on the web and manage everything on the go — with a fast,
native Android experience built for phones and tablets.

MANAGE YOUR SERVERS
• Live console with real-time output and command input
• Start, stop, and restart servers from anywhere
• Browse and edit server files
• Create and restore backups
• Manage databases, scheduled tasks, and sub-users
• Switch games and adjust settings

STAY ON TOP OF EVERYTHING
• Real-time status for every server you own or help manage
• Push notifications for the events that matter
• Support tickets — open, follow up, and get answers
• Billing overview: invoices, subscriptions, and store credit

FOR STAFF
• Role-aware staff tools for admins and support agents
• Platform overview, server and user management, node operations
• Support queue and audit visibility

SECURE BY DESIGN
• Two-factor authentication supported at sign-in
• Session tokens stored encrypted on your device
• All traffic encrypted in transit

ReFx Android is a companion client for the ReFx hosting platform and requires a
ReFx account. Some purchases and payment changes are completed on the ReFx
website.
```

## Listing metadata
- **Category:** Tools (alt: Productivity)
- **Tags:** game server, hosting, control panel, server management
- **Support URL:** https://refx.gg/support
- **Contact email:** support@refx.gg *(confirm the monitored address)*
- **Privacy policy URL:** https://refx.gg/privacy
- **Terms of Service URL:** https://refx.gg/terms
- **Content rating:** complete the IARC questionnaire; expected **Everyone**
  (utility app, no user-generated public content, no ads).

## Required graphic assets (produce separately — not in repo)
These are binary assets and are intentionally **not** committed. Generate from
brand kit before submission:

| Asset | Spec |
|---|---|
| App icon | 512×512 PNG, 32-bit |
| Feature graphic | 1024×500 PNG/JPG |
| Phone screenshots | ≥ 2 (up to 8), 16:9 or 9:16, min 320px |
| 7" tablet screenshots | recommended (tablet support is real) |
| 10" tablet screenshots | recommended |

> Screenshot suggestions that show real features: Servers list, Live console,
> Server detail (Manage sections), Billing overview, Support ticket thread,
> Staff overview. Capture from a **debug** build so purchasing-gated UI is
> visible if desired, or a release build for a compliance-accurate set.

## Pre-launch checklist
- [ ] Privacy policy URL live and linked (https://refx.gg/privacy)
- [ ] Data safety form filled per `play-data-safety.md`
- [ ] Content rating questionnaire complete
- [ ] Signed AAB uploaded (release signing via CI secrets)
- [ ] Internal testing track validated before production
- [ ] App access instructions provided (test account for review)
