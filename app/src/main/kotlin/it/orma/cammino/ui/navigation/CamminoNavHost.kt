package it.orma.cammino.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import it.orma.cammino.content.ContentLoadResult
import it.orma.cammino.core.content.ContentCatalog
import it.orma.cammino.di.AppContainer
import it.orma.cammino.ui.cards.CardPathsScreen
import it.orma.cammino.ui.cards.CardSessionScreen
import it.orma.cammino.ui.dialogue.ConversationScreen
import it.orma.cammino.ui.dialogue.DialogueListScreen
import it.orma.cammino.ui.garden.ContemplationScreen
import it.orma.cammino.ui.garden.GardenScreen
import it.orma.cammino.ui.home.HomeScreen
import it.orma.cammino.ui.journal.JournalEntryScreen
import it.orma.cammino.ui.journal.JournalScreen
import it.orma.cammino.ui.knowledge.KnowledgeScreen
import it.orma.cammino.ui.knowledge.SourceDetailScreen
import it.orma.cammino.ui.knowledge.TeachingDetailScreen
import it.orma.cammino.ui.settings.SettingsScreen
import it.orma.cammino.ui.viandante.ChapterScreen
import it.orma.cammino.ui.viandante.ViandanteMapScreen

@Composable
fun CamminoNavHost(
    container: AppContainer,
    catalog: ContentCatalog,
    contentIssues: List<it.orma.cammino.core.content.ContentIssue>,
    navController: NavHostController = rememberNavController(),
) {
    val reduceAnimations = LocalReduceAnimations.current
    val enter = if (reduceAnimations) fadeIn(tween(0)) else fadeIn(tween(220))
    val exit = if (reduceAnimations) fadeOut(tween(0)) else fadeOut(tween(220))

    NavHost(
        navController = navController,
        startDestination = Route.Home,
        enterTransition = { enter },
        exitTransition = { exit },
    ) {
        composable<Route.Home> {
            HomeScreen(container = container, catalog = catalog, navController = navController)
        }
        composable<Route.ViandanteMap> {
            ViandanteMapScreen(container = container, catalog = catalog, navController = navController)
        }
        composable<Route.Chapter> { entry ->
            val route = entry.toRoute<Route.Chapter>()
            ChapterScreen(container = container, catalog = catalog, chapterId = route.chapterId, navController = navController)
        }
        composable<Route.Garden> {
            GardenScreen(container = container, catalog = catalog, navController = navController)
        }
        composable<Route.Contemplation> { entry ->
            val route = entry.toRoute<Route.Contemplation>()
            ContemplationScreen(container = container, catalog = catalog, contemplationId = route.contemplationId, navController = navController)
        }
        composable<Route.CardPaths> {
            CardPathsScreen(container = container, catalog = catalog, navController = navController)
        }
        composable<Route.CardPathSession> { entry ->
            val route = entry.toRoute<Route.CardPathSession>()
            CardSessionScreen(container = container, catalog = catalog, pathId = route.pathId, navController = navController)
        }
        composable<Route.DialogueList> {
            DialogueListScreen(container = container, catalog = catalog, navController = navController)
        }
        composable<Route.Conversation> { entry ->
            val route = entry.toRoute<Route.Conversation>()
            ConversationScreen(container = container, catalog = catalog, conversationId = route.conversationId, navController = navController)
        }
        composable<Route.Knowledge> { entry ->
            val route = entry.toRoute<Route.Knowledge>()
            KnowledgeScreen(
                container = container,
                catalog = catalog,
                navController = navController,
                initialSection = route.section,
            )
        }
        composable<Route.TeachingDetail> { entry ->
            val route = entry.toRoute<Route.TeachingDetail>()
            TeachingDetailScreen(container = container, catalog = catalog, teachingId = route.teachingId, navController = navController)
        }
        composable<Route.SourceDetail> { entry ->
            val route = entry.toRoute<Route.SourceDetail>()
            SourceDetailScreen(container = container, catalog = catalog, documentId = route.documentId, navController = navController)
        }
        composable<Route.Journal> {
            JournalScreen(container = container, catalog = catalog, navController = navController)
        }
        composable<Route.JournalEntry> { entry ->
            val route = entry.toRoute<Route.JournalEntry>()
            JournalEntryScreen(
                container = container,
                catalog = catalog,
                entryId = route.entryId,
                teachingId = route.teachingId,
                chapterId = route.chapterId,
                navController = navController,
            )
        }
        composable<Route.Settings> {
            SettingsScreen(container = container, catalog = catalog, contentIssues = contentIssues, navController = navController)
        }
    }
}
