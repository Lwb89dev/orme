package it.orma.cammino.core.content

import it.orma.cammino.core.model.ReviewStatus
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Valida il pacchetto contenuti effettivamente distribuito nell'app
 * (app/src/main/assets/content). È il cancello di qualità editoriale:
 * nessun errore di integrità è ammesso sui contenuti rilasciati.
 */
class ShippedContentTest {

    private fun loadRealCatalog(language: String = ContentLoader.DEFAULT_LANGUAGE): ContentCatalog {
        val assetsDir = File("../../app/src/main/assets")
        assertTrue(
            "Directory assets non trovata da ${File(".").absolutePath}",
            assetsDir.isDirectory,
        )
        val loader = ContentLoader { path ->
            val f = File(assetsDir, path)
            if (f.isFile) f.readText() else null
        }
        return loader.load(language)
    }

    @Test
    fun `il pacchetto contenuti distribuito non ha errori di validazione`() {
        val catalog = loadRealCatalog()
        assertNotNull(catalog)
        val issues = ContentValidator().validate(catalog)
        val errors = issues.filter { it.severity == Severity.ERROR }
        assertTrue(
            "Errori di contenuto:\n" + errors.joinToString("\n"),
            errors.isEmpty(),
        )
    }

    @Test
    fun `i contenuti minimi della prima release sono presenti`() {
        val catalog = loadRealCatalog()
        assertTrue("servono almeno 10 insegnamenti", catalog.teachings.size >= 10)
        assertTrue("servono almeno 5 capitoli", catalog.storyChapters.size >= 5)
        assertTrue("serve almeno 1 capitolo biografico", catalog.storyChapters.any { it.isBiographical })
        assertTrue("servono almeno 3 personaggi del Viandante", catalog.storyCharacters.size >= 3)
        assertTrue("servono almeno 20 carte", catalog.cards.size >= 20)
        assertTrue("servono almeno 3 percorsi", catalog.cardPaths.size >= 3)
        assertTrue("servono almeno 5 scenari", catalog.cardScenarios.size >= 5)
        assertTrue("servono almeno 5 conversazioni", catalog.conversations.size >= 5)
        assertTrue("servono almeno 8 elementi del giardino", catalog.gardenElements.size >= 8)
        assertTrue("servono almeno 3 contemplazioni", catalog.contemplations.size >= 3)
        assertTrue("serve la biografia", catalog.biographyChapters.size >= 5)
        assertTrue("serve una timeline iniziale", catalog.historicalEvents.size >= 10)
        assertTrue("serve il corpus delle fonti", catalog.sourceDocuments.size >= 8)
    }

    @Test
    fun `nessun contenuto rifiutato è referenziato come autentico`() {
        val catalog = loadRealCatalog()
        // Le citazioni testuali (testo originale) devono provenire da documenti verificati
        catalog.sourcePassages.filter { it.originalText.isNotBlank() }.forEach { passage ->
            val doc = catalog.documentsById[passage.documentId]
            assertTrue(
                "Il passaggio '${passage.id}' cita testo originale da documento non verificato '${passage.documentId}'",
                doc?.reviewStatus == ReviewStatus.VERIFIED,
            )
        }
    }

    @Test
    fun `il pacchetto inglese distribuito non ha errori di validazione`() {
        val catalog = loadRealCatalog("en")
        assertTrue("il pacchetto inglese deve essere dichiarato en", catalog.manifest.language == "en")
        val errors = ContentValidator().errors(catalog)
        assertTrue(
            "Errori di contenuto inglese:\n" + errors.joinToString("\n"),
            errors.isEmpty(),
        )
    }

    @Test
    fun `ogni lingua distribuita conserva gli originali e non ha errori`() {
        val italian = loadRealCatalog("it")
        val content = File("../../app/src/main/assets/content")
        val languages = content.listFiles()?.filter { it.isDirectory && it.name != "ui" }?.map { it.name } ?: emptyList()
        for (language in languages) {
            if (language == "it") continue
            val catalog = loadRealCatalog(language)
            assertTrue("$language: manifest", catalog.manifest.language == language)
            val errors = ContentValidator().errors(catalog)
            assertTrue(
                "Errori di contenuto $language:\n" + errors.joinToString("\n"),
                errors.isEmpty(),
            )
            assertTrue(
                "$language: insegnamenti",
                catalog.teachings.map { it.id } == italian.teachings.map { it.id },
            )
            assertTrue(
                "$language: passaggi",
                catalog.sourcePassages.map { it.id } == italian.sourcePassages.map { it.id },
            )
            catalog.sourcePassages.forEach { passage ->
                assertTrue(
                    "$language ${passage.id}: l'originale medievale non va tradotto",
                    passage.originalText == italian.passagesById.getValue(passage.id).originalText,
                )
            }
            if (language != "en") {
                val italianNodes = italian.storyChapters.flatMap { chapter -> chapter.nodes.map { it.id } }.sorted()
                val nodes = catalog.storyChapters.flatMap { chapter -> chapter.nodes.map { it.id } }.sorted()
                assertTrue("$language: nodi del racconto", nodes == italianNodes)
            }
        }
    }
}
