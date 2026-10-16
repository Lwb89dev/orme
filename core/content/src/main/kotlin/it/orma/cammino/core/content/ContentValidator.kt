package it.orma.cammino.core.content

import it.orma.cammino.core.content.schema.CardScenario
import it.orma.cammino.core.content.schema.CharacterEncounterNode
import it.orma.cammino.core.content.schema.ChoiceNode
import it.orma.cammino.core.content.schema.CompletionNode
import it.orma.cammino.core.content.schema.ConditionalBranchNode
import it.orma.cammino.core.content.schema.DialogueLineNode
import it.orma.cammino.core.content.schema.DiscoverTeachingEffect
import it.orma.cammino.core.content.schema.HistoricalInsightNode
import it.orma.cammino.core.content.schema.NarrationNode
import it.orma.cammino.core.content.schema.NarrativeEffect
import it.orma.cammino.core.content.schema.ReflectionNode
import it.orma.cammino.core.content.schema.RewardUnlockNode
import it.orma.cammino.core.content.schema.StoryChapter
import it.orma.cammino.core.content.schema.StoryNode
import it.orma.cammino.core.content.schema.TeachingNode
import it.orma.cammino.core.content.schema.UnlockGardenEffect
import it.orma.cammino.core.model.ReviewStatus

enum class Severity { ERROR, WARNING }

data class ContentIssue(
    val severity: Severity,
    val area: String,
    val message: String,
) {
    override fun toString(): String = "[${severity}] $area: $message"
}

/**
 * Validatore del pacchetto contenuti. Garantisce:
 * identificatori univoci, riferimenti integri, grafi narrativi percorribili,
 * regole editoriali (niente citazioni originali senza licenza di pubblico dominio,
 * niente contenuti rifiutati esposti come autentici).
 */
class ContentValidator {

    fun validate(catalog: ContentCatalog): List<ContentIssue> {
        val issues = mutableListOf<ContentIssue>()
        checkUniqueIds(catalog, issues)
        checkSourceReferences(catalog, issues)
        checkTeachingReferences(catalog, issues)
        checkHistoryReferences(catalog, issues)
        checkStoryChapters(catalog, issues)
        checkCards(catalog, issues)
        checkDialogues(catalog, issues)
        checkGarden(catalog, issues)
        return issues
    }

    fun errors(catalog: ContentCatalog): List<ContentIssue> =
        validate(catalog).filter { it.severity == Severity.ERROR }

    // ------------------------------------------------------------------

    private fun checkUniqueIds(catalog: ContentCatalog, issues: MutableList<ContentIssue>) {
        val all = buildList<Pair<String, String>> {
            catalog.teachings.forEach { add("insegnamento" to it.id) }
            catalog.sourceDocuments.forEach { add("documento" to it.id) }
            catalog.sourcePassages.forEach { add("passaggio" to it.id) }
            catalog.historicalEvents.forEach { add("evento" to it.id) }
            catalog.historicalPersons.forEach { add("persona" to it.id) }
            catalog.places.forEach { add("luogo" to it.id) }
            catalog.glossary.forEach { add("glossario" to it.id) }
            catalog.biographyChapters.forEach { add("biografia" to it.id) }
            catalog.storyCharacters.forEach { add("personaggio" to it.id) }
            catalog.storyChapters.forEach { add("capitolo" to it.id) }
            catalog.storyChapters.flatMap { ch -> ch.nodes.map { "nodo(${ch.id})" to it.id } }
                .forEach { add(it) }
            catalog.cards.forEach { add("carta" to it.id) }
            catalog.cardPaths.forEach { add("percorso" to it.id) }
            catalog.cardScenarios.forEach { add("scenario" to it.id) }
            catalog.conversations.forEach { add("dialogo" to it.id) }
            catalog.conversations.flatMap { c -> c.nodes.map { "nodoDialogo(${c.id})" to it.id } }
                .forEach { add(it) }
            catalog.gardenElements.forEach { add("giardino" to it.id) }
            catalog.contemplations.forEach { add("contemplazione" to it.id) }
        }
        all.groupBy { it.second }.filterValues { it.size > 1 }.forEach { (id, occurrences) ->
            issues += ContentIssue(
                Severity.ERROR, "id",
                "Identificatore duplicato '$id' in: ${occurrences.joinToString { it.first }}"
            )
        }
    }

