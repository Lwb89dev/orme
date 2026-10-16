package it.orma.cammino.ui.knowledge

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import it.orma.cammino.core.database.repository.FavoriteType
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.core.designsystem.components.PergamenaSection
import it.orma.cammino.core.designsystem.components.TeachingStatusChip
import it.orma.cammino.core.progression.ProgressionState
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeachingDetailViewModel(
    private val container: AppContainer,
    private val teachingId: String,
) : ViewModel() {

    private val _progression = MutableStateFlow(ProgressionState())
    val progression: StateFlow<ProgressionState> = _progression

    val isFavorite: StateFlow<Boolean> =
        container.favoritesRepository.isFavorite(FavoriteType.TEACHING, teachingId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            container.progressionRepository?.state?.collect { _progression.value = it }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { container.favoritesRepository.toggle(FavoriteType.TEACHING, teachingId) }
    }

    companion object {
        fun factory(container: AppContainer, teachingId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TeachingDetailViewModel(container, teachingId) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingDetailScreen(
    container: AppContainer,
    catalog: ContentCatalog,
    teachingId: String,
    navController: NavHostController,
) {
    val vm: TeachingDetailViewModel = viewModel(factory = TeachingDetailViewModel.factory(container, teachingId), key = teachingId)
    val progression by vm.progression.collectAsStateWithLifecycle()
    val isFavorite by vm.isFavorite.collectAsStateWithLifecycle()
    val teaching = catalog.teachingsById[teachingId]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(teaching?.title ?: "Insegnamento") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Togli dai preferiti" else "Aggiungi ai preferiti",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (teaching == null) {
                Text("Insegnamento non trovato", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TeachingStatusChip(progression.teachings[teaching.id])
                Spacer(Modifier.weight(1f))
                Text(
                    "Tema: " + teaching.theme.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(teaching.summary, style = MaterialTheme.typography.titleMedium)
            Text(teaching.explanation, style = MaterialTheme.typography.bodyLarge)
            if (teaching.historicalContext.isNotBlank()) {
                PergamenaSection {
                    Text(
                        "Il contesto",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(teaching.historicalContext, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Fonti
            if (teaching.sourcePassageIds.isNotEmpty()) {
                OrnamentalDivider()
                Text(
                    "Le fonti",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                teaching.sourcePassageIds.forEach { passageId ->
                    catalog.passagesById[passageId]?.let { passage ->
                        PassageCard(catalog, passage, navController)
                    }
                }
            }

            // Episodi collegati
            if (teaching.relatedEventIds.isNotEmpty()) {
                OrnamentalDivider()
                Text(
                    "Nella vita di Francesco",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                teaching.relatedEventIds.forEach { eventId ->
                    catalog.eventsById[eventId]?.let { event ->
                        Text("• ${event.title} (${event.dateLabel})", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Esperienze nelle quattro modalità
            OrnamentalDivider()
            Text(
                "Vivilo nelle quattro vie",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            teaching.experiences.viandante.forEach { id ->
                catalog.chaptersById[id]?.let { chapter ->
                    OutlinedButton(onClick = { navController.navigate(Route.Chapter(id)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Il Viandante: ${chapter.title}")
                    }
                }
            }
            teaching.experiences.cards.forEach { id ->
                catalog.pathsById[id]?.let { path ->
                    OutlinedButton(onClick = { navController.navigate(Route.CardPathSession(id)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Le Vie: ${path.title}")
                    }
                }
                catalog.scenariosById[id]?.let {
                    OutlinedButton(onClick = { navController.navigate(Route.CardPaths) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Le Vie della Saggezza")
                    }
                }
            }
            teaching.experiences.dialogue.forEach { id ->
                catalog.conversationsById[id]?.let { conv ->
                    OutlinedButton(onClick = { navController.navigate(Route.Conversation(id)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Il Dialogo: ${conv.title}")
                    }
                }
            }
            teaching.experiences.garden.forEach { id ->
                catalog.contemplationsById[id]?.let { cont ->
                    OutlinedButton(onClick = { navController.navigate(Route.Contemplation(id)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Il Giardino: ${cont.title}")
                    }
                }
                catalog.gardenElementsById[id]?.let { element ->
                    OutlinedButton(onClick = { navController.navigate(Route.Garden) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Il Giardino: ${element.name}")
                    }
                }
            }

            // Riflessione
            OrnamentalDivider()
            OutlinedButton(
                onClick = { navController.navigate(Route.JournalEntry(entryId = null, teachingId = teaching.id, chapterId = null)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Scrivi una riflessione nel diario")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun PassageCard(catalog: ContentCatalog, passage: it.orma.cammino.core.model.SourcePassage, navController: NavHostController) {
    val doc = catalog.documentsById[passage.documentId]
    PergamenaCard(
        onClick = { navController.navigate(Route.SourceDetail(passage.documentId)) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (doc != null) {
            it.orma.cammino.core.designsystem.components.SourceBadge(doc.category)
            Spacer(Modifier.height(6.dp))
        }
        Text(
            passage.reference.ifBlank { doc?.title ?: passage.id },
            style = MaterialTheme.typography.titleMedium,
        )
        if (passage.originalText.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                passage.originalText,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            )
        }
        if (passage.translation.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(passage.translation, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Traduzione editoriale",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (passage.paraphrase.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(passage.paraphrase, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Parafrasi editoriale",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (passage.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(passage.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
