package it.orma.cammino

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import it.orma.cammino.core.database.settings.ThemeMode
import it.orma.cammino.core.designsystem.CamminoTheme
import it.orma.cammino.ui.navigation.LocalReduceAnimations
import it.orma.cammino.ui.navigation.CamminoNavHost
import it.orma.cammino.ui.i18n.LocalAppLanguage
import it.orma.cammino.ui.i18n.LocalUiExtras
import it.orma.cammino.ui.i18n.loadUiExtras
import it.orma.cammino.ui.i18n.uiText
import it.orma.cammino.ui.i18n.uiFormat
import it.orma.cammino.nostr.NostrHost
import it.orma.cammino.nostr.amberLoginIntent
import it.orma.cammino.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), NostrHost {

    // L'accesso e la firma passano da Amber, che si apre sopra l'app e risponde a questi due callback.
    private val amberLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        (application as CamminoApplication).container.nostrAccount.onAmberLoginResult(result.resultCode == RESULT_OK, result.data)
    }
    private val amberSigner = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        (application as CamminoApplication).container.nostrAccount.amberBridge.deliver(result.resultCode == RESULT_OK, result.data)
    }

    override fun launchAmberLogin(): Boolean = runCatching { amberLogin.launch(amberLoginIntent()) }.isSuccess

    override fun launchSigner(intent: Intent): Boolean = runCatching { amberSigner.launch(intent) }.isSuccess

    /** Un signer remoto chiede l'approvazione su una pagina web: si apre solo un indirizzo sicuro. */
    override fun openApprovalPage(url: String) {
        if (!url.startsWith("https://")) return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CamminoApplication
        app.container.nostrAccount.host = this
        setContent {
            val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val content by app.contentState.collectAsStateWithLifecycle()

            val darkTheme = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val language = settings?.language ?: "en"
            val uiExtras = remember(language) { loadUiExtras(app, language) }
            CamminoTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalReduceAnimations provides (settings?.reduceAnimations ?: false),
                    LocalAppLanguage provides language,
                    LocalUiExtras provides uiExtras,
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val currentContent = content
                        val currentSettings = settings
                        when {
                            currentContent == null || currentSettings == null -> LoadingScreen()
                            currentContent.catalog == null -> ContentErrorScreen(currentContent.fatalError)
                            !currentSettings.onboardingCompleted -> OnboardingScreen(
                                account = app.container.nostrAccount,
                                onFinish = {
                                    lifecycleScope.launch {
                                        app.container.settingsRepository.setOnboardingCompleted(true)
                                    }
                                }
                            )
                            else -> CamminoNavHost(
                                container = app.container,
                                catalog = currentContent.catalog,
                                contentIssues = currentContent.issues,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        val account = (application as CamminoApplication).container.nostrAccount
        if (account.host === this) account.host = null
        super.onDestroy()
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ContentErrorScreen(fatalError: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = uiFormat("Non riesco a leggere i contenuti dell'app.\n{0}", "The app's content could not be read.\n{0}", fatalError ?: ""),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
