package it.orma.cammino.core.content

import it.orma.cammino.core.content.schema.CardPath
import it.orma.cammino.core.content.schema.CardScenario
import it.orma.cammino.core.content.schema.Contemplation
import it.orma.cammino.core.content.schema.GardenElement
import it.orma.cammino.core.content.schema.GuidedConversation
import it.orma.cammino.core.content.schema.StoryChapter
import it.orma.cammino.core.content.schema.StoryCharacter
import it.orma.cammino.core.content.schema.WisdomCard
import it.orma.cammino.core.model.BiographyChapter
import it.orma.cammino.core.model.FranciscanPlace
import it.orma.cammino.core.model.GlossaryEntry
import it.orma.cammino.core.model.HistoricalEvent
import it.orma.cammino.core.model.HistoricalPerson
import it.orma.cammino.core.model.SourceDocument
import it.orma.cammino.core.model.SourcePassage
import it.orma.cammino.core.model.Teaching
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Astrazione sulla sorgente dei file: assets Android in produzione, filesystem nei test. */
fun interface ContentSource {
    /** Restituisce il testo del file, o null se assente. */
    fun read(path: String): String?
}

/** Contenitori dei file JSON per singola area. */
@Serializable
private data class TeachingsFile(val items: List<Teaching>)

@Serializable
private data class SourcesFile(
    val documents: List<SourceDocument>,
    val passages: List<SourcePassage>,
)

@Serializable
private data class HistoryFile(
    val events: List<HistoricalEvent> = emptyList(),
    val persons: List<HistoricalPerson> = emptyList(),
    val places: List<FranciscanPlace> = emptyList(),
    val glossary: List<GlossaryEntry> = emptyList(),
    val biography: List<BiographyChapter> = emptyList(),
)

@Serializable
private data class StoriesFile(
    val characters: List<StoryCharacter> = emptyList(),
    val chapters: List<StoryChapter> = emptyList(),
)

@Serializable
private data class CardsFile(
    val cards: List<WisdomCard> = emptyList(),
    val paths: List<CardPath> = emptyList(),
    val scenarios: List<CardScenario> = emptyList(),
)

@Serializable
private data class DialoguesFile(val conversations: List<GuidedConversation> = emptyList())

@Serializable
private data class GardenFile(
    val elements: List<GardenElement> = emptyList(),
    val contemplations: List<Contemplation> = emptyList(),
)

class ContentLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Carica il pacchetto contenuti da JSON. Tollerante verso chiavi sconosciute
 * (compatibilità futura), rigoroso su struttura e riferimenti (via [ContentValidator]).
 */
class ContentLoader(private val source: ContentSource) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun load(language: String = DEFAULT_LANGUAGE): ContentCatalog {
        val requestedLanguage = language.trim().lowercase().ifBlank { DEFAULT_LANGUAGE }
        val manifest = decode<ContentManifest>(
            optional("content/$requestedLanguage/manifest.json")
                ?: required("content/manifest.json")
        )
        if (manifest.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw ContentLoadException(
                "schemaVersion ${manifest.schemaVersion} non supportata (attesa $SUPPORTED_SCHEMA_VERSION)"
            )
        }
        val lang = manifest.language.ifBlank { requestedLanguage }
        val teachings = decode<TeachingsFile>(required("content/$lang/teachings.json")).items
        val sources = decode<SourcesFile>(required("content/$lang/sources.json"))
        val history = decode<HistoryFile>(optional("content/$lang/history.json") ?: EMPTY_OBJECT)
        val stories = decode<StoriesFile>(optional("content/$lang/stories.json") ?: EMPTY_OBJECT)
        val cards = decode<CardsFile>(optional("content/$lang/cards.json") ?: EMPTY_OBJECT)
        val dialogues = decode<DialoguesFile>(optional("content/$lang/dialogues.json") ?: EMPTY_OBJECT)
        val garden = decode<GardenFile>(optional("content/$lang/garden.json") ?: EMPTY_OBJECT)

        return ContentCatalog(
            manifest = manifest,
            teachings = teachings,
            sourceDocuments = sources.documents,
            sourcePassages = sources.passages,
            historicalEvents = history.events,
            historicalPersons = history.persons,
            places = history.places,
            glossary = history.glossary,
            biographyChapters = history.biography,
            storyCharacters = stories.characters,
            storyChapters = stories.chapters,
            cards = cards.cards,
            cardPaths = cards.paths,
            cardScenarios = cards.scenarios,
            conversations = dialogues.conversations,
            gardenElements = garden.elements,
            contemplations = garden.contemplations,
        )
    }

    private fun required(path: String): String =
        source.read(path) ?: throw ContentLoadException("File di contenuto mancante: $path")

    private fun optional(path: String): String? = source.read(path)

    private inline fun <reified T> decode(text: String): T = try {
        json.decodeFromString<T>(text)
    } catch (e: Exception) {
        throw ContentLoadException("JSON non valido: ${e.message}", e)
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val DEFAULT_LANGUAGE = "it"
        private const val EMPTY_OBJECT = "{}"
    }
}
