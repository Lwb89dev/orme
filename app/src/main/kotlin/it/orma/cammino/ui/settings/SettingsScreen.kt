package it.orma.cammino.ui.settings

import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.foundation.clickable
import android.widget.Toast
import android.net.Uri
import android.content.Intent
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import it.orma.cammino.ui.nostr.NostrLoginPanel
import it.orma.cammino.core.model.ReviewStatus
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.i18n.APP_LANGUAGES
import it.orma.cammino.ui.i18n.uiText
import it.orma.cammino.ui.i18n.uiFormat
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                APP_LANGUAGES.forEach { (language, label) ->
                    FilterChip(
                        selected = settings.language == language,
                        onClick = { scope.launch { vm.repository.setLanguage(language) } },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                uiText(
                    "Il racconto, gli scritti e l'interfaccia seguono la lingua scelta. L'originale medievale resta in latino o in volgare.",
                    "The story, the writings and the interface follow the chosen language. The medieval original stays in Latin or in the vernacular.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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

            // Account Nostr (facoltativo)
            Text(
                uiText("Account Nostr", "Nostr account"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            PergamenaSection {
                Text(
                    uiText(
                        "Orme non vede né conserva la tua chiave privata: la firma resta in Amber o nel tuo bunker. L'accesso è facoltativo e, senza, l'app non usa la rete.",
                        "Orme never sees or stores your private key: signing stays in Amber or in your bunker. Signing in is optional, and without it the app does not use the network.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                NostrLoginPanel(account = container.nostrAccount, allowLogout = true)
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
                    uiText("Nessun account richiesto, nessuna pubblicità né tracciamento. Diario e progressi restano sul dispositivo. L'app usa la rete solo se accedi con Nostr e condividi una voce. L'esportazione del diario è un file che scegli tu dove condividere. Il backup automatico di Android può includere i dati dell'app, secondo le impostazioni del dispositivo.", "No account is required, and there is no advertising or tracking. Journal and progress stay on the device. The app uses the network only if you sign in with Nostr and share an entry. A journal export is a file you choose where to share. Android's automatic backup may include app data, depending on your device settings."),
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
                Text(uiFormat("Versione contenuti: {0}", "Content version: {0}", catalog.manifest.contentVersion), style = MaterialTheme.typography.bodyMedium)
                Text(
                    uiFormat("Passi documentali: {0} · verificati: {1} · in verifica: {2}", "Source passages: {0} · verified: {1} · under review: {2}", catalog.sourcePassages.size, verified, toVerify),
                    style = MaterialTheme.typography.bodyMedium,
                )
                val errors = contentIssues.count { it.severity == Severity.ERROR }
                Text(
                    if (errors == 0) uiText("Integrità dei contenuti verificata: nessun riferimento mancante.", "Content integrity verified: no missing references.")
                    else uiFormat("Attenzione: {0} riferimenti da correggere nel pacchetto contenuti.", "Attention: {0} references need correction in the content package.", errors),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    uiText("Le citazioni letterali riproducono originali medievali in pubblico dominio con traduzione editoriale originale; le parafrasi sono sempre etichettate. I testi moderni protetti non sono riprodotti.", "Literal quotations reproduce medieval public-domain originals with an original editorial translation; paraphrases are always labelled. Protected modern texts are not reproduced."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OrnamentalDivider()

            // Donazioni: stesso indirizzo Lightning di Roadstr
            Text(
                uiText("Sostieni Orme", "Support Orme"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            DonationCard()
            Spacer(Modifier.height(24.dp))
        }
    }
}

private const val LIGHTNING_ADDRESS = "lwb89@blink.sv"

/** Apre il wallet Lightning sull'indirizzo; senza un wallet installato copia l'indirizzo negli appunti. */
@Composable
private fun DonationCard() {
    val context = LocalContext.current
    val copied = uiText("Nessun wallet Lightning trovato: indirizzo copiato.", "No Lightning wallet found: address copied.")
    PergamenaSection {
        Text(
            uiText(
                "Orme è gratuita, senza pubblicità e senza tracciamento. Se vuoi sostenerla, puoi donare in Lightning a questo indirizzo.",
                "Orme is free, with no advertising or tracking. If you want to support it, you can donate over Lightning to this address.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = uiText("Dona in Lightning", "Donate over Lightning")) {
                    val opened = runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("lightning:$LIGHTNING_ADDRESS")))
                    }.isSuccess
                    if (!opened) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Lightning", LIGHTNING_ADDRESS))
                        Toast.makeText(context, copied, Toast.LENGTH_LONG).show()
                    }
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(uiText("Dona in Lightning", "Donate over Lightning"), style = MaterialTheme.typography.titleMedium)
                Text(LIGHTNING_ADDRESS, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
