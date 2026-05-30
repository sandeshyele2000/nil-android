# N.I.L - Network Intelligence Layer

[![Maven Central](https://img.shields.io/maven-central/v/io.github.sandeshyele2000/nil)](https://central.sonatype.com/artifact/io.github.sandeshyele2000/nil)

N.I.L (Network Intelligence Layer) is an Android network inspector library for **OkHttp** and **HttpURLConnection** clients.
It captures request/response data, stores events locally with Room, and provides a built-in Jetpack Compose UI to inspect network traffic inside your app.

## What It Does

- Captures HTTP traffic via a single API: `NIL.interceptor()` (OkHttp) or `NIL.interceptor(NIL.InterceptorType.HTTP_URL_CONNECTION)` (HttpURLConnection).
- Persists events in local Room database.
- Optional non-persistent mode (`persistenceEnabled = false`) to avoid Room storage entirely.
- Provides inspector UI (`NILInspectorActivity`) with:
- Event list with search.
- Status code filter (2xx/3xx/4xx/5xx).
- Pause/resume logging.
- Clear all events.
- Request/response detail view.
- Body analysis screen with plain text and JSON tree search.
- cURL generation and copy from a captured event.
- Optional draggable floating button overlay to open inspector from any activity.

## Project Structure

- `:nil` → Android library module (runtime + inspector UI).
- `:app` → Sample host app demonstrating integration with OkHttp + Retrofit.

Key package areas in `:nil`:

- `core/` → public API (`NIL`).
- `interceptor/` → OkHttp + HttpURLConnection interceptor implementations.
- `storage/`, `database/`, `model/` → persistence layer.
- `ui/` → inspector activity/screens/components.
- `overlay/` → floating button controller.

## Requirements

- Android Studio (recent version with AGP 9 support).
- JDK 17+
- Android SDK:
- Library compile SDK: `35`
- Sample app compile/target SDK: `36`
- Min SDK: `24`

## Installation (Maven Central)

```kotlin
implementation("io.github.sandeshyele2000:nil:1.0.5")
```

## Quick Start

### 1) Initialize once (typically in `Application` or app entry activity)

```kotlin
NIL.initialize(
    context = applicationContext,
    enableFloatingButton = true, // optional
    inspectorPayloadCharLimit = 200_000, // optional; persisted request/response body cap
    maxStoredEvents = 500, // optional; keeps latest 500 events
    persistenceEnabled = true // optional; false => skip Room persistence
)
```

### 2) Add interceptor to your OkHttp client

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(NIL.interceptor())
    .build()
```

### 3) Use the client normally

All requests executed through that client are captured automatically.

### 4) Wrap HttpURLConnection calls (optional)

```kotlin
val connection = (URL("https://httpbin.org/get?from=http-url-connection").openConnection() as HttpURLConnection).apply {
    requestMethod = "GET"
}

val responseBody = NIL.interceptor(NIL.InterceptorType.HTTP_URL_CONNECTION).intercept(
    connection = connection,
    execute = { it.inputStream.bufferedReader().use { reader -> reader.readText() } },
    responseBodyExtractor = { it }
)
```

## Public API

### `NIL.initialize(context, enableFloatingButton = false, inspectorPayloadCharLimit = 200_000, maxStoredEvents = 100, persistenceEnabled = true)`

Initializes database/repository and the optional floating inspector button.
Initialization runs once.

- `inspectorPayloadCharLimit` caps persisted request/response payload text. Larger payloads are truncated before storage.
- `maxStoredEvents` trims stored history to the latest N events.
- `persistenceEnabled = false` disables Room-backed storage and keeps events in-memory only for the current process lifetime.

### `NIL.interceptor()`

Returns the singleton interceptor for OkHttp usage.

### `NIL.interceptor(type: NIL.InterceptorType)`

Single entrypoint with explicit transport selection.
Use `NIL.interceptor(NIL.InterceptorType.HTTP_URL_CONNECTION).intercept(...)` to wrap `HttpURLConnection` execution and log request/response details.

### `NIL.events: StateFlow<List<NetworkEventSummary>>`

Reactive stream of captured event summaries for list rendering.
Useful if you want to build your own custom UI.

### `NIL.getEventById(eventId: String)`

Suspend API that returns the full `NetworkEvent` for a selected summary item.

### `NIL.setFilter(query: String)`

Applies repository-backed text filtering over events.
Used by built-in list search.

### `NIL.pauseLogging()` / `NIL.resumeLogging()`

Temporarily disable/enable event capture.

### `NIL.clearEvents()`

Clears stored events asynchronously (host-app friendly).

### `NIL.clearEventsAwait()`

Suspend variant that clears events and waits for completion.

## Inspector UX

From the built-in inspector you can:

- Browse captured traffic in reverse chronological order.
- Search URL/method text.
- Filter by status code groups.
- Pause logging while reproducing flows.
- Open event details:
- Request headers/params/body.
- Response headers/body.
- Copy generated cURL command.
- Analyze body content:
- Plain text search with next/previous navigation.
- JSON tree mode with key/value/path-aware matching.

## Sample App

The `:app` module demonstrates:

- N.I.L initialization with floating button enabled.
- One Retrofit request (`jsonplaceholder.typicode.com`).
- One raw OkHttp request (`httpbin.org`).
- Automatic event visibility in inspector.

Main entry point:

- `app/src/main/java/com/sandesh/nil/sample/MainActivity.kt`

## Build & Run

From project root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Run tests:

```bash
./gradlew :nil:testDebugUnitTest :app:testDebugUnitTest
./gradlew :nil:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

## Library Internals (High-Level)

1. `NILInterceptor` reads request body + headers.
2. Executes network call.
3. Reads response body safely for logging.
4. Persists `NetworkEvent` via `NILRepository`.
5. `NILRepository` exposes `NetworkEventSummary` items as `StateFlow` from Room observers and loads full events on demand.
6. Inspector screens consume this reactive stream.

## Notes & Limitations

- N.I.L captures calls made by OkHttp clients where `NIL.interceptor()` is added.
- For `HttpURLConnection`, calls are captured only when execution is wrapped with `NIL.interceptor(NIL.InterceptorType.HTTP_URL_CONNECTION).intercept(...)`.
- Capture includes request/response body as strings; avoid enabling in production if payloads may contain sensitive data.
- Large payloads are truncated before persistence based on `inspectorPayloadCharLimit`.
- Floating button requires initialization with an `Application` context to attach across activities.

## Development

Useful commands:

```bash
./gradlew :nil:assemble
./gradlew :nil:lint
./gradlew :nil:testDebugUnitTest
```

If publishing configuration is needed, see `nil/build.gradle.kts` (`mavenPublishing` block).

## License

Apache License 2.0
