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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.orma.cammino.core.designsystem.components.OrnamentalDivider
import it.orma.cammino.ui.i18n.uiText
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val titleEnglish: String,
    val text: String,
    val textEnglish: String,
    val illustration: OnboardingIllustration,
)

private enum class OnboardingIllustration { TAU, FOUR_WAYS, OPEN_BOOK, SHELL }

private val pages = listOf(
    OnboardingPage(
        title = "Chi è Francesco",
        titleEnglish = "Who Is Francis",
        text = "Francesco d'Assisi (1181/82-1226), figlio di un mercante, lasciò una fraternità e un corpus di scritti. Qui si entra da un racconto, e si esce sapendo che cosa quei testi dicono davvero.",
        textEnglish = "Francis of Assisi (1181/82–1226), a merchant's son, left a fraternity and a body of writings. You enter through a story, and leave knowing what those texts actually say.",
        illustration = OnboardingIllustration.TAU,
    ),
    OnboardingPage(
        title = "Il racconto",
        titleEnglish = "The Story",
        text = "Si comincia dal racconto. Ogni insegnamento è appeso a uno scritto di Francesco o a una fonte dichiarata. Giardino, scelte e domande restano aperti se vuoi fermarti: non sono un punteggio e non misurano la tua vita.",
        textEnglish = "You begin with the story. Each teaching hangs from a writing of Francis or from a declared source. Garden, choices and questions stay open if you want to pause: they are not a score and they do not measure your life.",
        illustration = OnboardingIllustration.FOUR_WAYS,
    ),
    OnboardingPage(
        title = "Le fonti contano",
        titleEnglish = "Sources Matter",
        text = "Ogni insegnamento è legato ai suoi documenti: gli scritti di Francesco, le biografie medievali, la tradizione dei Fioretti. L'app dichiara sempre da dove viene ogni contenuto, e cosa è racconto di oggi.",
        textEnglish = "Each teaching is linked to its documents: Francis's writings, medieval biographies, and the tradition of the Little Flowers. The app always tells you where content comes from and what is today's narrative.",
        illustration = OnboardingIllustration.OPEN_BOOK,
    ),
    OnboardingPage(
        title = "Il tuo passo",
        titleEnglish = "Your Step",
        text = "Niente account, niente classifiche, niente misura della tua bontà. Quello che scopri resta sul dispositivo. Il diario non esce da qui.",
        textEnglish = "No account, no rankings, no measure of your goodness. What you discover stays on the device. The journal does not leave it.",
        illustration = OnboardingIllustration.SHELL,
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
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
            }
        }

        // Indicatori
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Canvas(modifier = Modifier.size(if (selected) 10.dp else 8.dp)) {
                    drawCircle(
                        color = if (selected) androidx.compose.ui.graphics.Color(0xFF344E41) else androidx.compose.ui.graphics.Color(0xFFB69B60),
                    )
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
            Text(uiText(if (pagerState.currentPage < pages.size - 1) "Avanti" else "Inizia il cammino", if (pagerState.currentPage < pages.size - 1) "Next" else "Begin the journey"))
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
