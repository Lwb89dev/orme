package it.orma.cammino.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.orma.cammino.nostr.NostrAccount
import it.orma.cammino.ui.i18n.uiText
import it.orma.cammino.ui.nostr.NostrLoginPanel
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val titleEnglish: String,
    val text: String,
    val textEnglish: String,
    val illustration: OnboardingIllustration,
    /** L'ultima pagina: spiega l'accesso con Nostr e lo offre, senza imporlo. */
    val login: Boolean = false,
)

private enum class OnboardingIllustration { TAU, FOUR_WAYS, OPEN_BOOK, SHELL, SHARE, KEY }

private val pages = listOf(
    OnboardingPage(
        title = "Chi è Francesco",
        titleEnglish = "Who Is Francis",
        text = "Francesco d'Assisi (1181/82-1226), figlio di un mercante, lasciò una fraternità e degli scritti. Qui si entra da un racconto e si esce sapendo che cosa quei testi dicono davvero.",
        textEnglish = "Francis of Assisi (1181/82–1226), a merchant's son, left a fraternity and his writings. You enter through a story and leave knowing what those texts really say.",
        illustration = OnboardingIllustration.TAU,
    ),
    OnboardingPage(
        title = "Il racconto",
        titleEnglish = "The Story",
        text = "Si comincia dal racconto. Ogni insegnamento è appeso a uno scritto di Francesco o a una fonte dichiarata. Giardino, scelte e domande restano a un passo: non sono un punteggio.",
        textEnglish = "You begin with the story. Each teaching hangs from a writing of Francis or a declared source. Garden, choices and questions stay one step away: they are not a score.",
        illustration = OnboardingIllustration.FOUR_WAYS,
    ),
    OnboardingPage(
        title = "Le fonti contano",
        titleEnglish = "Sources Matter",
        text = "Ogni insegnamento rimanda ai suoi documenti: gli scritti di Francesco, le biografie medievali, i Fioretti. L'app dice sempre da dove viene ogni contenuto e che cosa è racconto di oggi.",
        textEnglish = "Each teaching points to its documents: the writings of Francis, medieval biographies, the Little Flowers. The app always says where each content comes from and what is today's narrative.",
        illustration = OnboardingIllustration.OPEN_BOOK,
    ),
    OnboardingPage(
        title = "Il tuo passo",
        titleEnglish = "Your Step",
        text = "Niente classifiche, niente misura della tua bontà. Quello che scopri resta sul dispositivo, e il diario con lui, salvo ciò che scegli tu di condividere.",
        textEnglish = "No rankings, no measure of your goodness. What you discover stays on the device, and so does the journal, except what you choose to share.",
        illustration = OnboardingIllustration.SHELL,
    ),
    OnboardingPage(
        title = "Condividere è una scelta",
        titleEnglish = "Sharing is a choice",
        text = "Una voce del diario può diventare un testo Markdown su Nostr, una rete aperta senza server centrale: la leggi anche con Boris. Esce solo se attivi l'interruttore su quella voce.",
        textEnglish = "A journal entry can become a Markdown text on Nostr, an open network with no central server: you can also read it with Boris. It leaves only if you switch it on for that entry.",
        illustration = OnboardingIllustration.SHARE,
    ),
    OnboardingPage(
        title = "Accedi con Nostr",
        titleEnglish = "Sign in with Nostr",
        text = "Orme non vede né conserva la tua chiave privata: la firma resta in Amber o nel tuo bunker. L'accesso è facoltativo e, senza, l'app non usa la rete.",
        textEnglish = "Orme never sees or stores your private key: signing stays in Amber or in your bunker. Signing in is optional, and without it the app does not use the network.",
        illustration = OnboardingIllustration.KEY,
        login = true,
    ),
)

@Composable
fun OnboardingScreen(account: NostrAccount, onFinish: () -> Unit) {
    val identity by account.identity.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        // L'app disegna sotto le barre di sistema: senza questo «Salta» finisce sotto la barra di stato.
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Altezza fissa: quando «Salta» sparisce sull'ultima pagina il contenuto non salta in su.
        Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = onFinish) { Text(uiText("Salta", "Skip")) }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                OnboardingIllustrationView(page.illustration)
                Spacer(Modifier.height(28.dp))
                Text(
                    uiText(page.title, page.titleEnglish),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    uiText(page.text, page.textEnglish),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                if (page.login) {
                    Spacer(Modifier.height(18.dp))
                    NostrLoginPanel(account = account, allowLogout = false)
                }
            }
        }

        // Indicatori
        // Colori del tema: il verde bosco fisso spariva sul fondo scuro.
        val activeDot = MaterialTheme.colorScheme.primary
        val idleDot = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "${pagerState.currentPage + 1} / ${pages.size}"
            },
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Canvas(modifier = Modifier.size(if (selected) 12.dp else 8.dp)) {
                    drawCircle(color = if (selected) activeDot else idleDot)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        androidx.compose.material3.Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            val last = pagerState.currentPage >= pages.size - 1
            when {
                !last -> Text(uiText("Avanti", "Next"))
                identity == null -> Text(uiText("Continua senza accedere", "Continue without signing in"))
                else -> Text(uiText("Inizia il cammino", "Begin the journey"))
            }
        }
        Spacer(Modifier.height(8.dp))
        OrnamentalDivider()
    }
}

