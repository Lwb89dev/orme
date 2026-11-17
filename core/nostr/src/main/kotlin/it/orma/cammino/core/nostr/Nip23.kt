package it.orma.cammino.core.nostr

/**
 * NIP-23 long-form content (kind 30023) and NIP-09 deletion (kind 5).
 *
 * A journal entry becomes an addressable article whose `d` tag is the entry id: saving the same entry again
 * replaces the previous version on the relays, and switching sharing off asks them to delete it.
 */
object Nip23 {
    const val KIND_ARTICLE = 30023
    const val KIND_DELETION = 5

    /** Relays Boris also starts from; every one is `wss://`. */
    val DEFAULT_RELAYS: List<String> = listOf(
        "wss://relay.damus.io",
        "wss://nos.lol",
        "wss://relay.primal.net",
    )

    private const val TITLE_MAX_CHARS = 80

    fun articleDraft(
        pubkeyHex: String,
        createdAt: Long,
        identifier: String,
        title: String,
        markdown: String,
        publishedAt: Long,
        topics: List<String> = listOf("orme"),
        client: String = "Orme",
    ): NostrEventDraft {
        require(identifier.isNotBlank()) { "The article needs an identifier" }
        val tags = buildList {
            add(listOf("d", identifier))
            add(listOf("title", title))
            add(listOf("published_at", publishedAt.toString()))
            topics.forEach { add(listOf("t", it)) }
            add(listOf("client", client))
        }
        return NostrEventDraft(pubkeyHex, createdAt, KIND_ARTICLE, tags, markdown)
    }

    /** NIP-09 request to delete the addressable article [identifier] of [pubkeyHex]. */
    fun deletionDraft(
        pubkeyHex: String,
        createdAt: Long,
        identifier: String,
        reason: String = "",
    ): NostrEventDraft = NostrEventDraft(
        pubkeyHex,
        createdAt,
        KIND_DELETION,
        listOf(listOf("a", "$KIND_ARTICLE:$pubkeyHex:$identifier"), listOf("k", KIND_ARTICLE.toString())),
        reason,
    )

    /** First non-empty line of [text], without Markdown heading marks, cut to a readable length. */
    fun titleOf(text: String, fallback: String): String {
        val line = text.lineSequence().map { it.trim().trimStart('#', '*', '>', '-', ' ') }
            .firstOrNull { it.isNotEmpty() } ?: return fallback
        if (line.length <= TITLE_MAX_CHARS) return line
        val cut = line.take(TITLE_MAX_CHARS)
        val space = cut.lastIndexOf(' ')
        return (if (space > TITLE_MAX_CHARS / 2) cut.take(space) else cut).trimEnd() + "…"
    }
}
