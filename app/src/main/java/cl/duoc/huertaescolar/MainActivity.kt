package cl.duoc.huertaescolar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertaescolar.ui.theme.HuertaEscolarTheme
import cl.duoc.huertaescolar.ui.theme.screens.SectorScreen
import cl.duoc.huertaescolar.viewmodel.HuertaAppNavigation
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuertaEscolarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // AQUÍ LLAMAMOS AL MAPA DE NAVEGACIÓN
                    HuertaAppNavigation()
                }
            }
        }
    }
}