package it.orma.cammino.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import it.orma.cammino.core.database.dao.FavoritesDao
import it.orma.cammino.core.database.dao.JournalDao
import it.orma.cammino.core.database.dao.NarrativeDao
import it.orma.cammino.core.database.dao.ProgressionDao
import it.orma.cammino.core.database.entity.ChapterSessionEntity
import it.orma.cammino.core.database.entity.FavoriteEntity
import it.orma.cammino.core.database.entity.JournalEntryEntity
import it.orma.cammino.core.database.entity.NarrativeProfileEntity
import it.orma.cammino.core.database.entity.ProgressionStateEntity

@Database(
    entities = [
        ProgressionStateEntity::class,
        NarrativeProfileEntity::class,
        ChapterSessionEntity::class,
        JournalEntryEntity::class,
        FavoriteEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class CamminoDatabase : RoomDatabase() {
    abstract fun progressionDao(): ProgressionDao
    abstract fun narrativeDao(): NarrativeDao
    abstract fun journalDao(): JournalDao
    abstract fun favoritesDao(): FavoritesDao

    companion object {
        const val NAME = "cammino.db"

        /** Aggiunge le colonne della condivisione su Nostr: tutte le voci esistenti restano private. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN shareOnNostr INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN nostrPublishedAt INTEGER")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN nostrSyncedAt INTEGER")
            }
        }
    }
}
