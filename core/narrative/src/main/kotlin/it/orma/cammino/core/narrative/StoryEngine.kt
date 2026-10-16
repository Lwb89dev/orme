package it.orma.cammino.core.narrative

import it.orma.cammino.core.content.schema.ChoiceNode
import it.orma.cammino.core.content.schema.ChoiceOption
import it.orma.cammino.core.content.schema.CompletionNode
import it.orma.cammino.core.content.schema.ConditionalBranchNode
import it.orma.cammino.core.content.schema.StoryChapter
import it.orma.cammino.core.content.schema.StoryNode
import it.orma.cammino.core.model.ChapterCompleted
import it.orma.cammino.core.model.ExperienceEvent
import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.TeachingExplored

/** Risultato di un passo del motore: nodo da mostrare, stato aggiornato, eventi emessi. */
data class StepResult(
    val node: StoryNode,
    val profile: NarrativeProfile,
    val session: ChapterSession,
    val events: List<ExperienceEvent>,
    /** Opzioni della scelta corrente filtrate in base alle condizioni; vuota se non è una scelta. */
    val availableOptions: List<ChoiceOption> = emptyList(),
    val isChapterComplete: Boolean = false,
)

class NarrativeException(message: String) : Exception(message)

/**
 * Motore narrativo data-driven del Viandante. Separato dalla UI:
 * opera solo su [StoryChapter] (contenuto) e [NarrativeProfile]/[ChapterSession]
 * (stato persistente). Deterministico e interamente testabile su JVM.
 *
 * Semantica: entrando in un nodo se ne applicano gli effetti; i nodi
 * [ConditionalBranchNode] vengono risolti automaticamente; gli altri nodi
 * sono restituiti per la resa grafica. Le scorte di scelta filtrano le
 * opzioni le cui condizioni non sono soddisfatte.
 */
class StoryEngine {

    /** Avvia (o riavvia) un capitolo: la sessione riparte dal nodo iniziale. */
    fun startChapter(
        chapter: StoryChapter,
        profile: NarrativeProfile,
        timestampMillis: Long = 0L,
    ): StepResult {
        val session = ChapterSession(chapter.id, chapter.startNodeId)
        return enterNode(chapter, chapter.startNodeId, profile, session, timestampMillis)
    }

    /** Riprende una sessione interrotta senza riapplicare effetti. */
    fun resume(
        chapter: StoryChapter,
        profile: NarrativeProfile,
        session: ChapterSession,
    ): StepResult {
        val node = nodeOf(chapter, session.currentNodeId)
        return StepResult(
            node = node,
            profile = profile,
            session = session,
            events = emptyList(),
            availableOptions = filteredOptions(node, profile),
            isChapterComplete = chapter.id in profile.completedChapters,
        )
    }

    /** Avanza al nodo successivo per i nodi a prosecuzione singola. */
    fun advance(
        chapter: StoryChapter,
        profile: NarrativeProfile,
        session: ChapterSession,
        timestampMillis: Long = 0L,
    ): StepResult {
        val node = nodeOf(chapter, session.currentNodeId)
        val next = nextOf(node)
            ?: throw NarrativeException("Il nodo '${node.id}' non ha prosecuzione diretta")
        return enterNode(chapter, next, profile, session, timestampMillis)
    }

    /** Esegue la scelta dell'utente su un [ChoiceNode]. */
    fun choose(
        chapter: StoryChapter,
        profile: NarrativeProfile,
        session: ChapterSession,
        optionId: String,
        timestampMillis: Long = 0L,
    ): StepResult {
        val node = nodeOf(chapter, session.currentNodeId)
        if (node !is ChoiceNode) throw NarrativeException("Il nodo '${node.id}' non è una scelta")
        val option = node.options.firstOrNull { it.id == optionId }
            ?: throw NarrativeException("Opzione '$optionId' inesistente nel nodo '${node.id}'")
        if (!ConditionEvaluator.allMet(option.conditions, profile)) {
            throw NarrativeException("Opzione '$optionId' non disponibile nello stato corrente")
        }
        val (newProfile, events) = EffectApplier.applyAll(option.effects, profile)
        val logged = newProfile.copy(
            choiceLog = newProfile.choiceLog + ChoiceRecord(chapter.id, node.id, optionId, timestampMillis)
        )
        return enterNode(chapter, option.next, logged, session, timestampMillis, events)
    }

