package it.orma.cammino.ui.viandante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.schema.StoryChapter
import it.orma.cammino.core.content.schema.StoryCharacter
import it.orma.cammino.core.narrative.NarrativeProfile
import it.orma.cammino.core.progression.KnowledgeEngine
import it.orma.cammino.core.progression.ProgressionState
import it.orma.cammino.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlaceSection(
    val placeId: String,
    val placeName: String,
    val historicalNote: String,
    val isHistoricalPlace: Boolean,
    val chapters: List<ChapterRow>,
)

data class ChapterRow(
    val chapter: StoryChapter,
    val completed: Boolean,
    val available: Boolean,
    val lockedReason: String?,
)

data class ViandanteUiState(
    val loading: Boolean = true,
    val sections: List<PlaceSection> = emptyList(),
    val characters: List<StoryCharacter> = emptyList(),
    val encounteredIds: Set<String> = emptySet(),
    val relations: Map<String, Int> = emptyMap(),
    val completedCount: Int = 0,
)

class ViandanteMapViewModel(
    container: AppContainer,
    private val catalog: ContentCatalog,
) : ViewModel() {

    private val engine = KnowledgeEngine(catalog)
    private val _state = MutableStateFlow(ViandanteUiState())
    val state: StateFlow<ViandanteUiState> = _state

    init {
        viewModelScope.launch {
            val repo = container.progressionRepository ?: return@launch
            repo.state.collect { progression ->
                val profile = container.narrativeRepository.currentProfile()
                _state.value = buildState(progression, profile)
            }
        }
    }

    private fun buildState(progression: ProgressionState, profile: NarrativeProfile): ViandanteUiState {
        val chapters = catalog.storyChapters.sortedBy { it.order }
        val sections = chapters.groupBy { it.placeId }.map { (placeId, placeChapters) ->
            val place = catalog.placesById[placeId]
            PlaceSection(
                placeId = placeId,
                placeName = place?.name ?: placeId,
                historicalNote = place?.historicalNote ?: "",
                isHistoricalPlace = place?.isHistoricalPlace ?: false,
                chapters = placeChapters.map { chapter ->
                    val available = engine.isChapterAvailable(progression, chapter.id)
                    val missing = chapter.requiredChapters.filter { it !in progression.completedChapters }
                    ChapterRow(
                        chapter = chapter,
                        completed = chapter.id in progression.completedChapters,
                        available = available,
                        lockedReason = if (available) null
                        else "Prima completa: " + missing.joinToString { id -> catalog.chaptersById[id]?.title ?: id },
                    )
                },
            )
        }
        return ViandanteUiState(
            loading = false,
            sections = sections,
            characters = catalog.storyCharacters,
            encounteredIds = profile.encounteredCharacters,
            relations = profile.relations,
            completedCount = progression.completedChapters.size,
        )
    }

    companion object {
        fun factory(container: AppContainer, catalog: ContentCatalog): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ViandanteMapViewModel(container, catalog) as T
            }
    }
}
