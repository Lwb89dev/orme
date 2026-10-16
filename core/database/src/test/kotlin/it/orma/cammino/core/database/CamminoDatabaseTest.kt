package it.orma.cammino.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.database.entity.ProgressionStateEntity
import it.orma.cammino.core.database.repository.FavoriteType
import it.orma.cammino.core.database.repository.FavoritesRepository
import it.orma.cammino.core.database.repository.JournalKind
import it.orma.cammino.core.database.repository.JournalRepository
import it.orma.cammino.core.database.repository.ProgressionRepository
import it.orma.cammino.core.model.TeachingDiscovered
import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.TeachingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Test del database Room su JVM tramite Robolectric (database reale in memoria). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CamminoDatabaseTest {

    private lateinit var db: CamminoDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CamminoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun `progressione applica eventi, persiste e rilegge lo stato`() = runTest {
        val catalog = ContentCatalog(
            teachings = listOf(
                it.orma.cammino.core.model.Teaching(
                    id = "teaching.pace.001",
                    theme = it.orma.cammino.core.model.Theme.PACE,
                    title = "Pace",
                    summary = "s",
                )
            ),
        )
        val repo = ProgressionRepository(db, catalog)
        repo.applyEvents(listOf(TeachingDiscovered("teaching.pace.001", Modality.VIANDANTE)), timestampMillis = 10L)

        val restored = repo.currentState()
        assertEquals(TeachingStatus.SCOPERTO, restored.teachings["teaching.pace.001"])

        // Osservazione reattiva
        val viaFlow = repo.state.first()
        assertEquals(restored, viaFlow)
    }

    @Test
    fun `progressione stato corrotto degrada in sicurezza a stato vuoto`() = runTest {
        db.progressionDao().upsert(ProgressionStateEntity(json = "{json corrotto!!!", updatedAt = 1L))
        val repo = ProgressionRepository(db, ContentCatalog())
        val state = repo.currentState()
        assertTrue(state.teachings.isEmpty())
        // Il repository resta utilizzabile
        repo.applyEvents(listOf(), timestampMillis = 2L)
    }

    @Test
    fun `diario CRUD completo`() = runTest {
        val repo = JournalRepository(db)
        val entry = repo.save(kind = JournalKind.RIFLESSIONE, text = "Prima nota", teachingId = "teaching.pace.001")
        assertEquals(1, repo.entries.first().size)

        repo.save(id = entry.id, kind = JournalKind.APPUNTO, text = "Nota modificata")
        val updated = repo.entries.first().single()
        assertEquals("Nota modificata", updated.text)
        assertEquals(entry.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt >= entry.updatedAt)

        repo.delete(entry.id)
        assertTrue(repo.entries.first().isEmpty())
    }

    @Test
    fun `preferiti toggle idempotente`() = runTest {
        val repo = FavoritesRepository(db)
        assertFalse(repo.isFavorite(FavoriteType.TEACHING, "teaching.pace.001").first())
        repo.toggle(FavoriteType.TEACHING, "teaching.pace.001")
        assertTrue(repo.isFavorite(FavoriteType.TEACHING, "teaching.pace.001").first())
        repo.toggle(FavoriteType.TEACHING, "teaching.pace.001")
        assertFalse(repo.isFavorite(FavoriteType.TEACHING, "teaching.pace.001").first())
    }

    @Test
    fun `esportazione diario in JSON e Markdown`() = runTest {
        val repo = JournalRepository(db)
        repo.save(kind = JournalKind.LIBERA, text = "Oggi ho imparato la pace.")
        val json = repo.exportJson()
        assertTrue(json.contains("journal-json-1"))
        assertTrue(json.contains("Oggi ho imparato la pace."))
        val md = repo.exportMarkdown()
        assertTrue(md.contains("# Diario"))
        assertTrue(md.contains("Oggi ho imparato la pace."))
    }
}
