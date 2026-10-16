package it.orma.cammino.ui.journal

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import it.orma.cammino.core.database.repository.JournalKind
import it.orma.cammino.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    container: AppContainer,
    entryId: String?,
    teachingId: String?,
    chapterId: String?,
    navController: NavHostController,
) {
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(JournalKind.LIBERA.name) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(entryId) {
        if (entryId != null && !loaded) {
            val entry = container.journalRepository.entries.first()
                .firstOrNull { it.id == entryId }
            if (entry != null) {
                text = entry.text
                kind = entry.kind
                loaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) "Nuova voce" else "Modifica voce") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (entryId != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Elimina la voce")
                        }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JournalKind.entries.forEach { k ->
                    FilterChip(
                        selected = kind == k.name,
                        onClick = { kind = k.name },
                        label = { Text(k.name.lowercase(), style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it; saved = false },
                label = { Text("La tua riflessione") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 10,
            )

            Button(
                onClick = {
                    scope.launch {
                        container.journalRepository.save(
                            id = entryId,
                            kind = JournalKind.valueOf(kind),
                            text = text,
                            teachingId = teachingId,
                            chapterId = chapterId,
                        )
                        saved = true
                        navController.popBackStack()
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salva")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete && entryId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare questa voce?") },
            text = { Text("L'eliminazione è definitiva: il diario non ha copie sul cloud.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        container.journalRepository.delete(entryId)
                        navController.popBackStack()
                    }
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annulla") }
            },
        )
    }
}
