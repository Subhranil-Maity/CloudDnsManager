# Architecture — CloudDnsManager

> A concise reference for how the app is built, why it is built that way, andenable and what the technology choices enable.

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
│  Immutable data class representing the UI   │
│  at any given moment                        │
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

- **State is single-source-of-truth**: The `State` class (e.g., `OnBoardingState`, `SelectZoneState`) holds all data needed to render the screen.
- **Actions are explicit**: Users trigger `*ntent` sealed classes (e.g., `OnBoardingIntent.VerifyToken`, `SelectZoneIntent.SelectZone`).
- **State transitions are centralized**: The ViewModel handles intents, performs side effects (API calls, storage), and emits a new state.
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
| **Data Storage** | AndroidX DataStore | 1.2.1 | Structured preferences with protobuf support |
| **Build System** | Gradle (Kotlin DSL) | 9.2.1 | Build automation |

---

## 4. Package Structure

```
com.subhranil.clouddnsmanager/
├── di/                        # Dependency injection setup (Koin)
│   └── module.kt              # App module: ViewModels, NavigationRouter, DataStore
│
├── http/                      # Network layer
│   ├── engine.kt              # Ktor HTTP engine, JSON config, base URL handling
│   └── CloudflareClient.kt    # Typed API client for Cloudflare v4 endpoints
│
├── nav/                       # Navigation layer
│   ├── NavDestinations.kt     # Sealed interface of all possible screens
│   ├── NavigationRouter.kt    # Centralized navigation state management
│   └── RootNavigation.kt      # Root Composable wiring Navigation3 backstack
│
├── models/                    # Data models (all @Serializable)
│   ├── dns/                   # DNS record models (DnsRecord, DnsRecordType, etc.)
│   ├── zone/                  # Zone models (Zone, ZoneStatus, ZonePlan, etc.)
│   ├── token/                 # Token models (Token, TokenVerification, etc.)
│   └── [generic]              # Response wrappers, pagination, errors
│
├── storage/                   # Persistence layer
│   ├── crypto.kt              # Encryption helpers
│   └── SecureSharedPref.kt    # DataStore-backed user preferences
│
├── onboading/                 # Onboarding flow (API token input)
├── selectzones/               # Zone selection screen
├── zone/                      # Zone details (DNS records view)
├── start/                     # App entry/loading screen
│
└── ui/theme/                  # Material 3 theme (Color, Type, Theme)
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
│  - ValidatesBusiness logic      │
│  - Launches coroutine for I/O   │
│  - Calls CloudflareClient       │
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
│  ViewModel updates State        │
│  (via MutableStateFlow)         │
└─────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────┐
│  Compose observes State change   │
│  (collectAsStateWithLifecycle)   │
│  UI re-renders based on new state│
└──────────────────────────────────┘
```

---

## 6. Navigation

Navigation is handled via **AndroidX Navigation 3**, using a **sealed class** for type-safe destinations:

```kotlin
sealed interface NavDestinations : NavKey {
    data object StartScreenDestination : NavDestinations
    data object OnBoarding : NavDestinations
    data object SelectZonesDestination : NavDestinations
    data class ZoneDetailsDestination(val zoneId: String) : NavDestinations
}
```

Key characteristics:
- **Centralized state**: `NavigationRouter` (Koin singleton) owns the current backstack
- **Reactive updates**: ViewModels push destinations; `RootNavigation` observes and renders
- **Type safety**: Navigation arguments are typed (e.g., Fixed `zoneId: String` in `ZoneDetailsDestination`)

---

## 7. State Management

Each screen follows the **State-Intent-ViewModel** triad:

| Component | Role | Example |
|-----------|------|---------|
| **State** | Immutable snapshot of UI | `OnBoardingState(token: "", isVerifying: false, error: null)` |
| **Intent** | Sealed class of possible actions | `OnBoardingIntent.UpdateToken`, `VerifyToken`, `DismissError` |
| **ViewModel** | Processes intents, holds state | `OnBoardingViewModel` exposes `val state: StateFlow<OnBoardingState>` |

This makes the UI a **pure function of state**:

```kotlin
@Composable
fun OnBoardingScreen() {
    val state = viewModel.state.collectAsStateWithLifecycle()
    // UI is 100% determined by `state`
}

// No side effects in composables!
// All logic lives in ViewModel.
```

---

## 8. API Layer

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

## 9. Storage Layer

User preferences (API token) are stored using **DataStore** with a custom serializer:

```kotlin
DataStoreFactory.create(
    serializer = UserPreferencesSerializer,
    produceFile = { androidContext().dataStoreFile("user-preferences") }
)
```

- **Structured**: Protobuf-backed for type safety and performance
- **Encrypted**: Utilizes Android's security best practices for sensitive data
- **Reactive**: Exposed as `Flow<UserPreferences>` for reactive UI updates

---

## 10. Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Ktor over Retrofit** | First-class Kotlin coroutines support, cleaner suspend function signatures, and native `Flow` integration for paginated data城隍庙. |
| **Navigation 3 over legacy Navigation** | Simpler backstack management, better type safety with sealed classes, and more compose-native APIs. |
| **Screen-level state machines** | Makes testing trivial, prevents UI bugs from side effects, and enables easy reasoning about any screen's behavior. |
| **Manual pagination loops** | Eagerly loads all pages into memory for small-to-medium datasets, simplifying UI logic at the cost of upfront loading. Future work: true lazy pagination. |
| **Koin over Hilt/Dagger** | Simpler setup, no kapt/codegen, faster builds, and sufficient for the app's DI needs. |

---

## 11. Future Improvements

- **Lazy pagination**: Convert eager page loops to `Flow`-based lazy loading with `Paging 3`
- **DNS record editing**: Add create/update/delete operations for full DNS management
- **Offline support**: Add Room database for caching zone/record data
- **Biometric auth**: Secure token storage with biometric unlock
- **Dark mode**: Dynamic theme switching support