package it.orma.cammino.ui.dialogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import it.orma.cammino.R
import it.orma.cammino.ui.components.IllustratedBanner
import it.orma.cammino.ui.i18n.uiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueListScreen(container: AppContainer, catalog: ContentCatalog, navController: NavHostController) {
    val vm: DialogueListViewModel = viewModel(
        factory = DialogueListViewModel.factory(container, catalog),
        key = "dialogue-list:${catalog.manifest.language}",
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiText("Il Dialogo", "The Dialogue")) },
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
            IllustratedBanner(
                artworkRes = R.drawable.art_mode_dialogue,
                contentDescription = uiText("Due panche in un chiostro aperto sulle colline umbre", "Two benches in a cloister open to the Umbrian hills"),
            )
            Text(
                uiText("Una guida risponde alle grandi domande su Francesco. Non è il santo e non ne possiede l'autorità: ti accompagna tra le fonti. Funziona interamente offline.", "A guide answers great questions about Francis. It is not the saint and has no authority in his place: it accompanies you through the sources. It works entirely offline."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.conversations.forEach { row ->
                PergamenaCard(
                    onClick = { navController.navigate(Route.Conversation(row.conversation.id)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                row.conversation.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                row.conversation.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (row.completed) {
                                Text(
                                    uiText("Già percorsa — puoi ricominciarla", "Already walked — you may begin again"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                        if (row.completed) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = uiText("Completata", "Completed"), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
