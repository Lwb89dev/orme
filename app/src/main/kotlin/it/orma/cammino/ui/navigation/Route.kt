package it.orma.cammino.ui.navigation

import kotlinx.serialization.Serializable

/** Destinazioni type-safe dell'app. */
@Serializable
sealed interface Route {

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Home : Route

    // --- Il Viandante ---
    @Serializable
    data object ViandanteMap : Route

    @Serializable
    data class Chapter(val chapterId: String) : Route

    // --- Il Giardino ---
    @Serializable
    data object Garden : Route

    @Serializable
    data class Contemplation(val contemplationId: String) : Route

    // --- Le Vie della Saggezza ---
    @Serializable
    data object CardPaths : Route

    @Serializable
    data class CardPathSession(val pathId: String) : Route

    // --- Il Dialogo ---
    @Serializable
    data object DialogueList : Route

    @Serializable
    data class Conversation(val conversationId: String) : Route

    // --- Conoscenza ---
    /** `section` è il nome di [it.orma.cammino.ui.knowledge.KnowledgeSection]. */
    @Serializable
    data class Knowledge(val section: String = "SCRITTI") : Route

    @Serializable
    data class TeachingDetail(val teachingId: String) : Route

    @Serializable
    data class SourceDetail(val documentId: String) : Route

    // --- Diario ---
    @Serializable
    data object Journal : Route

    @Serializable
    data class JournalEntry(val entryId: String?, val teachingId: String?, val chapterId: String?) : Route

    // --- Impostazioni ---
    @Serializable
    data object Settings : Route
}
