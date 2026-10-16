package it.orma.cammino.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Classificazione delle fonti (specifica §5). Ogni contenuto testuale
 * dell'app deve poter essere ricondotto a una di queste cinque categorie.
 */
@Serializable
enum class SourceCategory {
    /** Scritti attribuiti direttamente a San Francesco. */
    @SerialName("scritto_autentico") SCRITTO_AUTENTICO,

    /** Biografie e testimonianze medievali (Celano, Bonaventura...). */
    @SerialName("testimonianza_medievale") TESTIMONIANZA_MEDIEVALE,

    /** Tradizione agiografica e racconti successivi (Fioretti...). */
    @SerialName("tradizione_agiografica") TRADIZIONE_AGIOGRAFICA,

    /** Commento o interpretazione editoriale moderna. */
    @SerialName("interpretazione_moderna") INTERPRETAZIONE_MODERNA,

    /** Narrazione originale creata per il gioco. */
    @SerialName("narrazione_originale") NARRAZIONE_ORIGINALE,
}

/** Stato editoriale di un contenuto (specifica §5). */
@Serializable
enum class ReviewStatus {
    @SerialName("draft") DRAFT,
    @SerialName("needs_verification") NEEDS_VERIFICATION,
    @SerialName("verified") VERIFIED,
    @SerialName("rejected") REJECTED,
}

/**
 * Livello di attribuzione del testo di un documento.
 * Distinto dalla categoria: un documento medievale può contenere
 * materiale leggendario; qui valutiamo l'attribuzione del testo stesso.
 */
@Serializable
enum class Authenticity {
    /** Attribuzione generalmente accettata dalla critica. */
    @SerialName("authentic") AUTHENTIC,

    /** Attribuzione tradizionale discussa dalla critica moderna. */
    @SerialName("disputed") DISPUTED,

    /** Testo che la critica NON attribuisce a Francesco (es. "Preghiera semplice"). */
    @SerialName("not_by_francis") NOT_BY_FRANCIS,

    /** Racconto agiografico/leggendario, non cronaca. */
    @SerialName("legendary") LEGENDARY,
}

/** Un documento del corpus francescano o moderno. */
@Serializable
data class SourceDocument(
    val id: String,
    val category: SourceCategory,
    val title: String,
    val author: String,
    val originalLanguage: String = "",
    /** Etichetta cronologica leggibile, es. "1225-1226" oppure "fine XIV sec.". */
    val compositionDate: String = "",
    /** Estremi ordinabili, quando ricavabili. */
    val dateFrom: Int? = null,
    val dateTo: Int? = null,
    val authenticity: Authenticity = Authenticity.AUTHENTIC,
    /** Descrizione editoriale del documento e della sua rilevanza. */
    val description: String = "",
    /** Nota sulla tradizione manoscritta / storia testuale. */
    val manuscriptNote: String = "",
    /** Stato dei diritti del testo riprodotto o parafrasato. */
    val licenseNote: String = "",
    val referenceUrls: List<String> = emptyList(),
    val reviewStatus: ReviewStatus = ReviewStatus.DRAFT,
)

/**
 * Un passo citabile di un documento. Può contenere testo originale
 * (pubblico dominio), traduzione editoriale e/o parafrasi editoriale.
 * I contenuti non verificati non devono mai apparire come citazioni certe.
 */
@Serializable
data class SourcePassage(
    val id: String,
    val documentId: String,
    /** Riferimento interno al documento, es. "Testamento 1-3". */
    val reference: String = "",
    /** Testo in lingua originale (latino/umbro), solo se di pubblico dominio. */
    val originalText: String = "",
    /** Traduzione editoriale originale dell'app, chiaramente etichettata. */
    val translation: String = "",
    /** Parafrasi editoriale: NON una citazione. */
    val paraphrase: String = "",
    val note: String = "",
    /** Es. "public_domain_original_editorial_translation", "editorial_paraphrase". */
    val license: String = "editorial_paraphrase",
    val reviewStatus: ReviewStatus = ReviewStatus.DRAFT,
)
