package it.orma.cammino.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import it.orma.cammino.core.database.repository.JournalKind
import it.orma.cammino.core.designsystem.components.label
import it.orma.cammino.core.model.SourceCategory
import it.orma.cammino.core.model.TeachingStatus
import it.orma.cammino.core.model.Theme
import kotlinx.serialization.json.Json

/** Lingue dell'interfaccia. Il gaelico irlandese e il maltese sono esclusi; il russo è aggiunto. */
val APP_LANGUAGES: List<Pair<String, String>> = listOf(
    "it" to "Italiano",
    "en" to "English",
    "bg" to "Български",
    "cs" to "Čeština",
    "da" to "Dansk",
    "de" to "Deutsch",
    "el" to "Ελληνικά",
    "es" to "Español",
    "et" to "Eesti",
    "fi" to "Suomi",
    "fr" to "Français",
    "hr" to "Hrvatski",
    "hu" to "Magyar",
    "lt" to "Lietuvių",
    "lv" to "Latviešu",
    "nl" to "Nederlands",
    "pl" to "Polski",
    "pt" to "Português",
    "ro" to "Română",
    "ru" to "Русский",
    "sk" to "Slovenčina",
    "sl" to "Slovenščina",
    "sv" to "Svenska",
)

/**
 * La prima lingua del dispositivo che l'app distribuisce, nell'ordine di preferenza di Android;
 * l'inglese se nessuna corrisponde.
 */
fun deviceLanguage(): String {
    val supported = APP_LANGUAGES.map { it.first }.toSet()
    val locales = android.os.LocaleList.getDefault()
    for (index in 0 until locales.size()) {
        val language = locales[index].language.lowercase()
        if (language in supported) return language
    }
    return "en"
}

/** Traduzioni dell'interfaccia per la lingua corrente, con chiave italiana. Vuota per it/en. */
val LocalUiExtras = compositionLocalOf<Map<String, String>> { emptyMap() }

private val uiJson = Json { ignoreUnknownKeys = true }

fun loadUiExtras(context: Context, language: String): Map<String, String> {
    if (language == "it" || language == "en") return emptyMap()
    return runCatching {
        context.assets.open("content/ui/$language.json").bufferedReader().use { it.readText() }
    }.mapCatching { text ->
        uiJson.decodeFromString<Map<String, String>>(text)
    }.getOrDefault(emptyMap())
}

/** Language of the app chrome. Content itself is loaded from the selected catalog. */
val LocalAppLanguage = compositionLocalOf { "en" }

@Composable
fun uiText(italian: String, english: String): String {
    val language = LocalAppLanguage.current
    if (language == "it") return italian
    if (language == "en") return english
    return LocalUiExtras.current[italian] ?: english
}

/**
 * Come [uiText], per un testo con valori dentro. La chiave è il modello italiano con segnaposto `{0}`, `{1}`…,
 * così la traduzione si trova sempre; i valori si inseriscono dopo. Un testo con un valore già interpolato
 * nella chiave non verrebbe mai trovato nei file `content/ui` e ricadrebbe sull'inglese.
 */
@Composable
fun uiFormat(italian: String, english: String, vararg args: Any?): String {
    var text = uiText(italian, english)
    args.forEachIndexed { index, value -> text = text.replace("{$index}", value?.toString().orEmpty()) }
    return text
}

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

/**
 * Il nome del tema. In italiano porta l'accento («povertà», non «poverta»); per le altre lingue la chiave
 * delle traduzioni resta il nome tecnico senza accento, com'è nei file `content/ui`.
 */
@Composable
fun themeText(theme: Theme): String {
    if (LocalAppLanguage.current == "it") {
        return when (theme) {
            Theme.FRATERNITA -> "fraternità"
            Theme.UMILTA -> "umiltà"
            Theme.POVERTA -> "povertà"
            Theme.CARITA -> "carità"
            Theme.COMUNITA -> "comunità"
            else -> theme.name.lowercase()
        }
    }
    return themeTranslation(theme)
}

@Composable
private fun themeTranslation(theme: Theme): String = uiText(
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
