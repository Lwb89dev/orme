package it.orma.cammino.core.progression

import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.TeachingStatus
import kotlinx.serialization.Serializable

/**
 * Stato di progressione globale e persistente, condiviso dalle quattro modalità.
 * Rappresenta ciò che l'utente ha scoperto, esplorato e approfondito:
 * mai un punteggio morale.
 */
@Serializable
data class ProgressionState(
    /** teachingId -> stato di esperienza raggiunto. */
    val teachings: Map<String, TeachingStatus> = emptyMap(),
    val completedChapters: Set<String> = emptySet(),
    /** scenarioId -> optionId dell'ultima scelta fatta. */
    val scenarioChoices: Map<String, String> = emptyMap(),
    val completedPaths: Set<String> = emptySet(),
    val completedDialogues: Set<String> = emptySet(),
    val completedContemplations: Set<String> = emptySet(),
    /** Elementi del giardino attualmente sbloccati. */
    val unlockedGardenElements: Set<String> = emptySet(),
    val lastActivity: ActivityRef? = null,
    /** Cronologia delle esperienze, in ordine temporale. */
    val experienceLog: List<ExperienceLogEntry> = emptyList(),
)

@Serializable
data class ActivityRef(
    val kind: ActivityKind,
    val id: String,
    val timestampMillis: Long,
)

@Serializable
enum class ActivityKind { CHAPTER, CARD_PATH, DIALOGUE, CONTEMPLATION, JOURNAL, KNOWLEDGE }

@Serializable
data class ExperienceLogEntry(
    val timestampMillis: Long,
    val kind: ActivityKind,
    val refId: String,
    val via: Modality,
)
