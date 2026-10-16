# AGENTS.md — Il Cammino di Francesco

App Android nativa (Kotlin + Jetpack Compose) per conoscere San Francesco d'Assisi attraverso quattro modalità interattive. Repo: `orma`.

## Comandi essenziali

```bash
export JAVA_HOME=$HOME/.local/opt/jdk-21.0.12.1+1   # JDK 21 locale (il sistema ha solo JRE 25)
./gradlew :app:assembleDebug                         # build completa
./gradlew test                                       # test JVM (core:content/narrative/progression)
./gradlew :core:content:test --tests "*ShippedContentTest*"  # cancello di qualità dei contenuti
```

Android SDK in `~/Android/Sdk` (già referenziato da `local.properties`). Gradle 9.8.1 via wrapper, AGP 9.4.1 (Kotlin built-in: NON applicare `org.jetbrains.kotlin.android`), Kotlin 2.3.21, KSP 2.3.12.

## Regole dure del progetto

1. **Mai inventare citazioni di Francesco.** Solo passaggi verificati; il resto è parafrasi editoriale etichettata (`license: editorial_paraphrase`). Le cinque categorie di fonte sono obbligatorie.
2. **I contenuti vivono in JSON** (`app/src/main/assets/content/it/`), mai hardcoded in Kotlin. Dopo ogni modifica ai contenuti eseguire `ShippedContentTest` — zero errori prima di considerare il lavoro fatto.
3. **Gli engine sono JVM-puri** (`core:narrative`, `core:progression`, `core:content`): niente dipendenze Android lì dentro; la UI non contiene logica narrativa.
4. **Niente gamification moralistica**: niente punteggi di bontà, streak, classifiche. La progressione traccia scoperte, non meriti.
5. Privacy-first: niente analytics, niente permessi superflui, diario solo locale.

## Struttura

- `core/model` — tipi di dominio serializzabili (Kotlin JVM puro)
- `core/content` — schemi JSON, loader, validatore (+ test sui contenuti reali in `app/src/main/assets`)
- `core/narrative` — motore storie del Viandante
- `core/progression` — Knowledge & Experience Engine
- `core/database` — Room + DataStore + repository
- `core/designsystem` — tema "manoscritto umbro"
- `app` — UI per feature (home, viandante, garden, cards, dialogue, knowledge, journal, settings), DI manuale in `AppContainer`

Vedi `ARCHITECTURE.md` e `CONTENT_SCHEMA.md` per i dettagli.
