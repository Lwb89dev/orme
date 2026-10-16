# Testing

## Piramide

| Livello | Cosa | Dove | Come gira |
|---|---|---|---|
| Unitari JVM puri | Motore narrativo, progressione, validatori, loader | `core:narrative`, `core:progression`, `core:content` | `./gradlew test` (nessun SDK necessario) |
| Contenuti reali | Validazione del pacchetto JSON distribuito + giocabilità completa dei 5 capitoli | `core:content` (`ShippedContentTest`), `core:progression` (`VerticalJourneyTest`) | `./gradlew test` |
| Database | Room su JVM via Robolectric (CRUD diario, preferiti, progressione, esportazione, stato corrotto) | `core:database` | `./gradlew :core:database:testDebugUnitTest` |
| UI / navigazione | Compose UI test: avvio, onboarding, home, navigazione verso Viandante e Dialogo | `app/src/androidTest` | `./gradlew :app:connectedDebugAndroidTest` (richiede emulatore/dispositivo) |

## Copertura per area (specifica §14)

- **Motore narrativo**: diramazioni, condizioni (flag/variabili/relazioni/capitoli), filtro opzioni, errore su scelta non disponibile, completamento, ripresa da sessione serializzata, rigioco con conseguenze persistenti, conseguenze tra capitoli (flag `mantello_dato` riletto nel capitolo 3 — coperto dal gioco automatico che attraversa tutti i capitoli su un profilo unico? no: profili separati; la lettura tra capitoli è coperta da test unitari del motore con profilo pre-impostato e dalla validazione strutturale).
- **Progressione**: condivisione tra modalità (scoperta → sblocco giardino → percorso carte → dialogo), mai regressione di stato, eventi verso id inesistenti ignorati, cronologia e ultima attività.
- **Contenuti**: id univoci, riferimenti integri, grafi percorribili, regole editoriali (pubblico dominio, fonti rifiutate), contenuti minimi di release, gestione JSON corrotto, `schemaVersion` sconosciuta rifiutata.
- **Database**: scritture e riletture, toggle preferiti, esportazione, degradazione sicura su stato corrotto.
- **UI**: navigazione principale su emulatore (vedi sotto).

## Cosa è stato eseguito in questo ambiente

- Tutti i test JVM (`test`) — ✅
- Test Robolectric del database — ✅
- `connectedDebugAndroidTest` — ⚠️ **non eseguito**: l'emulatore (37.2.12) va in segmentation fault all'avvio del guest su questo host (KVM rilevato ma instabile; crash identico con immagini google_apis e playstore, gpu guest/swiftshader, accel on/off). I test UI sono scritti e pronti in `app/src/androidTest`: su una macchina con emulatore funzionante si eseguono con `./gradlew :app:connectedDebugAndroidTest`.

## Limiti noti

- Le migrazioni Room non hanno ancora casi reali (v1): lo schema è esportato in `core/database/schemas` per abilitare i MigrationTest quando esisterà una v2.
- I test UI coprono il percorso principale di navigazione; i test di rotazione/ricreazione sono demandati alla prossima iterazione.
