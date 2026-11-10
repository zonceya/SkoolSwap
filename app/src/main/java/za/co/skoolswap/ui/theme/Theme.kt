package za.co.skoolswap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ✅ SKOOLSWAP COMPOSE THEME
 *
 * Wraps Compose UI in Material 3 theme.
 * Mirrors your XML theme (themes.xml) so chat looks consistent.
 *
 * WHY:
 * - Compose doesn't read XML themes
 * - We define colors here as Kotlin objects
 * - All ChatScreen composables use MaterialTheme.colorScheme.*
 */

// ─────────────────────────────────────────
// COLOR PALETTE (matches your themes.xml)
// ─────────────────────────────────────────

// Brand colors
private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)
private val Teal200 = Color(0xFF80CBC4)
private val Teal700 = Color(0xFF00796B)
private val LightBackground = Color(0xFFF7F7F7)
private val SurfaceVariant = Color(0xFFEEEEEE)
private val OnSurfaceVariant = Color(0xFF666666)

// Chat-specific colors
private val BubbleMine = Color(0xFF1F1F1F)      // dark bubble (my messages)
private val BubbleTheirs = Color(0xFFEEEEEE)    // light bubble (their messages)
private val ChatBackground = Color(0xFFF7F7F7)

// ─────────────────────────────────────────
// LIGHT COLOR SCHEME
// ─────────────────────────────────────────

private val LightColors = lightColorScheme(
    // Primary brand color
    primary = Black,
    onPrimary = White,
    primaryContainer = Black,
    onPrimaryContainer = White,

    // Secondary
    secondary = Teal700,
    onSecondary = White,

    // Surfaces
    surface = White,
    onSurface = Black,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    // Background
    background = LightBackground,
    onBackground = Black,

    // Error
    error = Color(0xFFB00020),
    onError = White
)

// ─────────────────────────────────────────
// DARK COLOR SCHEME (optional, for future)
// ─────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = White,
    onPrimaryContainer = Black,

    secondary = Teal200,
    onSecondary = Black,

    surface = Color(0xFF1E1E1E),
    onSurface = White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),

    background = Color(0xFF121212),
    onBackground = White,

    error = Color(0xFFCF6679),
    onError = Black
)

// ─────────────────────────────────────────
// THEME COMPOSABLE
// ─────────────────────────────────────────

/**
 * ✅ SkoolSwapTheme
 *
 * Usage:
 *   SkoolSwapTheme {
 *       ChatScreen(...)
 *   }
 *
 * By default follows system dark/light mode.
 * Force light mode by passing darkTheme = false.
 */
@Composable
fun SkoolSwapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}