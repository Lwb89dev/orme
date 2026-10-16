package it.orma.cammino.ui.journal

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.database.entity.FavoriteEntity
import it.orma.cammino.core.database.entity.JournalEntryEntity
import it.orma.cammino.core.database.repository.FavoriteType
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.navigation.Route
import it.orma.cammino.ui.i18n.LocalAppLanguage
import it.orma.cammino.ui.i18n.uiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JournalUiState(
    val loading: Boolean = true,
    val entries: List<JournalEntryEntity> = emptyList(),
    val favorites: List<FavoriteEntity> = emptyList(),
)

class JournalViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(JournalUiState())
    val state: StateFlow<JournalUiState> = _state

    init {
        viewModelScope.launch {
            container.journalRepository.entries.collect { entries ->
                _state.value = _state.value.copy(loading = false, entries = entries)
            }
        }
        viewModelScope.launch {
            container.favoritesRepository.favorites.collect { favorites ->
                _state.value = _state.value.copy(favorites = favorites)
            }
        }
    }

    suspend fun exportMarkdown(language: String): String = container.journalRepository.exportMarkdown(language)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    JournalViewModel(container) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(container: AppContainer, catalog: ContentCatalog, navController: NavHostController) {
    val vm: JournalViewModel = viewModel(factory = JournalViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language = LocalAppLanguage.current
    val exportChooserLabel = uiText("Esporta il diario", "Export journal")
    val exportSubject = uiText("Diario — Orme", "Journal — Orme")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiText("Il diario", "Journal")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = uiText("Indietro", "Back"))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val markdown = vm.exportMarkdown(language)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/markdown"
                                putExtra(Intent.EXTRA_SUBJECT, exportSubject)
                                putExtra(Intent.EXTRA_TEXT, markdown)
                            }
                                context.startActivity(Intent.createChooser(intent, exportChooserLabel))
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = uiText("Esporta il diario in Markdown", "Export journal as Markdown"))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Route.JournalEntry(null, null, null)) }) {
                Icon(Icons.Filled.Add, contentDescription = uiText("Nuova voce di diario", "New journal entry"))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                uiText("Privato e locale: nessuna analisi automatica, nessuna trasmissione. L'esportazione è un file Markdown che scegli tu dove condividere.", "Private and local: no automated analysis or transmission. Export is a Markdown file that you choose where to share."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.favorites.isNotEmpty()) {
                Text(
                    uiText("Preferiti", "Favourites"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                state.favorites.forEach { fav ->
                    val label = when (fav.itemType) {
                        FavoriteType.TEACHING.name -> catalog.teachingsById[fav.itemId]?.title ?: fav.itemId
                        FavoriteType.CHAPTER.name -> catalog.chaptersById[fav.itemId]?.title ?: fav.itemId
                        FavoriteType.PASSAGE.name -> catalog.passagesById[fav.itemId]?.reference ?: fav.itemId
                        else -> fav.itemId
                    }
                    Text("♥ $label", style = MaterialTheme.typography.bodyMedium)
                }
                OrnamentalDivider()
            }

            if (state.entries.isEmpty() && !state.loading) {
                Text(
                    uiText("Il diario è vuoto. Le riflessioni che scriverai lungo il cammino compariranno qui.", "The journal is empty. Reflections you write along the way will appear here."),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.entries.forEach { entry ->
                PergamenaCard(
                    onClick = { navController.navigate(Route.JournalEntry(entry.id, entry.teachingId, entry.chapterId)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            SimpleDateFormat("d MMMM y", if (language == "en") Locale.ENGLISH else Locale.ITALIAN).format(Date(entry.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            entry.kind.lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(entry.text, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
