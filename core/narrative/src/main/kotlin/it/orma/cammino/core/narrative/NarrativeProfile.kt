package it.orma.cammino.core.narrative

import kotlinx.serialization.Serializable

/**
 * Stato narrativo globale e persistente del giocatore: sopravvive ai capitoli
 * e alle sessioni dell'app. Le conseguenze delle decisioni vivono qui.
 */
@Serializable
data class NarrativeProfile(
    val flags: Map<String, Boolean> = emptyMap(),
    val variables: Map<String, Int> = emptyMap(),
    /** Relazione con i personaggi: valore intero libero, di norma -5..+5. */
    val relations: Map<String, Int> = emptyMap(),
    val completedChapters: Set<String> = emptySet(),
    val encounteredCharacters: Set<String> = emptySet(),
    val choiceLog: List<ChoiceRecord> = emptyList(),
)

@Serializable
data class ChoiceRecord(
    val chapterId: String,
    val nodeId: String,
    val optionId: String,
    val timestampMillis: Long = 0L,
)

/** Sessione di gioco di un singolo capitolo: consente di riprendere a metà. */
@Serializable
data class ChapterSession(
    val chapterId: String,
    val currentNodeId: String,
    val visitedNodeIds: List<String> = emptyList(),
)
