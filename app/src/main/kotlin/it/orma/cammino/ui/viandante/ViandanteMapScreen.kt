package it.orma.cammino.ui.viandante

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.navigation.Route
import it.orma.cammino.R
import it.orma.cammino.ui.components.IllustratedBanner
import it.orma.cammino.ui.components.chapterArtworkRes
import it.orma.cammino.ui.i18n.uiText
import it.orma.cammino.ui.i18n.uiFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViandanteMapScreen(container: AppContainer, catalog: ContentCatalog, navController: NavHostController) {
    val vm: ViandanteMapViewModel = viewModel(
        factory = ViandanteMapViewModel.factory(container, catalog),
        key = "viandante:${catalog.manifest.language}",
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiText("Il Viandante", "The Wayfarer")) },
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
            Text(
                uiText("Umbria, inizio XIII secolo. Sei un viandante: nessun eroe, solo passi e incontri. Le tue scelte restano.", "Umbria, early thirteenth century. You are a wayfarer: no hero, only steps and encounters. Your choices remain."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IllustratedBanner(
                artworkRes = R.drawable.art_mode_viandante,
                contentDescription = uiText("Un viandante percorre una strada umbra dopo la pioggia", "A wayfarer walks an Umbrian road after rain"),
            )
            Text(
                uiFormat("Capitoli completati: {0} / {1}", "Chapters completed: {0} / {1}", state.completedCount, catalog.storyChapters.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )

            state.sections.forEach { section ->
                Spacer(Modifier.height(4.dp))
                Text(
                    section.placeName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                if (section.historicalNote.isNotBlank()) {
                    Text(
                        section.historicalNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                section.chapters.forEach { row ->
                    ChapterCard(row) {
                        if (row.available) navController.navigate(Route.Chapter(row.chapter.id))
                    }
                }
            }

            if (state.encounteredIds.isNotEmpty()) {
                OrnamentalDivider()
                Text(
                    uiText("Incontri lungo la via", "Encounters along the way"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                state.characters.filter { it.id in state.encounteredIds }.forEach { character ->
                    PergamenaCard(modifier = Modifier.fillMaxWidth()) {
                        Text(character.name, style = MaterialTheme.typography.titleMedium)
                        if (character.description.isNotBlank()) {
                            Text(
                                character.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChapterCard(row: ChapterRow, onClick: () -> Unit) {
    PergamenaCard(
        onClick = if (row.available) onClick else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IllustratedBanner(
            artworkRes = chapterArtworkRes(row.chapter.id),
            contentDescription = uiFormat("Illustrazione del capitolo {0}", "Illustration for the chapter {0}", row.chapter.title),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (row.available) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    row.chapter.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (row.chapter.isBiographical) {
                    Text(
                        uiText("Capitolo biografico", "Biographical chapter"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (row.lockedReason != null) {
                    Text(
                        uiFormat("Prima completa: {0}", "Complete first: {0}", row.lockedReason),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                row.completed -> Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = uiText("Completato", "Completed"),
                    tint = MaterialTheme.colorScheme.primary,
                )
                !row.available -> Icon(
                    Icons.Filled.Lock,
                    contentDescription = uiText("Bloccato", "Locked"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
