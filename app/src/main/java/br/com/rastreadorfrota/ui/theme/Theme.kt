package br.com.rastreadorfrota.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val TrakSyncDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Cyan,
    onSecondary = Color(0xFF00232E),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = OnPrimary,
    outline = Surface2
)

data class TrakSyncExtendedColors(
    val success: Color,
    val warning: Color,
    val textSecondary: Color,
    val surface2: Color,
    val cyan: Color
)

private val defaultExtendedColors = TrakSyncExtendedColors(
    success = Success,
    warning = Warning,
    textSecondary = TextSecondary,
    surface2 = Surface2,
    cyan = Cyan
)

private val LocalTrakSyncColors = staticCompositionLocalOf { defaultExtendedColors }

object TrakSyncTheme {
    val colors: TrakSyncExtendedColors
        @Composable get() = LocalTrakSyncColors.current
}

@Composable
fun RastreadorFrotaTheme(
    content: @Composable () -> Unit
) {
    // Marca é "dark-first": sem tema claro nem cor dinâmica,
    // pra manter a identidade do TrakSync igual em qualquer aparelho.
    CompositionLocalProvider(LocalTrakSyncColors provides defaultExtendedColors) {
        MaterialTheme(
            colorScheme = TrakSyncDarkColorScheme,
            typography = Typography,
            shapes = TrakSyncShapes,
            content = content
        )
    }
}

@Composable
fun trakSyncTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = TrakSyncTheme.colors.surface2,
    disabledBorderColor = TrakSyncTheme.colors.surface2,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = TrakSyncTheme.colors.textSecondary,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = TrakSyncTheme.colors.textSecondary
)

@Composable
fun trakSyncPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = TrakSyncTheme.colors.surface2,
    disabledContentColor = TrakSyncTheme.colors.textSecondary
)