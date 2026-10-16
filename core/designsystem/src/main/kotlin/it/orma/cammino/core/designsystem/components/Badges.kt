package it.orma.cammino.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.orma.cammino.core.model.SourceCategory
import it.orma.cammino.core.model.TeachingStatus

/** Etichetta italiana della categoria di fonte. */
fun SourceCategory.label(): String = when (this) {
    SourceCategory.SCRITTO_AUTENTICO -> "Scritto di Francesco"
    SourceCategory.TESTIMONIANZA_MEDIEVALE -> "Testimonianza medievale"
    SourceCategory.TRADIZIONE_AGIOGRAFICA -> "Tradizione agiografica"
    SourceCategory.INTERPRETAZIONE_MODERNA -> "Nota editoriale"
    SourceCategory.NARRAZIONE_ORIGINALE -> "Narrazione del gioco"
}

/**
 * Badge che dichiara sempre la provenienza di un contenuto.
 * Il colore supporta la distinzione ma il testo è sempre presente
 * (nessuna informazione esclusivamente cromatica).
 */
@Composable
fun SourceBadge(category: SourceCategory, modifier: Modifier = Modifier) {
    val background: Color
    val content: Color
    val scheme = MaterialTheme.colorScheme
    when (category) {
        SourceCategory.SCRITTO_AUTENTICO -> { background = scheme.primary; content = scheme.onPrimary }
        SourceCategory.TESTIMONIANZA_MEDIEVALE -> { background = scheme.secondary; content = scheme.onSecondary }
        SourceCategory.TRADIZIONE_AGIOGRAFICA -> { background = scheme.tertiary; content = scheme.onTertiary }
        SourceCategory.INTERPRETAZIONE_MODERNA -> { background = scheme.surfaceVariant; content = scheme.onSurfaceVariant }
        SourceCategory.NARRAZIONE_ORIGINALE -> { background = scheme.surfaceVariant; content = scheme.onSurfaceVariant }
    }
    Surface(color = background, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = category.label(),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

fun TeachingStatus.label(): String = when (this) {
    TeachingStatus.SCOPERTO -> "Scoperto"
    TeachingStatus.ESPLORATO -> "Esplorato"
    TeachingStatus.APPROFONDITO -> "Approfondito"
}

/** Stato di esperienza di un insegnamento (non una valutazione morale). */
@Composable
fun TeachingStatusChip(status: TeachingStatus?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (background, content, text) = when (status) {
        null -> Triple(scheme.surfaceVariant, scheme.onSurfaceVariant, "Da scoprire")
        TeachingStatus.SCOPERTO -> Triple(scheme.tertiary, scheme.onTertiary, status.label())
        TeachingStatus.ESPLORATO -> Triple(scheme.secondary, scheme.onSecondary, status.label())
        TeachingStatus.APPROFONDITO -> Triple(scheme.primary, scheme.onPrimary, status.label())
    }
    Surface(color = background, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
