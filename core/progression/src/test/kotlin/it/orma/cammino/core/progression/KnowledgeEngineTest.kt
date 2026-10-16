package it.orma.cammino.core.progression

import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.schema.CardPath
import it.orma.cammino.core.content.schema.GardenElement
import it.orma.cammino.core.content.schema.GardenElementKind
import it.orma.cammino.core.content.schema.GuidedConversation
import it.orma.cammino.core.model.ChapterCompleted
import it.orma.cammino.core.model.DialogueCompleted
import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.Teaching
import it.orma.cammino.core.model.TeachingDeepened
import it.orma.cammino.core.model.TeachingDiscovered
import it.orma.cammino.core.model.TeachingExplored
import it.orma.cammino.core.model.TeachingStatus
import it.orma.cammino.core.model.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeEngineTest {

    private val catalog = ContentCatalog(
        teachings = listOf(
            Teaching(id = "teaching.pace.001", theme = Theme.PACE, title = "Pace", summary = "s"),
            Teaching(id = "teaching.carita.001", theme = Theme.CARITA, title = "Carità", summary = "s"),
        ),
        gardenElements = listOf(
            GardenElement(id = "garden.ulivo", name = "Ulivo", kind = GardenElementKind.ALBERO, requiresTeaching = "teaching.pace.001"),
            GardenElement(id = "garden.fontana", name = "Fontana", kind = GardenElementKind.ACQUA, requiresTeaching = null),
        ),
        cardPaths = listOf(
            CardPath(id = "path.pace", title = "Via della pace", theme = Theme.PACE, requiresTeachings = listOf("teaching.pace.001"), scenarioIds = listOf("sc.1")),
        ),
        conversations = listOf(
            GuidedConversation(id = "dlg.pace", title = "Sulla pace", theme = Theme.PACE, startNodeId = "a", nodes = emptyList()),
        ),
    )

    private val engine = KnowledgeEngine(catalog)

    @Test
    fun `scoperta di un insegnamento sblocca giardino e percorsi collegati`() {
        val report = engine.apply(ProgressionState(), TeachingDiscovered("teaching.pace.001", Modality.VIANDANTE))
        assertEquals(TeachingStatus.SCOPERTO, report.newState.teachings["teaching.pace.001"])
        assertTrue(report.newGardenElements.contains("garden.ulivo"))
        assertTrue(report.newState.unlockedGardenElements.contains("garden.ulivo"))
        assertTrue(report.newCardPaths.contains("path.pace"))
        assertTrue(engine.isCardPathAvailable(report.newState, "path.pace"))
        assertTrue(engine.isGardenElementUnlocked(report.newState, "garden.ulivo"))
        // L'elemento senza prerequisiti è sempre disponibile ma non "sbloccato" dall'evento
        assertTrue(engine.isGardenElementUnlocked(report.newState, "garden.fontana"))
        assertFalse(report.newGardenElements.contains("garden.fontana"))
    }

    @Test
    fun `gli stati degli insegnamenti avanzano e non regrediscono mai`() {
        var state = ProgressionState()
        state = engine.apply(state, TeachingDiscovered("teaching.pace.001", Modality.VIANDANTE)).newState
        state = engine.apply(state, TeachingExplored("teaching.pace.001", Modality.VIANDANTE)).newState
        assertEquals(TeachingStatus.ESPLORATO, state.teachings["teaching.pace.001"])
        // Evento duplicato o di rango inferiore: nessuna regressione
        state = engine.apply(state, TeachingDiscovered("teaching.pace.001", Modality.CARTE)).newState
        assertEquals(TeachingStatus.ESPLORATO, state.teachings["teaching.pace.001"])
        state = engine.apply(state, TeachingDeepened("teaching.pace.001", Modality.DIALOGO)).newState
        assertEquals(TeachingStatus.APPROFONDITO, state.teachings["teaching.pace.001"])
    }

    @Test
    fun `evento verso insegnamento inesistente viene ignorato senza errori`() {
        val report = engine.apply(ProgressionState(), TeachingDiscovered("teaching.fantasma", Modality.CARTE))
        assertTrue(report.newState.teachings.isEmpty())
        assertTrue(report.teachingsUpgraded.isEmpty())
    }

    @Test
    fun `completamento capitolo aggiorna attivita e cronologia`() {
        val report = engine.apply(ProgressionState(), ChapterCompleted("story.mantello.001"), timestampMillis = 42L)
        assertTrue(report.newState.completedChapters.contains("story.mantello.001"))
        assertEquals("story.mantello.001", report.newState.lastActivity?.id)
        assertEquals(1, report.newState.experienceLog.size)
        assertEquals(42L, report.newState.experienceLog[0].timestampMillis)
    }

    @Test
    fun `percorso verticale completo tra le quattro modalità`() {
        // Flusso della Fase 2: scoperta nel Viandante -> giardino e carte reagiscono ->
        // approfondimento nel Dialogo.
        var state = ProgressionState()
        assertNull(engine.statusOf(state, "teaching.pace.001"))
        assertFalse(engine.isCardPathAvailable(state, "path.pace"))

        val step1 = engine.applyAll(
            state,
            listOf(
                TeachingDiscovered("teaching.pace.001", Modality.VIANDANTE),
                ChapterCompleted("story.lupo.001"),
                TeachingExplored("teaching.pace.001", Modality.VIANDANTE),
            ),
            timestampMillis = 1000L,
        )
        state = step1.newState
        assertEquals(TeachingStatus.ESPLORATO, state.teachings["teaching.pace.001"])
        assertTrue(state.unlockedGardenElements.contains("garden.ulivo"))
        assertTrue(engine.isCardPathAvailable(state, "path.pace"))

        val step2 = engine.applyAll(
            state,
            listOf(
                DialogueCompleted("dlg.pace"),
                TeachingDeepened("teaching.pace.001", Modality.DIALOGO),
            ),
            timestampMillis = 2000L,
        )
        state = step2.newState
        assertEquals(TeachingStatus.APPROFONDITO, state.teachings["teaching.pace.001"])
        assertTrue(state.completedDialogues.contains("dlg.pace"))
        assertEquals("dlg.pace", state.lastActivity?.id)
        assertEquals(2, state.experienceLog.size)
    }

    @Test
    fun `le conversazioni restano sempre accessibili`() {
        assertTrue(engine.isConversationAvailable("dlg.pace"))
        assertFalse(engine.isConversationAvailable("dlg.fantasma"))
    }
}
