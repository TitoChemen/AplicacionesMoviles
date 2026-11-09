package cl.duoc.huertaescolar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cl.duoc.huertaescolar.ui.theme.HuertaEscolarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen() // <-- Esto activa la pantalla de carga
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuertaEscolarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Mapa de navegación (HuertaAppNavigation.kt, paquete raíz)
                    HuertaAppNavigation()
                }
            }
        }
    }

}
