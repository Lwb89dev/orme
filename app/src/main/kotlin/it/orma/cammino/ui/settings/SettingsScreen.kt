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
import it.orma.cammino.ui.i18n.uiText
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
                title = { Text(uiText("Impostazioni", "Settings")) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                uiText("Lingua", "Language"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("it" to "Italiano", "en" to "English").forEach { (language, label) ->
                    FilterChip(
                        selected = settings.language == language,
                        onClick = { scope.launch { vm.repository.setLanguage(language) } },
                        label = { Text(label) },
                    )
                }
            }

            // Aspetto
            Text(
                uiText("Aspetto", "Appearance"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.SYSTEM to uiText("Sistema", "System"),
                    ThemeMode.LIGHT to uiText("Chiaro", "Light"),
                    ThemeMode.DARK to uiText("Scuro", "Dark"),
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
                uiText("Accessibilità", "Accessibility"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiText("Riduci le animazioni", "Reduce animations"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        uiText("Transizioni e movimenti del giardino ridotti al minimo.", "Garden transitions and movement are reduced to a minimum."),
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
                uiText("Audio", "Audio"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiText("Paesaggi sonori", "Soundscapes"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        uiText("Suoni d'ambiente discreti nel giardino. Mai avviati a sorpresa. (In preparazione: nessun suono è ancora incluso in questa versione.)", "Quiet ambient sounds in the garden. Never started by surprise. (In preparation: no sound is included in this version yet.)"),
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
                uiText("Privacy", "Privacy"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            PergamenaSection {
                Text(
                    uiText("Nessun account, nessuna connessione richiesta, nessuna pubblicità né tracciamento. Il diario e i tuoi progressi restano sul dispositivo. L'esportazione del diario è un file che scegli tu dove condividere. Attenzione: la copia di sicurezza automatica di Android può includere i dati dell'app secondo le impostazioni del tuo dispositivo.", "No account, connection, advertising, or tracking is required. Your journal and progress remain on this device. You choose where to share an exported journal file. Android automatic backup may include app data according to your device settings."),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            OrnamentalDivider()

            // Stato editoriale dei contenuti
            Text(
                uiText("Qualità dei contenuti", "Content quality"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            PergamenaSection {
                val verified = catalog.sourcePassages.count { it.reviewStatus == ReviewStatus.VERIFIED }
                val toVerify = catalog.sourcePassages.count { it.reviewStatus == ReviewStatus.NEEDS_VERIFICATION }
                Text(uiText("Versione contenuti: ${catalog.manifest.contentVersion}", "Content version: ${catalog.manifest.contentVersion}"), style = MaterialTheme.typography.bodyMedium)
                Text(
                    uiText("Passi documentali: ${catalog.sourcePassages.size} · verificati: $verified · in verifica: $toVerify", "Source passages: ${catalog.sourcePassages.size} · verified: $verified · under review: $toVerify"),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val errors = contentIssues.count { it.severity == Severity.ERROR }
                Text(
                    if (errors == 0) uiText("Integrità dei contenuti verificata: nessun riferimento mancante.", "Content integrity verified: no missing references.")
                    else uiText("Attenzione: $errors riferimenti da correggere nel pacchetto contenuti.", "Attention: $errors references need correction in the content package."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    uiText("Le citazioni letterali riproducono originali medievali in pubblico dominio con traduzione editoriale originale; le parafrasi sono sempre etichettate. I testi moderni protetti non sono riprodotti.", "Literal quotations reproduce medieval public-domain originals with an original editorial translation; paraphrases are always labelled. Protected modern texts are not reproduced."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
