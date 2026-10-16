package it.orma.cammino.core.content.schema

import it.orma.cammino.core.model.Theme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Tipi di elementi piazzabili nel Giardino. */
@Serializable
enum class GardenElementKind {
    @SerialName("albero") ALBERO,
    @SerialName("fiore") FIORE,
    @SerialName("acqua") ACQUA,
    @SerialName("sentiero") SENTIERO,
    @SerialName("orto") ORTO,
    @SerialName("animali") ANIMALI,
    @SerialName("struttura") STRUTTURA,
    @SerialName("cielo") CIELO,
}

/**
 * Elemento del giardino. Si sblocca quando l'insegnamento richiesto
 * risulta almeno SCOPERTO; null = disponibile dall'inizio.
 */
@Serializable
data class GardenElement(
    val id: String,
    val name: String,
    val kind: GardenElementKind,
    val description: String = "",
    /** Spiegazione del simbolismo e del legame con gli insegnamenti. */
    val symbolism: String = "",
    val requiresTeaching: String? = null,
    val teachingId: String? = null,
)

/** Breve esperienza contemplativa facoltativa, senza pretese terapeutiche. */
@Serializable
data class Contemplation(
    val id: String,
    val title: String,
    val theme: Theme,
    val steps: List<ContemplationStep>,
    val requiresTeaching: String? = null,
    val teachingId: String? = null,
)

@Serializable
data class ContemplationStep(
    val kind: ContemplationStepKind,
    val text: String,
    val sourcePassageId: String? = null,
)

@Serializable
enum class ContemplationStepKind {
    @SerialName("osserva") OSSERVA,
    @SerialName("ascolta") ASCOLTA,
    @SerialName("leggi") LEGGI,
    @SerialName("rifletti") RIFLETTI,
    @SerialName("annota") ANNOTA,
}
