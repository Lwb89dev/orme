package it.orma.cammino.nostr

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import it.orma.cammino.core.database.entity.JournalEntryEntity
import it.orma.cammino.core.database.repository.JournalMarkdown
import it.orma.cammino.core.database.repository.JournalRepository
import it.orma.cammino.core.nostr.ShareOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Porta le voci del diario su Nostr solo quando la persona l'ha chiesto per quella voce, e solo con un
 * accesso attivo. Gira nello scope dell'app, così il salvataggio non aspetta la rete e l'uscita dalla
 * schermata non interrompe l'invio. La voce resta comunque salvata sul dispositivo.
 */
class JournalSharing(
    context: Context,
    private val journal: JournalRepository,
    private val account: NostrAccount,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    /**
     * Dopo un salvataggio: se la voce è da condividere e c'è un accesso, la pubblica; se la condivisione è
     * stata tolta da una voce già pubblicata, chiede la cancellazione. [kindLabel] e [context] sono nella
     * lingua dell'interfaccia; [failureMessage] è il testo da mostrare se la pubblicazione non riesce.
     */
    fun onSaved(entry: JournalEntryEntity, kindLabel: String, context: String?, failureMessage: String) {
        if (account.identity.value == null) return
        scope.launch {
            val outcome = when {
                entry.shareOnNostr -> {
                    val result = account.publish(
                        identifier = entry.id,
                        text = entry.text,
                        markdown = JournalMarkdown.entry(entry.text, kindLabel, context),
                        firstPublishedAtMillis = entry.nostrPublishedAt ?: entry.createdAt,
                    )
                    if (result == ShareOutcome.PUBLISHED) journal.markPublished(entry)
                    result
                }
                entry.nostrPublishedAt != null -> retract(entry.id)
                else -> return@launch
            }
            if (outcome != ShareOutcome.PUBLISHED) toast(failureMessage)
        }
    }

    /** Prima di eliminare una voce già pubblicata: chiede ai relay di cancellarla. */
    fun onDeleting(entry: JournalEntryEntity, failureMessage: String) {
        if (entry.nostrPublishedAt == null || account.identity.value == null) return
        scope.launch {
            if (retract(entry.id) != ShareOutcome.PUBLISHED) toast(failureMessage)
        }
    }

    private suspend fun retract(id: String): ShareOutcome {
        val result = account.unpublish(id)
        if (result == ShareOutcome.PUBLISHED) journal.clearPublished(id)
        return result
    }

    private fun toast(message: String) {
        main.post { Toast.makeText(appContext, message, Toast.LENGTH_LONG).show() }
    }
}
