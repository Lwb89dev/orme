# Schema dei contenuti

Tutti i contenuti editoriali vivono in `app/src/main/assets/content/`:

```
content/
  manifest.json     {"schemaVersion":1, "contentVersion":N, "language":"it"}
  it/
    teachings.json  {"items":[Teaching...]}
    sources.json    {"documents":[SourceDocument...], "passages":[SourcePassage...]}
    history.json    {"events":[],"persons":[],"places":[],"glossary":[],"biography":[]}
    stories.json    {"characters":[],"chapters":[]}
    cards.json      {"cards":[],"paths":[],"scenarios":[]}
    dialogues.json  {"conversations":[]}
    garden.json     {"elements":[],"contemplations":[]}
```

Gli schemi Kotlin corrispondenti sono in `core/content/src/main/kotlin/.../schema/` e `core/model/`. Il parser usa `ignoreUnknownKeys`: campi nuovi non rompono versioni precedenti dell'app.

## Regole sugli identificatori

- Gli id sono **stabili e globalmente univoci** (la progressione vi si ancora: rinominare un id = perdere il collegamento ai salvataggi).
- Prefissi: `teaching.*`, `source.*`, `pass.*`, `event.*`, `person.*`, `place.*`, `gloss.*`, `bio.*`, `story.*`, `char.*`, `card.*`, `path.*`, `scen.*`, `dlg.*`, `garden.*`, `cont.*`.
- Gli id dei nodi dentro capitoli e conversazioni devono essere univoci globalmente: usa un prefisso per capitolo (es. `lupo.c2`).

## Tipi di nodo del Viandante

`narration`, `dialogue` (speakerId: personaggio | `narrator` | `player`), `choice` (opzioni con `conditions` ed `effects`), `conditional_branch`, `character_encounter`, `historical_insight`, `teaching`, `reflection`, `reward_unlock`, `completion`.

**Condizioni**: `flag`, `var_gte`, `var_lte`, `relation_gte`, `chapter_completed`.
**Effetti**: `set_flag`, `add_var`, `add_relation`, `discover_teaching`, `unlock_garden`.

## Validazione automatica

`ContentValidator` (in `core:content`) + `ShippedContentTest` controllano:

- id univoci;
- ogni riferimento (passaggi, insegnamenti, luoghi, personaggi, fonti, elementi giardino, nodi) esiste;
- ogni capitolo: nodo iniziale valido, tutti i nodi raggiungibili, almeno un completamento raggiungibile, niente vicoli ciechi non-terminali;
- ogni conversazione: terminale raggiungibile, nessun nodo irraggiungibile;
- igiene editoriale: testo originale solo con licenza di pubblico dominio; insegnamento verificato non può citare fonti rifiutate;
- presenza dei contenuti minimi della release (10+ insegnamenti, 5+ capitoli, 20+ carte, ...).

Eseguire:

```bash
./gradlew :core:content:test --tests "*ShippedContentTest*"
```

## Aggiungere un insegnamento

1. Aggiungi la voce in `teachings.json` con `id` nuovo, tema, spiegazione, contesto, `sourcePassageIds` (crea prima il passaggio in `sources.json` se serve).
2. Collega le esperienze (`experiences.viandante/garden/cards/dialogue` con id esistenti o nuovi).
3. Esegui il test di validazione.

## Aggiungere un capitolo del Viandante

1. Definisci i nodi con prefisso unico, `startNodeId`, almeno un `completion`.
2. Registra luogo (`placeId` esistente o nuovo in `history.json`), personaggi parlanti in `characters`, `requiredChapters` per lo sblocco.
3. Collegalo dagli insegnamenti (`experiences.viandante`) se esplora un insegnamento.
4. Validazione obbligatoria; il test `VerticalJourneyTest` giocherà automaticamente il capitolo fino al completamento.

## Aggiungere una carta / uno scenario / un percorso

- Carta: voce in `cards` con `type`, tema, eventuali fonti.
- Scenario: `situation`, 2-4 `options` con `consequence` e `perspectives` (con `isEditorial` onesto).
- Percorso: `requiresTeachings` (sblocco morbido) + `scenarioIds` ordinati.

## Aggiungere una conversazione del Dialogo

Grafo di nodi con `mentorText` e `options`; almeno un nodo `isTerminal`; ricorda: il mentore non è Francesco e dichiara i limiti delle fonti.

## Lingue

Lingua principale: italiano (`content/it/`). Le altre lingue sono directory sorelle (`content/en/`, `content/fr/`, …) con gli stessi id. L'interfaccia legge `content/ui/{lingua}.json`, con chiave italiana; per italiano e inglese le stringhe stanno nel codice. Lingue distribuite: italiano, inglese, e le lingue ufficiali dell'Unione europea salvo gaelico irlandese e maltese, più il russo. Il campo `originalText` (latino o volgare umbro) non si traduce.
