package it.orma.cammino.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.orma.cammino.core.database.entity.ChapterSessionEntity
import it.orma.cammino.core.database.entity.FavoriteEntity
import it.orma.cammino.core.database.entity.JournalEntryEntity
import it.orma.cammino.core.database.entity.NarrativeProfileEntity
import it.orma.cammino.core.database.entity.ProgressionStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressionDao {
    @Query("SELECT * FROM progression_state WHERE id = 1")
    fun observe(): Flow<ProgressionStateEntity?>

    @Query("SELECT * FROM progression_state WHERE id = 1")
    suspend fun get(): ProgressionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProgressionStateEntity)
}

@Dao
interface NarrativeDao {
    @Query("SELECT * FROM narrative_profile WHERE id = 1")
    fun observeProfile(): Flow<NarrativeProfileEntity?>

    @Query("SELECT * FROM narrative_profile WHERE id = 1")
    suspend fun getProfile(): NarrativeProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: NarrativeProfileEntity)

    @Query("SELECT * FROM chapter_session WHERE chapterId = :chapterId")
    suspend fun getSession(chapterId: String): ChapterSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(entity: ChapterSessionEntity)

    @Query("DELETE FROM chapter_session WHERE chapterId = :chapterId")
    suspend fun deleteSession(chapterId: String)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun get(id: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun remove(itemType: String, itemId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE itemType = :itemType AND itemId = :itemId)")
    fun observeExists(itemType: String, itemId: String): Flow<Boolean>
}
