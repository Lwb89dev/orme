package it.orma.cammino.ui.cards

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
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.navigation.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPathsScreen(container: AppContainer, catalog: ContentCatalog, navController: NavHostController) {
    val vm: CardPathsViewModel = viewModel(factory = CardPathsViewModel.factory(container, catalog))
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Le Vie della Saggezza") },
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
            Text(
                "Scegli una via, affronta le situazioni, confronta le prospettive con le fonti. Non ci sono risposte giuste da indovinare: c'è da capire.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.paths.forEach { row ->
                PergamenaCard(
                    onClick = if (row.available) ({ navController.navigate(Route.CardPathSession(row.path.id)) }) else null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                row.path.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (row.available) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                row.path.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            if (row.available) {
                                Text(
                                    if (row.completed) "Percorso esplorato — puoi ripercorrerlo"
                                    else "Scenari: ${row.scenariosDone}/${row.scenariosTotal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            } else {
                                Text(
                                    "Si apre scoprendo: ${row.missingTeachings.joinToString()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (row.completed) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Percorso completato", tint = MaterialTheme.colorScheme.primary)
                        } else if (!row.available) {
                            Icon(Icons.Filled.Lock, contentDescription = "Percorso bloccato", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Le carte raccolte",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            catalog.cards.forEach { card ->
                PergamenaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        card.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(card.title, style = MaterialTheme.typography.titleMedium)
                    Text(card.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
