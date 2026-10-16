package it.orma.cammino.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
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
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.core.designsystem.components.TeachingStatusChip
import it.orma.cammino.core.progression.ActivityKind
import it.orma.cammino.core.progression.ProgressionState
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeUiState(
    val loading: Boolean = true,
    val progression: ProgressionState = ProgressionState(),
    val teachingOfDayId: String? = null,
    val lastActivityLabel: String? = null,
    val lastActivityRoute: Route? = null,
    val chaptersDone: Int = 0,
    val chaptersTotal: Int = 0,
)

class HomeViewModel(
    private val container: AppContainer,
    private val catalog: ContentCatalog,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        viewModelScope.launch {
            val repo = container.progressionRepository ?: return@launch
            repo.state.collect { progression ->
                _state.value = buildState(progression)
            }
        }
    }

    private fun buildState(progression: ProgressionState): HomeUiState {
        val dayIndex = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val teachingOfDay = if (catalog.teachings.isNotEmpty()) {
            val undiscovered = catalog.teachings.filter { progression.teachings[it.id] == null }
            val pool = undiscovered.ifEmpty { catalog.teachings }
            pool[dayIndex % pool.size]
        } else null

        val last = progression.lastActivity
        val (label, route) = when {
            last == null -> null to null
            last.kind == ActivityKind.CHAPTER -> {
                val title = catalog.chaptersById[last.id]?.title ?: last.id
                ("Riprendi: $title") to Route.ViandanteMap
            }
            last.kind == ActivityKind.CARD_PATH -> {
                val title = catalog.pathsById[last.id]?.title ?: last.id
                ("Riprendi: $title") to Route.CardPaths
            }
            last.kind == ActivityKind.DIALOGUE -> {
                val title = catalog.conversationsById[last.id]?.title ?: last.id
                ("Riprendi: $title") to Route.DialogueList
            }
            last.kind == ActivityKind.CONTEMPLATION -> ("Riprendi: il Giardino") to Route.Garden
            else -> null to null
        }

        return HomeUiState(
            loading = false,
            progression = progression,
            teachingOfDayId = teachingOfDay?.id,
            lastActivityLabel = label,
            lastActivityRoute = route,
            chaptersDone = progression.completedChapters.size,
            chaptersTotal = catalog.storyChapters.size,
        )
    }

    companion object {
        fun factory(container: AppContainer, catalog: ContentCatalog): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(container, catalog) as T
            }
    }
}

@Composable
fun HomeScreen(container: AppContainer, catalog: ContentCatalog, navController: NavHostController) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container, catalog))
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Il Cammino di Francesco",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN).format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { navController.navigate(Route.Journal) }) {
                Icon(Icons.Outlined.EditNote, contentDescription = "Apri il diario")
            }
            IconButton(onClick = { navController.navigate(Route.Settings) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Impostazioni")
            }
        }

        // Riprendi il cammino
        if (state.lastActivityLabel != null && state.lastActivityRoute != null) {
            PergamenaCard(
                onClick = { navController.navigate(state.lastActivityRoute!!) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Il tuo cammino", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text(state.lastActivityLabel!!, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Insegnamento del giorno
        val teachingOfDay = state.teachingOfDayId?.let { catalog.teachingsById[it] }
        if (teachingOfDay != null) {
            PergamenaCard(
                onClick = { navController.navigate(Route.TeachingDetail(teachingOfDay.id)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Un seme per oggi", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(teachingOfDay.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TeachingStatusChip(state.progression.teachings[teachingOfDay.id])
                }
                Spacer(Modifier.height(4.dp))
                Text(teachingOfDay.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }

        OrnamentalDivider()

        // Le quattro modalità
        Text(
            "Le quattro vie",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModalityCard(
                    title = "Il Viandante",
                    subtitle = if (state.chaptersTotal > 0) "${state.chaptersDone}/${state.chaptersTotal} capitoli" else "Storie",
                    icon = Icons.Filled.Terrain,
                    contentDescription = "Vai al Viandante: avventura narrativa",
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Route.ViandanteMap) }
                ModalityCard(
                    title = "Il Giardino",
                    subtitle = "Osserva e cura",
                    icon = Icons.Filled.Eco,
                    contentDescription = "Vai al Giardino: esperienza contemplativa",
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Route.Garden) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModalityCard(
                    title = "Le Vie della Saggezza",
                    subtitle = "Decidi",
                    icon = Icons.Filled.Style,
                    contentDescription = "Vai alle Vie della Saggezza: carte e dilemmi",
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Route.CardPaths) }
                ModalityCard(
                    title = "Il Dialogo",
                    subtitle = "Domanda",
                    icon = Icons.Filled.Forum,
                    contentDescription = "Vai al Dialogo: conversazioni guidate",
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Route.DialogueList) }
            }
        }

        OrnamentalDivider()

        // La vita di Francesco
        PergamenaCard(
            onClick = { navController.navigate(Route.Knowledge) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("La vita e le fonti", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Biografia, cronologia, insegnamenti e documenti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ModalityCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PergamenaCard(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
