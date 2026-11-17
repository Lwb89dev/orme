package it.orma.cammino.nostr

import android.content.Context
import android.util.Base64
import it.orma.cammino.core.nostr.NostrBunkerUri
import it.orma.cammino.core.nostr.NostrNip19
import it.orma.cammino.core.nostr.NostrSchnorr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class NostrFlavor(val wire: String) {
    AMBER("amber"),
    BUNKER("bunker"),
    ;

    companion object {
        fun fromWire(value: String?): NostrFlavor? = entries.firstOrNull { it.wire == value }
    }
}

/** Chi ha fatto l'accesso. Mai una chiave privata: solo la chiave pubblica e il modo in cui si firma. */
data class NostrIdentity(val pubkeyHex: String, val flavor: NostrFlavor) {
    val npub: String get() = NostrNip19.encodePublicKey(pubkeyHex)
}

/** Come parlare con il signer remoto di un accesso bunker; la chiave è di questa app, non dell'account. */
class BunkerSession(
    val clientKeyHex: String,
    val remotePubkeyHex: String,
    val relays: List<String>,
)

/**
 * Identità persistita. La chiave privata dell'account non arriva mai qui: con Amber si conserva solo la
 * chiave pubblica; con un bunker anche la chiave che identifica questa app presso il bunker, cifrata con una
 * chiave AES del Keystore di Android.
 */
class NostrIdentityStore(context: Context) {
    private val preferences = context.getSharedPreferences("orme_nostr_identity", Context.MODE_PRIVATE)

    private val _identity = MutableStateFlow(readIdentity())
    val identity: StateFlow<NostrIdentity?> = _identity.asStateFlow()

    private fun readIdentity(): NostrIdentity? {
        val pubkey = preferences.getString(PUBLIC_KEY, null)?.takeIf(HEX_64::matches) ?: return null
        val flavor = NostrFlavor.fromWire(preferences.getString(FLAVOR, null)) ?: return null
        // Un backup ripristinato su un altro telefono porta le preferenze ma non la chiave del Keystore.
        if (flavor == NostrFlavor.BUNKER && bunkerSession() == null) {
            clear()
            return null
        }
        return NostrIdentity(pubkey, flavor)
    }

    fun saveAmber(pubkeyHex: String): Boolean {
        require(HEX_64.matches(pubkeyHex))
        val saved = preferences.edit()
            .putString(PUBLIC_KEY, pubkeyHex)
            .putString(FLAVOR, NostrFlavor.AMBER.wire)
            .remove(BUNKER_CLIENT_KEY).remove(BUNKER_REMOTE).remove(BUNKER_RELAYS)
            .commit()
        if (saved) _identity.value = NostrIdentity(pubkeyHex, NostrFlavor.AMBER)
        return saved
    }

    fun saveBunker(pubkeyHex: String, clientKeyHex: String, remotePubkeyHex: String, relays: List<String>): Boolean {
        require(HEX_64.matches(pubkeyHex))
        val saved = runCatching {
            preferences.edit()
                .putString(PUBLIC_KEY, pubkeyHex)
                .putString(FLAVOR, NostrFlavor.BUNKER.wire)
                .putString(BUNKER_CLIENT_KEY, encrypt(clientKeyHex))
                .putString(BUNKER_REMOTE, remotePubkeyHex)
                .putString(BUNKER_RELAYS, relays.joinToString("\n"))
                .commit()
        }.getOrDefault(false)
        if (saved) _identity.value = NostrIdentity(pubkeyHex, NostrFlavor.BUNKER)
        return saved
    }

    fun bunkerSession(): BunkerSession? = runCatching {
        if (preferences.getString(FLAVOR, null) != NostrFlavor.BUNKER.wire) return null
        BunkerSession(
            clientKeyHex = decrypt(preferences.getString(BUNKER_CLIENT_KEY, null) ?: return null),
            remotePubkeyHex = preferences.getString(BUNKER_REMOTE, null)?.takeIf(HEX_64::matches) ?: return null,
            relays = (preferences.getString(BUNKER_RELAYS, null) ?: return null).split('\n')
                .filter(NostrBunkerUri::isRelay),
        ).takeIf { it.relays.isNotEmpty() }
    }.getOrNull()

    /** Il pacchetto dell'app signer scelto da Amber, per aprire proprio quella. */
    fun signerPackage(): String? =
        preferences.getString(SIGNER_PACKAGE, null)?.takeIf(PACKAGE_NAME::matches)

    fun saveSignerPackage(value: String?) {
        val candidate = value?.trim()?.takeIf(PACKAGE_NAME::matches) ?: return
        preferences.edit().putString(SIGNER_PACKAGE, candidate).apply()
    }

    fun logout() = clear()

    private fun clear() {
        preferences.edit().clear().commit()
        _identity.value = null
    }

    fun newClientKey(): String {
        val random = SecureRandom()
        while (true) {
            val candidate = ByteArray(32).also(random::nextBytes).joinToString("") { "%02x".format(it) }
            if (runCatching { NostrSchnorr.publicKey(candidate) }.isSuccess) return candidate
        }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, payload.copyOfRange(0, IV_BYTES)))
        return cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size)).toString(StandardCharsets.UTF_8)
    }

    private companion object {
        const val PUBLIC_KEY = "pubkey_hex"
        const val FLAVOR = "flavor"
        const val BUNKER_CLIENT_KEY = "bunker_client_key_gcm"
        const val BUNKER_REMOTE = "bunker_remote_pubkey"
        const val BUNKER_RELAYS = "bunker_relays"
        const val SIGNER_PACKAGE = "signer_package"
        const val KEY_ALIAS = "orme_nostr_bunker_client_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        val HEX_64 = Regex("[0-9a-f]{64}")
        val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    }
}
