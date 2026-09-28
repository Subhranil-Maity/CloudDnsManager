# Changelog

## v1.0 — (2026-09-28)

CloudDnsManager is no longer view-only. This release adds full DNS record management, Cloudflare Email Routing (aliases, forwarding addresses and incoming-mail activity), an optional app lock, and a rule that every change must be confirmed with your PIN or biometrics.

### ✨ New

**DNS management**
- **Create, edit and delete records.** A full-screen editor supports A, AAAA, CNAME, TXT, MX, NS, CAA and SRV, with the right fields for each type: TTL (Auto or preset), the proxied switch where allowed, MX priority, SRV and CAA fields, and the Cloudflare comment. Other record types stay viewable.
- **Locked by Cloudflare.** Records that Cloudflare itself manages, such as the Email Routing MX and TXT records, are marked and can't be edited or deleted by mistake.
- **Your own locks, synced to Cloudflare.** Lock any record to protect it from edits and deletion. The lock is stored in the record's Cloudflare comment, so it follows you to other devices and shows in the dashboard.
- **Private notes.** Attach a note to any record. Notes are stored only on your device and never sent to Cloudflare.
- Search now also matches comments and notes. There's also an "Add record" button and a refresh button, and the list updates automatically after changes.

**Email Routing**
- **Zone page.** Tapping a zone now opens a page with **DNS** and **Email**.
- **Aliases.** List, search, enable or disable, edit where they forward, and delete.
- **Random aliases.** "Generate random" creates readable, hard-to-guess aliases like `quiet-river-4821@yourdomain.com`.
- **Destination addresses.** See which addresses are verified or pending, and add new ones (Cloudflare emails a verification link). Deleting an address warns you if aliases still forward to it.
- **Activity.** See incoming mail (sender, recipient, subject and status) for the whole zone or for a single alias.
- **Copy buttons** for email addresses, plus local notes and locks for aliases and addresses.

**Security**
- **Optional app PIN** (6 digits) with **biometric unlock**. The app locks on launch and after a minute in the background. After 5 wrong PINs input is blocked for 30 seconds, and each further failure doubles the wait. Wrong PINs never wipe data.
- **Every change needs authentication.** Creating, editing or deleting records and aliases, adding or removing addresses, logging out and unlocking all ask for your PIN or biometrics. Without an app PIN, your phone's screen lock is used. Destructive actions also ask for confirmation first.
- **Log out**, with confirmation. It removes the token, the PIN, and local notes and locks.
- While a PIN is set, the app is hidden from screenshots and the recent-apps preview.

### 🎨 UI/UX

- A branded splash screen replaces the blank "Checking session status…" screen.
- PIN entry uses six separate digit boxes, and PIN setup is a full-screen two-step flow.
- Error messages say which token permission is likely missing and include Cloudflare's own reason.

### 🐛 Fixes

- The API token was being written to the device log. It no longer is.
- Errors from zone and record lists were silently shown as empty lists. They now appear as errors with a Retry button.
- The login error dialog could disappear before you saw it.
- Tapping Retry repeatedly could load the same data twice.
- Email Routing lists with more than 50 entries now load completely.

### 🏗 Architecture

- DNS and Email are self-contained feature packages, each with its own navigation, dependency injection and API class.
- Shared encrypted local storage for notes, and one central lock manager for every feature.
- Unit tests for PIN hashing, lockout rules, locks, DNS validation and request building, alias generation and email activity parsing.

### 📝 Notes

- **Token permissions.** Grant the permissions for the features you want to use:

  | Scope | Permission | Needed for |
  |---|---|---|
  | Zone | Zone: Read | Listing zones (required) |
  | Zone | DNS: Edit | Viewing and changing DNS records (DNS: Read is enough to only view) |
  | Zone | Email Routing Rules: Edit | Viewing and changing aliases |
  | Account | Email Routing Addresses: Edit | Destination (forwarding) addresses |
  | Zone | Analytics: Read | Email activity |
  | Zone | Zone Settings: Read | *(optional)* Showing whether Email Routing is switched on |

  Destination addresses belong to a Cloudflare **account**, so the token's *Account Resources* must include every account whose addresses you want to manage.
- **Activity history** only goes back as far as Cloudflare keeps it, which depends on your plan.
- **Upgrading from v0.1.** Install over the old version. This release has versionCode 2, and your saved token is kept.
- **Minimum API:** Android 12 (API 31).

## v0.1 — (2026-06-26)

The very first release of CloudDnsManager — a native Android tool for browsing your
Cloudflare DNS infrastructure on the go.

### ✨ New

- **API Token Setup** — Paste your Cloudflare API token to get started. The app
  verifies the token against Cloudflare's API before proceeding.
- **Zone Browser** — See all zones (domains) your token has access to, listed
  with their current status, plan type, and name servers.
- **DNS Record Viewer** — Browse every DNS record for any zone. Each record
  displays its type, name, content, TTL, and proxy status.
- **Record Search** — Filter records in real-time by name, type, or content.
- **Record Detail Drawer** — Tap any record to open a bottom sheet with the
  full set of record metadata (created date, modified date, proxied state, etc.).

### 🎨 UI/UX

- Jetpack Compose with Material 3 design language.
- Responsive layout — adapts between portrait and landscape orientations.
- Shimmer loading placeholders while data is being fetched.
- Error dialogs with readable messages from Cloudflare API errors.

### 🏗 Architecture

- Pure state-machine frontend (MVVM + MVI-style Intents).
- Ktor HTTP client with automatic Cloudflare envelope parsing and error mapping.
- Koin dependency injection for clean module separation.
- Encrypted token storage via AndroidX DataStore.

### 📝 Notes

- **Read-only:** This release is view-only. You can browse zones and records
  but cannot create, edit, or delete them yet.
- **Minimum API:** Android 12 (API 31).
- **Network:** Requires an active internet connection to communicate with the
  Cloudflare API.
