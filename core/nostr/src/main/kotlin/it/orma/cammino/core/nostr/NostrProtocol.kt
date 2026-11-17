package it.orma.cammino.core.nostr

import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import kotlin.math.abs

/** Deterministic NIP-01 event input. Signing is intentionally a separate gate. */
class NostrEventDraft(
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    tags: List<List<String>>,
    val content: String,
) {
    val tags: List<List<String>> = Collections.unmodifiableList(
        tags.map { tag -> Collections.unmodifiableList(ArrayList(tag)) },
    )

    fun canonicalJson(): String = NostrJson.encode(
        listOf(0, pubkey, createdAt, kind, tags, content),
    )

    fun id(): String = MessageDigest.getInstance("SHA-256")
        .digest(canonicalJson().toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }

    /** Ordered exactly like nostr_tools Event.toJson(), including an empty Amber signature. */
    fun toWireMap(signature: String = ""): Map<String, Any?> = linkedMapOf(
        "id" to id(),
        "pubkey" to pubkey,
        "created_at" to createdAt,
        "kind" to kind,
        "tags" to tags,
        "content" to content,
        "sig" to signature,
    )
}

object NostrJson {
    fun encode(value: Any?): String = buildString { appendValue(value) }

    private fun StringBuilder.appendValue(value: Any?) {
        when (value) {
            null -> append("null")
            is String -> appendString(value)
            is Boolean -> append(if (value) "true" else "false")
            is Byte, is Short, is Int, is Long -> append(value.toString())
            is List<*> -> {
                append('[')
                value.forEachIndexed { index, item ->
                    if (index > 0) append(',')
                    appendValue(item)
                }
                append(']')
            }

            is Map<*, *> -> {
                append('{')
                value.entries.forEachIndexed { index, entry ->
                    require(entry.key is String) { "JSON object keys must be strings" }
                    if (index > 0) append(',')
                    appendString(entry.key as String)
                    append(':')
                    appendValue(entry.value)
                }
                append('}')
            }

            else -> throw IllegalArgumentException(
                "Unsupported deterministic JSON value: ${value::class.java.name}",
            )
        }
    }

    private fun StringBuilder.appendString(value: String) {
        append('"')
        for (character in value) {
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\u000c' -> append("\\f")
                '\r' -> append("\\r")
                else -> {
                    if (character.code < 0x20) {
                        append("\\u")
                        append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
                }
            }
        }
        append('"')
    }
}

/** NIP-01 client frames. */
object NostrRelayWire {
    fun publish(event: Map<String, Any?>): List<Any?> = listOf("EVENT", event)

    fun close(subscriptionId: String): List<Any?> = listOf("CLOSE", subscriptionId)

    fun encode(message: List<Any?>): String = NostrJson.encode(message)
}
