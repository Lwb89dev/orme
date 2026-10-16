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
import it.orma.cammino.ui.i18n.uiText
import it.orma.cammino.ui.i18n.sourceCategoryText

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
                title = { Text(doc?.title ?: uiText("Fonte", "Source")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = uiText("Indietro", "Back"))
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
                Text(uiText("Documento non trovato", "Document not found"), color = MaterialTheme.colorScheme.error)
                return@Column
            }

            SourceBadge(doc.category, label = sourceCategoryText(doc.category))
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
                    Authenticity.AUTHENTIC -> uiText("Attribuzione generalmente accettata", "Generally accepted attribution")
                    Authenticity.DISPUTED -> uiText("Attribuzione discussa dalla critica", "Attribution debated by scholarship")
                    Authenticity.NOT_BY_FRANCIS -> uiText("Non attribuibile a Francesco", "Not attributable to Francis")
                    Authenticity.LEGENDARY -> uiText("Racconto agiografico", "Hagiographic tale")
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
                        uiText("Diritti e licenze", "Rights and licences"),
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
                    uiText("Passi citati in questa app", "Passages cited in this app"),
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
                                uiText("Traduzione editoriale", "Editorial translation"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (passage.paraphrase.isNotBlank()) {
                            Text(passage.paraphrase, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                uiText("Parafrasi editoriale", "Editorial paraphrase"),
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
                                uiText("Riferimento in corso di verifica: da non citare letteralmente.", "Reference under review: do not quote literally."),
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
