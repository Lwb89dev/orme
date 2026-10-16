package it.orma.cammino.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.schema.CardPath
import it.orma.cammino.core.content.schema.CardScenario
import it.orma.cammino.core.content.schema.ScenarioOption
import it.orma.cammino.core.model.CardPathCompleted
import it.orma.cammino.core.model.CardScenarioCompleted
import it.orma.cammino.core.model.ExperienceEvent
import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.TeachingDeepened
import it.orma.cammino.core.model.TeachingDiscovered
import it.orma.cammino.core.progression.KnowledgeEngine
import it.orma.cammino.core.progression.ProgressionState
import it.orma.cammino.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CardPathsUiState(
    val loading: Boolean = true,
    val paths: List<PathRow> = emptyList(),
    val completedPaths: Set<String> = emptySet(),
)

data class PathRow(
    val path: CardPath,
    val available: Boolean,
    val completed: Boolean,
    val scenariosDone: Int,
    val scenariosTotal: Int,
    val missingTeachings: List<String>,
)

class CardPathsViewModel(container: AppContainer, private val catalog: ContentCatalog) : ViewModel() {

    private val engine = KnowledgeEngine(catalog)
    private val _state = MutableStateFlow(CardPathsUiState())
    val state: StateFlow<CardPathsUiState> = _state

    init {
        viewModelScope.launch {
            val repo = container.progressionRepository ?: return@launch
            repo.state.collect { progression -> _state.value = build(progression) }
        }
    }

    private fun build(progression: ProgressionState): CardPathsUiState = CardPathsUiState(
        loading = false,
        completedPaths = progression.completedPaths,
        paths = catalog.cardPaths.map { path ->
            PathRow(
                path = path,
                available = engine.isCardPathAvailable(progression, path.id),
                completed = path.id in progression.completedPaths,
                scenariosDone = path.scenarioIds.count { it in progression.scenarioChoices },
                scenariosTotal = path.scenarioIds.size,
                missingTeachings = path.requiresTeachings
                    .filter { it !in progression.teachings }
                    .mapNotNull { catalog.teachingsById[it]?.title },
            )
        },
    )

    companion object {
        fun factory(container: AppContainer, catalog: ContentCatalog): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CardPathsViewModel(container, catalog) as T
            }
    }
}

// ---------------------------------------------------------------------------

enum class SessionPhase { SITUATION, OUTCOME, PATH_COMPLETE }

data class CardSessionUiState(
    val loading: Boolean = true,
    val pathTitle: String = "",
    val scenarioIndex: Int = 0,
    val scenarioCount: Int = 0,
    val scenario: CardScenario? = null,
    val chosenOption: ScenarioOption? = null,
    val phase: SessionPhase = SessionPhase.SITUATION,
    val discoveryMessage: String? = null,
    val error: String? = null,
)

class CardSessionViewModel(
    private val container: AppContainer,
    private val catalog: ContentCatalog,
    private val pathId: String,
) : ViewModel() {

    private val path: CardPath? = catalog.pathsById[pathId]
    private val english = catalog.manifest.language == "en"
    private val _state = MutableStateFlow(CardSessionUiState())
    val state: StateFlow<CardSessionUiState> = _state

    init {
        val p = path
        if (p == null || p.scenarioIds.isEmpty()) {
            _state.value = CardSessionUiState(
                loading = false,
                error = if (english) "Way not found" else "Percorso non trovato",
            )
        } else {
            loadScenario(0)
        }
    }

    private fun loadScenario(index: Int) {
        val p = path ?: return
        val scenario = catalog.scenariosById[p.scenarioIds[index]]
        _state.value = CardSessionUiState(
            loading = false,
            pathTitle = p.title,
            scenarioIndex = index,
            scenarioCount = p.scenarioIds.size,
            scenario = scenario,
            phase = SessionPhase.SITUATION,
        )
    }

    fun choose(optionId: String) {
        val current = _state.value
        val scenario = current.scenario ?: return
        val option = scenario.options.firstOrNull { it.id == optionId } ?: return
        viewModelScope.launch {
            val events = mutableListOf<ExperienceEvent>(
                CardScenarioCompleted(pathId, scenario.id, optionId),
            )
            scenario.teachingId?.let { events += TeachingDiscovered(it, Modality.CARTE) }
            container.dispatch(events)
            _state.value = current.copy(chosenOption = option, phase = SessionPhase.OUTCOME)
        }
    }

    fun next() {
        val current = _state.value
        val p = path ?: return
        val nextIndex = current.scenarioIndex + 1
        if (nextIndex < p.scenarioIds.size) {
            loadScenario(nextIndex)
        } else {
            viewModelScope.launch {
                val events = mutableListOf<ExperienceEvent>(CardPathCompleted(pathId))
                p.teachingIds.forEach { events += TeachingDeepened(it, Modality.CARTE) }
                container.dispatch(events)
                _state.value = current.copy(phase = SessionPhase.PATH_COMPLETE)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, catalog: ContentCatalog, pathId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CardSessionViewModel(container, catalog, pathId) as T
            }
    }
}
