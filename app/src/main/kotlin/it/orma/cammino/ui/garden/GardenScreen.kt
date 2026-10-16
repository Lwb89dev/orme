package it.orma.cammino.ui.garden

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
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
import it.orma.cammino.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

private data class GardenPlacement(
    @param:DrawableRes val artworkRes: Int,
    val x: Float,
    val baselineY: Float,
    val width: Float,
    val height: Float,
    val swayMultiplier: Float = 0f,
)

/** Posizione e ingombro relativi (0..1) nella scena 4:3. */
private val elementPlacements = mapOf(
    "garden.ulivo" to GardenPlacement(R.drawable.art_garden_olive, 0.17f, 0.68f, 0.28f, 0.45f, 0.20f),
    "garden.fontana" to GardenPlacement(R.drawable.art_garden_fountain, 0.55f, 0.65f, 0.20f, 0.18f),
    "garden.rose" to GardenPlacement(R.drawable.art_garden_roses, 0.83f, 0.79f, 0.20f, 0.28f, 0.35f),
    "garden.orto" to GardenPlacement(R.drawable.art_garden_vegetable_bed, 0.18f, 0.82f, 0.30f, 0.20f),
    "garden.cipresso" to GardenPlacement(R.drawable.art_garden_cypress, 0.89f, 0.58f, 0.12f, 0.46f, 0.12f),
    "garden.nido" to GardenPlacement(R.drawable.art_garden_finch_nest, 0.24f, 0.44f, 0.20f, 0.15f, 0.10f),
    "garden.panca" to GardenPlacement(R.drawable.art_garden_stone_bench, 0.60f, 0.88f, 0.25f, 0.14f),
    "garden.girasoli" to GardenPlacement(R.drawable.art_garden_sunflowers, 0.38f, 0.78f, 0.18f, 0.30f, 0.45f),
    "garden.api" to GardenPlacement(R.drawable.art_garden_beehive, 0.75f, 0.68f, 0.15f, 0.23f),
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
// Sfondo pittorico ottimizzato con elementi di progressione interattivi in overlay.
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

    BoxWithConstraints(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .pointerInput(unlockedIds) {
            detectTapGestures { offset ->
                val tapped = elementPlacements.entries
                    .filter { it.key in unlockedIds }
                    .minByOrNull { (_, placement) ->
                        val dx = placement.x * size.width - offset.x
                        val dy = placement.baselineY * size.height - offset.y
                        dx * dx + dy * dy
                    }
                if (tapped != null) {
                    val threshold = size.width * 0.16f
                    val dx = tapped.value.x * size.width - offset.x
                    val dy = tapped.value.baselineY * size.height - offset.y
                    if (dx * dx + dy * dy < threshold * threshold) {
                        catalog.gardenElementsById[tapped.key]?.let(onElementTap)
                    }
                }
            }
        },
    ) {
        Image(
            painter = painterResource(R.drawable.art_garden_base),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        catalog.gardenElements.forEach { element ->
            val placement = elementPlacements[element.id] ?: return@forEach
            if (element.id in unlockedIds) {
                val artworkWidth = maxWidth * placement.width
                val artworkHeight = maxHeight * placement.height
                Image(
                    painter = painterResource(placement.artworkRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .offset(
                            x = maxWidth * placement.x - artworkWidth / 2,
                            y = maxHeight * placement.baselineY - artworkHeight,
                        )
                        .size(artworkWidth, artworkHeight)
                        .graphicsLayer {
                            rotationZ = sway * placement.swayMultiplier
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        },
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            catalog.gardenElements.forEach { element ->
                if (element.id == "garden.sentiero") return@forEach
                val placement = elementPlacements[element.id] ?: return@forEach
                if (element.id !in unlockedIds) {
                    val center = Offset(placement.x * size.width, placement.baselineY * size.height)
                    drawSeedMarker(center)
                }
            }
        }
    }
}

private fun DrawScope.drawSeedMarker(center: Offset) {
    drawCircle(Color(0x66F2E8D5), radius = size.width * 0.02f, center = center)
    drawCircle(Color(0x66F2E8D5), radius = size.width * 0.02f, center = center, style = Stroke(width = 2f))
}
