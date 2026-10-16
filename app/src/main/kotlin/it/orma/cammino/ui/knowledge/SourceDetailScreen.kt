package it.orma.cammino.ui.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.core.designsystem.components.PergamenaSection
import it.orma.cammino.core.designsystem.components.SourceBadge
import it.orma.cammino.core.model.Authenticity
import it.orma.cammino.core.model.ReviewStatus
import it.orma.cammino.di.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceDetailScreen(
    container: AppContainer,
    catalog: ContentCatalog,
    documentId: String,
    navController: NavHostController,
) {
    val doc = catalog.documentsById[documentId]
    val passages = catalog.passagesOf(documentId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc?.title ?: "Fonte") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (doc == null) {
                Text("Documento non trovato", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            SourceBadge(doc.category)
            Text(
                doc.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                listOfNotNull(
                    doc.author,
                    doc.compositionDate.ifBlank { null },
                    doc.originalLanguage.ifBlank { null },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                when (doc.authenticity) {
                    Authenticity.AUTHENTIC -> "Attribuzione generalmente accettata"
                    Authenticity.DISPUTED -> "Attribuzione discussa dalla critica"
                    Authenticity.NOT_BY_FRANCIS -> "Non attribuibile a Francesco"
                    Authenticity.LEGENDARY -> "Racconto agiografico"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(doc.description, style = MaterialTheme.typography.bodyLarge)
            if (doc.manuscriptNote.isNotBlank()) {
                Text(doc.manuscriptNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (doc.licenseNote.isNotBlank()) {
                PergamenaSection {
                    Text(
                        "Diritti e licenze",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(doc.licenseNote, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (passages.isNotEmpty()) {
                OrnamentalDivider()
                Text(
                    "Passi citati in questa app",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() },
                )
                passages.forEach { passage ->
                    PergamenaSection {
                        Text(
                            passage.reference,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        if (passage.originalText.isNotBlank()) {
                            Text(
                                passage.originalText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        if (passage.translation.isNotBlank()) {
                            Text(passage.translation, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Traduzione editoriale",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (passage.paraphrase.isNotBlank()) {
                            Text(passage.paraphrase, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Parafrasi editoriale",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (passage.note.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(passage.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (passage.reviewStatus == ReviewStatus.NEEDS_VERIFICATION) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Riferimento in corso di verifica: da non citare letteralmente.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
