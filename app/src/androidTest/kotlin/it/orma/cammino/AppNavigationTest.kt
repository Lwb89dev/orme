package it.orma.cammino

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test UI fondamentali: avvio, onboarding, home, navigazione principale.
 * Girano su emulatore/dispositivo (connectedDebugAndroidTest).
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun completaOnboardingSePresente() {
        // Se l'app mostra l'onboarding, lo superiamo con "Salta"
        val skip = composeTestRule.onAllNodes(hasText("Salta")).fetchSemanticsNodes()
        if (skip.isNotEmpty()) {
            composeTestRule.onNodeWithText("Salta").performClick()
        }
    }

    private fun hasText(text: String) = androidx.compose.ui.test.hasText(text)

    @Test
    fun avvio_mostra_onboarding_o_home() {
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodes(hasText("Chi è Francesco")).fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodes(hasText("Il Cammino di Francesco")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun home_mostra_le_quattro_modalita() {
        completaOnboardingSePresente()
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodes(hasText("Le quattro vie")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Il Viandante").assertIsDisplayed()
        composeTestRule.onNodeWithText("Il Giardino").assertIsDisplayed()
        composeTestRule.onNodeWithText("Le Vie della Saggezza").assertIsDisplayed()
        composeTestRule.onNodeWithText("Il Dialogo").assertIsDisplayed()
    }

    @Test
    fun navigazione_viandante_mostra_capitoli() {
        completaOnboardingSePresente()
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodes(hasText("Il Viandante")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Vai al Viandante: avventura narrativa").performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodes(hasText("Il mantello del viandante")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Il mantello del viandante").assertIsDisplayed()
    }

    @Test
    fun navigazione_dialogo_mostra_conversazioni() {
        completaOnboardingSePresente()
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodes(hasText("Il Dialogo")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Vai al Dialogo: conversazioni guidate").performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodes(hasText("Che cosa significa essere umili?")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
