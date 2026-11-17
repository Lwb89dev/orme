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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.orma.cammino.core.content.ContentCatalog
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import it.orma.cammino.core.database.repository.JournalKind
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.i18n.journalKindText
import it.orma.cammino.ui.i18n.uiText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    container: AppContainer,
    catalog: ContentCatalog,
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
    var share by rememberSaveable { mutableStateOf(false) }
    var published by rememberSaveable { mutableStateOf(false) }
    val identity by container.nostrAccount.identity.collectAsStateWithLifecycle()
    val kindLabel = journalKindText(JournalKind.valueOf(kind))
    val contextLabel = teachingId?.let { catalog.teachingsById[it]?.title }
    val publishFailed = uiText(
        "Pubblicazione su Nostr non riuscita. La voce resta salvata sul dispositivo.",
        "Publishing on Nostr failed. The entry stays saved on the device.",
    )
    val retractFailed = uiText(
        "Richiesta di cancellazione su Nostr non riuscita.",
        "The deletion request on Nostr failed.",
    )

    androidx.compose.runtime.LaunchedEffect(entryId) {
        if (entryId != null && !loaded) {
            val entry = container.journalRepository.entries.first()
                .firstOrNull { it.id == entryId }
            if (entry != null) {
                text = entry.text
                kind = entry.kind
                share = entry.shareOnNostr
                published = entry.nostrPublishedAt != null
                loaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) uiText("Nuova voce", "New entry") else uiText("Modifica voce", "Edit entry")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = uiText("Indietro", "Back"))
                    }
                },
                actions = {
                    if (entryId != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = uiText("Elimina la voce", "Delete entry"))
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
                        label = { Text(journalKindText(k), style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it; saved = false },
                label = { Text(uiText("La tua riflessione", "Your reflection")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 10,
            )

            // La condivisione è una scelta per voce: spenta di default, e senza accesso non si può accendere.
            // Tutta la riga è l'interruttore: un lettore di schermo annuncia il titolo e lo stato insieme.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = share && identity != null,
                        enabled = identity != null,
                        role = Role.Switch,
                        onValueChange = { share = it },
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiText("Condividi su Nostr", "Share on Nostr"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (identity == null) {
                            uiText("Per condividere, accedi con Nostr dalle Impostazioni.", "To share, sign in with Nostr from Settings.")
                        } else {
                            uiText(
                                "Pubblica la voce come articolo Markdown e la aggiorna a ogni salvataggio. Se la disattivi, i relay vengono invitati a cancellarla, senza garanzia.",
                                "Publishes the entry as a Markdown article and updates it on every save. If you turn it off, relays are asked to delete it, with no guarantee.",
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (published) {
                        Text(
                            uiText("Pubblicata su Nostr", "Published on Nostr"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Switch(checked = share && identity != null, onCheckedChange = null, enabled = identity != null)
            }

            Button(
                onClick = {
                    scope.launch {
                        val entry = container.journalRepository.save(
                            id = entryId,
                            kind = JournalKind.valueOf(kind),
                            text = text,
                            teachingId = teachingId,
                            chapterId = chapterId,
                            shareOnNostr = share && identity != null,
                        )
                        container.journalSharing.onSaved(
                            entry,
                            kindLabel,
                            contextLabel,
                            if (entry.shareOnNostr) publishFailed else retractFailed,
                        )
                        saved = true
                        navController.popBackStack()
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(uiText("Salva", "Save"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete && entryId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(uiText("Eliminare questa voce?", "Delete this entry?")) },
            text = {
                Text(
                    if (published && identity != null) {
                        uiText(
                            "La voce è stata pubblicata su Nostr: i relay verranno invitati a cancellarla, senza garanzia.",
                            "This entry was published on Nostr: relays will be asked to delete it, with no guarantee.",
                        )
                    } else {
                        uiText("L'eliminazione è definitiva: il diario non ha copie sul cloud.", "Deletion is permanent: the journal has no cloud copies.")
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        container.journalRepository.get(entryId)?.let { container.journalSharing.onDeleting(it, retractFailed) }
                        container.journalRepository.delete(entryId)
                        navController.popBackStack()
                    }
                }) { Text(uiText("Elimina", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(uiText("Annulla", "Cancel")) }
            },
        )
    }
}
