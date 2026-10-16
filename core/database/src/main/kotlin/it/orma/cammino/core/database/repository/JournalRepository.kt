package it.orma.cammino.core.database.repository

import it.orma.cammino.core.database.CamminoDatabase
import it.orma.cammino.core.database.entity.FavoriteEntity
import it.orma.cammino.core.database.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

enum class JournalKind { LIBERA, RIFLESSIONE, DOMANDA, APPUNTO }

enum class FavoriteType { TEACHING, PASSAGE, CHAPTER, CARD }

/**
 * Diario personale: interamente locale, mai analizzato, mai trasmesso.
 * L'esportazione produce un documento Markdown o JSON aperto.
 */
class JournalRepository(private val db: CamminoDatabase) {

    val entries: Flow<List<JournalEntryEntity>> = db.journalDao().observeAll()

    suspend fun save(
        id: String? = null,
        kind: JournalKind,
        text: String,
        teachingId: String? = null,
        chapterId: String? = null,
        favorite: Boolean = false,
        timestampMillis: Long = System.currentTimeMillis(),
    ): JournalEntryEntity {
        val existing = id?.let { db.journalDao().get(it) }
        val entity = JournalEntryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            createdAt = existing?.createdAt ?: timestampMillis,
            updatedAt = timestampMillis,
            kind = kind.name,
            text = text,
            teachingId = teachingId,
            chapterId = chapterId,
            favorite = favorite,
        )
        db.journalDao().upsert(entity)
        return entity
    }

    suspend fun delete(id: String) = db.journalDao().delete(id)

    // ------------------------------------------------------------------
    // Esportazione in formati aperti
    // ------------------------------------------------------------------

    @Serializable
    private data class ExportEntry(
        val id: String,
        val createdAt: Long,
        val updatedAt: Long,
        val kind: String,
        val text: String,
        val teachingId: String?,
        val chapterId: String?,
        val favorite: Boolean,
    )

    @Serializable
    private data class JournalExport(
        val app: String = "Orme",
        val format: String = "journal-json-1",
        val exportedAt: Long,
        val entries: List<ExportEntry>,
    )

    suspend fun exportJson(): String {
        val all = db.journalDao().observeAll().first().map {
            ExportEntry(it.id, it.createdAt, it.updatedAt, it.kind, it.text, it.teachingId, it.chapterId, it.favorite)
        }
        val export = JournalExport(exportedAt = System.currentTimeMillis(), entries = all)
        return Json { prettyPrint = true; encodeDefaults = true }.encodeToString(JournalExport.serializer(), export)
    }

    suspend fun exportMarkdown(language: String = "it"): String {
        val all = db.journalDao().observeAll().first()
        val english = language.equals("en", ignoreCase = true)
        val sb = StringBuilder(if (english) "# Journal — Orme\n\n" else "# Diario — Orme\n\n")
        all.sortedBy { it.createdAt }.forEach { e ->
            sb.append("## ").append(java.text.SimpleDateFormat("d MMMM y, HH:mm", if (english) java.util.Locale.ENGLISH else java.util.Locale.ITALIAN).format(java.util.Date(e.createdAt))).append("\n\n")
            sb.append("*").append(if (english) e.kind.lowercase().replace("riflessione", "reflection").replace("domanda", "question").replace("appunto", "note").replace("libera", "free") else e.kind.lowercase()).append("*")
            e.teachingId?.let { sb.append(if (english) " · teaching: " else " · insegnamento: ").append(it) }
            e.chapterId?.let { sb.append(if (english) " · chapter: " else " · capitolo: ").append(it) }
            sb.append("\n\n").append(e.text).append("\n\n---\n\n")
        }
        return sb.toString()
    }
}

/** Preferiti su insegnamenti, passaggi e altri elementi di contenuto. */
class FavoritesRepository(private val db: CamminoDatabase) {

    val favorites: Flow<List<FavoriteEntity>> = db.favoritesDao().observeAll()

    fun isFavorite(type: FavoriteType, id: String): Flow<Boolean> =
        db.favoritesDao().observeExists(type.name, id)

    suspend fun toggle(type: FavoriteType, id: String) {
        val exists = db.favoritesDao().observeExists(type.name, id)
        if (exists.first()) {
            db.favoritesDao().remove(type.name, id)
        } else {
            db.favoritesDao().add(FavoriteEntity(type.name, id, System.currentTimeMillis()))
        }
    }
}
