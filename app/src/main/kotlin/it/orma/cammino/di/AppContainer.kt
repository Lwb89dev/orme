package it.orma.cammino.di

import android.content.Context
import androidx.room.Room
import it.orma.cammino.content.ContentRepository
import it.orma.cammino.core.database.CamminoDatabase
import it.orma.cammino.core.database.repository.FavoritesRepository
import it.orma.cammino.core.database.repository.JournalRepository
import it.orma.cammino.core.database.repository.NarrativeRepository
import it.orma.cammino.core.database.repository.ProgressionRepository
import it.orma.cammino.core.database.settings.SettingsRepository
import it.orma.cammino.core.model.ExperienceEvent
import it.orma.cammino.core.progression.UnlockReport
import it.orma.cammino.core.narrative.StoryEngine
import it.orma.cammino.nostr.JournalSharing
import it.orma.cammino.ui.i18n.deviceLanguage
import it.orma.cammino.nostr.NostrAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Iniezione delle dipendenze manuale e proporzionata (scelta documentata in
 * ARCHITECTURE.md): un unico contenitore costruito all'avvio dell'app.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val contentRepository = ContentRepository(appContext)

    val database: CamminoDatabase = Room.databaseBuilder(appContext, CamminoDatabase::class.java, CamminoDatabase.NAME)
        .addMigrations(CamminoDatabase.MIGRATION_1_2)
        .build()

    val settingsRepository = SettingsRepository(appContext, defaultLanguage = ::deviceLanguage)
    val journalRepository = JournalRepository(database)
    val favoritesRepository = FavoritesRepository(database)
    val narrativeRepository = NarrativeRepository(database)

    val storyEngine = StoryEngine()

    /** Lavoro che deve finire anche se la schermata che l'ha chiesto è già chiusa. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** L'account Nostr: facoltativo, nessuna rete finché la persona non lo usa. */
    val nostrAccount = NostrAccount(appContext, appScope)
    val journalSharing = JournalSharing(appContext, journalRepository, nostrAccount, appScope)

    /** Costruito dopo il caricamento del catalogo: vedi [CamminoAppState]. */
    var progressionRepository: ProgressionRepository? = null
        private set

    fun attachCatalog(catalog: it.orma.cammino.core.content.ContentCatalog) {
        progressionRepository = ProgressionRepository(database, catalog)
    }

    /** Punto unico di ingresso degli eventi di esperienza verso la progressione. */
    suspend fun dispatch(events: List<ExperienceEvent>, timestampMillis: Long = System.currentTimeMillis()): UnlockReport? =
        progressionRepository?.applyEvents(events, timestampMillis)
}
