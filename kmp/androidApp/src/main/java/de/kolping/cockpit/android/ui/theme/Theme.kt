package de.kolping.cockpit.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006493),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF50606E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4F5),
    onSecondaryContainer = Color(0xFF0C1D29),
    tertiary = Color(0xFF65587B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF201634),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E),
    inverseOnSurface = Color(0xFFF1F0F4),
    inverseSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF90CEFF)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CEFF),
    onPrimary = Color(0xFF003450),
    primaryContainer = Color(0xFF004B71),
    onPrimaryContainer = Color(0xFFCAE6FF),
    secondary = Color(0xFFB7C8D9),
    onSecondary = Color(0xFF22323F),
    secondaryContainer = Color(0xFF384956),
    onSecondaryContainer = Color(0xFFD3E4F5),
    tertiary = Color(0xFFCFC0E8),
    onTertiary = Color(0xFF362B4A),
    tertiaryContainer = Color(0xFF4D4162),
    onTertiaryContainer = Color(0xFFEBDDFF),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198),
    inverseOnSurface = Color(0xFF1A1C1E),
    inverseSurface = Color(0xFFE2E2E5),
    inversePrimary = Color(0xFF006493)
)

@Composable
fun KolpingCockpitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
