# Il Cammino di Francesco

A native Android app built with Kotlin and Jetpack Compose for exploring the life and teachings of Saint Francis of Assisi through four integrated interactive experiences. The current app content and interface are in Italian.

## Features

- **Il Viandante (The Wayfarer)** — a narrative adventure with choices and consequences in thirteenth-century Umbria.
- **Il Giardino di Francesco (Francis' Garden)** — a contemplative environment that grows as the user makes discoveries.
- **Le Vie della Saggezza (Paths of Wisdom)** — cards, scenarios, and dilemmas examined from different perspectives.
- **Il Dialogo (The Dialogue)** — offline guided conversations with a mentor, never with Francis himself.
- **Conoscenza (Knowledge)** — biography, timeline, teachings, sources, places, people, and a glossary.
- **Personal journal** — stored entirely on the device and exportable as Markdown.

## Project status

Version **0.1.0** is the first complete implementation of the four core experiences.

- The five-chapter narrative, garden, card paths, guided dialogues, knowledge section, and local journal are implemented.
- Level 1 of Dialogue uses an offline conversation graph. Level 2 (AI) is only represented by the documented `ConversationBackend` interface: there is no AI provider, network integration, or API key.
- Game content is stored as validated JSON assets rather than hardcoded in the UI.
- Automated JVM, shipped-content, and Room database tests are included. Compose UI navigation tests are also included and require a working Android emulator or device.

See `TESTING.md` for executed coverage, environment-specific limitations, and the current test strategy.

## Privacy and editorial principles

- No accounts, advertising, analytics, or tracking.
- Fully offline functionality and local-only personal data.
- No moralistic gamification: progression records discoveries, not merit.
- Every teaching is linked to its sources, distinguishing authentic writings, medieval testimonies, hagiography, modern interpretation, and game narrative.
- Direct quotations must come from verified sources; other text is identified as editorial paraphrase.

## Tech stack

- Kotlin 2.3.21 and Jetpack Compose
- Android Gradle Plugin 9.4.1 and Gradle 9.8.1 (wrapper)
- Room 2.8.5 and DataStore for local persistence
- Navigation Compose with type-safe routes
- kotlinx-serialization for content and game-state persistence
- Minimum SDK 26; compile and target SDK 37
- Manual dependency injection through `AppContainer`

## Project structure

- `app/` — UI, navigation, dependency injection, and asset-backed content repository
- `core/model/` — serializable domain types, pure Kotlin/JVM
- `core/content/` — JSON schemas, content loader, and validator, pure Kotlin/JVM
- `core/narrative/` — Wayfarer story engine, pure Kotlin/JVM
- `core/progression/` — Knowledge & Experience Engine, pure Kotlin/JVM
- `core/database/` — Room, DataStore, and repositories
- `core/designsystem/` — Umbrian-manuscript theme and Compose components

## Build

Prerequisites:

- JDK 21
- Android SDK with platform 37 and build-tools 37.0.0

Build the debug APK:

```bash
./gradlew :app:assembleDebug
```

## Test

Run the JVM test suites, including validation of the shipped JSON content:

```bash
./gradlew test
```

Run the content quality gate directly:

```bash
./gradlew :core:content:test --tests "*ShippedContentTest*"
```

Run the Room database tests:

```bash
./gradlew :core:database:testDebugUnitTest
```

Run the instrumented Compose UI tests on a connected device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Documentation

| File | Contents |
|---|---|
| `ARCHITECTURE.md` | Module structure, technical decisions, and persistence |
| `GAME_DESIGN.md` | Mechanics of the four experiences and progression |
| `CONTENT_SCHEMA.md` | JSON content schema and instructions for adding content |
| `FRANCISCAN_SOURCES.md` | Sources, editorial decisions, and certainty levels |
| `EDITORIAL_GUIDELINES.md` | Rules for quotation attribution and paraphrase |
| `ASSET_LICENSES.md` | Asset inventory and licensing |
| `TESTING.md` | Test strategy, coverage, and known limitations |
