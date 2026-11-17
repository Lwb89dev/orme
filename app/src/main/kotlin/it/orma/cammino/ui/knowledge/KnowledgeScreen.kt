package it.orma.cammino.ui.knowledge

import androidx.compose.foundation.horizontalScroll
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
import it.orma.cammino.core.model.Authenticity
import it.orma.cammino.core.model.CertaintyLevel
import it.orma.cammino.core.model.SourceCategory
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
import it.orma.cammino.ui.i18n.sourceCategoryText
import it.orma.cammino.ui.i18n.teachingStatusText
import it.orma.cammino.ui.i18n.uiText

enum class KnowledgeSection(val label: String) {
    SCRITTI("Scritti"),
    INSEGNAMENTI("Insegnamenti"),
    BIOGRAFIA("Biografia"),
    CRONOLOGIA("Cronologia"),
    FONTI("Altre fonti"),
    LUOGHI("Luoghi"),
    PERSONE("Persone"),
    GLOSSARIO("Glossario"),
}

private fun KnowledgeSection.englishLabel(): String = when (this) {
    KnowledgeSection.SCRITTI -> "Writings"
    KnowledgeSection.INSEGNAMENTI -> "Teachings"
    KnowledgeSection.BIOGRAFIA -> "Biography"
    KnowledgeSection.CRONOLOGIA -> "Chronology"
    KnowledgeSection.FONTI -> "Other sources"
    KnowledgeSection.LUOGHI -> "Places"
    KnowledgeSection.PERSONE -> "People"
    KnowledgeSection.GLOSSARIO -> "Glossary"
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
fun KnowledgeScreen(
    container: AppContainer,
    catalog: ContentCatalog,
    navController: NavHostController,
    initialSection: String = KnowledgeSection.SCRITTI.name,
) {
    val vm: KnowledgeViewModel = viewModel(factory = KnowledgeViewModel.factory(container))
    val progression by vm.progression.collectAsStateWithLifecycle()
    val start = KnowledgeSection.entries.firstOrNull { it.name == initialSection }?.name
        ?: KnowledgeSection.SCRITTI.name
    var section by rememberSaveable(start) { mutableStateOf(start) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiText("Leggere Francesco", "Reading Francis")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = uiText("Indietro", "Back"))
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                KnowledgeSection.entries.forEach { s ->
                    FilterChip(
                        selected = section == s.name,
                        onClick = { section = s.name },
                        label = { Text(uiText(s.label, s.englishLabel())) },
                    )
                }
            }
            OrnamentalDivider()

            when (KnowledgeSection.valueOf(section)) {
                KnowledgeSection.SCRITTI -> WritingsSection(catalog, navController)
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
                    uiText("Fonti: ", "Sources: ") + chapter.sourceDocumentIds.mapNotNull { catalog.documentsById[it]?.title }.joinToString(", "),
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
                        CertaintyLevel.DOCUMENTED -> uiText("documentato", "documented")
                        CertaintyLevel.TRADITION -> uiText("tradizione", "tradition")
                        CertaintyLevel.LEGEND -> uiText("leggenda", "legend")
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
                    uiText("Incertezza: ${event.chronologicalUncertainty}", "Uncertainty: ${event.chronologicalUncertainty}"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (event.teachingIds.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    uiText("Insegnamenti: ", "Teachings: ") + event.teachingIds.mapNotNull { catalog.teachingsById[it]?.title }.joinToString(", "),
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
                TeachingStatusChip(
                    progression.teachings[teaching.id],
                    label = teachingStatusText(progression.teachings[teaching.id]),
                )
            }
        }
    }
}

@Composable
private fun WritingsSection(catalog: ContentCatalog, navController: NavHostController) {
    val writings = catalog.sourceDocuments.filter {
        it.category == SourceCategory.SCRITTO_AUTENTICO && it.authenticity != Authenticity.NOT_BY_FRANCIS
    }.sortedBy { it.dateFrom ?: Int.MAX_VALUE }
    Text(
        uiText(
            "I testi attribuiti a Francesco. L'originale medievale è in corsivo; la traduzione è editoriale, di questa app.",
            "Texts attributed to Francis. The medieval original is in italics; the translation is this app's own.",
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    writings.forEach { doc ->
        val readable = catalog.passagesOf(doc.id).count { it.originalText.isNotBlank() }
        PergamenaCard(
            onClick = { navController.navigate(Route.SourceDetail(doc.id)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                doc.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(
                    doc.compositionDate.ifBlank { null },
                    if (readable > 0) uiText("$readable passi nel testo", "$readable passages in the text") else uiText("scheda, senza testo integrale", "note, without the full text"),
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(doc.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SourcesSection(catalog: ContentCatalog, navController: NavHostController) {
    catalog.sourceDocuments.filter { it.category != SourceCategory.SCRITTO_AUTENTICO }.forEach { doc ->
        PergamenaCard(
            onClick = { navController.navigate(Route.SourceDetail(doc.id)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            it.orma.cammino.core.designsystem.components.SourceBadge(doc.category, label = sourceCategoryText(doc.category))
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
                if (place.isHistoricalPlace) uiText("Luogo storico", "Historical place") else uiText("Ambientazione narrativa", "Narrative setting"),
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
