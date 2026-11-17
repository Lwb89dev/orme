package it.orma.cammino.core.content

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le stringhe dell'interfaccia (app/src/main/assets/content/ui) hanno chiave italiana. Ogni lingua
 * distribuita deve tradurre le stesse chiavi: una chiave mancante ricadrebbe in silenzio sull'inglese.
 */
class ShippedUiStringsTest {

    private val uiDir = File("../../app/src/main/assets/content/ui")

    private fun load(file: File): Map<String, String> =
        (Json.parseToJsonElement(file.readText()) as JsonObject).mapValues { it.value.jsonPrimitive.content }

    @Test
    fun `ogni lingua traduce le stesse chiavi e nessuna traduzione e vuota`() {
        val files = uiDir.listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }
        assertTrue("Nessun file in ${uiDir.absolutePath}", files.size >= 21)
        val reference = load(files.first()).keys
        files.forEach { file ->
            val strings = load(file)
            assertEquals("Chiavi diverse in ${file.name}", reference, strings.keys)
            val empty = strings.filterValues { it.isBlank() }.keys
            assertTrue("Traduzioni vuote in ${file.name}: $empty", empty.isEmpty())
        }
    }

    @Test
    fun `ogni traduzione conserva i segnaposto del modello`() {
        val placeholder = Regex("""\{\d}""")
        uiDir.listFiles { f -> f.extension == "json" }.orEmpty().forEach { file ->
            load(file).forEach { (key, value) ->
                val expected = placeholder.findAll(key).map { it.value }.sorted().toList()
                val actual = placeholder.findAll(value).map { it.value }.sorted().toList()
                assertEquals("Segnaposto diversi in ${file.name} per «$key»", expected, actual)
            }
        }
    }

    @Test
    fun `i testi di accesso e condivisione su Nostr sono tradotti ovunque`() {
        val mustTranslate = listOf(
            "Accedi con Nostr",
            "Accedi con Amber",
            "Accedi con il bunker",
            "Continua senza accedere",
            "Condividi su Nostr",
            "Condividere è una scelta",
        )
        uiDir.listFiles { f -> f.extension == "json" }.orEmpty().forEach { file ->
            val strings = load(file)
            mustTranslate.forEach { key ->
                assertTrue("${file.name} non traduce «$key»", strings.containsKey(key))
            }
            // Nessuna lingua chiede mai la chiave privata: il campo dell'accesso è un link bunker://.
            assertTrue(strings.getValue("Link bunker://").contains("bunker://"))
        }
    }
}
