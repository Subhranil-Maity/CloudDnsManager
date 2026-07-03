# Architecture — CloudDnsManager

> A concise reference for how the app is built, why it is built that way, and what the technology choices enable.

---

## 1. Overview

CloudDnsManager is an Android application for managing Cloudflare DNS infrastructure. It communicates directly with the Cloudflare v4 REST API to let users verify their API tokens, browse zones (domains), and view DNS records in a clean mobile interface.

The application is built around a **pure state machine frontend** where the UI is a pure function of state. This means composables are free of side effects; they simply render the current state emitted by a ViewModel and dispatch user actions as intents.

---

## 2. Architecture Pattern

The project follows an **MVVM (Model-View-ViewModel)** architecture enhanced with an **MVI-like Intent pattern**.

```
┌─────────────────────────────────────────────┐
│                    UI (Compose)             │
│  Renders state | Dispatches user actions    │
└──────────────────────┬──────────────────────┘
                       │ Intents
                       ▼
┌─────────────────────────────────────────────┐
│            ViewModel (Business Logic)       │
│  Receives intents | Updates state | Launches│
│  coroutines for I/O                         │
└──────────────────────┬──────────────────────┘
                       │ State Flow updates
                       ▼
┌─────────────────────────────────────────────┐
│                    State                    │
│  Immutable sealed interface representing    │
│  the UI at any given moment                 │
└──────────────────────┬──────────────────────┘
                       │ Emitted to UI
                       ▼
┌─────────────────────────────────────────────┐
│                    UI (Compose)             │
│  Re-renders based on new state              │
└─────────────────────────────────────────────┘
```

### The Pure State Machine Frontend

Every screen is implemented as a **pure state machine**:

- **State is a sealed interface**: Each screen has a sealed state type with distinct substates (e.g., `OnBoardingState: Idle`, `Verifying`, `Error`, `Verified`). Only one state is active at a time.
- **Actions are explicit**: Users trigger `*Intent` sealed classes (e.g., `OnBoardingIntent.VerifyToken`, `SelectZoneIntent.SelectZone`).
- **State transitions are centralized**: The ViewModel handles intents, performs side effects (API calls, storage), and transitions between sealed state types.
- **Composable purity**: Compose functions are deterministic given their state, making testing trivial and rendering predictable.

This approach deliberately borrows from Elm/MVI philosophy while remaining pragmatic with Ktor and standard Kotlin tooling.

---

## 3. Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Kotlin | 2.4.0 | Primary development language |
| **UI Framework** | Jetpack Compose | BOM 2026.06.00 | Declarative UI toolkit |
| **Design System** | Material 3 | — | Modern Material Design components |
| **Dependency Injection** | Koin | 4.2.2 | Lightweight DI without code generation |
| **HTTP Client** | Ktor | 3.5.0 | Type-safe networking with CIO engine |
| **Serialization** | kotlinx.serialization | 1.11.0 | JSON parsing with compile-time safety |
| **Navigation** | AndroidX Navigation 3 | 1.1.3 | Type-safe navigation with backstack management |
| **Data Storage** | AndroidX DataStore | 1.2.1 | Preferences storage with custom serializer |
| **Encryption** | AES via `Crypto` | — | At-rest token encryption with Base64 encoding |
| **Build System** | Gradle (Kotlin DSL) | 9.2.1 | Build automation |

---

## 4. Package Structure

