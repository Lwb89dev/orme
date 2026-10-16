package it.orma.cammino.ui.garden

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.core.content.schema.Contemplation
import it.orma.cammino.core.content.schema.GardenElement
import it.orma.cammino.core.designsystem.components.PergamenaCard
import it.orma.cammino.core.progression.KnowledgeEngine
import it.orma.cammino.core.progression.ProgressionState
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.navigation.LocalReduceAnimations
import it.orma.cammino.ui.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class GardenUiState(
    val loading: Boolean = true,
    val unlockedIds: Set<String> = emptySet(),
    val contemplations: List<ContemplationRow> = emptyList(),
)

data class ContemplationRow(
    val contemplation: Contemplation,
    val available: Boolean,
    val completed: Boolean,
)

class GardenViewModel(container: AppContainer, private val catalog: ContentCatalog) : ViewModel() {

    private val engine = KnowledgeEngine(catalog)
    private val _state = MutableStateFlow(GardenUiState())
    val state: StateFlow<GardenUiState> = _state

    init {
        viewModelScope.launch {
            val repo = container.progressionRepository ?: return@launch
            repo.state.collect { progression -> _state.value = build(progression) }
        }
    }

    private fun build(progression: ProgressionState): GardenUiState = GardenUiState(
        loading = false,
        unlockedIds = catalog.gardenElements
            .filter { engine.isGardenElementUnlocked(progression, it.id) }
            .map { it.id }.toSet(),
        contemplations = catalog.contemplations.map { c ->
            ContemplationRow(
                contemplation = c,
                available = engine.isContemplationAvailable(progression, c.id),
                completed = c.id in progression.completedContemplations,
            )
        },
    )

    companion object {
        fun factory(container: AppContainer, catalog: ContentCatalog): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GardenViewModel(container, catalog) as T
            }
    }
}

