package it.orma.cammino.core.nostr

/** What came of a share or unshare request. */
enum class ShareOutcome {
    /** At least one relay accepted the event. */
    PUBLISHED,

    /** Nobody is signed in. */
    NOT_SIGNED_IN,

    /** The signer declined, closed, or did not answer. */
    NOT_SIGNED,

    /** The event was signed but no relay accepted it. */
    NOT_ACCEPTED,
}

/**
 * Signs an article with the logged-in account's signer (Amber or a bunker, never a key held here) and sends it
 * to the relays. Nothing is published unless a caller asks for it.
 */
class ArticleSharer(
    private val signer: () -> NostrSigner?,
    private val relays: List<String> = Nip23.DEFAULT_RELAYS,
    private val connector: RelayConnector,
    private val nowSeconds: () -> Long = NostrWire::nowSeconds,
    private val diagnostics: (String) -> Unit = {},
) {
    suspend fun publish(
        identifier: String,
        title: String,
        markdown: String,
        firstPublishedAt: Long,
    ): ShareOutcome {
        val active = signer() ?: return ShareOutcome.NOT_SIGNED_IN
        val pubkey = active.pubkeyHex ?: return ShareOutcome.NOT_SIGNED_IN
        val draft = Nip23.articleDraft(
            pubkeyHex = pubkey,
            createdAt = nowSeconds(),
            identifier = identifier,
            title = title,
            markdown = markdown,
            publishedAt = firstPublishedAt,
        )
        return send(active, draft)
    }

    suspend fun delete(identifier: String): ShareOutcome {
        val active = signer() ?: return ShareOutcome.NOT_SIGNED_IN
        val pubkey = active.pubkeyHex ?: return ShareOutcome.NOT_SIGNED_IN
        return send(active, Nip23.deletionDraft(pubkey, nowSeconds(), identifier))
    }

    private suspend fun send(active: NostrSigner, draft: NostrEventDraft): ShareOutcome {
        val signed = active.sign(draft) ?: return ShareOutcome.NOT_SIGNED
        val accepted = RelayPublisher(connector, relays, diagnostics = diagnostics).publish(signed)
        return if (accepted) ShareOutcome.PUBLISHED else ShareOutcome.NOT_ACCEPTED
    }
}
