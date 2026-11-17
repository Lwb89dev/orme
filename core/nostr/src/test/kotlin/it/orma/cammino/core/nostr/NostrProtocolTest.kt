package it.orma.cammino.core.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NostrProtocolTest {
    private val zero32 = "00".repeat(32)

    @Test
    fun `BIP-340 official vector 0`() {
        val secret = "0000000000000000000000000000000000000000000000000000000000000003"
        assertEquals(
            "f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9",
            NostrSchnorr.publicKey(secret),
        )
        val signature = NostrSchnorr.signHashWithAux(secret, zero32, zero32)
        assertEquals(
            "e907831f80848d1069a5371b402410364bdf1c5f8307b0084c55f1ce2dca821525f66a4a85ea8b71e482a74f382d2ce5ebeee8fdb2172f477df4900d310536c0",
            signature,
        )
        assertTrue(NostrSchnorr.verifyHash(NostrSchnorr.publicKey(secret), zero32, signature))
    }

    @Test
    fun `a signed event verifies and a tampered one does not`() {
        val key = "0000000000000000000000000000000000000000000000000000000000000007"
        val pub = NostrSchnorr.publicKey(key)
        val draft = NostrEventDraft(pub, 1_700_000_000L, 1, listOf(listOf("t", "x")), "ciao")
        val event = NostrSchnorr.signEvent(draft, key)
        assertTrue(NostrWire.verify(event))
        assertFalse(NostrWire.verify(event + ("content" to "altro")))
    }

    @Test
    fun `NIP-44 round trip between two keys`() {
        val a = "0000000000000000000000000000000000000000000000000000000000000011"
        val b = "0000000000000000000000000000000000000000000000000000000000000022"
        val encrypted = Nip44V2.encrypt(a, NostrSchnorr.publicKey(b), "Pace e bene 🌿")
        assertEquals("Pace e bene 🌿", Nip44V2.decrypt(b, NostrSchnorr.publicKey(a), encrypted))
    }

    @Test
    fun `npub round trip`() {
        val pub = NostrSchnorr.publicKey("0000000000000000000000000000000000000000000000000000000000000005")
        assertEquals(pub, NostrNip19.decodePublicKey(NostrNip19.encodePublicKey(pub)))
    }
}
