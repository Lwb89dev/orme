package it.orma.cammino.nostr

import android.content.Context
import android.content.Intent
import android.net.Uri
import it.orma.cammino.core.nostr.ArticleSharer
import it.orma.cammino.core.nostr.BunkerClient
import it.orma.cammino.core.nostr.BunkerSigner
import it.orma.cammino.core.nostr.Nip23
import it.orma.cammino.core.nostr.NostrBunkerUri
import it.orma.cammino.core.nostr.NostrNip19
import it.orma.cammino.core.nostr.NostrSigner
import it.orma.cammino.core.nostr.OkHttpRelayConnector
import it.orma.cammino.core.nostr.PersistentRelayConnector
import it.orma.cammino.core.nostr.ShareOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Cosa non è andato nell'accesso; la schermata lo traduce. */
enum class LoginFailure { AMBER, INVALID_LINK, NO_ANSWER, REFUSED }

/** L'Activity presta ad [NostrAccount] ciò che richiede una finestra: aprire Amber o una pagina web. */
interface NostrHost {
    fun launchAmberLogin(): Boolean
    fun launchSigner(intent: Intent): Boolean
    fun openApprovalPage(url: String)
}

/**
 * L'account Nostr della persona: accesso con Amber (NIP-55) o con un bunker (NIP-46), firma, e
 * pubblicazione di un articolo. Non ha accesso alla chiave privata dell'account e non pubblica nulla da solo.
 */
class NostrAccount(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val store = NostrIdentityStore(appContext)
    private val connector = PersistentRelayConnector(OkHttpRelayConnector())

    @Volatile
    var host: NostrHost? = null

    val identity: StateFlow<NostrIdentity?> = store.identity

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _failure = MutableStateFlow<LoginFailure?>(null)
    val failure: StateFlow<LoginFailure?> = _failure.asStateFlow()

    val amberBridge = AmberBridge(
        resolver = { appContext.contentResolver },
        launch = { intent -> host?.launchSigner(intent) ?: false },
        signerPackage = { store.signerPackage() },
    )

    private var bunker: Pair<BunkerSession, NostrSigner>? = null

    /** Il signer dell'accesso attuale, o null se nessuno ha fatto l'accesso. */
    fun signer(): NostrSigner? = when (store.identity.value?.flavor) {
        NostrFlavor.AMBER -> AmberSigner(amberBridge) { store.identity.value?.pubkeyHex }
        NostrFlavor.BUNKER -> bunkerSigner()
        null -> null
    }

    private fun bunkerSigner(): NostrSigner? {
        val session = store.bunkerSession() ?: return null
        bunker?.takeIf { it.first.clientKeyHex == session.clientKeyHex }?.let { return it.second }
        val client = BunkerClient(
            connector = connector,
            clientKeyHex = session.clientKeyHex,
            remotePubkeyHex = session.remotePubkeyHex,
            relays = session.relays,
            onAuthUrl = { url -> host?.openApprovalPage(url) },
        )
        return BunkerSigner(client) { store.identity.value?.pubkeyHex }.also { bunker = session to it }
    }

    fun clearFailure() {
        _failure.value = null
    }

    // ------------------------------------------------------------------
    // Accesso
    // ------------------------------------------------------------------

    fun loginWithAmber() {
        _failure.value = null
        if (host?.launchAmberLogin() != true) _failure.value = LoginFailure.AMBER
    }

    /** Chiamato dall'Activity con la risposta di Amber alla richiesta della chiave pubblica. */
    fun onAmberLoginResult(resultOk: Boolean, data: Intent?) {
        val candidates = listOfNotNull(
            data?.getStringExtra("signature"),
            data?.getStringExtra("result"),
            data?.data?.schemeSpecificPart,
        ).map(String::trim).filter(String::isNotEmpty)
        val loggedIn = resultOk && candidates.any(::acceptAmberPublicKey)
        if (loggedIn) {
            store.saveSignerPackage(data?.getStringExtra("package"))
        } else {
            _failure.value = LoginFailure.AMBER
        }
    }

    private fun acceptAmberPublicKey(encodedOrHex: String): Boolean = runCatching {
        val publicHex = if (encodedOrHex.startsWith("npub1")) {
            NostrNip19.decodePublicKey(encodedOrHex)
        } else {
            require(encodedOrHex.matches(Regex("[0-9a-fA-F]{64}")))
            encodedOrHex.lowercase()
        }
        store.saveAmber(publicHex)
    }.getOrDefault(false)

    fun loginWithBunker(link: String) {
        _failure.value = null
        val uri = NostrBunkerUri.parse(link)
        if (uri == null) {
            _failure.value = LoginFailure.INVALID_LINK
            return
        }
        if (_busy.value) return
        _busy.value = true
        scope.launch {
            try {
                _failure.value = connectBunker(uri)
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun connectBunker(uri: NostrBunkerUri): LoginFailure? {
        val clientKey = store.newClientKey()
        val client = BunkerClient(
            connector = connector,
            clientKeyHex = clientKey,
            remotePubkeyHex = uri.remotePubkeyHex,
            relays = uri.relays,
            onAuthUrl = { url -> host?.openApprovalPage(url) },
        )
        val secret = uri.secret
        val params = if (secret == null) {
            listOf(uri.remotePubkeyHex)
        } else {
            listOf(uri.remotePubkeyHex, secret, REQUESTED_PERMISSIONS)
        }
        val connect = client.call("connect", params, BunkerClient.CONNECT_TIMEOUT_MILLIS)
        if (!connect.ok) return if (connect.error != null) LoginFailure.REFUSED else LoginFailure.NO_ANSWER
        if (connect.result != "ack" && connect.result != secret) return LoginFailure.REFUSED
        val account = client.call("get_public_key", emptyList())
        val publicHex = account.result?.trim()?.lowercase()?.takeIf { account.ok && HEX_64.matches(it) }
            ?: return if (account.error != null) LoginFailure.REFUSED else LoginFailure.NO_ANSWER
        return if (store.saveBunker(publicHex, clientKey, uri.remotePubkeyHex, uri.relays)) null else LoginFailure.NO_ANSWER
    }

    fun logout() {
        bunker = null
        store.logout()
    }

    // ------------------------------------------------------------------
    // Pubblicazione
    // ------------------------------------------------------------------

    private fun sharer() = ArticleSharer(signer = ::signer, connector = connector)

    /** Pubblica (o aggiorna) l'articolo [identifier]. Chiamata solo dopo la scelta esplicita della persona. */
    suspend fun publish(identifier: String, text: String, markdown: String, firstPublishedAtMillis: Long): ShareOutcome =
        sharer().publish(
            identifier = identifier,
            title = Nip23.titleOf(text, fallback = "Orme"),
            markdown = markdown,
            firstPublishedAt = firstPublishedAtMillis / 1000,
        )

    /** Chiede ai relay di cancellare l'articolo [identifier] (NIP-09: non è garantito). */
    suspend fun unpublish(identifier: String): ShareOutcome = sharer().delete(identifier)

    private companion object {
        const val REQUESTED_PERMISSIONS = "get_public_key,sign_event"
        val HEX_64 = Regex("[0-9a-f]{64}")
    }
}

internal fun amberLoginIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:login"))
    .putExtra("type", "get_public_key")
    .putExtra("uri_data", "login")
    .putExtra("permissions", "[{\"type\":\"sign_event\",\"kind\":${Nip23.KIND_ARTICLE}},{\"type\":\"sign_event\",\"kind\":${Nip23.KIND_DELETION}}]")
