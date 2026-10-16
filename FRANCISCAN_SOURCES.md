# Fonti francescane — decisioni editoriali e livelli di certezza

Questo documento registra quali fonti usa l'app, con quali criteri, e cosa resta da verificare.

## Principio guida

L'accuratezza è un requisito architetturale. Ogni contenuto testuale dichiara la sua categoria:

1. **scritto_autentico** — scritti attribuiti a Francesco (attendibilità indicata per documento)
2. **testimonianza_medievale** — biografie e testimonianze medievali
3. **tradizione_agiografica** — racconti della tradizione (es. Fioretti)
4. **interpretazione_moderna** — note editoriali dell'app
5. **narrazione_originale** — fiction del gioco

Regola d'oro: **nessuna citazione letterale inventata**. Dove non riproduciamo il testo originale, usiamo parafrasi editoriale etichettata.

## Citazioni letterali incluse (verificate sul web il 15/10/2026)

| Passo | Testo | Verifica |
|---|---|---|
| Testamento 1-3 (latino) | "Dominus ita dedit mihi fratri Francisco..." | Concordante tra Wikisource latina, franciscanos.org, CapDox |
| Cantico delle creature vv. 1-9 (volgare umbro) | "Altissimu, onnipotente, bon Signore..." | Concordante tra più riproduzioni della lezione del Codice di Assisi 338 (ed. Contini) |

Entrambi i testi sono medievali: pubblico dominio. Le traduzioni italiane sono editoriali originali di questa app (etichettate "traduzione editoriale").

## Parafrasi editoriali (nessun testo letterale)

I riferimenti agli episodi (rinuncia davanti al vescovo, servizio ai lebbrosi, Natale di Greccio, stimmate, uccelli, lupo di Gubbio, perfetta letizia, lettera al ministro, ecc.) sono parafrasi originali con riferimento al documento. I riferimenti a capitoli/paragrafi seguono la citazione accademica corrente (es. Fioretti cap. 21 per il lupo); tre passaggi sono marcati `needs_verification` per la numerazione esatta dei paragrafi (saluto della pace nel Testamento, Ammonizione sulla propria volontà, Regola non bollata sul fratello) e l'app li presenta come "in verifica", mai come citazioni.

## La "Preghiera semplice"

"Fammi strumento della tua pace" **non è di Francesco**: prima attestazione nota 1912 (rivista La Clochette). L'app la documenta come non attribuibile (`source.preghiera_semplice`) e non la usa mai come citazione del santo.

## Copyright

- Originali medievali (latino/volgare): pubblico dominio.
- Traduzioni italiane moderne esistenti (es. edizioni FF/San Francesco Editrice): **non riprodotte** — l'app usa solo traduzioni editoriali originali.
- Parafrasi, descrizioni, biografia, contenuti di gioco: testi originali dell'app.
- I siti indicati (sanfrancesco.org, ofm.org, treccani.it, capdox.capuchin.org.au) sono stati usati per verifica e confronto, non per copia integrale.

## Storia vs agiografia vs fiction

- Eventi `documented`: rinuncia 1206, approvazione 1223, Greccio 1223, morte 1226, canonizzazione 1228, sultano 1219 (nell'essenziale), ecc.
- Eventi `tradition`: nascita 1181/82, crocifisso di San Damiano, approvazione orale 1209/10, stimmate 1224.
- Eventi `legend`: lupo di Gubbio.
- Il Viandante è fiction dichiarata (`narrazione_originale`); i suoi pannelli storici separano esplicitamente i tre livelli.

## Lavoro ancora aperto

- Fissare i riferimenti di paragrafo esatti per i tre passaggi `needs_verification` sull'edizione critica (Esser per gli Opuscula).
- Valutare l'aggiunta del testo latino integrale del Testamento (pubblico dominio) in un'appendice.
- Revisione umana di uno studioso prima di una pubblicazione pubblica: questa release è accurata ma non sostituisce una revisione scientifica.
