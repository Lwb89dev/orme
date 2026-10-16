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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.core.designsystem.components.TeachingStatusChip
import it.orma.cammino.core.model.CertaintyLevel
import it.orma.cammino.core.progression.ProgressionState
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.navigation.Route
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class KnowledgeSection(val label: String) {
    BIOGRAFIA("Biografia"),
    CRONOLOGIA("Cronologia"),
    INSEGNAMENTI("Insegnamenti"),
    FONTI("Fonti"),
    LUOGHI("Luoghi"),
    PERSONE("Persone"),
    GLOSSARIO("Glossario"),
}

class KnowledgeViewModel(container: AppContainer) : ViewModel() {
    private val _progression = MutableStateFlow(ProgressionState())
    val progression: StateFlow<ProgressionState> = _progression

    init {
        viewModelScope.launch {
            container.progressionRepository?.state?.collect { _progression.value = it }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    KnowledgeViewModel(container) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(container: AppContainer, catalog: ContentCatalog, navController: NavHostController) {
    val vm: KnowledgeViewModel = viewModel(factory = KnowledgeViewModel.factory(container))
    val progression by vm.progression.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(KnowledgeSection.BIOGRAFIA.name) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("La vita e le fonti") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Selettore di sezione
            Text("Esplora", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KnowledgeSection.entries.take(4).forEach { s ->
                        FilterChip(
                            selected = section == s.name,
                            onClick = { section = s.name },
                            label = { Text(s.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KnowledgeSection.entries.drop(4).forEach { s ->
                        FilterChip(
                            selected = section == s.name,
                            onClick = { section = s.name },
                            label = { Text(s.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
            OrnamentalDivider()

            when (KnowledgeSection.valueOf(section)) {
                KnowledgeSection.BIOGRAFIA -> BiographySection(catalog)
                KnowledgeSection.CRONOLOGIA -> TimelineSection(catalog)
                KnowledgeSection.INSEGNAMENTI -> TeachingsSection(catalog, progression, navController)
                KnowledgeSection.FONTI -> SourcesSection(catalog, navController)
                KnowledgeSection.LUOGHI -> PlacesSection(catalog)
                KnowledgeSection.PERSONE -> PersonsSection(catalog)
                KnowledgeSection.GLOSSARIO -> GlossarySection(catalog)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BiographySection(catalog: ContentCatalog) {
    catalog.biographyChapters.sortedBy { it.order }.forEach { chapter ->
        PergamenaCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                chapter.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(chapter.text, style = MaterialTheme.typography.bodyMedium)
            if (chapter.sourceDocumentIds.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Fonti: " + chapter.sourceDocumentIds.mapNotNull { catalog.documentsById[it]?.title }.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TimelineSection(catalog: ContentCatalog) {
    catalog.historicalEvents.sortedBy { it.yearFrom }.forEach { event ->
        PergamenaCard(modifier = Modifier.fillMaxWidth()) {
            Row {
                Text(
                    event.dateLabel.ifBlank { event.yearFrom.toString() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    when (event.certaintyLevel) {
                        CertaintyLevel.DOCUMENTED -> "documentato"
                        CertaintyLevel.TRADITION -> "tradizione"
                        CertaintyLevel.LEGEND -> "leggenda"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(4.dp))
            Text(event.description, style = MaterialTheme.typography.bodyMedium)
            if (event.chronologicalUncertainty.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Incertezza: ${event.chronologicalUncertainty}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (event.teachingIds.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Insegnamenti: " + event.teachingIds.mapNotNull { catalog.teachingsById[it]?.title }.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun TeachingsSection(catalog: ContentCatalog, progression: ProgressionState, navController: NavHostController) {
    catalog.teachings.forEach { teaching ->
        PergamenaCard(
            onClick = { navController.navigate(Route.TeachingDetail(teaching.id)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        teaching.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(teaching.summary, style = MaterialTheme.typography.bodyMedium)
                }
                TeachingStatusChip(progression.teachings[teaching.id])
            }
        }
    }
}

@Composable
private fun SourcesSection(catalog: ContentCatalog, navController: NavHostController) {
    catalog.sourceDocuments.forEach { doc ->
        PergamenaCard(
            onClick = { navController.navigate(Route.SourceDetail(doc.id)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            it.orma.cammino.core.designsystem.components.SourceBadge(doc.category)
            Spacer(Modifier.height(6.dp))
            Text(
                doc.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(doc.author, doc.compositionDate.ifBlank { null }).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(doc.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PlacesSection(catalog: ContentCatalog) {
    catalog.places.forEach { place ->
        PergamenaCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                place.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                if (place.isHistoricalPlace) "Luogo storico" else "Ambientazione narrativa",
                style = MaterialTheme.typography.labelSmall,
                color = if (place.isHistoricalPlace) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(4.dp))
            Text(place.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PersonsSection(catalog: ContentCatalog) {
    catalog.historicalPersons.forEach { person ->
        PergamenaCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                person.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(person.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(person.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GlossarySection(catalog: ContentCatalog) {
    catalog.glossary.forEach { entry ->
        PergamenaCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                entry.term,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(entry.definition, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
