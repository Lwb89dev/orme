package it.orma.cammino.ai

import it.orma.cammino.core.content.schema.ConversationNode

/**
 * Livello 2 del Dialogo (specifica §2D): contratto per una futura
 * conversazione assistita da un modello linguistico. NON implementata:
 * nessun provider, nessuna rete, nessuna chiave API.
 *
 * Requisiti vincolanti per una futura implementazione:
 * - opt-in esplicito dell'utente, con spiegazione di cosa viene trasmesso;
 * - mai il contenuto del diario;
 * - risposte fondate su retrieval dalla base documentale locale (RAG), con
 *   le fonti mostrate e la distinzione citazione/parafrasi;
 * - il modello non impersona mai Francesco, non giudica la fede dell'utente,
 *   dichiara quando non sa;
 * - nessuna chiave API nell'APK (eventuale proxy lato server);
 * - l'app resta pienamente funzionante senza: il Livello 1 offline è il default.
 */
interface ConversationBackend {

    /** Il prossimo passo della conversazione dato il percorso compiuto. */
    suspend fun next(request: ConversationRequest): ConversationNode

    /** true se il backend richiede la rete o servizi esterni. */
    val requiresNetwork: Boolean

    /** Spiegazione mostrata all'utente prima dell'attivazione (per l'opt-in). */
    val privacyNote: String
}

data class ConversationRequest(
    val conversationId: String,
    val currentNodeId: String,
    val chosenOptionId: String?,
    val history: List<String>,
)

/** Livello 1: il motore offline deterministico, default permanente dell'app. */
class OfflineConversationBackend : ConversationBackend {
    override val requiresNetwork: Boolean = false
    override val privacyNote: String =
        "Conversazione guidata interamente sul dispositivo: nessun contenuto lascia il telefono."

    override suspend fun next(request: ConversationRequest): ConversationNode {
        // Il motore offline è guidato dal grafo dei contenuti (ConversationViewModel);
        // questo backend esiste come punto di aggancio architetturale.
        throw UnsupportedOperationException("Usare ConversationViewModel con il grafo dei contenuti")
    }
}