    // ------------------------------------------------------------------

    private fun enterNode(
        chapter: StoryChapter,
        nodeId: String,
        profile: NarrativeProfile,
        session: ChapterSession,
        timestampMillis: Long,
        pendingEvents: List<ExperienceEvent> = emptyList(),
        guard: Int = 0,
    ): StepResult {
        if (guard > MAX_AUTO_STEPS) throw NarrativeException("Ciclo infinito nel capitolo '${chapter.id}'")
        val node = nodeOf(chapter, nodeId)
        val newSession = session.copy(
            currentNodeId = nodeId,
            visitedNodeIds = session.visitedNodeIds + nodeId,
        )
        return when (node) {
            is ConditionalBranchNode -> {
                val target = node.branches
                    .firstOrNull { ConditionEvaluator.allMet(it.conditions, profile) }
                    ?.next ?: node.defaultNext
                enterNode(chapter, target, profile, newSession, timestampMillis, pendingEvents, guard + 1)
            }

            is CompletionNode -> {
                val completedProfile = profile.copy(
                    completedChapters = profile.completedChapters + chapter.id,
                )
                val events = pendingEvents +
                    ChapterCompleted(chapter.id) +
                    node.exploredTeachingIds.map { TeachingExplored(it, Modality.VIANDANTE) }
                StepResult(node, completedProfile, newSession, events, isChapterComplete = true)
            }

            is it.orma.cammino.core.content.schema.CharacterEncounterNode -> {
                val withEncounter = profile.copy(
                    encounteredCharacters = profile.encounteredCharacters + node.characterId
                )
                val (applied, events) = EffectApplier.applyAll(node.effects, withEncounter)
                StepResult(node, applied, newSession, pendingEvents + events)
            }

            else -> {
                val effects = when (node) {
                    is it.orma.cammino.core.content.schema.NarrationNode -> node.effects
                    is it.orma.cammino.core.content.schema.DialogueLineNode -> node.effects
                    is it.orma.cammino.core.content.schema.RewardUnlockNode -> node.unlocks
                    else -> emptyList()
                }
                val (applied, events) = EffectApplier.applyAll(effects, profile)
                StepResult(
                    node = node,
                    profile = applied,
                    session = newSession,
                    events = pendingEvents + events,
                    availableOptions = filteredOptions(node, applied),
                )
            }
        }
    }

    private fun nodeOf(chapter: StoryChapter, nodeId: String): StoryNode =
        chapter.nodes.firstOrNull { it.id == nodeId }
            ?: throw NarrativeException("Nodo '$nodeId' inesistente nel capitolo '${chapter.id}'")

    private fun nextOf(node: StoryNode): String? = when (node) {
        is it.orma.cammino.core.content.schema.NarrationNode -> node.next
        is it.orma.cammino.core.content.schema.DialogueLineNode -> node.next
        is it.orma.cammino.core.content.schema.CharacterEncounterNode -> node.next
        is it.orma.cammino.core.content.schema.HistoricalInsightNode -> node.next
        is it.orma.cammino.core.content.schema.TeachingNode -> node.next
        is it.orma.cammino.core.content.schema.ReflectionNode -> node.next
        is it.orma.cammino.core.content.schema.RewardUnlockNode -> node.next
        else -> null
    }

    private fun filteredOptions(node: StoryNode, profile: NarrativeProfile): List<ChoiceOption> =
        when (node) {
            is ChoiceNode -> node.options.filter { ConditionEvaluator.allMet(it.conditions, profile) }
            else -> emptyList()
        }

    companion object {
        private const val MAX_AUTO_STEPS = 100
    }
}
