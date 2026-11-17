package it.orma.cammino.core.nostr

import it.orma.cammino.core.nostr.BoundedJsonParser
import it.orma.cammino.core.nostr.NostrEventDraft

/**
 * Signs and encrypts through a remote signer reached over NIP-46 ("bunker"). The private key of the account
 * never reaches this phone. Each call can wait for the person to approve in the signer app, so this signer
 * is not silent: chores that are only nice to have are skipped for it, as for Amber.
 */
class BunkerSigner(
    private val client: BunkerClient,
    private val accountPubkeyHex: () -> String?,
) : NostrSigner {
    override val pubkeyHex: String? get() = accountPubkeyHex()

    override val signsSilently: Boolean = false

    override suspend fun sign(draft: NostrEventDraft): Map<String, Any?>? {
        val account = pubkeyHex ?: return null
        if (draft.pubkey != account) return null
        val unsigned = it.orma.cammino.core.nostr.Nip46.eventJson(draft)
        val reply = client.call("sign_event", listOf(unsigned))
        val signed = reply.result?.takeIf { reply.ok }?.let {
            runCatching { BoundedJsonParser(it).parse() as? Map<*, *> }.getOrNull()
        } ?: return null
        @Suppress("UNCHECKED_CAST")
        val event = signed as Map<String, Any?>
        // The signer is trusted to sign, not to substitute: the event must be ours, from this account, and valid.
        if (event["pubkey"] != account) return null
        if (event["kind"]?.let(NostrWire::integral) != draft.kind.toLong()) return null
        // The time too: for a replaceable event it decides which version wins, so a signer that moved
        // it forward could pin its own copy of the favourites or the profile above every later one.
        if (event["created_at"]?.let(NostrWire::integral) != draft.createdAt) return null
        if (event["content"] != draft.content) return null
        if (NostrWire.tags(event["tags"]) != draft.tags) return null
        if (!NostrWire.verify(event)) return null
        return event
    }

    override suspend fun nip44Encrypt(peerPubkeyHex: String, plaintext: String): String? {
        val reply = client.call("nip44_encrypt", listOf(peerPubkeyHex, plaintext))
        return reply.result?.takeIf { reply.ok && it.isNotEmpty() }
    }

    override suspend fun nip44Decrypt(peerPubkeyHex: String, payload: String): String? {
        val reply = client.call("nip44_decrypt", listOf(peerPubkeyHex, payload))
        return reply.result?.takeIf { reply.ok }
    }
}
