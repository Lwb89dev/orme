package it.orma.cammino.core.model

import kotlinx.serialization.Serializable

/**
 * Collegamenti di un insegnamento alle esperienze delle quattro modalità.
 * Gli id sono riferimenti stabili ai contenuti JSON delle modalità.
 */
@Serializable
data class TeachingExperiences(
    /** Capitoli del Viandante che esplorano l'insegnamento. */
    val viandante: List<String> = emptyList(),
    /** Elementi/contemplazioni del Giardino collegati. */
    val garden: List<String> = emptyList(),
    /** Carte o percorsi delle Vie della Saggezza collegati. */
    val cards: List<String> = emptyList(),
    /** Conversazioni guidate del Dialogo collegate. */
    val dialogue: List<String> = emptyList(),
)

/**
 * Un insegnamento francescano: l'unità didattica centrale dell'app,
 * riutilizzata da tutte e quattro le modalità tramite il suo id stabile.
 */
@Serializable
data class Teaching(
    val id: String,
    val theme: Theme,
    val title: String,
    /** Sintesi di una riga, mostrata nelle liste. */
    val summary: String,
    /** Spiegazione editoriale estesa (categoria: interpretazione moderna). */
    val explanation: String = "",
    /** Contesto storico-religioso in cui l'insegnamento matura. */
    val historicalContext: String = "",
    val sourcePassageIds: List<String> = emptyList(),
    val relatedEventIds: List<String> = emptyList(),
    val relatedTeachingIds: List<String> = emptyList(),
    val experiences: TeachingExperiences = TeachingExperiences(),
    val reviewStatus: ReviewStatus = ReviewStatus.DRAFT,
)
