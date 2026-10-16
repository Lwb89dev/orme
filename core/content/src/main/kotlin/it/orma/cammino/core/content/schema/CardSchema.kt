package it.orma.cammino.core.content.schema

import it.orma.cammino.core.model.ReviewStatus
import it.orma.cammino.core.model.Theme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Tipologie di carte delle Vie della Saggezza. */
@Serializable
enum class CardType {
    @SerialName("insegnamento") INSEGNAMENTO,
    @SerialName("evento") EVENTO,
    @SerialName("personaggio") PERSONAGGIO,
    @SerialName("contesto") CONTESTO,
    @SerialName("riflessione") RIFLESSIONE,
    @SerialName("percorso") PERCORSO,
}

/** Una carta: strumento di esplorazione, non da collezione. */
@Serializable
data class WisdomCard(
    val id: String,
    val type: CardType,
    val title: String,
    val text: String,
    val theme: Theme? = null,
    val teachingId: String? = null,
    val sourcePassageIds: List<String> = emptyList(),
    val reviewStatus: ReviewStatus = ReviewStatus.DRAFT,
)

/** Percorso tematico: sequenza editoriale di scenari. */
@Serializable
data class CardPath(
    val id: String,
    val title: String,
    val theme: Theme,
    val description: String = "",
    /** Sblocco morbido: il percorso si attiva quando questi insegnamenti sono scoperti. */
    val requiresTeachings: List<String> = emptyList(),
    val scenarioIds: List<String> = emptyList(),
    val teachingIds: List<String> = emptyList(),
)

/** Scenario decisionale: situazione, opzioni, conseguenze, prospettive, fonte. */
@Serializable
data class CardScenario(
    val id: String,
    val title: String,
    val situation: String,
    val teachingId: String? = null,
    val options: List<ScenarioOption>,
    /** Fonte francescana messa a confronto alla fine dello scenario. */
    val sourcePassageIds: List<String> = emptyList(),
    val closingReflection: String = "",
)

@Serializable
data class ScenarioOption(
    val id: String,
    val text: String,
    /** Conseguenza narrativa della scelta. */
    val consequence: String,
    /** Analisi della scelta da diversi punti di vista (meccanica delle prospettive). */
    val perspectives: List<Perspective> = emptyList(),
)

@Serializable
data class Perspective(
    /** Es. "La via francescana", "La prudenza mondana", "Il diritto del tempo". */
    val label: String,
    val text: String,
    /** true: interpretazione editoriale moderna; false: affermazione sostenuta dalle fonti citate. */
    val isEditorial: Boolean = true,
    val sourcePassageIds: List<String> = emptyList(),
)
