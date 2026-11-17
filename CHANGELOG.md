# Changelog

All notable changes to Orme are documented here.

## [0.3.0] — 2026-11-17

### Added

- Optional sharing of a journal entry on Nostr as a Markdown article (NIP-23). It is a switch on each entry, off by default; saving publishes and updates, switching it off or deleting the entry sends a deletion request.
- Sign-in with Amber (NIP-55) or a remote signer through a `bunker://` link (NIP-46), in the onboarding and in Settings. The app never holds a private key.
- Two new onboarding screens, on sharing and on signing in, in every language of the app. The existing onboarding texts are now translated everywhere instead of falling back to English.
- `core:nostr` module with unit tests, including the official BIP-340 vector and a relay stand-in for publishing.
- A Lightning donation link at the bottom of Settings (`lwb89@blink.sv`, the same address as Roadstr). It opens the wallet, or copies the address when no wallet is installed.
- On a new installation the app starts in the device language when it is one of the shipped languages, and in English otherwise.

### Fixed

- Twenty-seven interface texts that contain a value, such as "Read the work: …", "Step 2 of 5", or "Content version: 2", showed in English in every language other than Italian. They now use templates with placeholders and are translated everywhere; a test checks that each translation keeps the placeholders.
- Italian theme names now carry their accent ("povertà", "umiltà", "carità", "fraternità", "comunità").
- The onboarding no longer draws "Skip" under the status bar, keeps the bunker link field above the keyboard, and shows the current page with a dot that is visible on the dark background.
- The share switch on a journal entry is one control with its title, so a screen reader announces what it turns on.

### Changed

- The app now declares the `INTERNET` permission, used only for the sharing above. Journal text, onboarding, Settings, and README no longer say that the app has no network access.
- Database version 2 adds the sharing columns; existing entries stay private. A migration test covers the upgrade.

### Build

- New dependencies: BouncyCastle `bcprov-jdk18on` 1.86 (MIT) and OkHttp 4.12.0 (Apache-2.0). The protocol code is adapted from the author's Roadstr app (MIT).

## [0.2.0] — 2026-11-17

### Changed

- The home opens on the story. Under it are one writing and one teaching. The garden, the situations, and the questions stay one step away.
- The Testament, the Earlier Rule, the Later Rule with the bull of Honorius III, the Letter to the Entire Order, and the Canticle of the Creatures can be read in full. The medieval original stays in Latin, or in Umbrian for the Canticle. The translation beside it is editorial and original to this app.
- The greeting "The Lord give you peace" is sentence 23 of the Testament. It is no longer marked as an unverified reference.
- Content and interface ship in Italian, English, the official languages of the European Union except Irish and Maltese, and Russian. The medieval original is the same in every language.
- A teaching opens the work it cites. The reader sees the translation first and the original after it.

### Added

- Three teachings grounded in the newly included texts: the work of one's hands, the Eucharist as what Francis says he can see of Christ in this world, and the Gospel as the form of life he had written down.
- A writings shelf in the knowledge section, separate from biographies, legend, and editorial notes.
- Language packs and interface strings for Bulgarian, Croatian, Czech, Danish, Dutch, Estonian, Finnish, French, German, Greek, Hungarian, Latvian, Lithuanian, Polish, Portuguese, Romanian, Russian, Slovak, Slovenian, Spanish, and Swedish, in addition to Italian and English.

### Integrity

- No quotation is invented. Where a clean public-domain transcription was not available, the app keeps a labelled paraphrase. The 1912 Peace Prayer is recorded as not by Francis.
- Modern copyrighted translations are not reproduced.
- Every shipped language passes the content validator. Identifiers match the Italian catalogue, and `originalText` is unchanged.

### Build

- Release APK signed with the existing Orme release key, certificate `CN=Orme`.

## [0.1.0] — 2026-10-16

First complete cut of the four experiences.

### Added

- English catalogue for the Wayfarer, the garden, the situations, the dialogues, the journal, the history, the sources, and the teachings.
- English interface, with the language chosen in Settings. A new installation opens in English.
- Local journal export as Markdown.
- Source badges, authenticity notices, and labels that distinguish an editorial translation from a paraphrase.

### Integrity

- Original texts sit beside their editorial translations.
- Paraphrase is labelled as paraphrase.
