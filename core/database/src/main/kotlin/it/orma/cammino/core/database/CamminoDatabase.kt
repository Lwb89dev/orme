package it.orma.cammino.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = true,
)
abstract class CamminoDatabase : RoomDatabase() {
    abstract fun progressionDao(): ProgressionDao
    abstract fun narrativeDao(): NarrativeDao
    abstract fun journalDao(): JournalDao
    abstract fun favoritesDao(): FavoritesDao

    companion object {
        const val NAME = "cammino.db"
    }
}