/** Piccole illustrazioni disegnate via Canvas: nessun asset esterno. */
@Composable
private fun OnboardingIllustrationView(kind: OnboardingIllustration) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = Modifier.size(160.dp)) {
        when (kind) {
            OnboardingIllustration.TAU -> {
                // Croce Tau
                val w = size.width
                drawRect(secondary, topLeft = Offset(w * 0.30f, w * 0.22f), size = Size(w * 0.40f, w * 0.10f))
                drawRect(secondary, topLeft = Offset(w * 0.45f, w * 0.22f), size = Size(w * 0.10f, w * 0.56f))
            }
            OnboardingIllustration.FOUR_WAYS -> {
                // Quattro sentieri che partono da un punto
                val c = Offset(size.width / 2, size.height * 0.75f)
                val targets = listOf(
                    Offset(size.width * 0.15f, size.height * 0.2f),
                    Offset(size.width * 0.4f, size.height * 0.12f),
                    Offset(size.width * 0.6f, size.height * 0.12f),
                    Offset(size.width * 0.85f, size.height * 0.2f),
                )
                targets.forEach { t ->
                    drawLine(tertiary, c, t, strokeWidth = 6f)
                    drawCircle(primary, radius = 12f, center = t)
                }
                drawCircle(secondary, radius = 18f, center = c)
            }
            OnboardingIllustration.OPEN_BOOK -> {
                // Libro aperto
                val w = size.width
                val h = size.height
                drawArc(
                    color = secondary, startAngle = 0f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.30f), size = Size(w * 0.38f, h * 0.5f),
                    style = Stroke(width = 6f),
                )
                drawArc(
                    color = secondary, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(w * 0.50f, h * 0.30f), size = Size(w * 0.38f, h * 0.5f),
                    style = Stroke(width = 6f),
                )
                drawLine(secondary, Offset(w / 2, h * 0.32f), Offset(w / 2, h * 0.78f), strokeWidth = 6f)
            }
            OnboardingIllustration.SHARE -> {
                // Un punto che parte verso una rete aperta: tre nodi, nessun centro
                val c = Offset(size.width * 0.25f, size.height * 0.55f)
                val nodes = listOf(
                    Offset(size.width * 0.70f, size.height * 0.18f),
                    Offset(size.width * 0.85f, size.height * 0.55f),
                    Offset(size.width * 0.70f, size.height * 0.88f),
                )
                nodes.forEach { n -> drawLine(tertiary, c, n, strokeWidth = 5f) }
                drawLine(tertiary, nodes[0], nodes[1], strokeWidth = 3f)
                drawLine(tertiary, nodes[1], nodes[2], strokeWidth = 3f)
                nodes.forEach { n -> drawCircle(primary, radius = 12f, center = n) }
                drawCircle(secondary, radius = 18f, center = c)
            }
            OnboardingIllustration.KEY -> {
                // Una chiave: l'anello e il fusto con due denti
                val w = size.width
                val h = size.height
                drawCircle(secondary, radius = w * 0.14f, center = Offset(w * 0.30f, h * 0.5f), style = Stroke(width = 8f))
                drawLine(secondary, Offset(w * 0.44f, h * 0.5f), Offset(w * 0.86f, h * 0.5f), strokeWidth = 8f)
                drawLine(secondary, Offset(w * 0.72f, h * 0.5f), Offset(w * 0.72f, h * 0.66f), strokeWidth = 8f)
                drawLine(secondary, Offset(w * 0.84f, h * 0.5f), Offset(w * 0.84f, h * 0.62f), strokeWidth = 8f)
            }
            OnboardingIllustration.SHELL -> {
                // Conchiglia del pellegrino
                val c = Offset(size.width / 2, size.height * 0.72f)
                for (i in -2..2) {
                    drawLine(
                        tertiary,
                        c,
                        Offset(size.width / 2 + i * size.width * 0.16f, size.height * 0.18f),
                        strokeWidth = 5f,
                    )
                }
                drawCircle(secondary, radius = 16f, center = c)
                drawArc(
                    color = primary, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(size.width * 0.18f, size.height * 0.16f),
                    size = Size(size.width * 0.64f, size.height * 0.60f),
                    style = Stroke(width = 6f),
                )
            }
        }
    }
}
