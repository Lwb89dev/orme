package it.orma.cammino.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Contenitore base dell'app: carta-pergamena con bordo inchiostro tenue
 * e angoli appena tagliati, come una pagina miniata.
 */
@Composable
fun PergamenaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val shape = MaterialTheme.shapes.medium
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, border = border, shape = shape) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier, colors = colors, border = border, shape = shape) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

/** Contenitore a tutta larghezza per gli approfondimenti. */
@Composable
fun PergamenaSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    PergamenaCard(modifier = modifier.fillMaxWidth(), content = content)
}
