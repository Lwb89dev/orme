# Inventario asset e licenze

## Principio

L'app non incorpora asset di terzi recuperati dal web. La grafica comprende risorse originali fornite dall'utente, elementi programmatici e illustrazioni generate appositamente per il progetto. I testi sono originali o di pubblico dominio medievale; non c'è audio.

## Grafica

| Asset | Tipo | Origine | Licenza |
|---|---|---|---|
| Icona launcher e splash Orme | PNG + adaptive icon | Fornita dall'utente in `assets/icons/icon.png` | Asset fornito dall'utente; diritti gestiti dal progetto |
| Style key, quattro modalità e cinque scene del Viandante | PNG master + WebP ottimizzati | Generati per Orme con OpenAI ImageGen, sotto direzione editoriale del progetto | Asset generati per il progetto |
| Character sheet di Francesco (1206) | PNG master | Generato per Orme con OpenAI ImageGen come reference di coerenza | Asset generato per il progetto |
| Illustrazioni onboarding (4) | Compose Canvas | Disegnate per l'app | Originale dell'app |
| Base e nove elementi interattivi del giardino | PNG master + WebP trasparenti ottimizzati | Generati per Orme con OpenAI ImageGen e composti dinamicamente in Compose | Asset generati per il progetto |
| Icone di interfaccia | Material Icons (AndroidX) | Google | Apache License 2.0 |
| Font | Serif/sans di sistema | Android | Licenza di sistema |

## Testi

| Contenuto | Origine | Licenza |
|---|---|---|
| Testamento 1-3 (latino) | Opera medievale (1226) | Pubblico dominio |
| Cantico delle creature vv. 1-9 (volgare umbro) | Opera medievale (1225-26) | Pubblico dominio |
| Traduzioni italiane dei due passi | Scritte per l'app | Originali dell'app |
| Parafrasi, descrizioni, biografia, timeline, glossario | Scritti per l'app | Originali dell'app |
| Capitoli del Viandante, carte, dialoghi | Scritti per l'app | Originali dell'app |

La "Preghiera semplice" non è riprodotta integralmente (testo moderno): ne documentiamo soltanto la non attribuibilità a Francesco.

## Audio

Nessun file audio incluso in questa release. L'impostazione "paesaggi sonori" è predisposta; asset futuri dovranno essere auto-prodotti o con licenza compatibile documentata qui.

Dettagli tecnici, stato di integrazione e provenienza puntuale sono in `ASSET_MANIFEST.json`; i prompt normalizzati sono in `assets/art/GENERATION_PROMPTS.md`.

## Dipendenze software

AndroidX, Kotlin, kotlinx-*, Material3: Apache 2.0 / licenze permissive standard. Nessun SDK di tracciamento, nessuna pubblicità.
