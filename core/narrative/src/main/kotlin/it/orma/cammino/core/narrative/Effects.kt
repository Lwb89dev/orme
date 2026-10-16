package it.orma.cammino.core.narrative

import it.orma.cammino.core.content.schema.AddRelationEffect
import it.orma.cammino.core.content.schema.AddVarEffect
import it.orma.cammino.core.content.schema.ChapterCompletedCondition
import it.orma.cammino.core.content.schema.DiscoverTeachingEffect
import it.orma.cammino.core.content.schema.FlagCondition
import it.orma.cammino.core.content.schema.NarrativeCondition
import it.orma.cammino.core.content.schema.NarrativeEffect
import it.orma.cammino.core.content.schema.RelationGteCondition
import it.orma.cammino.core.content.schema.SetFlagEffect
import it.orma.cammino.core.content.schema.UnlockGardenEffect
import it.orma.cammino.core.content.schema.VarGteCondition
import it.orma.cammino.core.content.schema.VarLteCondition
import it.orma.cammino.core.model.ExperienceEvent
import it.orma.cammino.core.model.GardenElementUnlocked
import it.orma.cammino.core.model.Modality
import it.orma.cammino.core.model.TeachingDiscovered

/** Valutazione pura delle condizioni narrative su un profilo. */
object ConditionEvaluator {

    fun allMet(conditions: List<NarrativeCondition>, profile: NarrativeProfile): Boolean =
        conditions.all { isMet(it, profile) }

    fun isMet(condition: NarrativeCondition, profile: NarrativeProfile): Boolean = when (condition) {
        is FlagCondition -> (profile.flags[condition.flag] ?: false) == condition.value
        is VarGteCondition -> (profile.variables[condition.variable] ?: 0) >= condition.value
        is VarLteCondition -> (profile.variables[condition.variable] ?: 0) <= condition.value
        is RelationGteCondition -> (profile.relations[condition.characterId] ?: 0) >= condition.value
        is ChapterCompletedCondition -> condition.chapterId in profile.completedChapters
    }
}

/**
 * Applicazione pura degli effetti: restituisce il nuovo profilo e gli eventi
 * di esperienza da inoltrare al motore di progressione.
 */
object EffectApplier {

    fun applyAll(
        effects: List<NarrativeEffect>,
        profile: NarrativeProfile,
        via: Modality = Modality.VIANDANTE,
    ): Pair<NarrativeProfile, List<ExperienceEvent>> {
        var current = profile
        val events = mutableListOf<ExperienceEvent>()
        effects.forEach { effect ->
            when (effect) {
                is SetFlagEffect -> current = current.copy(flags = current.flags + (effect.flag to effect.value))
                is AddVarEffect -> current = current.copy(
                    variables = current.variables + (effect.variable to (current.variables[effect.variable] ?: 0) + effect.delta)
                )
                is AddRelationEffect -> current = current.copy(
                    relations = current.relations + (effect.characterId to (current.relations[effect.characterId] ?: 0) + effect.delta)
                )
                is DiscoverTeachingEffect -> events += TeachingDiscovered(effect.teachingId, via)
                is UnlockGardenEffect -> events += GardenElementUnlocked(effect.elementId, via)
            }
        }
        return current to events
    }
}
