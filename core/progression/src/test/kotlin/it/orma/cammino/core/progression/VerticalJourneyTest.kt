package it.orma.cammino.core.progression

import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.ContentLoader
import it.orma.cammino.core.model.ChapterCompleted
import it.orma.cammino.core.model.DialogueCompleted
import it.orma.cammino.core.model.ExperienceEvent
import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.TeachingDeepened
import it.orma.cammino.core.model.TeachingStatus
import it.orma.cammino.core.narrative.NarrativeProfile
import it.orma.cammino.core.narrative.StoryEngine
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifica end-to-end sui contenuti reali distribuiti: ogni capitolo può
 * essere giocato fino al completamento e il percorso verticale della Fase 2
 * (storia -> scoperta -> giardino -> carte -> dialogo) funziona sui motori reali.
 */
class VerticalJourneyTest {

    private fun loadCatalog(): ContentCatalog {
        val assetsDir = File("../../app/src/main/assets")
        assertTrue("assets non trovati", assetsDir.isDirectory)
        return ContentLoader { path ->
            val f = File(assetsDir, path)
            if (f.isFile) f.readText() else null
        }.load()
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `ogni capitolo distribuito può essere completato giocando le prime opzioni`() {
        val catalog = loadCatalog()
        val engine = StoryEngine()
        catalog.storyChapters.forEach { chapter ->
            var profile = NarrativeProfile()
            var step = engine.startChapter(chapter, profile)
            val visits = mutableMapOf<String, Int>()
            var guard = 0
            while (!step.isChapterComplete && guard < 300) {
                val nodeId = step.node.id
                val seen = (visits[nodeId] ?: 0)
                visits[nodeId] = seen + 1
                step = if (step.availableOptions.isNotEmpty()) {
                    // Anti-ciclo: se rivedo la stessa scelta, provo l'opzione successiva
                    val option = step.availableOptions[seen % step.availableOptions.size]
                    engine.choose(chapter, step.profile, step.session, option.id)
                } else {
                    engine.advance(chapter, step.profile, step.session)
                }
                profile = step.profile
                guard++
            }
            assertTrue("Capitolo ${chapter.id} non completato in $guard passi", step.isChapterComplete)
            assertTrue("Capitolo ${chapter.id} non registrato come completato", chapter.id in profile.completedChapters)
        }
    }

    @Test
    fun `ogni conversazione distribuita raggiunge un nodo terminale`() {
        val catalog = loadCatalog()
        catalog.conversations.forEach { conv ->
            val nodesById = conv.nodes.associateBy { it.id }
            // BFS: un nodo terminale deve essere raggiungibile da qualche percorso
            val visited = mutableSetOf<String>()
            val queue = ArrayDeque<String>()
            queue.add(conv.startNodeId)
            var terminalFound = false
            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                if (!visited.add(id)) continue
                val node = nodesById.getValue(id)
                if (node.isTerminal) { terminalFound = true; break }
                node.options.mapNotNull { it.next }.forEach(queue::add)
            }
            assertTrue("Conversazione ${conv.id}: nessun terminale raggiungibile", terminalFound)
        }
    }

    @Test
    fun `esperienza verticale completa della Fase 2 sui contenuti reali`() {
        val catalog = loadCatalog()
        val engine = StoryEngine()
        val knowledge = KnowledgeEngine(catalog)

        // 1. Il capitolo del mantello si gioca fino in fondo
        val chapter = catalog.chaptersById.getValue("story.mantello.001")
        var profile = NarrativeProfile()
        var step = engine.startChapter(chapter, profile)
        val events = mutableListOf<ExperienceEvent>()
        var guard = 0
        while (!step.isChapterComplete && guard < 300) {
            events += step.events
            step = if (step.availableOptions.isNotEmpty()) {
                engine.choose(chapter, step.profile, step.session, step.availableOptions.first().id)
            } else {
                engine.advance(chapter, step.profile, step.session)
            }
            guard++
        }
        events += step.events
        assertTrue(step.isChapterComplete)

        // 2. Gli eventi attraversano la progressione condivisa
        var state = ProgressionState()
        val report = knowledge.applyAll(state, events, timestampMillis = 1000L)
        state = report.newState

        assertTrue(events.any { it is ChapterCompleted })
        // L'insegnamento del capitolo è almeno esplorato
        val teachingId = "teaching.accoglienza.ogni_uomo"
        val status = state.teachings[teachingId]
        assertTrue("insegnamento non scoperto: $status", status != null && status.rank >= TeachingStatus.SCOPERTO.rank)
        // Il giardino reagisce: il nido si sblocca con l'accoglienza
        assertTrue(
            "il giardino non ha reagito: ${state.unlockedGardenElements}",
            state.unlockedGardenElements.contains("garden.nido"),
        )

        // 3. La sessione di gioco sopravvive a salvataggio e ripristino
        val sessionJson = json.encodeToString(it.orma.cammino.core.narrative.ChapterSession.serializer(), step.session)
        val restoredSession = json.decodeFromString(it.orma.cammino.core.narrative.ChapterSession.serializer(), sessionJson)
        assertEquals(step.session, restoredSession)
        val profileJson = json.encodeToString(NarrativeProfile.serializer(), step.profile)
        assertEquals(step.profile, json.decodeFromString(NarrativeProfile.serializer(), profileJson))

        // 4. Il dialogo approfondisce (simulazione completamento conversazione)
        val report2 = knowledge.applyAll(
            state,
            listOf(DialogueCompleted("dlg.perdono"), TeachingDeepened("teaching.perdono.mille_volte", Modality.DIALOGO)),
            timestampMillis = 2000L,
        )
        state = report2.newState
        assertEquals(TeachingStatus.APPROFONDITO, state.teachings["teaching.perdono.mille_volte"])
        assertTrue(state.completedDialogues.contains("dlg.perdono"))
    }

    @Test
    fun `i contenuti richiesti dalla specifica sono tutti presenti e collegati`() {
        val catalog = loadCatalog()
        // Capitolo biografico
        assertTrue(catalog.storyChapters.any { it.isBiographical })
        // Almeno una scelta con condizioni (conseguenze reali)
        val conditionalChoices = catalog.storyChapters.flatMap { it.nodes }
            .filterIsInstance<it.orma.cammino.core.content.schema.ChoiceNode>()
            .any { c -> c.options.any { it.conditions.isNotEmpty() } }
        assertTrue("nessuna scelta condizionata nei capitoli", conditionalChoices)
        // Almeno una diramazione condizionale che usa flag tra capitoli
        val branches = catalog.storyChapters.flatMap { it.nodes }
            .filterIsInstance<it.orma.cammino.core.content.schema.ConditionalBranchNode>()
        assertTrue(branches.isNotEmpty())
        // Insegnamenti con esperienze collegate
        val withExperiences = catalog.teachings.count {
            it.experiences.viandante.isNotEmpty() || it.experiences.garden.isNotEmpty() ||
                it.experiences.cards.isNotEmpty() || it.experiences.dialogue.isNotEmpty()
        }
        assertTrue("troppi pochi insegnamenti collegati alle modalità: $withExperiences", withExperiences >= 10)
    }
}
