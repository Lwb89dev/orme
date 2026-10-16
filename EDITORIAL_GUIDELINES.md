# Linee guida editoriali

Regole vincolanti per chiunque aggiunga o modifichi contenuti.

## 1. Attribuzione

- **Mai attribuire a Francesco parole inventate.** Niente citazioni apocrife, niente "come direbbe Francesco".
- Ogni contenuto dichiara la categoria di fonte (`scritto_autentico`, `testimonianza_medievale`, `tradizione_agiografica`, `interpretazione_moderna`, `narrazione_originale`).
- Citazione letterale = testo originale medievale (pubblico dominio) + `license: public_domain_original_editorial_translation` + `reviewStatus: verified` dopo verifica su almeno due riproduzioni concordi o su edizione critica.
- Se non puoi verificare: **parafrasa** e usa `license: editorial_paraphrase`. Se il riferimento preciso (capitolo/paragrafo) è incerto, `reviewStatus: needs_verification` e l'app lo presenterà come "in verifica".
- Le traduzioni italiane sono sempre editoriali originali dell'app: mai copiare traduzioni moderne protette.

## 2. Rispetto

- La fede cristiana e cattolica di Francesco non si rimuove e non si edulcora; si presenta senza proselitismo e senza dare per scontata l'adesione dell'utente.
- Nessun giudizio morale o spirituale sull'utente: l'app descrive, invita, interroga. Non valuta.
- Le persone non credenti sono utenti a pieno titolo: il tono è "ecco cosa diceva e viveva Francesco", mai "ecco cosa devi fare tu".
- Temi sensibili (malattia, povertà, morte) con sobrietà: niente spettacolarizzazione.

## 3. Fiction onesta

- Il Viandante è fiction storica dichiarata: i capitoli portano `category: narrazione_originale` e i luoghi inventati sono marcati "ambientazione narrativa".
- Personaggi storici: battute sobrie e coerenti con le fonti; Francesco parla pochissimo e mai con citazioni inventate.
- I pannelli `historical_insight` dichiarano sempre cosa è documentato, cosa è tradizione, cosa è invenzione del gioco.

## 4. Niente gamification moralistica

Vietati: punteggi di bontà, classifiche di santità, streak punitive, meccaniche di colpa, ricompense per atti spirituali, timer che puniscono l'assenza.

## 5. Processo

1. Scrivi o modifica il contenuto JSON.
2. Esegui `./gradlew :core:content:test --tests "*ShippedContentTest*"` — zero errori prima del commit.
3. Segna `needs_verification` tutto ciò che non hai verificato direttamente: meglio un "in verifica" che un errore spacciato per fonte.
4. Aggiorna `FRANCISCAN_SOURCES.md` se aggiungi fonti o passaggi.
