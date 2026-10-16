package it.orma.cammino.ui.dialogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.schema.ConversationNode
import it.orma.cammino.core.content.schema.GuidedConversation
import it.orma.cammino.core.model.DialogueCompleted
import it.orma.cammino.core.model.ExperienceEvent
import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.TeachingDeepened
import it.orma.cammino.core.progression.ProgressionState
import it.orma.cammino.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DialogueListUiState(
    val loading: Boolean = true,
    val conversations: List<ConversationRow> = emptyList(),
)

data class ConversationRow(
    val conversation: GuidedConversation,
    val completed: Boolean,
)

class DialogueListViewModel(container: AppContainer, private val catalog: ContentCatalog) : ViewModel() {

    private val _state = MutableStateFlow(DialogueListUiState())
    val state: StateFlow<DialogueListUiState> = _state

    init {
        viewModelScope.launch {
            val repo = container.progressionRepository ?: return@launch
            repo.state.collect { progression ->
                _state.value = DialogueListUiState(
                    loading = false,
                    conversations = catalog.conversations.map {
                        ConversationRow(it, it.id in progression.completedDialogues)
                    },
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, catalog: ContentCatalog): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DialogueListViewModel(container, catalog) as T
            }
    }
}

// ---------------------------------------------------------------------------

data class ConversationUiState(
    val loading: Boolean = true,
    val title: String = "",
    val node: ConversationNode? = null,
    val isComplete: Boolean = false,
    val error: String? = null,
)

class ConversationViewModel(
    private val container: AppContainer,
    private val catalog: ContentCatalog,
    private val conversationId: String,
) : ViewModel() {

    private val conversation: GuidedConversation? = catalog.conversationsById[conversationId]
    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state

    init {
        val c = conversation
        if (c == null) {
            _state.value = ConversationUiState(loading = false, error = "Conversazione non trovata")
        } else {
            goTo(c.startNodeId, completed = false)
        }
    }

    private fun goTo(nodeId: String, completed: Boolean) {
        val c = conversation ?: return
        val node = c.nodes.firstOrNull { it.id == nodeId }
        if (node == null) {
            _state.value = _state.value.copy(error = "Passo mancante: $nodeId")
            return
        }
        _state.value = ConversationUiState(
            loading = false,
            title = c.title,
            node = node,
            isComplete = completed || node.isTerminal,
        )
    }

    fun choose(optionId: String) {
        val node = _state.value.node ?: return
        val option = node.options.firstOrNull { it.id == optionId } ?: return
        val next = option.next ?: return
        goTo(next, completed = false)
    }

    fun complete() {
        val c = conversation ?: return
        viewModelScope.launch {
            val events = mutableListOf<ExperienceEvent>(DialogueCompleted(c.id))
            c.teachingIds.forEach { events += TeachingDeepened(it, Modality.DIALOGO) }
            container.dispatch(events)
        }
    }

    fun restart() {
        conversation?.let { goTo(it.startNodeId, completed = false) }
    }

    companion object {
        fun factory(container: AppContainer, catalog: ContentCatalog, conversationId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ConversationViewModel(container, catalog, conversationId) as T
            }
    }
}
