package it.orma.cammino.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import it.orma.cammino.core.model.Authenticity
import it.orma.cammino.core.model.SourceCategory
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeUiState(
    val loading: Boolean = true,
    val progression: ProgressionState = ProgressionState(),
    val teachingOfDayId: String? = null,
    val nextChapterId: String? = null,
    val nextChapterTitle: String? = null,
    val writingId: String? = null,
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
        val orderedChapters = catalog.storyChapters.sortedBy { it.order }
        val nextChapter = orderedChapters.firstOrNull { chapter ->
            chapter.id !in progression.completedChapters &&
                chapter.requiredChapters.all { it in progression.completedChapters }
        } ?: orderedChapters.firstOrNull()
        val writings = catalog.sourceDocuments.filter { document ->
            document.category == SourceCategory.SCRITTO_AUTENTICO &&
                document.authenticity == Authenticity.AUTHENTIC &&
                catalog.passagesOf(document.id).any { it.originalText.isNotBlank() }
        }
        val writing = writings.getOrNull(dayIndex % writings.size.coerceAtLeast(1))

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
            nextChapterId = nextChapter?.id,
            nextChapterTitle = nextChapter?.title,
            writingId = writing?.id,
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
                    text = SimpleDateFormat("EEEE d MMMM", Locale.forLanguageTag(language)).format(Date()),
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
            text = uiText(
                "Un racconto per leggere gli insegnamenti di Francesco dai suoi scritti.",
                "A story for reading Francis's teachings from his own writings.",
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.nextChapterId != null) {
            val storyDescription = uiText("Vai al Viandante: avventura narrativa", "Open the Wayfarer: narrative adventure")
            PergamenaCard(
                onClick = { navController.navigate(Route.Chapter(state.nextChapterId!!)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = storyDescription },
            ) {
                Text(
                    uiText("Il racconto", "The story"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    uiText("Il Viandante", "The Wayfarer"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    state.nextChapterTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.chaptersTotal > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${state.chaptersDone}/${state.chaptersTotal} ${uiText("capitoli", "chapters")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val writing = state.writingId?.let { catalog.documentsById[it] }
        if (writing != null) {
            PergamenaCard(
                onClick = { navController.navigate(Route.SourceDetail(writing.id)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    uiText("Lo scritto", "The writing"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(writing.title, style = MaterialTheme.typography.titleMedium)
                if (writing.compositionDate.isNotBlank()) {
                    Text(
                        writing.compositionDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val teachingOfDay = state.teachingOfDayId?.let { catalog.teachingsById[it] }
        if (teachingOfDay != null) {
            PergamenaCard(
                onClick = { navController.navigate(Route.TeachingDetail(teachingOfDay.id)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(uiText("L'insegnamento", "The teaching"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
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

        if (state.lastActivityLabel != null && state.lastActivityRoute != null) {
            val resumeLabel = if (state.lastActivityLabel == "__garden__") {
                uiText("Riprendi dal Giardino", "Resume from the Garden")
            } else {
                uiText("Riprendi: ${state.lastActivityLabel!!}", "Resume: ${state.lastActivityLabel!!}")
            }
            TextButton(onClick = { navController.navigate(state.lastActivityRoute!!) }) {
                Text(resumeLabel)
            }
        }

        OrnamentalDivider()

        Text(
            uiText("Per fermarti", "If you want to stay"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        QuietLink(
            title = uiText("Gli scritti", "The writings"),
            subtitle = uiText("Le opere, in originale e in traduzione", "The works, in the original and in translation"),
        ) { navController.navigate(Route.Knowledge(section = "SCRITTI")) }
        QuietLink(
            title = uiText("Gli insegnamenti", "The teachings"),
            subtitle = uiText("Quello che i testi dicono, con la fonte a fianco", "What the texts say, with the source beside it"),
        ) { navController.navigate(Route.Knowledge(section = "INSEGNAMENTI")) }
        QuietLink(
            title = uiText("Il Giardino", "The Garden"),
            subtitle = uiText("Un luogo per rileggere ciò che hai incontrato", "A place to reread what you have met"),
            contentDescription = uiText("Vai al Giardino: esperienza contemplativa", "Open the Garden: contemplative experience"),
        ) { navController.navigate(Route.Garden) }
        QuietLink(
            title = uiText("Le Vie della Saggezza", "The Ways of Wisdom"),
            subtitle = uiText("Situazioni, senza una risposta da indovinare", "Situations, with no answer to guess"),
            contentDescription = uiText("Vai alle Vie della Saggezza: carte e dilemmi", "Open the Ways of Wisdom: cards and dilemmas"),
        ) { navController.navigate(Route.CardPaths) }
        QuietLink(
            title = uiText("Il Dialogo", "The Dialogue"),
            subtitle = uiText("Domande, con il limite delle fonti dichiarato", "Questions, with the limit of the sources stated"),
            contentDescription = uiText("Vai al Dialogo: conversazioni guidate", "Open the Dialogue: guided conversations"),
        ) { navController.navigate(Route.DialogueList) }
        QuietLink(
            title = uiText("La vita e le fonti", "Life and sources"),
            subtitle = uiText("Biografia, cronologia, luoghi, persone", "Biography, chronology, places, people"),
        ) { navController.navigate(Route.Knowledge(section = "BIOGRAFIA")) }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuietLink(
    title: String,
    subtitle: String,
    contentDescription: String? = null,
    onClick: () -> Unit,
) {
    val descriptionModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    PergamenaCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(descriptionModifier),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
