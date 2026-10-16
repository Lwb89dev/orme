package it.orma.cammino.ui.cards

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.core.designsystem.components.PergamenaSection
import it.orma.cammino.di.AppContainer
import it.orma.cammino.R
import it.orma.cammino.ui.components.IllustratedBanner
import it.orma.cammino.ui.navigation.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardSessionScreen(
    container: AppContainer,
    catalog: ContentCatalog,
    pathId: String,
    navController: NavHostController,
) {
    val vm: CardSessionViewModel = viewModel(factory = CardSessionViewModel.factory(container, catalog, pathId))
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.pathTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Esci dal percorso")
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IllustratedBanner(
                artworkRes = R.drawable.art_mode_wisdom,
                contentDescription = "Carte illustrate su un tavolo medievale",
            )
            if (state.scenarioCount > 0) {
                LinearProgressIndicator(
                    progress = { (state.scenarioIndex) / state.scenarioCount.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Scenario ${state.scenarioIndex + 1} di ${state.scenarioCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val scenario = state.scenario
            when {
                state.error != null -> Text(
                    state.error!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                state.phase == SessionPhase.PATH_COMPLETE -> {
                    Text(
                        "Via percorsa",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "Hai attraversato tutti gli scenari di questa via. Le prospettive incontrate restano nel tuo cammino; ripercorri quando vuoi: le situazioni si rileggono diversamente con il tempo.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Torna alle vie")
                    }
                }
                scenario == null -> Text("Caricamento…")
                state.phase == SessionPhase.SITUATION -> {
                    Text(
                        scenario.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(scenario.situation, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(6.dp))
                    scenario.options.forEach { option ->
                        OutlinedButton(
                            onClick = { vm.choose(option.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(option.text, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                state.phase == SessionPhase.OUTCOME -> {
                    val chosen = state.chosenOption
                    Text(
                        scenario.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (chosen != null) {
                        PergamenaSection {
                            Text("Hai scelto: ${chosen.text}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(chosen.consequence, style = MaterialTheme.typography.bodyLarge)
                        }
                        chosen.perspectives.forEach { perspective ->
                            PergamenaSection {
                                Text(
                                    perspective.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(perspective.text, style = MaterialTheme.typography.bodyMedium)
                                if (perspective.isEditorial) {
                                    Text(
                                        "Interpretazione editoriale",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    scenario.sourcePassageIds.forEach { passageId ->
                        catalog.passagesById[passageId]?.let { passage ->
                            PergamenaCard(
                                onClick = { navController.navigate(Route.SourceDetail(passage.documentId)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "La fonte a confronto",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    passage.reference.ifBlank { catalog.documentsById[passage.documentId]?.title ?: passageId },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    passage.paraphrase.ifBlank { passage.translation },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    if (scenario.closingReflection.isNotBlank()) {
                        OrnamentalDivider()
                        Text(scenario.closingReflection, style = MaterialTheme.typography.bodyLarge)
                    }
                    Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Prosegui")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
