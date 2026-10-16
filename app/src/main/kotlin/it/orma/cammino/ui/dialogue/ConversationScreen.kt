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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import it.orma.cammino.ui.i18n.uiText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    container: AppContainer,
    catalog: ContentCatalog,
    conversationId: String,
    navController: NavHostController,
) {
    val vm: ConversationViewModel = viewModel(
        factory = ConversationViewModel.factory(container, catalog, conversationId),
        key = "conversation:$conversationId:${catalog.manifest.language}",
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) vm.complete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = uiText("Chiudi la conversazione", "Close conversation"))
                    }
                },
                actions = {
                    IconButton(onClick = { vm.restart() }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = uiText("Ricomincia la conversazione", "Restart conversation"))
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
                artworkRes = R.drawable.art_mode_dialogue,
                contentDescription = uiText("Spazio di ascolto in un chiostro umbro", "A listening space in an Umbrian cloister"),
            )
            val node = state.node
            when {
                state.error != null -> Text(
                    state.error!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                node == null -> Text(uiText("Caricamento…", "Loading…"))
                else -> {
                    PergamenaSection {
                        Text(
                            uiText("La guida", "The guide"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(node.mentorText, style = MaterialTheme.typography.bodyLarge)
                    }

                    node.sourcePassageIds.forEach { passageId ->
                        catalog.passagesById[passageId]?.let { passage ->
                            PergamenaCard(
                                onClick = { navController.navigate(Route.SourceDetail(passage.documentId)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    uiText("Dalle fonti", "From the sources"),
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

                    node.teachingId?.let { teachingId ->
                        catalog.teachingsById[teachingId]?.let { teaching ->
                            PergamenaCard(
                                onClick = { navController.navigate(Route.TeachingDetail(teachingId)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    uiText("L'insegnamento collegato", "Linked teaching"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(teaching.title, style = MaterialTheme.typography.titleMedium)
                                Text(teaching.summary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    if (node.options.isNotEmpty()) {
                        node.options.forEach { option ->
                            OutlinedButton(
                                onClick = { vm.choose(option.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(option.text, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else if (node.isTerminal) {
                        OrnamentalDivider()
                        Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                            Text(uiText("Concludi", "Conclude"))
                        }
                        OutlinedButton(onClick = { vm.restart() }, modifier = Modifier.fillMaxWidth()) {
                            Text(uiText("Ricomincia", "Restart"))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
