package it.orma.cammino.ui.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.ContentIssue
import it.orma.cammino.core.content.Severity
import it.orma.cammino.core.database.settings.AppSettings
import it.orma.cammino.core.database.settings.ThemeMode
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaSection
import it.orma.cammino.core.model.ReviewStatus
import it.orma.cammino.di.AppContainer
import kotlinx.coroutines.launch

class SettingsViewModel(container: AppContainer) : ViewModel() {
    val settings = container.settingsRepository.settings
    val repository = container.settingsRepository

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(container) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    catalog: ContentCatalog,
    contentIssues: List<ContentIssue>,
    navController: NavHostController,
) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val settings by vm.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Aspetto
            Text(
                "Aspetto",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.SYSTEM to "Sistema",
                    ThemeMode.LIGHT to "Chiaro",
                    ThemeMode.DARK to "Scuro",
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { scope.launch { vm.repository.setThemeMode(mode) } },
                        label = { Text(label) },
                    )
                }
            }

            // Accessibilità
            Text(
                "Accessibilità",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Riduci le animazioni", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Transizioni e movimenti del giardino ridotti al minimo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.reduceAnimations,
                    onCheckedChange = { scope.launch { vm.repository.setReduceAnimations(it) } },
                )
            }

            // Audio
            Text(
                "Audio",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Paesaggi sonori", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Suoni d'ambiente discreti nel giardino. Mai avviati a sorpresa. (In preparazione: nessun suono è ancora incluso in questa versione.)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.audioEnabled,
                    onCheckedChange = { scope.launch { vm.repository.setAudioEnabled(it) } },
                )
            }

            OrnamentalDivider()

            // Privacy
            Text(
                "Privacy",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            PergamenaSection {
                Text(
                    "Nessun account, nessuna connessione richiesta, nessuna pubblicità né tracciamento. Il diario e i tuoi progressi restano sul dispositivo. L'esportazione del diario è un file che scegli tu dove condividere. Attenzione: la copia di sicurezza automatica di Android può includere i dati dell'app secondo le impostazioni del tuo dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            OrnamentalDivider()

            // Stato editoriale dei contenuti
            Text(
                "Qualità dei contenuti",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            PergamenaSection {
                val verified = catalog.sourcePassages.count { it.reviewStatus == ReviewStatus.VERIFIED }
                val toVerify = catalog.sourcePassages.count { it.reviewStatus == ReviewStatus.NEEDS_VERIFICATION }
                Text("Versione contenuti: ${catalog.manifest.contentVersion}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Passi documentali: ${catalog.sourcePassages.size} · verificati: $verified · in verifica: $toVerify",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val errors = contentIssues.count { it.severity == Severity.ERROR }
                Text(
                    if (errors == 0) "Integrità dei contenuti verificata: nessun riferimento mancante."
                    else "Attenzione: $errors riferimenti da correggere nel pacchetto contenuti.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Le citazioni letterali riproducono originali medievali in pubblico dominio con traduzione editoriale originale; le parafrasi sono sempre etichettate. I testi moderni protetti non sono riprodotti.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
