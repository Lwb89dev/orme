package it.orma.cammino.core.content.schema

import it.orma.cammino.core.model.ReviewStatus
import it.orma.cammino.core.model.SourceCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Personaggio del Viandante (storico o di finzione, dichiarato). */
@Serializable
data class StoryCharacter(
    val id: String,
    val name: String,
    val description: String = "",
    val isHistorical: Boolean = false,
)

/** Un capitolo giocabile del Viandante: un grafo di nodi narrativi. */
@Serializable
data class StoryChapter(
    val id: String,
    val title: String,
    val order: Int = 0,
    /** Luogo della mappa a cui il capitolo appartiene. */
    val placeId: String,
    val summary: String = "",
    /** true per i capitoli dedicati direttamente alla biografia di Francesco. */
    val isBiographical: Boolean = false,
    /** Capitoli che devono essere completati prima di sbloccare questo. */
    val requiredChapters: List<String> = emptyList(),
    val startNodeId: String,
    val nodes: List<StoryNode>,
    /** Fonti medievali di riferimento per l'episodio (può essere vuota per pura invenzione). */
    val sourceDocumentIds: List<String> = emptyList(),
    /** Categoria editoriale del capitolo: quasi sempre narrazione_originale. */
    val category: SourceCategory = SourceCategory.NARRAZIONE_ORIGINALE,
    val reviewStatus: ReviewStatus = ReviewStatus.DRAFT,
)

// ---------------------------------------------------------------------------
// Nodi narrativi
// ---------------------------------------------------------------------------

@Serializable
sealed interface StoryNode {
    val id: String
}

/** Testo narrativo del narratore. */
@Serializable
@SerialName("narration")
data class NarrationNode(
    override val id: String,
    val text: String,
    val next: String? = null,
    val effects: List<NarrativeEffect> = emptyList(),
) : StoryNode

/** Battuta di un personaggio (speakerId = id di StoryCharacter, "player" o "narrator"). */
@Serializable
@SerialName("dialogue")
data class DialogueLineNode(
    override val id: String,
    val speakerId: String,
    val text: String,
    val next: String? = null,
    val effects: List<NarrativeEffect> = emptyList(),
) : StoryNode

/** Scelta dell'utente fra 2-4 opzioni, ciascuna con condizioni ed effetti. */
@Serializable
@SerialName("choice")
data class ChoiceNode(
    override val id: String,
    val prompt: String,
    val options: List<ChoiceOption>,
) : StoryNode

@Serializable
data class ChoiceOption(
    val id: String,
    val text: String,
    val next: String,
    val conditions: List<NarrativeCondition> = emptyList(),
    val effects: List<NarrativeEffect> = emptyList(),
)

/** Diramazione automatica basata sullo stato narrativo accumulato. */
@Serializable
@SerialName("conditional_branch")
data class ConditionalBranchNode(
    override val id: String,
    val branches: List<ConditionalBranch>,
    val defaultNext: String,
) : StoryNode

@Serializable
data class ConditionalBranch(
    val conditions: List<NarrativeCondition>,
    val next: String,
)

/** Incontro con un personaggio: lo registra e può modificare la relazione. */
@Serializable
@SerialName("character_encounter")
data class CharacterEncounterNode(
    override val id: String,
    val characterId: String,
    val text: String,
    val next: String? = null,
    val effects: List<NarrativeEffect> = emptyList(),
) : StoryNode

/** Pannello di approfondimento storico mostrato dentro la storia. */
@Serializable
@SerialName("historical_insight")
data class HistoricalInsightNode(
    override val id: String,
    val title: String,
    val text: String,
    val sourceDocumentIds: List<String> = emptyList(),
    val next: String? = null,
) : StoryNode

/** Nodo che collega la scena a un insegnamento: lo marca come scoperto. */
@Serializable
@SerialName("teaching")
data class TeachingNode(
    override val id: String,
    val teachingId: String,
    val text: String,
    val next: String? = null,
) : StoryNode

/** Invito alla riflessione personale, salvabile nel diario. */
@Serializable
@SerialName("reflection")
data class ReflectionNode(
    override val id: String,
    val prompt: String,
    val teachingId: String? = null,
    val next: String? = null,
) : StoryNode

/** Sblocco esplicito di un elemento (es. giardino) dentro la narrazione. */
@Serializable
@SerialName("reward_unlock")
data class RewardUnlockNode(
    override val id: String,
    val text: String,
    val unlocks: List<NarrativeEffect> = emptyList(),
    val next: String? = null,
) : StoryNode

/** Fine del capitolo. Ogni capitolo deve avere almeno un percorso che vi conduce. */
@Serializable
@SerialName("completion")
data class CompletionNode(
    override val id: String,
    val text: String,
    /** Insegnamenti marcati ESPLORATO al completamento del capitolo. */
    val exploredTeachingIds: List<String> = emptyList(),
) : StoryNode

// ---------------------------------------------------------------------------
// Condizioni ed effetti (mini-DSL narrativo)
// ---------------------------------------------------------------------------

@Serializable
sealed interface NarrativeCondition

@Serializable
@SerialName("flag")
data class FlagCondition(val flag: String, val value: Boolean = true) : NarrativeCondition

@Serializable
@SerialName("var_gte")
data class VarGteCondition(val variable: String, val value: Int) : NarrativeCondition

@Serializable
@SerialName("var_lte")
data class VarLteCondition(val variable: String, val value: Int) : NarrativeCondition

@Serializable
@SerialName("relation_gte")
data class RelationGteCondition(val characterId: String, val value: Int) : NarrativeCondition

@Serializable
@SerialName("chapter_completed")
data class ChapterCompletedCondition(val chapterId: String) : NarrativeCondition

@Serializable
sealed interface NarrativeEffect

@Serializable
@SerialName("set_flag")
data class SetFlagEffect(val flag: String, val value: Boolean = true) : NarrativeEffect

@Serializable
@SerialName("add_var")
data class AddVarEffect(val variable: String, val delta: Int) : NarrativeEffect

@Serializable
@SerialName("add_relation")
data class AddRelationEffect(val characterId: String, val delta: Int) : NarrativeEffect

/** Emette un evento di progressione: l'insegnamento viene scoperto. */
@Serializable
@SerialName("discover_teaching")
data class DiscoverTeachingEffect(val teachingId: String) : NarrativeEffect

/** Emette uno sblocco diretto di un elemento del giardino. */
@Serializable
@SerialName("unlock_garden")
data class UnlockGardenEffect(val elementId: String) : NarrativeEffect
