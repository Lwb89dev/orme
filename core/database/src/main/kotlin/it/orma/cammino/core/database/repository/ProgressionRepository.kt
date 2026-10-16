package it.orma.cammino.core.database.repository

import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.database.CamminoDatabase
import it.orma.cammino.core.database.entity.ChapterSessionEntity
import it.orma.cammino.core.database.entity.NarrativeProfileEntity
import it.orma.cammino.core.database.entity.ProgressionStateEntity
import it.orma.cammino.core.model.ExperienceEvent
import it.orma.cammino.core.narrative.ChapterSession
import it.orma.cammino.core.narrative.NarrativeProfile
import it.orma.cammino.core.progression.KnowledgeEngine
import it.orma.cammino.core.progression.ProgressionState
import it.orma.cammino.core.progression.UnlockReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Accesso alla progressione condivisa: applica gli eventi tramite il
 * [KnowledgeEngine] e persiste lo stato risultante. Punto unico di scrittura.
 */
class ProgressionRepository(
    private val db: CamminoDatabase,
    private val catalog: ContentCatalog,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {
    private val engine = KnowledgeEngine(catalog)

    val state: Flow<ProgressionState> = db.progressionDao().observe().map { entity ->
        entity?.let { decodeState(it.json) } ?: ProgressionState()
    }

    suspend fun currentState(): ProgressionState =
        db.progressionDao().get()?.let { decodeState(it.json) } ?: ProgressionState()

    /**
     * Applica un batch di eventi in modo atomico rispetto allo stato corrente
     * e restituisce il rapporto degli sblocchi per la UI.
     */
    suspend fun applyEvents(events: List<ExperienceEvent>, timestampMillis: Long = System.currentTimeMillis()): UnlockReport {
        if (events.isEmpty()) return UnlockReport(currentState())
        val before = currentState()
        val report = engine.applyAll(before, events, timestampMillis)
        db.progressionDao().upsert(
            ProgressionStateEntity(json = encodeState(report.newState), updatedAt = timestampMillis)
        )
        return report
    }

    private fun decodeState(raw: String): ProgressionState = try {
        json.decodeFromString(ProgressionState.serializer(), raw)
    } catch (e: Exception) {
        // Stato corrotto: si riparte da uno stato vuoto invece di bloccare l'app.
        ProgressionState()
    }

    private fun encodeState(state: ProgressionState): String =
        json.encodeToString(ProgressionState.serializer(), state)
}

/** Persistenza del profilo narrativo globale e delle sessioni di capitolo. */
class NarrativeRepository(
    private val db: CamminoDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {
    val profile: Flow<NarrativeProfile> = db.narrativeDao().observeProfile().map { entity ->
        entity?.let { decodeProfile(it.json) } ?: NarrativeProfile()
    }

    suspend fun currentProfile(): NarrativeProfile =
        db.narrativeDao().getProfile()?.let { decodeProfile(it.json) } ?: NarrativeProfile()

    suspend fun saveProfile(profile: NarrativeProfile, timestampMillis: Long = System.currentTimeMillis()) {
        db.narrativeDao().upsertProfile(
            NarrativeProfileEntity(
                json = json.encodeToString(NarrativeProfile.serializer(), profile),
                updatedAt = timestampMillis,
            )
        )
    }

    suspend fun loadSession(chapterId: String): ChapterSession? =
        db.narrativeDao().getSession(chapterId)?.let { entity ->
            try {
                json.decodeFromString(ChapterSession.serializer(), entity.sessionJson)
            } catch (e: Exception) {
                null
            }
        }

    suspend fun saveSession(session: ChapterSession, timestampMillis: Long = System.currentTimeMillis()) {
        db.narrativeDao().upsertSession(
            ChapterSessionEntity(
                chapterId = session.chapterId,
                sessionJson = json.encodeToString(ChapterSession.serializer(), session),
                updatedAt = timestampMillis,
            )
        )
    }

    suspend fun clearSession(chapterId: String) = db.narrativeDao().deleteSession(chapterId)

    private fun decodeProfile(raw: String): NarrativeProfile = try {
        json.decodeFromString(NarrativeProfile.serializer(), raw)
    } catch (e: Exception) {
        NarrativeProfile()
    }
}
