package it.orma.cammino.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Eventi di esperienza emessi dalle quattro modalità e interpretati dal
 * Knowledge & Experience Engine. Rappresentano scoperte e completamenti,
 * mai valutazioni morali dell'utente.
 */
@Serializable
sealed interface ExperienceEvent {
    /** Modalità che ha generato l'evento. */
    val via: Modality
}

/** L'utente ha incontrato per la prima volta un insegnamento. */
@Serializable
@SerialName("teaching_discovered")
data class TeachingDiscovered(val teachingId: String, override val via: Modality) : ExperienceEvent

/** L'utente ha esplorato l'insegnamento in un'esperienza (es. capitolo completato). */
@Serializable
@SerialName("teaching_explored")
data class TeachingExplored(val teachingId: String, override val via: Modality) : ExperienceEvent

/** L'utente ha approfondito l'insegnamento (es. percorso carte o dialogo completato). */
@Serializable
@SerialName("teaching_deepened")
data class TeachingDeepened(val teachingId: String, override val via: Modality) : ExperienceEvent

@Serializable
@SerialName("chapter_completed")
data class ChapterCompleted(val chapterId: String, override val via: Modality = Modality.VIANDANTE) : ExperienceEvent

@Serializable
@SerialName("card_scenario_completed")
data class CardScenarioCompleted(
    val pathId: String,
    val scenarioId: String,
    val optionId: String,
    override val via: Modality = Modality.CARTE,
) : ExperienceEvent

@Serializable
@SerialName("card_path_completed")
data class CardPathCompleted(val pathId: String, override val via: Modality = Modality.CARTE) : ExperienceEvent

@Serializable
@SerialName("dialogue_completed")
data class DialogueCompleted(val conversationId: String, override val via: Modality = Modality.DIALOGO) : ExperienceEvent

@Serializable
@SerialName("contemplation_completed")
data class ContemplationCompleted(val contemplationId: String, override val via: Modality = Modality.GIARDINO) : ExperienceEvent

/** Sblocco esplicito di un elemento del giardino (effetto narrativo). */
@Serializable
@SerialName("garden_element_unlocked")
data class GardenElementUnlocked(val elementId: String, override val via: Modality) : ExperienceEvent
