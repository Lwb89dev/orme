# Game Design — Orme

Filosofia: l'utente non accumula virtù digitali; scopre una figura storica e i suoi insegnamenti. La progressione registra **scoperto → esplorato → approfondito** per ogni insegnamento, mai un giudizio morale.

## Le quattro vie

### Il Viandante (insegna attraverso le esperienze)

Il giocatore è un viandante immaginario (non Francesco) che attraversa l'Umbria dell'inizio Duecento.

- **Struttura**: mappa dei luoghi → capitoli (grafo di nodi JSON). Luoghi storici e ambientazioni narrative sono dichiarati.
- **Nodi**: narrazione, dialogo, scelta (2-4 opzioni), diramazione condizionale automatica, incontro con personaggio, pannello storico (distingue documentato/agiografia/fiction), nodo insegnamento, riflessione, sblocco, completamento.
- **Conseguenze reali**: le scelte scrivono in un profilo narrativo persistente (flag, variabili, relazioni con i personaggi, registro delle scelte). Esempio distribuito: dare il mantello nel capitolo 1 fa riconoscere Martino nel capitolo 3 tramite il flag `mantello_dato`; aiutare a San Damiano nel capitolo 2 apre un'opzione condizionata.
- **Rigiocabilità**: ogni capitolo si può rigiocare; il registro delle scelte e gli effetti restano (le conseguenze non si cancellano fingendo che nulla sia successo).
- **Sblocco**: i capitoli si aprono completando i capitoli richiesti (progressione narrativa, non valuta).
- **Contenuto distribuito**: 5 capitoli, 9 personaggi (3 ricorrenti fittizi + comparse storiche), 5 diramazioni significative, 1 capitolo biografico (la rinuncia del 1206), 21-23 nodi per capitolo.

### Il Giardino di Francesco (insegna attraverso l'osservazione)

Spazio illustrato disegnato in vettoriale (Canvas): cielo dinamico che segue l'ora del giorno, colline, elementi sbloccabili.

- **Evoluzione**: ogni elemento è legato a un insegnamento; scoprirlo (in qualunque modalità) fa comparire l'elemento. Niente timer, niente raccolti obbligatori, niente penalità per inattività.
- **Interazione**: toccare un elemento mostra descrizione e simbolismo e porta all'insegnamento.
- **Contemplazioni**: 3 esperienze brevi e facoltative (osserva/ascolta/leggi/rifletti/annota), senza pretese terapeutiche; il passo "annota" salva nel diario.
- **Contenuto distribuito**: 10 elementi, 3 contemplazioni.
- **Accessibilità**: le animazioni seguono l'impostazione di riduzione; nessun suono parte da solo.

### Le Vie della Saggezza (insegnano attraverso le decisioni)

Carte narrative e scenari decisionali: strumenti di esplorazione, non collezionismo.

- **Flusso**: percorso tematico → scenario (situazione) → scelta → conseguenza → prospettive (analisi da più punti di vista) → confronto con la fonte → riflessione conclusiva.
- **Prospettive**: il punto non è indovinare la risposta francescana, ma capire perché Francesco avrebbe privilegiato certi atteggiamenti; ciò che afferma la fonte è separato da ciò che è interpretazione editoriale (flag `isEditorial` sempre visibile).
- **Sblocco morbido**: i percorsi si aprono scoprendo gli insegnamenti richiesti; le carte sono sempre leggibili.
- **Contenuto distribuito**: 23 carte (insegnamento/evento/personaggio/contesto/riflessione/percorso), 3 percorsi (pace, semplicità, fraternità), 5 scenari da 3 opzioni ciascuno, tutti rigiocabili.

### Il Dialogo (insegna attraverso le domande)

- **Livello 1 (distribuito)**: conversazioni guidate offline su grafo JSON. Il mentore è una guida onesta: dichiara sempre cosa è fonte, cosa è agiografia, cosa è interpretazione, e quando le fonti non bastano. Mai Francesco, mai giudizi sull'utente.
- **Contenuto distribuito**: 5 conversazioni (umiltà, perdono, povertà, creato, fede), 12-13 nodi ciascuna, rami reali, terminali multipli, ricominciabili.
- **Livello 2 (non implementato)**: interfaccia `ConversationBackend` documentata con requisiti vincolanti (opt-in, RAG sulle fonti locali, niente diario, niente impersonificazione).

## Progressione condivisa (Knowledge & Experience Engine)

Eventi → stato unico → sblocchi:

- `TeachingDiscovered/Explored/Deepened` aggiornano lo stato di esperienza dell'insegnamento (mai a ritroso).
- `ChapterCompleted`, `CardScenarioCompleted`, `CardPathCompleted`, `DialogueCompleted`, `ContemplationCompleted` registrano i completamenti e l'ultima attività (home: "riprendi il cammino").
- Gli elementi del giardino si sbloccano quando l'insegnamento richiesto è almeno scoperto (o via `GardenElementUnlocked` esplicito nelle storie).
- I percorsi di carte si attivano quando gli insegnamenti richiesti sono scoperti.
- Le conversazioni del Dialogo sono sempre accessibili (niente barriere artificiali all'educativo).

Esempio di catena verificata in test: completare "Il mantello del viandante" → `teaching.accoglienza.ogni_uomo` scoperto → il nido compare nel giardino → il percorso collegato si attiva → il dialogo può approfondire.

## Onboarding

Quattro pagine illustrate (vettoriali): chi è Francesco, le quattro vie, l'importanza delle fonti, la privacy. Nessun account, nessun permesso richiesto.

## Diario

Voci libere/riflessioni/domande/appunti, collegabili a un insegnamento o capitolo. Esportazione Markdown/JSON via share sheet. Mai analizzato, mai trasmesso.

## Direzione artistica

Palette pergamena/inchiostro/oliva/terracotta/oro (vedi `core/designsystem/Color.kt`), tema scuro progettato a parte ("notte e candela"), tipografia serif di sistema, divisori ornamentali, badge che dichiarano sempre la categoria di fonte di ogni contenuto.
