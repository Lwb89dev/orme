package it.orma.cammino.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Riga singola (id = 1) con lo stato di progressione serializzato in JSON. */
@Entity(tableName = "progression_state")
data class ProgressionStateEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long,
)

/** Riga singola (id = 1) con il profilo narrativo serializzato in JSON. */
@Entity(tableName = "narrative_profile")
data class NarrativeProfileEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long,
)

/** Sessione di gioco in corso per un capitolo del Viandante. */
@Entity(tableName = "chapter_session")
data class ChapterSessionEntity(
    @PrimaryKey val chapterId: String,
    val sessionJson: String,
    val updatedAt: Long,
)

/** Voce del diario personale: privata, locale, mai analizzata. */
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val kind: String,
    val text: String,
    val teachingId: String? = null,
    val chapterId: String? = null,
    val favorite: Boolean = false,
    /** Scelta esplicita della persona: questa voce viene pubblicata su Nostr al salvataggio. */
    @ColumnInfo(defaultValue = "0") val shareOnNostr: Boolean = false,
    /** Prima volta che un relay ha accettato la voce (millisecondi); null se mai pubblicata. */
    val nostrPublishedAt: Long? = null,
    /** `updatedAt` della versione che un relay ha accettato per ultima. */
    val nostrSyncedAt: Long? = null,
)

/** Elementi preferiti (insegnamenti, passaggi, capitoli...). */
@Entity(tableName = "favorites", primaryKeys = ["itemType", "itemId"])
data class FavoriteEntity(
    val itemType: String,
    val itemId: String,
    val createdAt: Long,
)
