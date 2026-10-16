# Architettura

## Stack (versioni verificate il 15/10/2026)

| Componente | Versione | Note |
|---|---|---|
| Gradle | 9.8.1 | wrapper |
| AGP | 9.4.1 | Kotlin built-in (non si applica `org.jetbrains.kotlin.android`) |
| Kotlin | 2.3.21 | allineata a KSP 2.3.x |
| KSP | 2.3.12 | per Room |
| Compose BOM | 2026.09.00 | |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 | |
| Room | 2.8.5 | schema esportato in `core/database/schemas` |
| Navigation Compose | 2.10.2 | rotte type-safe `@Serializable` |
| DataStore | 1.2.1 | preferenze |
| kotlinx-serialization | 1.11.0 | contenuti + stati di gioco |
| Coroutines | 1.11.0 | |

## Moduli

```
app/                  UI, navigazione, DI (AppContainer), repository di contenuto dagli asset
core/model/           Tipi di dominio @Serializable (Kotlin JVM puro)
core/content/         Schemi JSON, ContentLoader, ContentValidator (JVM puro)
core/narrative/       StoryEngine del Viandante (JVM puro)
core/progression/     Knowledge & Experience Engine (JVM puro)
core/database/        Room, DataStore, repository (Android library)
core/designsystem/    Tema "manoscritto umbro", componenti (Android library + Compose)
```

Regole di dipendenza: `model ← content ← narrative`, `model ← progression`, `database → {progression, narrative}`, `designsystem → model`, `app → tutti`. I moduli JVM non toccano Android: sono testabili con JUnit puro e velocissimi.

## Decisioni chiave

### DI manuale invece di Hilt
La specifica permette "Hilt o soluzione proporzionata". Il progetto ha un unico grafo di oggetti costruito in `AppContainer` (Application). Meno magia, meno KSP, avvio più semplice da ragionare. Se il progetto cresce, la migrazione a Hilt è lineare (i costruttori sono già a iniezione).

### Contenuti in asset JSON, non nel codice
Ogni insegnamento, capitolo, carta, dialogo, fonte è un documento JSON in `app/src/main/assets/content/it/`, validato da `ContentValidator` e dal test `ShippedContentTest` che legge gli stessi file distribuiti. Il `manifest.json` porta `schemaVersion` (rifiutato se non supportato) e `contentVersion` (persistito in DataStore; la progressione è ancorata a id stabili, quindi gli aggiornamenti di contenuto non distruggono i salvataggi).

### Stato di gioco come JSON in Room
`ProgressionState` e `NarrativeProfile` sono serializzati interi (una riga ciascuno); le sessioni di capitolo una riga per capitolo; diario e preferiti sono tabelle relazionali. Motivo: gli stati di gioco evolvono con le feature; il JSON tollera campi nuovi (`ignoreUnknownKeys`), e il repository degrada in sicurezza su stato corrotto (riparte da stato vuoto invece di bloccare l'app). Diario e preferiti restano relazionali perché hanno query vere.

### Grafica vettoriale programmatica
Nessun bitmap esterno: icone launcher (croce Tau), illustrazioni dell'onboarding e l'intera scena del giardino sono disegnati con Compose Canvas. Zero problemi di licenza, peso minimo, animazioni rispettose del lifecycle e dell'impostazione "riduci animazioni".

### Il mentore non è Francesco
La modalità Dialogo usa un grafo conversazionale offline (livello 1). Il livello 2 (AI) è definito solo come interfaccia `ConversationBackend` con requisiti di privacy e di onestà documentati; nessun provider, nessuna rete, nessuna chiave.

## Flusso degli eventi

Le quattro modalità emettono `ExperienceEvent` (`TeachingDiscovered/Explored/Deepened`, `ChapterCompleted`, `CardPathCompleted`, `DialogueCompleted`, `ContemplationCompleted`, `GardenElementUnlocked`). Un unico punto di ingresso (`AppContainer.dispatch` → `ProgressionRepository.applyEvents`) li applica tramite `KnowledgeEngine`, persiste lo stato e restituisce un `UnlockReport` che la UI usa per messaggi sobri di sblocco.

## Persistenza

- Room `cammino.db` v1: `progression_state`, `narrative_profile`, `chapter_session`, `journal_entries`, `favorites`.
- DataStore `cammino_settings`: onboarding, tema, riduzione animazioni, audio, ultima versione contenuti vista.
- Nessuna rete: l'app non dichiara permessi oltre ai default.

## Migrazioni

`exportSchema = true`: gli schema Room sono in versione. Nuove tabelle/campi → nuova `version` + `Migration`. Le sessioni di capitolo e gli stati JSON non richiedono migrazione purché gli id di contenuto restino stabili (vincolo editoriale documentato in CONTENT_SCHEMA.md).