    private fun checkSourceReferences(catalog: ContentCatalog, issues: MutableList<ContentIssue>) {
        catalog.sourcePassages.forEach { p ->
            if (p.documentId !in catalog.documentsById) {
                issues += ContentIssue(Severity.ERROR, "fonti", "Passaggio '${p.id}': documento '${p.documentId}' inesistente")
            }
            if (p.originalText.isNotBlank() && !p.license.startsWith("public_domain")) {
                issues += ContentIssue(
                    Severity.ERROR, "fonti",
                    "Passaggio '${p.id}': testo originale riprodotto senza licenza di pubblico dominio ('${p.license}')"
                )
            }
            if (p.originalText.isBlank() && p.translation.isBlank() && p.paraphrase.isBlank()) {
                issues += ContentIssue(Severity.WARNING, "fonti", "Passaggio '${p.id}' privo di testo, traduzione e parafrasi")
            }
            if (p.translation.isNotBlank() && p.originalText.isBlank()) {
                issues += ContentIssue(Severity.WARNING, "fonti", "Passaggio '${p.id}': traduzione senza testo originale")
            }
        }
        catalog.sourceDocuments.forEach { d ->
            if (d.licenseNote.isBlank()) {
                issues += ContentIssue(Severity.WARNING, "fonti", "Documento '${d.id}' senza nota di licenza")
            }
        }
    }

