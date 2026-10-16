package it.orma.cammino.content

import android.content.Context
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.ContentIssue
import it.orma.cammino.core.content.ContentLoader
import it.orma.cammino.core.content.ContentValidator
import it.orma.cammino.core.content.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Esito del caricamento del pacchetto contenuti dagli asset. */
data class ContentLoadResult(
    val catalog: ContentCatalog?,
    val issues: List<ContentIssue> = emptyList(),
    val fatalError: String? = null,
) {
    val errors: List<ContentIssue> get() = issues.filter { it.severity == Severity.ERROR }
}

/** Carica e valida i contenuti JSON dagli asset dell'app (offline). */
class ContentRepository(private val context: Context) {

    suspend fun load(): ContentLoadResult = withContext(Dispatchers.IO) {
        val source = it.orma.cammino.core.content.ContentSource { path ->
            try {
                context.assets.open(path).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }
        }
        try {
            val catalog = ContentLoader(source).load()
            val issues = ContentValidator().validate(catalog)
            ContentLoadResult(catalog = catalog, issues = issues)
        } catch (e: Exception) {
            ContentLoadResult(catalog = null, fatalError = e.message ?: "Errore di caricamento")
        }
    }
}
