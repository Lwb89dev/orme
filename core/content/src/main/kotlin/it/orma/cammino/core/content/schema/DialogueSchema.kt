package it.orma.cammino.core.content.schema

import it.orma.cammino.core.model.Theme
import kotlinx.serialization.Serializable

/**
 * Conversazione guidata del Dialogo (livello 1: offline deterministico).
 * Il mentore NON è San Francesco e non parla mai in sua vece.
 */
@Serializable
data class GuidedConversation(
    val id: String,
    val title: String,
    val theme: Theme,
    val description: String = "",
    val teachingIds: List<String> = emptyList(),
    val startNodeId: String,
    val nodes: List<ConversationNode>,
)

@Serializable
data class ConversationNode(
    val id: String,
    /** Testo del mentore-guida. */
    val mentorText: String,
    val options: List<ConversationOption> = emptyList(),
    /** Fonti mostrate a supporto di questo passo. */
    val sourcePassageIds: List<String> = emptyList(),
    /** Se presente, questo nodo approfondisce l'insegnamento indicato. */
    val teachingId: String? = null,
    /** true: nodo conclusivo; la conversazione può essere ripresa o ricominciata. */
    val isTerminal: Boolean = false,
)

@Serializable
data class ConversationOption(
    val id: String,
    val text: String,
    val next: String? = null,
)
