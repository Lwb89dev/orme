package it.orma.cammino.nostr

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import it.orma.cammino.core.nostr.BoundedJsonParser
import it.orma.cammino.core.nostr.NostrEventDraft
import it.orma.cammino.core.nostr.NostrJson
import it.orma.cammino.core.nostr.NostrSigner
import it.orma.cammino.core.nostr.NostrWire
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Ciò che l'app signer ha risposto; ogni campo può mancare. */
class AmberReply(val signature: String?, val result: String?, val event: String?)

/**
 * Una richiesta NIP-55 alla volta verso l'app signer Android (Amber).
 *
 * Il content provider del signer risponde in silenzio dopo «ricorda la scelta»; altrimenti si apre la sua
 * interfaccia con un Intent. Il risultato dell'Intent arriva dal callback dell'Activity, che può essere
 * ricreata (rotazione) mentre il signer è in primo piano: la richiesta pendente vive nel companion.
 */
class AmberBridge(
    private val resolver: () -> ContentResolver,
    private val launch: (Intent) -> Boolean,
    private val signerPackage: () -> String?,
) {
    /** La risposta del signer, o null se rifiutata, chiusa, assente o troppo lenta. */
    suspend fun request(
        type: String,
        payload: String,
        currentUser: String,
        id: String,
        peerPubkey: String? = null,
    ): AmberReply? = gate.withLock {
        silentReply(type, payload, currentUser, peerPubkey)?.let { return@withLock it }
        val reply = CompletableDeferred<AmberReply?>()
        pending = reply
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:$payload"))
            .putExtra("type", type)
            .putExtra("id", id)
            .putExtra("current_user", currentUser)
            .putExtra("pubKey", peerPubkey.orEmpty())
        signerPackage()?.let { intent.setPackage(it) }
        try {
            val launched = runCatching { withContext(Dispatchers.Main) { launch(intent) } }.getOrDefault(false)
            if (!launched) return@withLock null
            withTimeoutOrNull(REPLY_TIMEOUT_MILLIS) { reply.await() }
        } finally {
            if (pending === reply) pending = null
        }
    }

    /** Chiamato dal callback del risultato dell'Activity. */
    fun deliver(resultOk: Boolean, data: Intent?) {
        val reply = if (resultOk && data != null) {
            AmberReply(
                signature = data.getStringExtra("signature"),
                result = data.getStringExtra("result"),
                event = data.getStringExtra("event"),
            )
        } else {
            null
        }
        pending?.complete(reply)
    }

    private suspend fun silentReply(
        type: String,
        payload: String,
        currentUser: String,
        peerPubkey: String?,
    ): AmberReply? = withContext(Dispatchers.IO) {
        val authority = "${signerPackage() ?: DEFAULT_SIGNER_PACKAGE}.${type.uppercase()}"
        runCatching {
            resolver().query(
                Uri.parse("content://$authority"),
                arrayOf(payload, peerPubkey.orEmpty(), currentUser),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                fun column(name: String): String? =
                    cursor.getColumnIndex(name).takeIf { it >= 0 }?.let(cursor::getString)
                // Una colonna "rejected" significa che la persona ha negato per sempre.
                if (cursor.getColumnIndex("rejected") >= 0) return@use null
                AmberReply(column("signature"), column("result"), column("event"))
            }
        }.getOrNull()
    }

    private companion object {
        const val REPLY_TIMEOUT_MILLIS = 120_000L
        const val DEFAULT_SIGNER_PACKAGE = "com.greenart7c3.nostrsigner"
        val gate = Mutex()

        @Volatile
        var pending: CompletableDeferred<AmberReply?>? = null
    }
}

/** Firma tramite Amber: la chiave privata non arriva mai a questa app. */
class AmberSigner(
    private val bridge: AmberBridge,
    private val currentUser: () -> String?,
) : NostrSigner {
    override val pubkeyHex: String? get() = currentUser()

    /** Ogni chiamata può mostrare una conferma. */
    override val signsSilently: Boolean = false

    override suspend fun sign(draft: NostrEventDraft): Map<String, Any?>? {
        val user = currentUser()
        if (user == null || draft.pubkey != user) return null
        val reply = bridge.request(
            type = "sign_event",
            payload = NostrJson.encode(draft.toWireMap()),
            currentUser = user,
            id = draft.id(),
        ) ?: return null
        val signature = signatureOf(reply) ?: return null
        // Ricostruito dalla nostra bozza e verificato: qualunque cosa risponda il signer non può
        // mettere in rete contenuto diverso da quello che abbiamo chiesto di firmare.
        val signed = draft.toWireMap(signature)
        return signed.takeIf { NostrWire.verify(it) }
    }

    override suspend fun nip44Encrypt(peerPubkeyHex: String, plaintext: String): String? =
        transform("nip44_encrypt", peerPubkeyHex, plaintext)

    override suspend fun nip44Decrypt(peerPubkeyHex: String, payload: String): String? =
        transform("nip44_decrypt", peerPubkeyHex, payload)

    private suspend fun transform(type: String, peer: String, input: String): String? {
        val user = currentUser() ?: return null
        if (!NostrWire.isHex32(peer)) return null
        val reply = bridge.request(type, input, user, NostrWire.randomSubscriptionId(), peer) ?: return null
        return listOfNotNull(reply.result, reply.signature)
            .map(String::trim)
            .firstOrNull { it.isNotEmpty() && it.length <= MAX_REPLY_CHARS }
    }

    private fun signatureOf(reply: AmberReply): String? {
        reply.signature?.trim()?.takeIf(SIGNATURE::matches)?.let { return it }
        val eventJson = reply.event?.takeIf { it.length <= MAX_REPLY_CHARS } ?: return null
        val event = runCatching { BoundedJsonParser(eventJson).parse() as? Map<*, *> }.getOrNull()
        return (event?.get("sig") as? String)?.trim()?.takeIf(SIGNATURE::matches)
    }

    private companion object {
        const val MAX_REPLY_CHARS = 262_144
        val SIGNATURE = Regex("[0-9a-f]{128}")
    }
}
