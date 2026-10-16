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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.annotation.DrawableRes
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
import it.orma.cammino.R
import it.orma.cammino.ui.components.IllustratedBanner
import it.orma.cammino.ui.navigation.Route
import it.orma.cammino.ui.i18n.LocalAppLanguage
import it.orma.cammino.ui.i18n.teachingStatusText
import it.orma.cammino.ui.i18n.uiText
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
                title to Route.ViandanteMap
            }
            last.kind == ActivityKind.CARD_PATH -> {
                val title = catalog.pathsById[last.id]?.title ?: last.id
                title to Route.CardPaths
            }
            last.kind == ActivityKind.DIALOGUE -> {
                val title = catalog.conversationsById[last.id]?.title ?: last.id
                title to Route.DialogueList
            }
            last.kind == ActivityKind.CONTEMPLATION -> ("__garden__") to Route.Garden
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
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(container, catalog),
        key = "home:${catalog.manifest.language}",
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
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
                    text = "Orme",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = SimpleDateFormat("EEEE d MMMM", if (language == "en") Locale.ENGLISH else Locale.ITALIAN).format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { navController.navigate(Route.Journal) }) {
                Icon(Icons.Outlined.EditNote, contentDescription = uiText("Apri il diario", "Open journal"))
            }
            IconButton(onClick = { navController.navigate(Route.Settings) }) {
                Icon(Icons.Filled.Settings, contentDescription = uiText("Impostazioni", "Settings"))
            }
        }

        IllustratedBanner(
            artworkRes = R.drawable.art_home_umbria,
            contentDescription = uiText("Paesaggio umbro con un sentiero tra ulivi e colline", "Umbrian landscape with a path among olive trees and hills"),
        )
        Text(
            text = uiText("Quattro vie per incontrare la storia, le fonti e l'eredità umana di Francesco d'Assisi.", "Four ways to encounter the history, sources, and human legacy of Francis of Assisi."),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Riprendi il cammino
        if (state.lastActivityLabel != null && state.lastActivityRoute != null) {
            PergamenaCard(
                onClick = { navController.navigate(state.lastActivityRoute!!) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(uiText("Il tuo cammino", "Your journey"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text(if (state.lastActivityLabel == "__garden__") uiText("Riprendi: il Giardino", "Resume: the Garden") else uiText("Riprendi: ${state.lastActivityLabel!!}", "Resume: ${state.lastActivityLabel!!}"), style = MaterialTheme.typography.titleMedium)
            }
        }

        // Insegnamento del giorno
        val teachingOfDay = state.teachingOfDayId?.let { catalog.teachingsById[it] }
        if (teachingOfDay != null) {
            PergamenaCard(
                onClick = { navController.navigate(Route.TeachingDetail(teachingOfDay.id)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(uiText("Un seme per oggi", "A seed for today"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(teachingOfDay.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TeachingStatusChip(
                        state.progression.teachings[teachingOfDay.id],
                        label = teachingStatusText(state.progression.teachings[teachingOfDay.id]),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(teachingOfDay.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }

        OrnamentalDivider()

        // Le quattro modalità
        Text(
            uiText("Le quattro vie", "The four ways"),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModalityCard(
                    title = uiText("Il Viandante", "The Wayfarer"),
                    subtitle = if (state.chaptersTotal > 0) "${state.chaptersDone}/${state.chaptersTotal} ${uiText("capitoli", "chapters")}" else uiText("Storie", "Stories"),
                    icon = Icons.Filled.Terrain,
                    artworkRes = R.drawable.art_mode_viandante,
                    contentDescription = uiText("Vai al Viandante: avventura narrativa", "Open the Wayfarer: narrative adventure"),
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Route.ViandanteMap) }
                ModalityCard(
                    title = uiText("Il Giardino", "The Garden"),
                    subtitle = uiText("Osserva e cura", "Observe and tend"),
                    icon = Icons.Filled.Eco,
                    artworkRes = R.drawable.art_mode_garden,
                    contentDescription = uiText("Vai al Giardino: esperienza contemplativa", "Open the Garden: contemplative experience"),
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Route.Garden) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModalityCard(
                    title = uiText("Le Vie della Saggezza", "The Ways of Wisdom"),
                    subtitle = uiText("Decidi", "Decide"),
                    icon = Icons.Filled.Style,
                    artworkRes = R.drawable.art_mode_wisdom,
                    contentDescription = uiText("Vai alle Vie della Saggezza: carte e dilemmi", "Open the Ways of Wisdom: cards and dilemmas"),
                    modifier = Modifier.weight(1f),
                ) { navController.navigate(Route.CardPaths) }
                ModalityCard(
                    title = uiText("Il Dialogo", "The Dialogue"),
                    subtitle = uiText("Domanda", "Ask"),
                    icon = Icons.Filled.Forum,
                    artworkRes = R.drawable.art_mode_dialogue,
                    contentDescription = uiText("Vai al Dialogo: conversazioni guidate", "Open the Dialogue: guided conversations"),
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
                    Text(uiText("La vita e le fonti", "Life and sources"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        uiText("Biografia, cronologia, insegnamenti e documenti", "Biography, chronology, teachings, and documents"),
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
    @DrawableRes artworkRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PergamenaCard(
        onClick = onClick,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        IllustratedBanner(
            artworkRes = artworkRes,
            contentDescription = null,
            aspectRatio = 4f / 3f,
        )
        Spacer(Modifier.height(10.dp))
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
