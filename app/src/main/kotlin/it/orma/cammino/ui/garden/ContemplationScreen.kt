package it.orma.cammino.ui.garden

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.schema.ContemplationStepKind
import it.orma.cammino.core.database.repository.JournalKind
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaSection
import it.orma.cammino.core.model.ContemplationCompleted
import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.TeachingExplored
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.i18n.uiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContemplationScreen(
    container: AppContainer,
    catalog: ContentCatalog,
    contemplationId: String,
    navController: NavHostController,
) {
    val contemplation = catalog.contemplationsById[contemplationId]
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var noteSaved by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contemplation?.title ?: uiText("Contemplazione", "Contemplation")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = uiText("Chiudi la contemplazione", "Close contemplation"))
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
            if (contemplation == null) {
                Text(uiText("Contemplazione non trovata", "Contemplation not found"), color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Text(
                uiText("Passo ${stepIndex + 1} di ${contemplation.steps.size}", "Step ${stepIndex + 1} of ${contemplation.steps.size}"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val step = contemplation.steps[stepIndex]
            Text(
                when (step.kind) {
                    ContemplationStepKind.OSSERVA -> uiText("Osserva", "Observe")
                    ContemplationStepKind.ASCOLTA -> uiText("Ascolta", "Listen")
                    ContemplationStepKind.LEGGI -> uiText("Leggi", "Read")
                    ContemplationStepKind.RIFLETTI -> uiText("Rifletti", "Reflect")
                    ContemplationStepKind.ANNOTA -> uiText("Annota", "Write")
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )

            Text(step.text, style = MaterialTheme.typography.bodyLarge)

            step.sourcePassageId?.let { passageId ->
                catalog.passagesById[passageId]?.let { passage ->
                    PergamenaSection {
                        Text(
                            passage.reference,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        if (passage.originalText.isNotBlank()) {
                            Text(passage.originalText, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            passage.translation.ifBlank { passage.paraphrase },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            if (step.kind == ContemplationStepKind.ANNOTA) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it; noteSaved = false },
                    label = { Text(uiText("La tua nota (resta nel diario, solo sul dispositivo)", "Your note (kept in the journal, on this device only)")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                if (noteText.isNotBlank() && !noteSaved) {
                    TextButton(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            container.journalRepository.save(
                                kind = JournalKind.RIFLESSIONE,
                                text = noteText,
                                teachingId = contemplation.teachingId,
                            )
                        }
                        noteSaved = true
                    }) { Text(uiText("Salva nel diario", "Save to journal")) }
                }
                if (noteSaved) {
                    Text(uiText("Annotato nel diario.", "Added to the journal."), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }

            OrnamentalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (stepIndex > 0) {
                    TextButton(onClick = { stepIndex-- }) { Text(uiText("Indietro", "Back")) }
                }
                Spacer(Modifier.weight(1f))
                if (stepIndex < contemplation.steps.size - 1) {
                    Button(onClick = { stepIndex++ }) { Text(uiText("Avanti", "Next")) }
                } else if (!completed) {
                    Button(onClick = {
                        completed = true
                        val events = mutableListOf<it.orma.cammino.core.model.ExperienceEvent>(
                            ContemplationCompleted(contemplation.id)
                        )
                        contemplation.teachingId?.let { events += TeachingExplored(it, Modality.GIARDINO) }
                        CoroutineScope(Dispatchers.IO).launch { container.dispatch(events) }
                    }) { Text(uiText("Concludi", "Conclude")) }
                } else {
                    Button(onClick = { navController.popBackStack() }) { Text(uiText("Torna al giardino", "Back to the garden")) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
