package br.com.rastreadorfrota.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val TrakSyncDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = Color(0xFFD8E7FF),
    secondary = Cyan,
    onSecondary = Color(0xFF00232E),
    secondaryContainer = Color(0xFF003746),
    onSecondaryContainer = Color(0xFFB8F1FF),
    tertiary = Color(0xFFB38CFF),
    onTertiary = Color(0xFF1E0A47),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Background,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3,
    inverseSurface = TextPrimary,
    inverseOnSurface = Background,
    error = ErrorRed,
    onError = OnPrimary,
    errorContainer = Color(0xFF4A0F1A),
    onErrorContainer = Color(0xFFFFD9DE),
    outline = Color(0xFF2A4560),
    outlineVariant = Surface2
)

private val TrakSyncLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = Color(0xFF00296B),
    secondary = CyanLight,
    onSecondary = OnPrimary,
    secondaryContainer = Color(0xFFCDF4FF),
    onSecondaryContainer = Color(0xFF002A35),
    tertiary = Color(0xFF6D3FD6),
    onTertiary = OnPrimary,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Surface2Light,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = Surface2Light,
    surfaceContainerHighest = Surface3Light,
    inverseSurface = Color(0xFF1A2737),
    inverseOnSurface = Color(0xFFF1F5FA),
    error = ErrorLight,
    onError = OnPrimary,
    errorContainer = Color(0xFFFFDADF),
    onErrorContainer = Color(0xFF5C0012),
    outline = Color(0xFFB5C3D2),
    outlineVariant = Surface3Light
)

/** Cores que o Material 3 não tem: status da frota, texto secundário e superfícies dos cards. */
data class TrakSyncExtendedColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val textSecondary: Color,
    val surface2: Color,
    val surface3: Color,
    val cyan: Color,
    val isDark: Boolean
)

private val darkExtendedColors = TrakSyncExtendedColors(
    success = Success,
    successContainer = Color(0xFF0B3B29),
    warning = Warning,
    warningContainer = Color(0xFF45300C),
    info = Info,
    textSecondary = TextSecondary,
    surface2 = Surface2,
    surface3 = Surface3,
    cyan = Cyan,
    isDark = true
)

private val lightExtendedColors = TrakSyncExtendedColors(
    success = SuccessLight,
    successContainer = Color(0xFFD4F5E6),
    warning = WarningLight,
    warningContainer = Color(0xFFFFEBCC),
    info = InfoLight,
    textSecondary = TextSecondaryLight,
    surface2 = Surface2Light,
    surface3 = Surface3Light,
    cyan = CyanLight,
    isDark = false
)

private val LocalTrakSyncColors = staticCompositionLocalOf { darkExtendedColors }

object TrakSyncTheme {
    val colors: TrakSyncExtendedColors
        @Composable get() = LocalTrakSyncColors.current
}

/**
 * Segue o tema do aparelho (claro ou escuro), sempre com a identidade TrakSync.
 * Sem cor dinâmica, para a marca ficar igual em qualquer celular.
 */
@Composable
fun RastreadorFrotaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTrakSyncColors provides if (darkTheme) darkExtendedColors else lightExtendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) TrakSyncDarkColorScheme else TrakSyncLightColorScheme,
            typography = Typography,
            shapes = TrakSyncShapes,
            content = content
        )
    }
}

@Composable
fun trakSyncTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = TrakSyncTheme.colors.surface2,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = TrakSyncTheme.colors.textSecondary,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = TrakSyncTheme.colors.textSecondary,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = TrakSyncTheme.colors.textSecondary,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)

@Composable
fun trakSyncPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = TrakSyncTheme.colors.surface2,
    disabledContentColor = TrakSyncTheme.colors.textSecondary
)
