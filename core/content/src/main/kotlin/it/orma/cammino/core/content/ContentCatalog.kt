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

/** Manifest del pacchetto contenuti: versionamento e lingua. */
@Serializable
data class ContentManifest(
    val schemaVersion: Int,
    val contentVersion: Int,
    val language: String,
)

/**
 * L'intero pacchetto contenuti caricato e indicizzato.
 * Costruito da [ContentLoader], validato da [ContentValidator].
 */
class ContentCatalog(
    val manifest: ContentManifest = ContentManifest(1, 1, "it"),
    val teachings: List<Teaching> = emptyList(),
    val sourceDocuments: List<SourceDocument> = emptyList(),
    val sourcePassages: List<SourcePassage> = emptyList(),
    val historicalEvents: List<HistoricalEvent> = emptyList(),
    val historicalPersons: List<HistoricalPerson> = emptyList(),
    val places: List<FranciscanPlace> = emptyList(),
    val glossary: List<GlossaryEntry> = emptyList(),
    val biographyChapters: List<BiographyChapter> = emptyList(),
    val storyCharacters: List<StoryCharacter> = emptyList(),
    val storyChapters: List<StoryChapter> = emptyList(),
    val cards: List<WisdomCard> = emptyList(),
    val cardPaths: List<CardPath> = emptyList(),
    val cardScenarios: List<CardScenario> = emptyList(),
    val conversations: List<GuidedConversation> = emptyList(),
    val gardenElements: List<GardenElement> = emptyList(),
    val contemplations: List<Contemplation> = emptyList(),
) {
    val teachingsById: Map<String, Teaching> = teachings.associateBy { it.id }
    val documentsById: Map<String, SourceDocument> = sourceDocuments.associateBy { it.id }
    val passagesById: Map<String, SourcePassage> = sourcePassages.associateBy { it.id }
    val eventsById: Map<String, HistoricalEvent> = historicalEvents.associateBy { it.id }
    val personsById: Map<String, HistoricalPerson> = historicalPersons.associateBy { it.id }
    val placesById: Map<String, FranciscanPlace> = places.associateBy { it.id }
    val chaptersById: Map<String, StoryChapter> = storyChapters.associateBy { it.id }
    val charactersById: Map<String, StoryCharacter> = storyCharacters.associateBy { it.id }
    val cardsById: Map<String, WisdomCard> = cards.associateBy { it.id }
    val pathsById: Map<String, CardPath> = cardPaths.associateBy { it.id }
    val scenariosById: Map<String, CardScenario> = cardScenarios.associateBy { it.id }
    val conversationsById: Map<String, GuidedConversation> = conversations.associateBy { it.id }
    val gardenElementsById: Map<String, GardenElement> = gardenElements.associateBy { it.id }
    val contemplationsById: Map<String, Contemplation> = contemplations.associateBy { it.id }

    /** Passaggi appartenenti a un documento, nell'ordine di dichiarazione. */
    fun passagesOf(documentId: String): List<SourcePassage> =
        sourcePassages.filter { it.documentId == documentId }
}
