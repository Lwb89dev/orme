package it.orma.cammino

import android.app.Application
import it.orma.cammino.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import it.orma.cammino.content.ContentLoadResult

class CamminoApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _contentState = MutableStateFlow<ContentLoadResult?>(null)
    val contentState: StateFlow<ContentLoadResult?> = _contentState

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.settingsRepository.settings
                .map { it.language }
                .distinctUntilChanged()
                .collect { language ->
                    val result = container.contentRepository.load(language)
                    result.catalog?.let { catalog ->
                        container.attachCatalog(catalog)
                        // Versionamento dei contenuti: la progressione è ancorata agli id stabili,
                        // quindi un aggiornamento del pacchetto non distrugge i salvataggi.
                        val seen = container.settingsRepository.settings.first().lastSeenContentVersion
                        if (catalog.manifest.contentVersion != seen) {
                            container.settingsRepository.setLastSeenContentVersion(catalog.manifest.contentVersion)
                        }
                    }
                    _contentState.value = result
                }
        }
    }
}
