package it.orma.cammino.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Quanto è solida la collocazione storica di un evento. */
@Serializable
enum class CertaintyLevel {
    /** Documentato da fonti coeve o quasi coeve attendibili. */
    @SerialName("documented") DOCUMENTED,

    /** Basato su testimonianze medievali con elementi discussi. */
    @SerialName("tradition") TRADITION,

    /** Episodio della tradizione agiografica, non verificabile come fatto. */
    @SerialName("legend") LEGEND,
}

/** Evento della cronologia francescana. */
@Serializable
data class HistoricalEvent(
    val id: String,
    val title: String,
    val description: String = "",
    /** Anno ordinabile (migliore stima). */
    val yearFrom: Int,
    val yearTo: Int? = null,
    /** Etichetta leggibile, es. "1205-1206 (data incerta)". */
    val dateLabel: String = "",
    /** Spiegazione dell'incertezza cronologica, quando presente. */
    val chronologicalUncertainty: String = "",
    val placeId: String? = null,
    val personIds: List<String> = emptyList(),
    val sourceDocumentIds: List<String> = emptyList(),
    val teachingIds: List<String> = emptyList(),
    val certaintyLevel: CertaintyLevel = CertaintyLevel.TRADITION,
    val reviewStatus: ReviewStatus = ReviewStatus.DRAFT,
)

/** Persona della storia francescana (o personaggio dichiaratamente fittizio). */
@Serializable
data class HistoricalPerson(
    val id: String,
    val name: String,
    val role: String = "",
    val description: String = "",
    /** false per i personaggi inventati per il gioco (categoria narrazione_originale). */
    val isHistorical: Boolean = true,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val sourceDocumentIds: List<String> = emptyList(),
)

/** Luogo francescano o ambientazione narrativa. */
@Serializable
data class FranciscanPlace(
    val id: String,
    val name: String,
    val description: String = "",
    /** Nota che distingue luogo storico da ambientazione ricostruita. */
    val historicalNote: String = "",
    val isHistoricalPlace: Boolean = true,
    val region: String = "Umbria",
)

/** Voce del glossario (termini storici, religiosi, francescani). */
@Serializable
data class GlossaryEntry(
    val id: String,
    val term: String,
    val definition: String,
)

/** Capitolo della biografia introduttiva di Francesco. */
@Serializable
data class BiographyChapter(
    val id: String,
    val order: Int,
    val title: String,
    val text: String,
    val sourceDocumentIds: List<String> = emptyList(),
    val teachingIds: List<String> = emptyList(),
)
