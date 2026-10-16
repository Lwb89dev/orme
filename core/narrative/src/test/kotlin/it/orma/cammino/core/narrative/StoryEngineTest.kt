package it.orma.cammino.core.narrative

import it.orma.cammino.core.content.schema.AddRelationEffect
import it.orma.cammino.core.content.schema.ChoiceNode
import it.orma.cammino.core.content.schema.ChoiceOption
import it.orma.cammino.core.content.schema.CompletionNode
import it.orma.cammino.core.content.schema.ConditionalBranch
import it.orma.cammino.core.content.schema.ConditionalBranchNode
import it.orma.cammino.core.content.schema.DialogueLineNode
import it.orma.cammino.core.content.schema.DiscoverTeachingEffect
import it.orma.cammino.core.content.schema.FlagCondition
import it.orma.cammino.core.content.schema.NarrationNode
import it.orma.cammino.core.content.schema.RelationGteCondition
import it.orma.cammino.core.content.schema.SetFlagEffect
import it.orma.cammino.core.content.schema.StoryChapter
import it.orma.cammino.core.content.schema.TeachingNode
import it.orma.cammino.core.content.schema.VarGteCondition
import it.orma.cammino.core.model.ChapterCompleted
import it.orma.cammino.core.model.TeachingDiscovered
import it.orma.cammino.core.model.TeachingExplored
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryEngineTest {

    private val engine = StoryEngine()

    /**
     * Capitolo di prova con: narrazione, scelta con effetti, diramazione
     * condizionale basata su flag e relazione, insegnamento, completamento.
     */
    private fun chapter() = StoryChapter(
        id = "story.test.001",
        title = "Prova",
        placeId = "place.assisi",
        startNodeId = "n1",
        nodes = listOf(
            NarrationNode(id = "n1", text = "Inizio", next = "c1"),
            ChoiceNode(
                id = "c1",
                prompt = "Cosa fai?",
                options = listOf(
                    ChoiceOption(
                        id = "generoso", text = "Offri il mantello", next = "n2",
                        effects = listOf(
                            SetFlagEffect("mantello_dato"),
                            AddRelationEffect("char.bernardo", 1),
                            DiscoverTeachingEffect("teaching.carita.001"),
                        ),
                    ),
                    ChoiceOption(id = "prudente", text = "Passi oltre", next = "n3"),
                    ChoiceOption(
                        id = "segreto", text = "Opzione nascosta", next = "n3",
                        conditions = listOf(FlagCondition("mantello_dato", value = false)),
                    ),
                ),
            ),
            DialogueLineNode(id = "n2", speakerId = "char.bernardo", text = "Grazie.", next = "branch1"),
            NarrationNode(id = "n3", text = "Tiri dritto.", next = "branch1"),
            ConditionalBranchNode(
                id = "branch1",
                branches = listOf(
                    ConditionalBranch(
                        conditions = listOf(FlagCondition("mantello_dato")),
                        next = "t1",
                    ),
                    ConditionalBranch(
                        conditions = listOf(RelationGteCondition("char.bernardo", 5)),
                        next = "t1",
                    ),
                ),
                defaultNext = "end",
            ),
            TeachingNode(id = "t1", teachingId = "teaching.carita.001", text = "Hai scoperto la carità.", next = "end"),
            CompletionNode(id = "end", text = "Fine.", exploredTeachingIds = listOf("teaching.carita.001")),
        ),
    )

    @Test
    fun `percorso generoso applica effetti, emette eventi e completa il capitolo`() {
        var profile = NarrativeProfile()
        var step = engine.startChapter(chapter(), profile)

        assertEquals("n1", step.node.id)
        assertTrue(step.availableOptions.isEmpty())

        step = engine.advance(chapter(), step.profile, step.session)
        val choice = step.node as ChoiceNode
        // L'opzione "segreto" è visibile solo se il flag è assente: all'inizio lo è
        assertEquals(3, step.availableOptions.size)

        step = engine.choose(chapter(), step.profile, step.session, "generoso")
        profile = step.profile
        assertEquals("n2", step.node.id)
        assertTrue(profile.flags["mantello_dato"] == true)
        assertEquals(1, profile.relations["char.bernardo"])
        assertTrue(step.events.any { it is TeachingDiscovered && it.teachingId == "teaching.carita.001" })
        assertEquals(1, profile.choiceLog.size)

        step = engine.advance(chapter(), step.profile, step.session)
        // La diramazione condizionale porta al nodo di insegnamento
        assertEquals("t1", step.node.id)

        step = engine.advance(chapter(), step.profile, step.session)
        assertTrue(step.node is CompletionNode)
        assertTrue(step.isChapterComplete)
        assertTrue(step.profile.completedChapters.contains("story.test.001"))
        assertTrue(step.events.any { it is ChapterCompleted })
        assertTrue(step.events.any { it is TeachingExplored && it.teachingId == "teaching.carita.001" })
    }

    @Test
    fun `percorso prudente salta la diramazione e completa comunque`() {
        var step = engine.startChapter(chapter(), NarrativeProfile())
        step = engine.advance(chapter(), step.profile, step.session)
        step = engine.choose(chapter(), step.profile, step.session, "prudente")
        assertEquals("n3", step.node.id)
        assertFalse(step.profile.flags.containsKey("mantello_dato"))

        step = engine.advance(chapter(), step.profile, step.session)
        assertTrue(step.node is CompletionNode)
        assertTrue(step.isChapterComplete)
    }

    @Test
    fun `le opzioni con condizioni non soddisfatte sono filtrate`() {
        var step = engine.startChapter(chapter(), NarrativeProfile())
        step = engine.advance(chapter(), step.profile, step.session)
        // Dopo aver dato il mantello in una partita precedente il flag persiste:
        // simuliamo un profilo con flag già attivo
        val profileConFlag = step.profile.copy(flags = mapOf("mantello_dato" to true))
        val resumed = engine.resume(chapter(), profileConFlag, step.session)
        assertEquals(2, resumed.availableOptions.size)
        assertFalse(resumed.availableOptions.any { it.id == "segreto" })
    }

    @Test(expected = NarrativeException::class)
    fun `scegliere un'opzione non disponibile genera errore`() {
        var step = engine.startChapter(chapter(), NarrativeProfile())
        step = engine.advance(chapter(), step.profile, step.session)
        val profileConFlag = step.profile.copy(flags = mapOf("mantello_dato" to true))
        engine.choose(chapter(), profileConFlag, step.session, "segreto")
    }

    @Test
    fun `condizioni su variabili funzionano`() {
        val profile = NarrativeProfile(variables = mapOf("calore" to 3))
        assertTrue(ConditionEvaluator.isMet(VarGteCondition("calore", 3), profile))
        assertFalse(ConditionEvaluator.isMet(VarGteCondition("calore", 4), profile))
    }

    @Test
    fun `profilo e sessione sopravvivono alla serializzazione`() {
        val json = Json { ignoreUnknownKeys = true }
        var step = engine.startChapter(chapter(), NarrativeProfile())
        step = engine.advance(chapter(), step.profile, step.session)
        step = engine.choose(chapter(), step.profile, step.session, "generoso")

        val profileJson = json.encodeToString(NarrativeProfile.serializer(), step.profile)
        val sessionJson = json.encodeToString(ChapterSession.serializer(), step.session)
        val restoredProfile = json.decodeFromString(NarrativeProfile.serializer(), profileJson)
        val restoredSession = json.decodeFromString(ChapterSession.serializer(), sessionJson)

        assertEquals(step.profile, restoredProfile)
        assertEquals(step.session, restoredSession)

        // Ripresa da stato ripristinato: il gioco continua identico
        val resumed = engine.resume(chapter(), restoredProfile, restoredSession)
        assertEquals("n2", resumed.node.id)
        val next = engine.advance(chapter(), resumed.profile, resumed.session)
        assertEquals("t1", next.node.id)
    }

    @Test
    fun `rigiocare un capitolo riparte dall'inizio ma conserva la storia del profilo`() {
        var step = engine.startChapter(chapter(), NarrativeProfile())
        step = engine.advance(chapter(), step.profile, step.session)
        step = engine.choose(chapter(), step.profile, step.session, "generoso")
        step = engine.advance(chapter(), step.profile, step.session)
        step = engine.advance(chapter(), step.profile, step.session)
        assertTrue(step.isChapterComplete)

        val replay = engine.startChapter(chapter(), step.profile)
        assertEquals("n1", replay.node.id)
        assertTrue(replay.profile.completedChapters.contains("story.test.001"))
        assertTrue(replay.profile.flags["mantello_dato"] == true)
        assertEquals(1, replay.profile.choiceLog.size)
    }
}