    private fun checkTeachingReferences(catalog: ContentCatalog, issues: MutableList<ContentIssue>) {
        catalog.teachings.forEach { t ->
            t.sourcePassageIds.forEach { ref ->
                if (ref !in catalog.passagesById) {
                    issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento '${t.id}': passaggio '$ref' inesistente")
                } else {
                    val passage = catalog.passagesById.getValue(ref)
                    val doc = catalog.documentsById[passage.documentId]
                    if (t.reviewStatus == ReviewStatus.VERIFIED && passage.reviewStatus == ReviewStatus.REJECTED) {
                        issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento verificato '${t.id}' cita il passaggio rifiutato '$ref'")
                    }
                    if (t.reviewStatus == ReviewStatus.VERIFIED && doc?.reviewStatus == ReviewStatus.REJECTED) {
                        issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento verificato '${t.id}' cita il documento rifiutato '${doc.id}'")
                    }
                }
            }
            t.relatedEventIds.forEach { ref ->
                if (ref !in catalog.eventsById) issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento '${t.id}': evento '$ref' inesistente")
            }
            t.relatedTeachingIds.forEach { ref ->
                if (ref !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento '${t.id}': insegnamento collegato '$ref' inesistente")
            }
            t.experiences.viandante.forEach { ref ->
                if (ref !in catalog.chaptersById) issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento '${t.id}': capitolo '$ref' inesistente")
            }
            t.experiences.garden.forEach { ref ->
                if (ref !in catalog.gardenElementsById && ref !in catalog.contemplationsById) {
                    issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento '${t.id}': esperienza giardino '$ref' inesistente")
                }
            }
            t.experiences.cards.forEach { ref ->
                if (ref !in catalog.pathsById && ref !in catalog.cardsById && ref !in catalog.scenariosById) {
                    issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento '${t.id}': esperienza carte '$ref' inesistente")
                }
            }
            t.experiences.dialogue.forEach { ref ->
                if (ref !in catalog.conversationsById) issues += ContentIssue(Severity.ERROR, "insegnamenti", "Insegnamento '${t.id}': conversazione '$ref' inesistente")
            }
            if (t.sourcePassageIds.isEmpty()) {
                issues += ContentIssue(Severity.WARNING, "insegnamenti", "Insegnamento '${t.id}' senza riferimenti testuali")
            }
        }
    }

    private fun checkHistoryReferences(catalog: ContentCatalog, issues: MutableList<ContentIssue>) {
        catalog.historicalEvents.forEach { e ->
            e.placeId?.let { if (it !in catalog.placesById) issues += ContentIssue(Severity.ERROR, "storia", "Evento '${e.id}': luogo '$it' inesistente") }
            e.personIds.forEach { if (it !in catalog.personsById) issues += ContentIssue(Severity.ERROR, "storia", "Evento '${e.id}': persona '$it' inesistente") }
            e.sourceDocumentIds.forEach { if (it !in catalog.documentsById) issues += ContentIssue(Severity.ERROR, "storia", "Evento '${e.id}': documento '$it' inesistente") }
            e.teachingIds.forEach { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "storia", "Evento '${e.id}': insegnamento '$it' inesistente") }
        }
        catalog.biographyChapters.forEach { b ->
            b.sourceDocumentIds.forEach { if (it !in catalog.documentsById) issues += ContentIssue(Severity.ERROR, "storia", "Biografia '${b.id}': documento '$it' inesistente") }
            b.teachingIds.forEach { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "storia", "Biografia '${b.id}': insegnamento '$it' inesistente") }
        }
        catalog.historicalPersons.forEach { p ->
            p.sourceDocumentIds.forEach { if (it !in catalog.documentsById) issues += ContentIssue(Severity.ERROR, "storia", "Persona '${p.id}': documento '$it' inesistente") }
        }
    }

    // ------------------------------------------------------------------
    // Grafi narrativi del Viandante
    // ------------------------------------------------------------------

    private fun checkStoryChapters(catalog: ContentCatalog, issues: MutableList<ContentIssue>) {
        val chapterOrders = catalog.storyChapters.groupBy { it.order }.filterValues { it.size > 1 }
        chapterOrders.forEach { (order, chapters) ->
            issues += ContentIssue(Severity.WARNING, "viandante", "Ordine $order condiviso da più capitoli: ${chapters.joinToString { it.id }}")
        }
        catalog.storyChapters.forEach { chapter ->
            checkStoryChapter(catalog, chapter, issues)
        }
    }

    private fun checkStoryChapter(catalog: ContentCatalog, chapter: StoryChapter, issues: MutableList<ContentIssue>) {
        val area = "viandante/${chapter.id}"
        val nodeIds = chapter.nodes.map { it.id }.toSet()
        if (chapter.placeId !in catalog.placesById) {
            issues += ContentIssue(Severity.ERROR, area, "Luogo '${chapter.placeId}' inesistente")
        }
        chapter.requiredChapters.forEach { req ->
            if (req !in catalog.chaptersById) issues += ContentIssue(Severity.ERROR, area, "Capitolo richiesto '$req' inesistente")
            if (req == chapter.id) issues += ContentIssue(Severity.ERROR, area, "Il capitolo richiede sé stesso")
        }
        chapter.sourceDocumentIds.forEach { doc ->
            if (doc !in catalog.documentsById) issues += ContentIssue(Severity.ERROR, area, "Documento '$doc' inesistente")
        }
        if (chapter.startNodeId !in nodeIds) {
            issues += ContentIssue(Severity.ERROR, area, "Nodo iniziale '${chapter.startNodeId}' inesistente")
            return
        }

        val nodesById = chapter.nodes.associateBy { it.id }

        // Riferimenti interni + riferimenti a personaggi/insegnamenti/effetti
        chapter.nodes.forEach { node ->
            outgoingRefs(node).forEach { ref ->
                if (ref !in nodeIds) issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}' punta al nodo inesistente '$ref'")
            }
            nodeSpeaker(node)?.let { speaker ->
                if (speaker != NARRATOR_ID && speaker != PLAYER_ID && speaker !in catalog.charactersById) {
                    issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}': personaggio parlante '$speaker' inesistente")
                }
            }
            when (node) {
                is CharacterEncounterNode -> if (node.characterId !in catalog.charactersById) {
                    issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}': personaggio '${node.characterId}' inesistente")
                }
                is TeachingNode -> if (node.teachingId !in catalog.teachingsById) {
                    issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}': insegnamento '${node.teachingId}' inesistente")
                }
                is ReflectionNode -> node.teachingId?.let {
                    if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}': insegnamento '$it' inesistente")
                }
                is HistoricalInsightNode -> node.sourceDocumentIds.forEach {
                    if (it !in catalog.documentsById) issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}': documento '$it' inesistente")
                }
                else -> Unit
            }
            allEffects(node).forEach { effect -> checkEffect(catalog, effect, area, node.id, issues) }
        }

        // Raggiungibilità da start
        val reachable = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        stack.add(chapter.startNodeId)
        while (stack.isNotEmpty()) {
            val id = stack.removeFirst()
            if (!reachable.add(id)) continue
            val node = nodesById[id] ?: continue
            stack.addAll(outgoingRefs(node))
        }
        chapter.nodes.forEach { node ->
            if (node.id !in reachable) {
                issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}' irraggiungibile dal nodo iniziale")
            }
        }
        // Esistenza di un percorso conclusivo
        val completions = chapter.nodes.filterIsInstance<CompletionNode>()
        if (completions.isEmpty()) {
            issues += ContentIssue(Severity.ERROR, area, "Nessun nodo di completamento")
        } else if (completions.none { it.id in reachable }) {
            issues += ContentIssue(Severity.ERROR, area, "Nessun percorso raggiunge un completamento")
        }
        // Nodi terminali: solo il completamento può non avere prosecuzione
        chapter.nodes.forEach { node ->
            if (node !is CompletionNode && outgoingRefs(node).isEmpty()) {
                when (node) {
                    is ChoiceNode -> if (node.options.isEmpty()) {
                        issues += ContentIssue(Severity.ERROR, area, "Scelta '${node.id}' senza opzioni")
                    }
                    else -> issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}' senza prosecuzione e senza completamento")
                }
            }
        }
        completions.forEach { c ->
            c.exploredTeachingIds.forEach { t ->
                if (t !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, area, "Completamento '${c.id}': insegnamento '$t' inesistente")
            }
        }
    }

    private fun checkEffect(
        catalog: ContentCatalog,
        effect: NarrativeEffect,
        area: String,
        nodeId: String,
        issues: MutableList<ContentIssue>,
    ) {
        when (effect) {
            is DiscoverTeachingEffect -> if (effect.teachingId !in catalog.teachingsById) {
                issues += ContentIssue(Severity.ERROR, area, "Nodo '$nodeId': effetto su insegnamento '${effect.teachingId}' inesistente")
            }
            is UnlockGardenEffect -> if (effect.elementId !in catalog.gardenElementsById) {
                issues += ContentIssue(Severity.ERROR, area, "Nodo '$nodeId': sblocco giardino '${effect.elementId}' inesistente")
            }
            is it.orma.cammino.core.content.schema.AddRelationEffect -> if (effect.characterId !in catalog.charactersById) {
                issues += ContentIssue(Severity.ERROR, area, "Nodo '$nodeId': relazione con personaggio '${effect.characterId}' inesistente")
            }
            else -> Unit
        }
    }

    private fun outgoingRefs(node: StoryNode): List<String> = when (node) {
        is NarrationNode -> listOfNotNull(node.next)
        is DialogueLineNode -> listOfNotNull(node.next)
        is ChoiceNode -> node.options.map { it.next }
        is ConditionalBranchNode -> node.branches.map { it.next } + node.defaultNext
        is CharacterEncounterNode -> listOfNotNull(node.next)
        is HistoricalInsightNode -> listOfNotNull(node.next)
        is TeachingNode -> listOfNotNull(node.next)
        is ReflectionNode -> listOfNotNull(node.next)
        is RewardUnlockNode -> listOfNotNull(node.next)
        is CompletionNode -> emptyList()
    }

    private fun nodeSpeaker(node: StoryNode): String? = when (node) {
        is DialogueLineNode -> node.speakerId
        else -> null
    }

    private fun allEffects(node: StoryNode): List<NarrativeEffect> = when (node) {
        is NarrationNode -> node.effects
        is DialogueLineNode -> node.effects
        is ChoiceNode -> node.options.flatMap { it.effects }
        is CharacterEncounterNode -> node.effects
        is RewardUnlockNode -> node.unlocks
        else -> emptyList()
    }

    // ------------------------------------------------------------------
    // Carte
    // ------------------------------------------------------------------

    private fun checkCards(catalog: ContentCatalog, issues: MutableList<ContentIssue>) {
        catalog.cards.forEach { card ->
            card.teachingId?.let { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "carte", "Carta '${card.id}': insegnamento '$it' inesistente") }
            card.sourcePassageIds.forEach { if (it !in catalog.passagesById) issues += ContentIssue(Severity.ERROR, "carte", "Carta '${card.id}': passaggio '$it' inesistente") }
        }
        catalog.cardPaths.forEach { path ->
            path.requiresTeachings.forEach { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "carte", "Percorso '${path.id}': prerequisito '$it' inesistente") }
            path.teachingIds.forEach { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "carte", "Percorso '${path.id}': insegnamento '$it' inesistente") }
            if (path.scenarioIds.isEmpty()) issues += ContentIssue(Severity.ERROR, "carte", "Percorso '${path.id}' senza scenari")
            path.scenarioIds.forEach { if (it !in catalog.scenariosById) issues += ContentIssue(Severity.ERROR, "carte", "Percorso '${path.id}': scenario '$it' inesistente") }
        }
        catalog.cardScenarios.forEach { scenario -> checkScenario(catalog, scenario, issues) }
    }

    private fun checkScenario(catalog: ContentCatalog, scenario: CardScenario, issues: MutableList<ContentIssue>) {
        if (scenario.options.size < 2) {
            issues += ContentIssue(Severity.ERROR, "carte", "Scenario '${scenario.id}' con meno di 2 opzioni")
        }
        scenario.teachingId?.let { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "carte", "Scenario '${scenario.id}': insegnamento '$it' inesistente") }
        scenario.sourcePassageIds.forEach { if (it !in catalog.passagesById) issues += ContentIssue(Severity.ERROR, "carte", "Scenario '${scenario.id}': passaggio '$it' inesistente") }
        scenario.options.forEach { opt ->
            opt.perspectives.forEach { p ->
                p.sourcePassageIds.forEach { if (it !in catalog.passagesById) issues += ContentIssue(Severity.ERROR, "carte", "Scenario '${scenario.id}' opzione '${opt.id}': passaggio prospettiva '$it' inesistente") }
                if (!p.isEditorial && p.sourcePassageIds.isEmpty()) {
                    issues += ContentIssue(Severity.WARNING, "carte", "Prospettiva '${p.label}' dello scenario '${scenario.id}' dichiarata non editoriale ma senza fonti")
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Dialoghi
    // ------------------------------------------------------------------

    private fun checkDialogues(catalog: ContentCatalog, issues: MutableList<ContentIssue>) {
        catalog.conversations.forEach { conv ->
            val area = "dialogo/${conv.id}"
            val nodeIds = conv.nodes.map { it.id }.toSet()
            if (conv.startNodeId !in nodeIds) {
                issues += ContentIssue(Severity.ERROR, area, "Nodo iniziale '${conv.startNodeId}' inesistente")
                return@forEach
            }
            conv.teachingIds.forEach { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, area, "Insegnamento '$it' inesistente") }
            conv.nodes.forEach { node ->
                node.options.forEach { opt ->
                    val next = opt.next
                    if (next == null) {
                        if (!node.isTerminal) issues += ContentIssue(Severity.ERROR, area, "Opzione '${opt.id}' del nodo '${node.id}' senza destinazione in nodo non terminale")
                    } else if (next !in nodeIds) {
                        issues += ContentIssue(Severity.ERROR, area, "Opzione '${opt.id}' del nodo '${node.id}' punta a '$next' inesistente")
                    }
                }
                node.sourcePassageIds.forEach { if (it !in catalog.passagesById) issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}': passaggio '$it' inesistente") }
                node.teachingId?.let { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, area, "Nodo '${node.id}': insegnamento '$it' inesistente") }
            }
            // Raggiungibilità e terminalità
            val nodesById = conv.nodes.associateBy { it.id }
            val reachable = mutableSetOf<String>()
            val stack = ArrayDeque<String>()
            stack.add(conv.startNodeId)
            while (stack.isNotEmpty()) {
                val id = stack.removeFirst()
                if (!reachable.add(id)) continue
                nodesById[id]?.options?.mapNotNull { it.next }?.let(stack::addAll)
            }
            conv.nodes.forEach { if (it.id !in reachable) issues += ContentIssue(Severity.ERROR, area, "Nodo '${it.id}' irraggiungibile") }
            if (conv.nodes.none { it.isTerminal && it.id in reachable }) {
                issues += ContentIssue(Severity.ERROR, area, "Nessun nodo terminale raggiungibile")
            }
        }
    }

    // ------------------------------------------------------------------
    // Giardino
    // ------------------------------------------------------------------

    private fun checkGarden(catalog: ContentCatalog, issues: MutableList<ContentIssue>) {
        catalog.gardenElements.forEach { el ->
            el.requiresTeaching?.let { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "giardino", "Elemento '${el.id}': insegnamento richiesto '$it' inesistente") }
            el.teachingId?.let { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "giardino", "Elemento '${el.id}': insegnamento '$it' inesistente") }
        }
        catalog.contemplations.forEach { c ->
            if (c.steps.isEmpty()) issues += ContentIssue(Severity.ERROR, "giardino", "Contemplazione '${c.id}' senza passi")
            c.requiresTeaching?.let { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "giardino", "Contemplazione '${c.id}': insegnamento richiesto '$it' inesistente") }
            c.teachingId?.let { if (it !in catalog.teachingsById) issues += ContentIssue(Severity.ERROR, "giardino", "Contemplazione '${c.id}': insegnamento '$it' inesistente") }
            c.steps.forEach { s ->
                s.sourcePassageId?.let { if (it !in catalog.passagesById) issues += ContentIssue(Severity.ERROR, "giardino", "Contemplazione '${c.id}': passaggio '$it' inesistente") }
            }
        }
    }

    companion object {
        const val NARRATOR_ID = "narrator"
        const val PLAYER_ID = "player"
    }
}
