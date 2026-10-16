package it.orma.cammino.core.content

import it.orma.cammino.core.content.schema.ChoiceNode
import it.orma.cammino.core.content.schema.ChoiceOption
import it.orma.cammino.core.content.schema.CompletionNode
import it.orma.cammino.core.content.schema.ConversationNode
import it.orma.cammino.core.content.schema.ConversationOption
import it.orma.cammino.core.content.schema.GuidedConversation
import it.orma.cammino.core.content.schema.NarrationNode
import it.orma.cammino.core.content.schema.StoryChapter
import it.orma.cammino.core.model.FranciscanPlace
import it.orma.cammino.core.model.ReviewStatus
import it.orma.cammino.core.model.SourceCategory
import it.orma.cammino.core.model.SourceDocument
import it.orma.cammino.core.model.SourcePassage
import it.orma.cammino.core.model.Teaching
import it.orma.cammino.core.model.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentValidatorTest {

    private val validator = ContentValidator()

    private fun minimalCatalog(
        chapters: List<StoryChapter> = emptyList(),
        passages: List<SourcePassage> = emptyList(),
        documents: List<SourceDocument> = emptyList(),
        teachings: List<Teaching> = emptyList(),
        conversations: List<GuidedConversation> = emptyList(),
    ) = ContentCatalog(
        teachings = teachings,
        sourceDocuments = documents,
        sourcePassages = passages,
        places = listOf(FranciscanPlace(id = "place.assisi", name = "Assisi")),
        storyChapters = chapters,
        conversations = conversations,
    )

    private fun validChapter() = StoryChapter(
        id = "story.test.001",
        title = "Prova",
        placeId = "place.assisi",
        startNodeId = "n1",
        nodes = listOf(
            NarrationNode(id = "n1", text = "Inizio", next = "c1"),
            ChoiceNode(
                id = "c1", prompt = "Scegli",
                options = listOf(
                    ChoiceOption(id = "a", text = "A", next = "end"),
                    ChoiceOption(id = "b", text = "B", next = "end"),
                ),
            ),
            CompletionNode(id = "end", text = "Fine"),
        ),
    )

    @Test
    fun `capitolo ben formato non produce errori`() {
        val errors = validator.errors(minimalCatalog(chapters = listOf(validChapter())))
        assertTrue("Errori inattesi: $errors", errors.isEmpty())
    }

    @Test
    fun `rilevamento identificatori duplicati`() {
        val chapter = validChapter()
        val dup = minimalCatalog(
            chapters = listOf(chapter, chapter.copy(title = "Altro")),
        )
        val errors = validator.errors(dup)
        assertTrue(errors.any { it.message.contains("story.test.001") && it.area == "id" })
    }

    @Test
    fun `rilevamento riferimenti mancanti`() {
        val chapter = validChapter().let {
            it.copy(nodes = it.nodes.map { n ->
                if (n is NarrationNode) n.copy(next = "nodo.fantasma") else n
            })
        }
        val errors = validator.errors(minimalCatalog(chapters = listOf(chapter)))
        assertTrue(errors.any { it.message.contains("nodo.fantasma") })
    }

    @Test
    fun `rilevamento nodi irraggiungibili`() {
        val chapter = validChapter().let {
            it.copy(nodes = it.nodes + NarrationNode(id = "orfano", text = "Mai raggiunto", next = "end"))
        }
        val errors = validator.errors(minimalCatalog(chapters = listOf(chapter)))
        assertTrue(errors.any { it.message.contains("orfano") && it.message.contains("irraggiungibile") })
    }

    @Test
    fun `rilevamento assenza di completamento`() {
        val chapter = StoryChapter(
            id = "story.senzafine", title = "Loop", placeId = "place.assisi", startNodeId = "n1",
            nodes = listOf(NarrationNode(id = "n1", text = "x", next = "n1")),
        )
        val errors = validator.errors(minimalCatalog(chapters = listOf(chapter)))
        assertTrue(errors.any { it.message.contains("completamento") })
    }

    @Test
    fun `testo originale senza licenza di pubblico dominio e errore`() {
        val doc = SourceDocument(
            id = "source.x", category = SourceCategory.SCRITTO_AUTENTICO, title = "X", author = "Y",
            licenseNote = "testo medievale PD",
        )
        val bad = SourcePassage(id = "pass.x.1", documentId = "source.x", originalText = "Altissimu...", license = "proprietary_modern_translation")
        val good = bad.copy(id = "pass.x.2", license = "public_domain_original_editorial_translation")
        val errors = validator.errors(minimalCatalog(documents = listOf(doc), passages = listOf(bad, good)))
        assertTrue(errors.any { it.message.contains("pass.x.1") })
        assertTrue(errors.none { it.message.contains("pass.x.2") && it.message.contains("pubblico dominio") })
    }

    @Test
    fun `insegnamento verificato non puo citare fonti rifiutate`() {
        val doc = SourceDocument(
            id = "source.apocrifo", category = SourceCategory.TRADIZIONE_AGIOGRAFICA, title = "A", author = "Anonimo",
            reviewStatus = ReviewStatus.REJECTED, licenseNote = "PD",
        )
        val passage = SourcePassage(id = "pass.apocrifo.1", documentId = "source.apocrifo", paraphrase = "x", reviewStatus = ReviewStatus.REJECTED)
        val teaching = Teaching(
            id = "teaching.pace.001", theme = Theme.PACE, title = "Pace", summary = "s",
            sourcePassageIds = listOf("pass.apocrifo.1"), reviewStatus = ReviewStatus.VERIFIED,
        )
        val errors = validator.errors(
            minimalCatalog(documents = listOf(doc), passages = listOf(passage), teachings = listOf(teaching))
        )
        assertTrue(errors.any { it.message.contains("rifiutato") })
    }

    @Test
    fun `dialoghi con nodi irraggiungibili o senza terminale sono errori`() {
        val conv = GuidedConversation(
            id = "dlg.test", title = "T", theme = Theme.PACE, startNodeId = "a",
            nodes = listOf(
                ConversationNode(id = "a", mentorText = "ciao", options = listOf(ConversationOption(id = "o1", text = "x", next = "b"))),
                ConversationNode(id = "b", mentorText = "fine", isTerminal = true),
                ConversationNode(id = "orfano", mentorText = "mai visto", isTerminal = true),
            ),
        )
        val errors = validator.errors(minimalCatalog(conversations = listOf(conv)))
        assertTrue(errors.any { it.message.contains("orfano") })
    }

    @Test
    fun `loader legge un pacchetto minimo e rifiuta schemaVersion sconosciuta`() {
        val files = mapOf(
            "content/manifest.json" to """{"schemaVersion":1,"contentVersion":1,"language":"it"}""",
            "content/it/teachings.json" to """{"items":[]}""",
            "content/it/sources.json" to """{"documents":[],"passages":[]}""",
        )
        val loader = ContentLoader { path -> files[path] }
        val catalog = loader.load()
        assertEquals(1, catalog.manifest.contentVersion)

        val badLoader = ContentLoader { path ->
            if (path == "content/manifest.json") """{"schemaVersion":99,"contentVersion":1,"language":"it"}""" else files[path]
        }
        try {
            badLoader.load()
            org.junit.Assert.fail("Attesa ContentLoadException")
        } catch (e: ContentLoadException) {
            assertTrue(e.message!!.contains("schemaVersion"))
        }
    }

    @Test
    fun `loader gestisce JSON corrotto con errore esplicito`() {
        val files = mapOf(
            "content/manifest.json" to """{"schemaVersion":1,"contentVersion":1,"language":"it"}""",
            "content/it/teachings.json" to """{non json""",
            "content/it/sources.json" to """{"documents":[],"passages":[]}""",
        )
        try {
            ContentLoader { path -> files[path] }.load()
            org.junit.Assert.fail("Attesa ContentLoadException")
        } catch (e: ContentLoadException) {
            assertTrue(e.message!!.contains("JSON"))
        }
    }
}
