package it.orma.cammino.ui.viandante

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.schema.CharacterEncounterNode
import it.orma.cammino.core.content.schema.ChoiceNode
import it.orma.cammino.core.content.schema.CompletionNode
import it.orma.cammino.core.content.schema.DialogueLineNode
import it.orma.cammino.core.content.schema.HistoricalInsightNode
import it.orma.cammino.core.content.schema.NarrationNode
import it.orma.cammino.core.content.schema.ReflectionNode
import it.orma.cammino.core.content.schema.RewardUnlockNode
import it.orma.cammino.core.content.schema.StoryNode
import it.orma.cammino.core.content.schema.TeachingNode
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.core.designsystem.components.PergamenaSection
import it.orma.cammino.core.designsystem.components.SourceBadge
import it.orma.cammino.core.model.SourceCategory
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.navigation.LocalReduceAnimations
import it.orma.cammino.ui.navigation.Route
import it.orma.cammino.ui.components.IllustratedBanner
import it.orma.cammino.ui.components.chapterArtworkRes
import it.orma.cammino.ui.i18n.uiText
import it.orma.cammino.ui.i18n.sourceCategoryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(
    container: AppContainer,
    catalog: ContentCatalog,
    chapterId: String,
    navController: NavHostController,
) {
    val vm: ChapterViewModel = viewModel(
        factory = ChapterViewModel.factory(container, catalog, chapterId),
        key = "chapter:$chapterId:${catalog.manifest.language}",
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.discoveryMessage) {
        val message = state.discoveryMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            vm.dismissDiscovery()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.chapterTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = uiText("Chiudi il capitolo", "Close chapter"))
                    }
                },
                actions = {
                    IconButton(onClick = { vm.restart() }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = uiText("Ricomincia il capitolo", "Restart chapter"))
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
                artworkRes = chapterArtworkRes(chapterId),
                contentDescription = uiText("Illustrazione introduttiva del capitolo ${state.chapterTitle}", "Introductory illustration for ${state.chapterTitle}"),
            )
            when {
                state.loading -> Text(uiText("Caricamento…", "Loading…"), style = MaterialTheme.typography.bodyLarge)
                state.error != null -> Text(
                    uiText("Qualcosa non va: ${state.error}", "Something went wrong: ${state.error}"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                state.node != null -> NodeContent(
                    state = state,
                    catalog = catalog,
                    onAdvance = { vm.advance() },
                    onChoose = { vm.choose(it) },
                    onOpenTeaching = { navController.navigate(Route.TeachingDetail(it)) },
                    onWriteJournal = { teachingId ->
                        navController.navigate(Route.JournalEntry(entryId = null, teachingId = teachingId, chapterId = chapterId))
                    },
                    onBackToMap = { navController.popBackStack() },
                    onRestart = { vm.restart() },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NodeContent(
    state: ChapterUiState,
    catalog: ContentCatalog,
    onAdvance: () -> Unit,
    onChoose: (String) -> Unit,
    onOpenTeaching: (String) -> Unit,
    onWriteJournal: (String?) -> Unit,
    onBackToMap: () -> Unit,
    onRestart: () -> Unit,
) {
    val node = state.node ?: return
    val reduceAnimations = LocalReduceAnimations.current
    val duration = if (reduceAnimations) 0 else 260

    AnimatedContent(
        targetState = node,
        transitionSpec = { fadeIn(tween(duration)) togetherWith fadeOut(tween(duration)) },
        label = "node",
    ) { animatedNode ->
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            NodeBody(animatedNode, state, catalog, onOpenTeaching)

            when (animatedNode) {
                is ChoiceNode -> {
                    state.options.forEach { option ->
                        OutlinedButton(
                            onClick = { onChoose(option.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(option.text, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                is CompletionNode -> {
                    Button(onClick = onBackToMap, modifier = Modifier.fillMaxWidth()) {
                        Text(uiText("Torna alla mappa", "Back to map"))
                    }
                    TextButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                        Text(uiText("Rivivi questo capitolo", "Relive this chapter"))
                    }
                }
                is ReflectionNode -> {
                    Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
                        Text(uiText("Prosegui", "Proceed"))
                    }
                    TextButton(
                        onClick = { onWriteJournal(animatedNode.teachingId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(uiText("Scrivi nel diario", "Write in the journal"))
                    }
                }
                else -> {
                    Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth()) {
                        Text(uiText("Continua", "Continue"))
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeBody(
    node: StoryNode,
    state: ChapterUiState,
    catalog: ContentCatalog,
    onOpenTeaching: (String) -> Unit,
) {
    when (node) {
        is NarrationNode -> {
            Text(node.text, style = MaterialTheme.typography.bodyLarge)
        }
        is DialogueLineNode -> {
            PergamenaSection {
                state.speakerName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(node.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        is ChoiceNode -> {
            Text(
                node.prompt,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
        }
        is CharacterEncounterNode -> {
            PergamenaSection {
                Text(
                    uiText("Incontri: ${state.speakerName ?: ""}", "Encounter: ${state.speakerName ?: ""}"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(6.dp))
                Text(node.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        is HistoricalInsightNode -> {
            PergamenaSection {
                SourceBadge(
                    SourceCategory.TESTIMONIANZA_MEDIEVALE,
                    label = sourceCategoryText(SourceCategory.TESTIMONIANZA_MEDIEVALE),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    node.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(6.dp))
                Text(node.text, style = MaterialTheme.typography.bodyMedium)
                node.sourceDocumentIds.forEach { docId ->
                    catalog.documentsById[docId]?.let { doc ->
                        Text(
                            uiText("Fonte: ${doc.title}", "Source: ${doc.title}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        is TeachingNode -> {
            PergamenaSection {
                val teaching = catalog.teachingsById[node.teachingId]
                Text(
                    uiText("Un insegnamento", "A teaching"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(node.text, style = MaterialTheme.typography.bodyLarge)
                if (teaching != null) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { onOpenTeaching(node.teachingId) }) {
                        Text(uiText("Approfondisci: ${teaching.title}", "Explore: ${teaching.title}"))
                    }
                }
            }
        }
        is ReflectionNode -> {
            PergamenaSection {
                Text(
                    uiText("Per riflettere", "For reflection"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(6.dp))
                Text(node.prompt, style = MaterialTheme.typography.bodyLarge)
            }
        }
        is RewardUnlockNode -> {
            PergamenaSection {
                Text(node.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        is CompletionNode -> {
            Text(node.text, style = MaterialTheme.typography.bodyLarge)
        }
        else -> Unit
    }
}
