# Orme — Visual Style Bible

## Direzione sintetica

Orme rappresenta l'Umbria medievale come un mondo dipinto a mano, bucolico e lievemente nostalgico. Il registro è adulto e narrativo: realismo stilizzato, anatomie credibili, architetture sobrie e paesaggi vissuti. L'immagine deve sostenere la lettura e l'esplorazione, non competere con esse.

La style key di riferimento è `assets/art/master/home_umbria_style_key_v2.png`. Tutte le nuove illustrazioni devono riprenderne profondità atmosferica, materia pittorica, gamma cromatica e livello di dettaglio.

## Palette

| Ruolo | Colore |
|---|---|
| Verde oliva | `#697B4B` |
| Verde salvia | `#99A486` |
| Verde bosco | `#344E41` |
| Terracotta | `#B97753` |
| Ocra naturale | `#C49A59` |
| Pergamena | `#F2E8D5` |
| Avorio caldo | `#FAF6ED` |
| Marrone terra | `#75604B` |
| Marrone inchiostro | `#40382F` |
| Blu grigio atmosferico | `#899AA3` |

Le variazioni di luce possono spostare la palette, ma non devono produrre saturazione da fantasy o gradienti digitali evidenti.

## Luce e atmosfera

- Alternare mattino velato, luce diffusa, pioggia, interni a finestra, fuoco e notte.
- Usare la foschia per separare i piani del paesaggio.
- Conservare ombre leggibili e morbide; niente controluce spettacolare come formula ricorrente.
- Il calore è umano e materico, non un bagliore soprannaturale.

## Materia e bordi

- Guazzo opaco con transizioni leggere da acquerello e texture di carta discreta.
- Bordi più netti sul soggetto narrativo e più morbidi sui piani lontani.
- Pietra, legno, lana e terra devono mostrare usura e irregolarità naturali.
- Le immagini restano chiaramente illustrate: niente resa fotografica o 3D.

## Paesaggio e architettura

- Colline umbre irregolari, querceti, lecci, uliveti, campi piccoli e sentieri in pietra o terra.
- I cipressi sono accenti occasionali, non filari decorativi dominanti.
- Gli ulivi sono alberi interi, con tronchi contorti e chiome naturali.
- Borghi compatti in pietra, mura basse, edifici romanici e torri quadrate sobrie.
- Vietati ville rinascimentali, grandi basiliche tarde, fortezze fantasy e infrastrutture moderne.

## Personaggi

- Proporzioni, mani e posture credibili; emozioni affidate a sguardo, distanza e gesto.
- Abiti in lana e lino con tinte naturali, tagli plausibili per l'inizio del XIII secolo.
- Francesco giovane segue `assets/art/master/character_francesco_1206_sheet.png`: volto, età, capelli e corporatura non vanno rigenerati liberamente.
- Nessuna aureola, luce mistica, stimmata fuori contesto o saio moderno applicato retroattivamente.
- Malattia e povertà non sono spettacolo: volti individuali, distanza rispettosa, nessuna caricatura.

## Composizione per Android

- Sfondi narrativi: master 16:9, export app 1200×675 WebP.
- Card di modalità: export 800×450 WebP; il soggetto deve sopravvivere anche a un ritaglio 4:3.
- Giardino: base 4:3 con zone libere per gli elementi di progressione.
- Volti e oggetti importanti non devono occupare il bordo inferiore, dove possono comparire controlli o testo.
- Il testo è sempre Compose; non va rasterizzato nelle illustrazioni.
- Per testi lunghi usare superfici opache o semitrasparenti, mai testo direttamente su fondali complessi.

## Trattamento delle fonti

- Episodi documentati: composizione sobria, nessun dettaglio narrativo aggiunto senza necessità.
- Tradizioni agiografiche: il manifest le identifica esplicitamente; l'immagine evita di trasformare il prodigio in spettacolo documentario.
- Storie editoriali: personaggi originali distinti dalle figure storiche e nessuna citazione inventata.

## Criteri di revisione

Ogni asset deve superare questi controlli prima dell'integrazione:

1. Coerenza con style key e palette.
2. Assenza di testo, watermark e simboli decorativi non richiesti.
3. Anatomia, mani, oggetti e interazioni credibili.
4. Abiti e architetture plausibili per luogo e periodo.
5. Safe area adatta al layout reale.
6. Contrasto sufficiente in tema chiaro e scuro.
7. Peso e risoluzione proporzionati all'uso.

## Da evitare

Fotorealismo, 3D da animazione, anime, chibi, fantasy medievale, castelli inventati, occhi enormi, bagliori magici, particelle, tramonti onnipresenti, saturazione forte, cornici vegetali ricorrenti, mani deformate, finti manoscritti illeggibili, composizioni pubblicitarie da app di meditazione.
