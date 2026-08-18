package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CleanMinimalismColorScheme = lightColorScheme(
    primary = GeoTealPrimary,
    onPrimary = GeoTealOnPrimary,
    primaryContainer = GeoTealContainer,
    onPrimaryContainer = GeoTealOnContainer,
    secondary = GeoAzureSecondary,
    onSecondary = GeoAzureOnSecondary,
    secondaryContainer = GeoAzureContainer,
    onSecondaryContainer = GeoAzureOnContainer,
    tertiary = GeoMagentaTertiary,
    onTertiary = GeoMagentaOnTertiary,
    tertiaryContainer = GeoMagentaContainer,
    onTertiaryContainer = GeoMagentaOnContainer,
    background = GeoDarkBackground,
    onBackground = GeoDarkOnBackground,
    surface = GeoDarkSurface,
    onSurface = GeoDarkOnSurface,
    surfaceVariant = GeoDarkSurfaceVariant,
    onSurfaceVariant = GeoDarkOnSurfaceVariant,
    outline = GeoDarkOutline,
    error = GeoRedError
)

@Composable
fun GeoConnectTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CleanMinimalismColorScheme,
        typography = Typography,
        content = content
    )
}

