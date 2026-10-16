package it.orma.cammino.ui.viandante

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.schema.ChoiceOption
import it.orma.cammino.core.content.schema.CompletionNode
import it.orma.cammino.core.content.schema.StoryChapter
import it.orma.cammino.core.content.schema.StoryNode
import it.orma.cammino.core.narrative.ChapterSession
import it.orma.cammino.core.narrative.NarrativeProfile
import it.orma.cammino.core.narrative.StepResult
import it.orma.cammino.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChapterUiState(
    val loading: Boolean = true,
    val chapterTitle: String = "",
    val node: StoryNode? = null,
    val speakerName: String? = null,
    val options: List<ChoiceOption> = emptyList(),
    val isComplete: Boolean = false,
    /** Messaggio discreto sugli sblocchi appena avvenuti (es. "Hai scoperto: ..."). */
    val discoveryMessage: String? = null,
    val error: String? = null,
)

class ChapterViewModel(
    private val container: AppContainer,
    private val catalog: ContentCatalog,
    private val chapterId: String,
) : ViewModel() {

    private val engine = container.storyEngine
    private val chapter: StoryChapter? = catalog.chaptersById[chapterId]

    private var profile: NarrativeProfile? = null
    private var session: ChapterSession? = null

    private val _state = MutableStateFlow(ChapterUiState())
    val state: StateFlow<ChapterUiState> = _state

    init {
        viewModelScope.launch {
            if (chapter == null) {
                _state.value = ChapterUiState(loading = false, error = "Capitolo non trovato")
                return@launch
            }
            profile = container.narrativeRepository.currentProfile()
            val saved = container.narrativeRepository.loadSession(chapterId)
            val currentProfile = profile!!
            val step = if (saved != null && chapterId !in currentProfile.completedChapters) {
                engine.resume(chapter, currentProfile, saved)
            } else {
                engine.startChapter(chapter, currentProfile)
            }
            applyStep(step)
        }
    }

    fun advance() = mutate { p, s -> engine.advance(chapter!!, p, s) }

    fun choose(optionId: String) = mutate { p, s -> engine.choose(chapter!!, p, s, optionId) }

    fun restart() = mutate { p, _ -> engine.startChapter(chapter!!, p) }

    fun dismissDiscovery() {
        _state.value = _state.value.copy(discoveryMessage = null)
    }

    private fun mutate(block: (NarrativeProfile, ChapterSession) -> StepResult) {
        val p = profile ?: return
        val s = session ?: return
        viewModelScope.launch {
            try {
                applyStep(block(p, s))
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private suspend fun applyStep(step: StepResult) {
        val chapter = chapter ?: return
        profile = step.profile
        session = step.session
        container.narrativeRepository.saveProfile(step.profile)
        if (step.isChapterComplete) {
            container.narrativeRepository.clearSession(chapterId)
        } else {
            container.narrativeRepository.saveSession(step.session)
        }
        val report = container.dispatch(step.events)

        val discoveries = mutableListOf<String>()
        report?.teachingsUpgraded?.keys?.forEach { id ->
            catalog.teachingsById[id]?.let { discoveries += "Hai scoperto: ${it.title}" }
        }
        report?.newGardenElements?.forEach { id ->
            catalog.gardenElementsById[id]?.let { discoveries += "Nel giardino compare: ${it.name}" }
        }
        report?.newCardPaths?.forEach { id ->
            catalog.pathsById[id]?.let { discoveries += "Nuovo percorso di carte: ${it.title}" }
        }

        val node = step.node
        _state.value = ChapterUiState(
            loading = false,
            chapterTitle = chapter.title,
            node = node,
            speakerName = speakerNameOf(node),
            options = step.availableOptions,
            isComplete = node is CompletionNode,
            discoveryMessage = discoveries.takeIf { it.isNotEmpty() }?.joinToString("\n"),
        )
    }

    private fun speakerNameOf(node: StoryNode?): String? = when (node) {
        is it.orma.cammino.core.content.schema.DialogueLineNode -> when (node.speakerId) {
            "player" -> "Tu"
            "narrator" -> null
            else -> catalog.charactersById[node.speakerId]?.name ?: node.speakerId
        }
        is it.orma.cammino.core.content.schema.CharacterEncounterNode ->
            catalog.charactersById[node.characterId]?.name ?: node.characterId
        else -> null
    }

    companion object {
        fun factory(container: AppContainer, catalog: ContentCatalog, chapterId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChapterViewModel(container, catalog, chapterId) as T
            }
    }
}
