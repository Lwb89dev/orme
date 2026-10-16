package it.orma.cammino.core.progression

import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.model.CardPathCompleted
import it.orma.cammino.core.model.CardScenarioCompleted
import it.orma.cammino.core.model.ChapterCompleted
import it.orma.cammino.core.model.ContemplationCompleted
import it.orma.cammino.core.model.DialogueCompleted
import it.orma.cammino.core.model.ExperienceEvent
import it.orma.cammino.core.model.GardenElementUnlocked
import it.orma.cammino.core.model.TeachingDeepened
import it.orma.cammino.core.model.TeachingDiscovered
import it.orma.cammino.core.model.TeachingExplored
import it.orma.cammino.core.model.TeachingStatus

/** Cosa è cambiato applicando un evento: serve alla UI per celebrare (con sobrietà) gli sblocchi. */
data class UnlockReport(
    val newState: ProgressionState,
    val teachingsUpgraded: Map<String, TeachingStatus> = emptyMap(),
    val newGardenElements: List<String> = emptyList(),
    val newCardPaths: List<String> = emptyList(),
)

/**
 * Knowledge & Experience Engine: applica gli [ExperienceEvent] emessi dalle
 * quattro modalità allo stato di progressione condiviso e calcola gli sblocchi.
 * Logica pura, deterministica, senza dipendenze Android.
 */
class KnowledgeEngine(private val catalog: ContentCatalog) {

    fun apply(
        state: ProgressionState,
        event: ExperienceEvent,
        timestampMillis: Long = 0L,
    ): UnlockReport {
        var newState = when (event) {
            is TeachingDiscovered -> upgradeTeaching(state, event.teachingId, TeachingStatus.SCOPERTO)
            is TeachingExplored -> upgradeTeaching(state, event.teachingId, TeachingStatus.ESPLORATO)
            is TeachingDeepened -> upgradeTeaching(state, event.teachingId, TeachingStatus.APPROFONDITO)
            is ChapterCompleted -> state.copy(completedChapters = state.completedChapters + event.chapterId)
                .withActivity(ActivityKind.CHAPTER, event.chapterId, event.via, timestampMillis)
            is CardScenarioCompleted -> state.copy(
                scenarioChoices = state.scenarioChoices + (event.scenarioId to event.optionId)
            )
            is CardPathCompleted -> state.copy(completedPaths = state.completedPaths + event.pathId)
                .withActivity(ActivityKind.CARD_PATH, event.pathId, event.via, timestampMillis)
            is DialogueCompleted -> state.copy(completedDialogues = state.completedDialogues + event.conversationId)
                .withActivity(ActivityKind.DIALOGUE, event.conversationId, event.via, timestampMillis)
            is ContemplationCompleted -> state.copy(
                completedContemplations = state.completedContemplations + event.contemplationId
            ).withActivity(ActivityKind.CONTEMPLATION, event.contemplationId, event.via, timestampMillis)
            is GardenElementUnlocked -> state.copy(
                unlockedGardenElements = state.unlockedGardenElements + event.elementId
            )
        }
        // Ricalcola gli sblocchi dipendenti dagli insegnamenti scoperti
        val previousGarden = newState.unlockedGardenElements
        val newGarden = catalog.gardenElements
            .filter { el -> el.requiresTeaching != null && el.id !in previousGarden }
            .filter { el -> statusOf(newState, el.requiresTeaching!!) != null }
            .map { it.id }
        if (newGarden.isNotEmpty()) {
            newState = newState.copy(unlockedGardenElements = previousGarden + newGarden)
        }
        return UnlockReport(
            newState = newState,
            teachingsUpgraded = teachingsUpgradedBetween(state, newState),
            newGardenElements = newGarden,
            newCardPaths = newlyAvailablePaths(state, newState),
        )
    }

    fun applyAll(
        state: ProgressionState,
        events: List<ExperienceEvent>,
        timestampMillis: Long = 0L,
    ): UnlockReport {
        var current = state
        val upgraded = mutableMapOf<String, TeachingStatus>()
        val garden = mutableListOf<String>()
        val paths = mutableListOf<String>()
        events.forEach { event ->
            val report = apply(current, event, timestampMillis)
            current = report.newState
            report.teachingsUpgraded.forEach { (id, s) -> upgraded.merge(id, s) { a, b -> if (b.rank > a.rank) b else a } }
            garden += report.newGardenElements
            paths += report.newCardPaths
        }
        return UnlockReport(current, upgraded, garden.distinct(), paths.distinct())
    }

    // ------------------------------------------------------------------
    // Query di disponibilità
    // ------------------------------------------------------------------

    fun statusOf(state: ProgressionState, teachingId: String): TeachingStatus? = state.teachings[teachingId]

    /** Un capitolo è disponibile se i capitoli richiesti sono completati. */
    fun isChapterAvailable(state: ProgressionState, chapterId: String): Boolean {
        val chapter = catalog.chaptersById[chapterId] ?: return false
        return chapter.requiredChapters.all { it in state.completedChapters }
    }

    /** Sblocco morbido dei percorsi: servono gli insegnamenti richiesti almeno scoperti. */
    fun isCardPathAvailable(state: ProgressionState, pathId: String): Boolean {
        val path = catalog.pathsById[pathId] ?: return false
        return path.requiresTeachings.all { state.teachings.containsKey(it) }
    }

    fun isGardenElementUnlocked(state: ProgressionState, elementId: String): Boolean {
        val element = catalog.gardenElementsById[elementId] ?: return false
        val required = element.requiresTeaching ?: return true
        return elementId in state.unlockedGardenElements || state.teachings.containsKey(required)
    }

    fun isContemplationAvailable(state: ProgressionState, contemplationId: String): Boolean {
        val contemplation = catalog.contemplationsById[contemplationId] ?: return false
        val required = contemplation.requiresTeaching ?: return true
        return state.teachings.containsKey(required)
    }

    /** Le conversazioni guidate restano sempre accessibili: niente ostacoli artificiali. */
    fun isConversationAvailable(conversationId: String): Boolean =
        catalog.conversationsById.containsKey(conversationId)

    // ------------------------------------------------------------------

    private fun upgradeTeaching(
        state: ProgressionState,
        teachingId: String,
        target: TeachingStatus,
    ): ProgressionState {
        if (catalog.teachingsById[teachingId] == null) return state
        val current = state.teachings[teachingId]
        if (current != null && current.rank >= target.rank) return state
        return state.copy(teachings = state.teachings + (teachingId to target))
    }

    private fun teachingsUpgradedBetween(
        before: ProgressionState,
        after: ProgressionState,
    ): Map<String, TeachingStatus> =
        after.teachings.filter { (id, s) -> before.teachings[id] != s }

    private fun newlyAvailablePaths(before: ProgressionState, after: ProgressionState): List<String> =
        catalog.cardPaths
            .filter { !isCardPathAvailable(before, it.id) && isCardPathAvailable(after, it.id) }
            .map { it.id }

    private fun ProgressionState.withActivity(
        kind: ActivityKind,
        id: String,
        via: it.orma.cammino.core.model.Modality,
        timestampMillis: Long,
    ): ProgressionState = copy(
        lastActivity = ActivityRef(kind, id, timestampMillis),
        experienceLog = experienceLog + ExperienceLogEntry(timestampMillis, kind, id, via),
    )
}