/** Posizione relativa (0..1) di ogni elemento nella scena. */
private val elementPositions = mapOf(
    "garden.sentiero" to (0.50f to 0.92f),
    "garden.ulivo" to (0.18f to 0.52f),
    "garden.fontana" to (0.52f to 0.60f),
    "garden.rose" to (0.83f to 0.72f),
    "garden.orto" to (0.14f to 0.80f),
    "garden.cipresso" to (0.88f to 0.42f),
    "garden.nido" to (0.24f to 0.42f),
    "garden.panca" to (0.62f to 0.82f),
    "garden.girasoli" to (0.36f to 0.74f),
    "garden.api" to (0.72f to 0.52f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(container: AppContainer, catalog: ContentCatalog, navController: NavHostController) {
    val vm: GardenViewModel = viewModel(factory = GardenViewModel.factory(container, catalog))
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedElement by remember { mutableStateOf<GardenElement?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Il Giardino di Francesco") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Il giardino cresce con ciò che scopri lungo le vie. Tocca gli elementi per conoscerne il simbolismo.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val unlockedCount = state.unlockedIds.size
            val total = catalog.gardenElements.size
            GardenScene(
                catalog = catalog,
                unlockedIds = state.unlockedIds,
                onElementTap = { selectedElement = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .semantics {
                        contentDescription = "Scena del giardino: $unlockedCount elementi su $total sbocciati"
                    },
            )

            Spacer(Modifier.height(4.dp))
            Text(
                "Contemplazioni",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            state.contemplations.forEach { row ->
                PergamenaCard(
                    onClick = if (row.available) ({ navController.navigate(Route.Contemplation(row.contemplation.id)) }) else null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(row.contemplation.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            row.completed -> "Già vissuta — puoi riviverla quando vuoi"
                            row.available -> "Disponibile"
                            else -> "Si apre scoprendo l'insegnamento collegato"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    selectedElement?.let { element ->
        val teaching = element.teachingId?.let { catalog.teachingsById[it] }
        AlertDialog(
            onDismissRequest = { selectedElement = null },
            title = { Text(element.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(element.description, style = MaterialTheme.typography.bodyMedium)
                    Text(element.symbolism, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                if (teaching != null) {
                    TextButton(onClick = {
                        selectedElement = null
                        navController.navigate(Route.TeachingDetail(teaching.id))
                    }) { Text("L'insegnamento") }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedElement = null }) { Text("Chiudi") }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Scena disegnata con Canvas: nessun asset bitmap, tutto vettoriale.
// ---------------------------------------------------------------------------

@Composable
private fun GardenScene(
    catalog: ContentCatalog,
    unlockedIds: Set<String>,
    onElementTap: (GardenElement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceAnimations = LocalReduceAnimations.current
    val sway: Float
    if (reduceAnimations) {
        sway = 0f
    } else {
        val transition = rememberInfiniteTransition(label = "garden")
        sway = transition.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "sway",
        ).value
    }

    Canvas(
        modifier = modifier.pointerInput(unlockedIds) {
            detectTapGestures { offset ->
                val tapped = elementPositions.entries
                    .filter { it.key in unlockedIds }
                    .minByOrNull { (_, pos) ->
                        val dx = pos.first * size.width - offset.x
                        val dy = pos.second * size.height - offset.y
                        dx * dx + dy * dy
                    }
                if (tapped != null) {
                    val threshold = size.width * 0.16f
                    val dx = tapped.value.first * size.width - offset.x
                    val dy = tapped.value.second * size.height - offset.y
                    if (dx * dx + dy * dy < threshold * threshold) {
                        catalog.gardenElementsById[tapped.key]?.let(onElementTap)
                    }
                }
            }
        },
    ) {
        drawSkyAndGround()
        catalog.gardenElements.forEach { element ->
            val pos = elementPositions[element.id] ?: return@forEach
            val center = Offset(pos.first * size.width, pos.second * size.height)
            if (element.id in unlockedIds) {
                drawElement(element, center, sway)
            } else {
                drawSeedMarker(center)
            }
        }
    }
}

private fun DrawScope.drawSkyAndGround() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (skyTop, skyBottom) = when (hour) {
        in 6..11 -> Color(0xFFCFDFE8) to Color(0xFFEFE8D2)
        in 12..17 -> Color(0xFFC4D8E4) to Color(0xFFF2E8D5)
        in 18..20 -> Color(0xFFD9B08C) to Color(0xFFF2E0C8)
        else -> Color(0xFF2A3242) to Color(0xFF4A4A52)
    }
    drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)), size = size)
    // Sole o luna
    val t = ((hour % 24) / 24f)
    val celestial = Offset(size.width * (0.15f + 0.7f * t), size.height * (0.10f + 0.08f * kotlin.math.sin(t * Math.PI).toFloat()))
    if (hour in 6..20) {
        drawCircle(Color(0xFFE8C977), radius = size.width * 0.045f, center = celestial)
    } else {
        drawCircle(Color(0xFFEDE4D0), radius = size.width * 0.035f, center = celestial)
    }
    // Colline
    val hills = Path().apply {
        moveTo(0f, size.height * 0.62f)
        quadraticTo(size.width * 0.3f, size.height * 0.5f, size.width * 0.62f, size.height * 0.6f)
        quadraticTo(size.width * 0.85f, size.height * 0.68f, size.width, size.height * 0.58f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(hills, Color(0xFF697B4B))
    val front = Path().apply {
        moveTo(0f, size.height * 0.78f)
        quadraticTo(size.width * 0.5f, size.height * 0.68f, size.width, size.height * 0.80f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(front, Color(0xFF596E3F))
}

private fun DrawScope.drawSeedMarker(center: Offset) {
    drawCircle(Color(0x66F2E8D5), radius = size.width * 0.02f, center = center)
    drawCircle(Color(0x66F2E8D5), radius = size.width * 0.02f, center = center, style = Stroke(width = 2f))
}

private fun DrawScope.drawElement(element: GardenElement, center: Offset, sway: Float) {
    val u = size.width / 100f  // unità relativa
    when (element.id) {
        "garden.sentiero" -> {
            val path = Path().apply {
                moveTo(center.x - 6 * u, size.height)
                quadraticTo(center.x + 4 * u, center.y, center.x - 2 * u, size.height * 0.66f)
            }
            drawPath(path, Color(0xFFCBBC9E), style = Stroke(width = 3.5f * u))
        }
        "garden.ulivo" -> {
            drawLine(Color(0xFF6B5138), center, center.copy(y = center.y - 10 * u), strokeWidth = 1.6f * u)
            rotate(degrees = sway, pivot = center.copy(y = center.y - 10 * u)) {
                drawCircle(Color(0xFF8A9A66), radius = 7 * u, center = center.copy(y = center.y - 13 * u))
                drawCircle(Color(0xFF697B4B), radius = 4.5f * u, center = center.copy(x = center.x - 4 * u, y = center.y - 10 * u))
            }
        }
        "garden.fontana" -> {
            drawCircle(Color(0xFF8C8578), radius = 5 * u, center = center)
            drawCircle(Color(0xFF9FC2CC), radius = 3.6f * u, center = center)
            drawCircle(Color(0x66FFFFFF), radius = (1.2f + sway * 0.2f) * u, center = center.copy(y = center.y - 2 * u))
        }
        "garden.rose" -> {
            for (i in 0..4) {
                val a = i * 1.256f + sway * 0.02f
                val p = center + Offset(kotlin.math.cos(a) * 3 * u, kotlin.math.sin(a) * 2 * u)
                drawCircle(Color(0xFFB95C62), radius = 1.5f * u, center = p)
            }
            drawCircle(Color(0xFF697B4B), radius = 4.4f * u, center = center, style = Stroke(width = 1f * u))
        }
        "garden.orto" -> {
            for (r in 0..2) {
                val y = center.y + r * 1.8f * u
                drawLine(Color(0xFF7A5C3E), Offset(center.x - 6 * u, y), Offset(center.x + 6 * u, y), strokeWidth = 0.9f * u)
                for (cIdx in 0..4) {
                    drawCircle(Color(0xFF8A9A66), radius = 0.7f * u, center = Offset(center.x - 5 * u + cIdx * 2.5f * u, y - 0.9f * u))
                }
            }
        }
        "garden.cipresso" -> {
            rotate(degrees = sway * 0.4f, pivot = center) {
                val cyp = Path().apply {
                    moveTo(center.x, center.y - 16 * u)
                    quadraticTo(center.x + 2.6f * u, center.y - 8 * u, center.x + 1.6f * u, center.y)
                    lineTo(center.x - 1.6f * u, center.y)
                    quadraticTo(center.x - 2.6f * u, center.y - 8 * u, center.x, center.y - 16 * u)
                    close()
                }
                drawPath(cyp, Color(0xFF2F4536))
            }
        }
        "garden.nido" -> {
            drawArc(Color(0xFF6B5138), 0f, 180f, false, topLeft = Offset(center.x - 3 * u, center.y - 1.5f * u), size = Size(6 * u, 3 * u), style = Stroke(width = 0.8f * u))
            val bx = center.x + sway * 0.8f * u
            drawCircle(Color(0xFFEDE4D0), radius = 0.9f * u, center = Offset(bx, center.y - 3.2f * u))
            drawCircle(Color(0xFF40382F), radius = 0.25f * u, center = Offset(bx + 0.5f * u, center.y - 3.4f * u))
        }
        "garden.panca" -> {
            drawRect(Color(0xFF8C8578), topLeft = Offset(center.x - 5 * u, center.y - 1.2f * u), size = Size(10 * u, 1.4f * u))
            drawRect(Color(0xFF6E675C), topLeft = Offset(center.x - 4 * u, center.y + 0.2f * u), size = Size(1.2f * u, 2 * u))
            drawRect(Color(0xFF6E675C), topLeft = Offset(center.x + 2.8f * u, center.y + 0.2f * u), size = Size(1.2f * u, 2 * u))
        }
        "garden.girasoli" -> {
            for (i in 0..2) {
                val x = center.x + (i - 1) * 3 * u
                drawLine(Color(0xFF596E3F), Offset(x, center.y), Offset(x, center.y - 4 * u), strokeWidth = 0.5f * u)
                rotate(degrees = sway * 1.5f, pivot = Offset(x, center.y - 4.6f * u)) {
                    drawCircle(Color(0xFFB69B60), radius = 1.6f * u, center = Offset(x, center.y - 4.6f * u))
                    drawCircle(Color(0xFF6B4F2C), radius = 0.7f * u, center = Offset(x, center.y - 4.6f * u))
                }
            }
        }
        "garden.api" -> {
            drawRect(Color(0xFFB69B60), topLeft = Offset(center.x - 3 * u, center.y - 4 * u), size = Size(6 * u, 4.6f * u))
            drawLine(Color(0xFF6B4F2C), Offset(center.x - 3 * u, center.y - 2 * u), Offset(center.x + 3 * u, center.y - 2 * u), strokeWidth = 0.4f * u)
            drawCircle(Color(0xFF40382F), radius = 0.5f * u, center = Offset(center.x, center.y - 0.5f * u))
            val bee = Offset(center.x + 4 * u + sway * u, center.y - 6 * u)
            drawCircle(Color(0xFFE8C977), radius = 0.6f * u, center = bee)
        }
    }
}
