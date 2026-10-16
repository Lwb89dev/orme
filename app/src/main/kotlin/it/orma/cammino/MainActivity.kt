package it.orma.cammino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import it.orma.cammino.ui.i18n.uiText
import it.orma.cammino.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CamminoApplication
        setContent {
            val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val content by app.contentState.collectAsStateWithLifecycle()

            val darkTheme = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            CamminoTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalReduceAnimations provides (settings?.reduceAnimations ?: false),
                    LocalAppLanguage provides (settings?.language ?: "en"),
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val currentContent = content
                        val currentSettings = settings
                        when {
                            currentContent == null || currentSettings == null -> LoadingScreen()
                            currentContent.catalog == null -> ContentErrorScreen(currentContent.fatalError)
                            !currentSettings.onboardingCompleted -> OnboardingScreen(
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
            text = uiText("Non riesco a leggere i contenuti dell'app.\n${fatalError ?: ""}", "The app's content could not be read.\n${fatalError ?: ""}"),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
