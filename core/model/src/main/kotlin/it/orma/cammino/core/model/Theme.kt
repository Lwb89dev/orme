package it.orma.cammino.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * I 15 grandi temi del curriculum educativo (cfr. specifica §6).
 * Ogni insegnamento appartiene esattamente a un tema.
 */
@Serializable
enum class Theme {
    @SerialName("fraternita") FRATERNITA,
    @SerialName("pace") PACE,
    @SerialName("umilta") UMILTA,
    @SerialName("poverta") POVERTA,
    @SerialName("perdono") PERDONO,
    @SerialName("misericordia") MISERICORDIA,
    @SerialName("carita") CARITA,
    @SerialName("gratitudine") GRATITUDINE,
    @SerialName("gioia") GIOIA,
    @SerialName("creato") CREATO,
    @SerialName("preghiera") PREGHIERA,
    @SerialName("fede") FEDE,
    @SerialName("comunita") COMUNITA,
    @SerialName("accoglienza") ACCOGLIENZA,
    @SerialName("rinuncia") RINUNCIA,
}

/** Le quattro modalità di gioco. */
@Serializable
enum class Modality {
    @SerialName("viandante") VIANDANTE,
    @SerialName("giardino") GIARDINO,
    @SerialName("carte") CARTE,
    @SerialName("dialogo") DIALOGO,
}

/**
 * Stato di esperienza di un insegnamento: NON è una valutazione morale,
 * traccia soltanto quanto l'utente lo ha incontrato nell'app.
 * SCOPERTO -> ESPLORATO -> APPROFONDITO.
 */
@Serializable
enum class TeachingStatus(val rank: Int) {
    @SerialName("scoperto") SCOPERTO(1),
    @SerialName("esplorato") ESPLORATO(2),
    @SerialName("approfondito") APPROFONDITO(3),
}