```
com.subhranil.clouddnsmanager/
├── di/                           # Dependency injection setup (Koin)
│   └── module.kt                 # App module: ViewModels, SessionManager,
│                                 # TokenStorage, CloudflareClient factory
│
├── http/                         # Network layer
│   ├── engine.kt                 # Ktor HTTP engine, JSON config, base URL handling
│   ├── CloudflareClient.kt       # Typed API client for Cloudflare v4 endpoints
│   └── SessionManager.kt         # Singleton session: holds CloudflareClient,
│                                 # manages Authenticated/Unauthenticated state
│
├── nav/                          # Navigation layer
│   ├── NavDestinations.kt        # Sealed interface of all possible screens
│   ├── NavigationRouter.kt       # Centralized navigation state management
│   └── RootNavigation.kt         # Root Composable wiring Navigation3 backstack
│
├── models/                       # Data models (all @Serializable)
│   ├── dns/                      # DNS record models (DnsRecord, DnsRecordType, etc.)
│   ├── zone/                     # Zone models (Zone, ZoneStatus, ZonePlan, etc.)
│   ├── token/                    # Token models (Token, TokenVerification, etc.)
│   └── [generic]                 # Response wrappers, pagination, errors
│
├── storage/                      # Persistence layer
│   ├── crypto.kt                 # AES encryption/decryption helpers
│   ├── SecureSharedPref.kt       # DataStore serializer with encrypted I/O
│   └── TokenStorage.kt           # Interface + DataStore-backed implementation
│
├── dns/                          # DNS record viewer (replaces old zone/ package)
│   ├── DnsRecordScreen.kt        # Composable with BackHandler + state-driven rendering
│   ├── DnsRecordViewModel.kt     # Flow-based record loading with error/retry support
│   ├── DnsRecordState.kt         # Sealed DnsRecordDataState: Loading | Error | DnsRecordData
│   ├── DnsRecordIntent.kt        # ShowDetailed, DismissDetailedDrawer, Retry, GoBack
│   └── components/               # Reusable UI: DnsRecordRow, DnsRecordDetailDrawer, etc.
│
├── onboading/                    # Onboarding flow (API token input)
├── selectzones/                  # Zone selection screen
├── start/                        # App entry/loading screen
│
└── ui/theme/                     # Material 3 theme (Color, Type, Theme)
```

---

## 5. Data Flow

The data flow is unidirectional and predictable:

```
User Action
    │
    ▼
┌─────────────────────────────────┐
│  Compose emits Intent           │
│  (e.g., OnBoardingIntent.Verify)│
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│  ViewModel processes Intent     │
│  - Validates business logic      │
│  - Launches coroutine for I/O  │
│  - Calls SessionManager.login() │
│    (which creates CloudflareClient│
│     and verifies token)          │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│  CloudflareClient / Ktor        │
│  - Makes HTTP request           │
│  - Parses JSON response         │
│  - Returns typed objects        │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│  ViewModel transitions state    │
│  (sealed interface → new state)  │
└─────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────┐
│  Compose observes State change   │
│  (collectAsStateWithLifecycle)   │
│  UI re-renders based on new state│
└──────────────────────────────────┘
```

Once authenticated, SessionManager holds the `CloudflareClient` and downstream ViewModels (`SelectZoneViewModel`, `DnsRecordViewModel`) receive it via Koin injection — they never create their own client.

---

## 6. Navigation

Navigation is handled via **AndroidX Navigation 3**, using a **sealed interface** for type-safe destinations:

```kotlin
sealed interface NavDestinations : NavKey {
    data object StartScreenDestination : NavDestinations
    data object OnBoarding : NavDestinations
    data object SelectZonesDestination : NavDestinations
    data class DnsRecordsDestination(val zoneId: String) : NavDestinations
}
```

Key characteristics:
- **Centralized state**: `NavigationRouter` (Koin singleton) owns the current backstack
- **Reactive updates**: ViewModels push destinations; `RootNavigation` observes and renders
- **Type safety**: Navigation arguments are typed (e.g., `zoneId: String` in `DnsRecordsDestination`)
- **BackHandler support**: `DnsRecordScreen` intercepts system back presses via `DnsRecordIntent.GoBack`

---

## 7. State Management

Each screen follows the **State-Intent-ViewModel** triad with **sealed state types**:

| Component | Role | Example |
|-----------|------|---------|
| **State** | Sealed interface of possible UI states | `OnBoardingState { Idle, Verifying, Error, Verified }` |
| **Intent** | Sealed class of possible actions | `DnsRecordIntent { ShowDetailed, DismissDetailedDrawer, Retry, GoBack }` |
| **ViewModel** | Processes intents, transitions state | `DnsRecordViewModel` exposes `val state: StateFlow<DnsRecordState>` |

### Sealed Data States

Data-driven screens use a nested sealed type for content state:

