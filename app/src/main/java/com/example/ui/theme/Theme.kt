package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpaceDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = StarGold,
    tertiary = EmeraldAliged,
    background = DeepSpaceBlack,
    surface = SurfaceDarkBlue,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = OffWhite,
    onSurface = OffWhite,
    surfaceVariant = CardDarkBlue,
    onSurfaceVariant = OffWhite,
    inversePrimary = StarGold,
    error = SignalRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for SatFinder Standard
    dynamicColor: Boolean = false, // Use our professional custom palette
    content: @Composable () -> Unit
) {
    // We enforce our SpaceDarkColorScheme for a unified, high-contrast, professional "Dark mode professionnel"
    MaterialTheme(
        colorScheme = SpaceDarkColorScheme,
        typography = Typography,
        content = content
    )
}
