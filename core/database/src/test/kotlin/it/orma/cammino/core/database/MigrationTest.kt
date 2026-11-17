package it.orma.cammino.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Il diario di chi aggiorna dalla versione 1 deve sopravvivere: si ricrea il database com'era
 * (dallo schema esportato) con una voce, si apre con la versione corrente e si rilegge.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @Test
    fun `una voce della versione 1 resta leggibile e privata dopo la migrazione`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("migration-test.db").also { it.parentFile?.mkdirs(); it.delete() }

        val schema = Json.parseToJsonElement(
            File("schemas/it.orma.cammino.core.database.CamminoDatabase/1.json").readText(),
        ).jsonObject["database"]!!.jsonObject
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        (schema["entities"] as JsonArray).forEach { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            raw.execSQL(entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
        }
        (schema["setupQueries"] as JsonArray).forEach { raw.execSQL(it.jsonPrimitive.content) }
        raw.execSQL(
            "INSERT INTO journal_entries (id, createdAt, updatedAt, kind, text, teachingId, chapterId, favorite) " +
                "VALUES ('old-1', 1, 2, 'LIBERA', 'Scritta nella versione 1', NULL, NULL, 0)",
        )
        raw.version = 1
        raw.close()

        val db = Room.databaseBuilder(context, CamminoDatabase::class.java, "migration-test.db")
            .addMigrations(CamminoDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val entry = db.journalDao().observeAll().first().single()
            assertEquals("Scritta nella versione 1", entry.text)
            assertFalse("una voce vecchia non esce mai da sola", entry.shareOnNostr)
            assertNull(entry.nostrPublishedAt)
        } finally {
            db.close()
        }
    }
}
