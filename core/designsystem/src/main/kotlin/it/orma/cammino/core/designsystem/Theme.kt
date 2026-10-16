package it.orma.cammino.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val CamminoLightColors = lightColorScheme(
    primary = VerdeBosco,
    onPrimary = AvorioCaldo,
    primaryContainer = PergamenaScura,
    onPrimaryContainer = VerdeBosco,
    secondary = Terracotta,
    onSecondary = AvorioCaldo,
    secondaryContainer = Pergamena,
    onSecondaryContainer = TerracottaProfonda,
    tertiary = OroSpento,
    onTertiary = MarroneInchiostro,
    background = AvorioCaldo,
    onBackground = MarroneInchiostro,
    surface = Pergamena,
    onSurface = MarroneInchiostro,
    surfaceVariant = PergamenaScura,
    onSurfaceVariant = InchiostroTenue,
    outline = InchiostroTenue,
    outlineVariant = PergamenaScura,
)

val CamminoDarkColors = darkColorScheme(
    primary = OlivaNotte,
    onPrimary = NotteFondo,
    primaryContainer = NotteContenitore,
    onPrimaryContainer = CandelaAvorio,
    secondary = TerracottaNotte,
    onSecondary = NotteFondo,
    secondaryContainer = NotteContenitore,
    onSecondaryContainer = TerracottaNotte,
    tertiary = OroNotte,
    onTertiary = NotteFondo,
    background = NotteFondo,
    onBackground = CandelaAvorio,
    surface = NotteSuperficie,
    onSurface = CandelaAvorio,
    surfaceVariant = NotteContenitore,
    onSurfaceVariant = CandelaTenue,
    outline = CandelaTenue,
    outlineVariant = NotteContenitore,
)

@Composable
fun CamminoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) CamminoDarkColors else CamminoLightColors,
        typography = CamminoTypography,
        content = content,
    )
}
