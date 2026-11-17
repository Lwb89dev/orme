package it.orma.cammino.core.nostr

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleSharerTest {
    private val key = "0000000000000000000000000000000000000000000000000000000000000009"
    private val pub = NostrSchnorr.publicKey(key)

    private class LocalSigner(private val key: String, override val pubkeyHex: String?) : NostrSigner {
        override val signsSilently = true
        override suspend fun sign(draft: NostrEventDraft) = NostrSchnorr.signEvent(draft, key)
        override suspend fun nip44Encrypt(peerPubkeyHex: String, plaintext: String): String? = null
        override suspend fun nip44Decrypt(peerPubkeyHex: String, payload: String): String? = null
    }

    /** A relay that answers every EVENT with an OK carrying [accepted]. */
    private class FakeRelays(private val accepted: Boolean) : RelayConnector {
        val frames = mutableListOf<String>()
        override fun connect(url: String, events: RelayEvents): RelaySocket {
            Thread { events.onOpen() }.start()
            return object : RelaySocket {
                override fun send(text: String): Boolean {
                    synchronized(frames) { frames += text }
                    val id = Regex("\"id\":\"([0-9a-f]{64})\"").find(text)?.groupValues?.get(1) ?: return true
                    events.onMessage("[\"OK\",\"$id\",$accepted,\"\"]")
                    return true
                }

                override fun close() = Unit
            }
        }
    }

    private fun sharer(relays: FakeRelays, signer: NostrSigner? = LocalSigner(key, pub)) =
        ArticleSharer(signer = { signer }, relays = listOf("wss://relay.test"), connector = relays, nowSeconds = { 1_800_000_000L })

    @Test
    fun `publishes a signed kind 30023 article`() = runBlocking {
        val relays = FakeRelays(accepted = true)
        val outcome = sharer(relays).publish("entry-1", "Una riflessione", "# Una riflessione\n\nTesto", 1_700_000_000L)
        assertEquals(ShareOutcome.PUBLISHED, outcome)
        val frame = relays.frames.single()
        assertTrue(frame.startsWith("[\"EVENT\","))
        assertTrue(frame.contains("\"kind\":30023"))
        assertTrue(frame.contains("[\"d\",\"entry-1\"]"))
        assertTrue(frame.contains("[\"published_at\",\"1700000000\"]"))
    }

    @Test
    fun `reports a relay that refuses`() = runBlocking {
        val outcome = sharer(FakeRelays(accepted = false)).publish("e", "t", "m", 1L)
        assertEquals(ShareOutcome.NOT_ACCEPTED, outcome)
    }

    @Test
    fun `nothing is sent when signed out or declined`() = runBlocking {
        val relays = FakeRelays(accepted = true)
        assertEquals(ShareOutcome.NOT_SIGNED_IN, sharer(relays, signer = null).publish("e", "t", "m", 1L))
        val declining = object : NostrSigner by LocalSigner(key, pub) {
            override suspend fun sign(draft: NostrEventDraft): Map<String, Any?>? = null
        }
        assertEquals(ShareOutcome.NOT_SIGNED, sharer(relays, declining).publish("e", "t", "m", 1L))
        assertTrue(relays.frames.isEmpty())
    }

    @Test
    fun `deletion targets the addressable article`() = runBlocking {
        val relays = FakeRelays(accepted = true)
        assertEquals(ShareOutcome.PUBLISHED, sharer(relays).delete("entry-1"))
        val frame = relays.frames.single()
        assertTrue(frame.contains("\"kind\":5"))
        assertTrue(frame.contains("30023:$pub:entry-1"))
    }

    @Test
    fun `title is the first line, trimmed`() {
        assertEquals("Una riflessione", Nip23.titleOf("\n## Una riflessione\naltro", "x"))
        assertEquals("fallback", Nip23.titleOf("  \n ", "fallback"))
        assertTrue(Nip23.titleOf("parola ".repeat(30), "x").endsWith("…"))
    }
}
