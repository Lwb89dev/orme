package it.orma.cammino.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import it.orma.cammino.core.database.repository.JournalKind
import it.orma.cammino.core.designsystem.components.label
import it.orma.cammino.core.model.SourceCategory
import it.orma.cammino.core.model.TeachingStatus
import it.orma.cammino.core.model.Theme

/** Language of the app chrome. Content itself is loaded from the selected catalog. */
val LocalAppLanguage = compositionLocalOf { "en" }

@Composable
fun uiText(italian: String, english: String): String =
    if (LocalAppLanguage.current == "en") english else italian

@Composable
fun sourceCategoryText(category: SourceCategory): String = uiText(
    category.label(),
    when (category) {
        SourceCategory.SCRITTO_AUTENTICO -> "Writing attributed to Francis"
        SourceCategory.TESTIMONIANZA_MEDIEVALE -> "Medieval testimony"
        SourceCategory.TRADIZIONE_AGIOGRAFICA -> "Hagiographic tradition"
        SourceCategory.INTERPRETAZIONE_MODERNA -> "Editorial note"
        SourceCategory.NARRAZIONE_ORIGINALE -> "Original game narrative"
    },
)

@Composable
fun teachingStatusText(status: TeachingStatus?): String = uiText(
    status?.label() ?: "Da scoprire",
    when (status) {
        null -> "Not yet discovered"
        TeachingStatus.SCOPERTO -> "Discovered"
        TeachingStatus.ESPLORATO -> "Explored"
        TeachingStatus.APPROFONDITO -> "Studied further"
    },
)

@Composable
fun journalKindText(kind: JournalKind): String = uiText(
    when (kind) {
        JournalKind.LIBERA -> "libera"
        JournalKind.RIFLESSIONE -> "riflessione"
        JournalKind.DOMANDA -> "domanda"
        JournalKind.APPUNTO -> "appunto"
    },
    when (kind) {
        JournalKind.LIBERA -> "free"
        JournalKind.RIFLESSIONE -> "reflection"
        JournalKind.DOMANDA -> "question"
        JournalKind.APPUNTO -> "note"
    },
)

@Composable
fun themeText(theme: Theme): String = uiText(
    theme.name.lowercase(),
    when (theme) {
        Theme.FRATERNITA -> "fraternity"
        Theme.PACE -> "peace"
        Theme.UMILTA -> "humility"
        Theme.POVERTA -> "poverty"
        Theme.PERDONO -> "forgiveness"
        Theme.MISERICORDIA -> "mercy"
        Theme.CARITA -> "charity"
        Theme.GRATITUDINE -> "gratitude"
        Theme.GIOIA -> "joy"
        Theme.CREATO -> "creation"
        Theme.PREGHIERA -> "prayer"
        Theme.FEDE -> "faith"
        Theme.COMUNITA -> "community"
        Theme.ACCOGLIENZA -> "welcome"
        Theme.RINUNCIA -> "renunciation"
    },
)