```kotlin
sealed interface DnsRecordDataState {
    data object Loading : DnsRecordDataState
    data class Error(val error: String) : DnsRecordDataState
    data class DnsRecordData(val dnsList: List<DnsRecord>) : DnsRecordDataState
}

data class DnsRecordState(
    val dnsRecordDataState: DnsRecordDataState = DnsRecordDataState.Loading,
    val openDetailedDrawer: DnsRecord? = null,
)
```

This pattern enables exhaustive `when` branches in Compose:
```kotlin
when (val dataState = state.dnsRecordDataState) {
    is DnsRecordDataState.Loading       -> /* shimmer */
    is DnsRecordDataState.Error         -> /* error + retry */
    is DnsRecordDataState.DnsRecordData -> /* record list */
}
```

Every screen uses the same pattern — no `if/else` chains, no nullable state fields.

---

## 8. Session Management

The `SessionManager` in `http/` is the core orchestration singleton:

```kotlin
sealed interface SessionState {
    data object Unauthenticated : SessionState
    data object Loading : SessionState
    data class Authenticated(val client: CloudflareClient) : SessionState
}
```

- `initialize()` — Reads token from DataStore, creates client if token exists
- `login(token)` — Creates temp client, verifies token against Cloudflare, saves on success
- `logout()` — Closes client, clears token, transitions to Unauthenticated

Koin provides `CloudflareClient` as a **factory** that reads from `SessionManager`:

```kotlin
factory<CloudflareClient> {
    val state = get<SessionManager>().sessionState.value
    if (state is SessionState.Authenticated) state.client
    else throw IllegalStateException("CloudflareClient requested but user is not authenticated!")
}
```

---

## 9. API Layer

### `CloudflareHttpClient`
- Configures Ktor with CIO engine
- Attaches Bearer token to every request
- Handles JSON serialization with lenient config (ignores unknown keys)
- Validates Cloudflare envelope (`success`, `errors`, `result`)
- Transforms failures into typed `CloudflareException`

### `CloudflareClient`
- High-level typed API over the HTTP engine
- Methods map to Cloudflare v4 endpoints:
  - `verifyToken()` → `GET /user/tokens/verify`
  - `listZones()` → `GET /zones`
  - `listDnsRecords(zoneId)` → `GET /zones/{zoneId}/dns_records`
- Supports **auto-pagination** via `Flow<T>` for streaming all pages

---

## 10. Storage Layer

Token and preferences are stored using **DataStore** with a **custom encrypted serializer**:

```kotlin
DataStoreFactory.create(
    serializer = UserPreferencesSerializer,
    produceFile = { androidContext().dataStoreFile("user-preferences") }
)
```

- **Encrypted I/O**: `UserPreferencesSerializer` AES-encrypts the JSON bytes before writing and decrypts on read (with Base64 encoding for safe file storage)
- **`TokenStorage` interface**: Abstracted behind `DataStoreTokenStorage` for testability
- **Reactive**: Exposed as `Flow<UserPreferences>` for reactive UI updates

---

## 11. Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Ktor over Retrofit** | First-class Kotlin coroutines support, cleaner suspend function signatures, and native `Flow` integration for paginated data. |
| **Navigation 3 over legacy Navigation** | Simpler backstack management, better type safety with sealed classes, and more compose-native APIs. |
| **Sealed state over flat state** | Exhaustive `when` branches prevent unhandled UI states and make impossible states unrepresentable. |
| **Flow-based pagination** | Eagerly loads all pages via `client.allZones()` Flow with `catch`/`collect` instead of manual page loops. |
| **SessionManager singleton** | Centralizes auth state and client lifecycle; ViewModels never create their own HTTP clients. |
| **Koin over Hilt/Dagger** | Simpler setup, no kapt/codegen, faster builds, and sufficient for the app's DI needs. |
| **Encrypted DataStore** | Token stored at-rest with AES encryption + Base64 transport encoding. |

---

## 12. Future Improvements

- **Write operations**: Add create/update/delete for DNS records
- **Paging 3**: Replace eager Flow-based loading with true lazy pagination
- **Offline support**: Room database for caching zone/record data
- **Biometric auth**: Secure token access with biometric unlock
- **Dark mode**: Dynamic theme switching