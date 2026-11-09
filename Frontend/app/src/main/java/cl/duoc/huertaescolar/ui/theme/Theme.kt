package cl.duoc.huertaescolar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HuertaVerdeClaro,
    secondary = HuertaVerdeMusgo,
    tertiary = HuertaTierra,
    background = Color(0xFF121C12),
    surface = Color(0xFF1B2E1B),
    onPrimary = Color.White,
    onSecondary = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = HuertaVerdePrimario,
    secondary = HuertaVerdeMusgo,
    tertiary = HuertaTierra,
    background = HuertaFondoClaro,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1B3B1B),
    onSurface = Color(0xFF1A1C1A)
)

@Composable
fun HuertaEscolarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 💡 Lo pasamos a false por defecto para que la app mande con los colores del Huerto y no tome los del celular
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}