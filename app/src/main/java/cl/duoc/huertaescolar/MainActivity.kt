package cl.duoc.huertaescolar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertaescolar.ui.screens.SectorScreen
import cl.duoc.huertaescolar.ui.theme.HuertaEscolarTheme
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuertaEscolarTheme {
                // Instanciamos el ViewModel que armamos para sectores y sensores
                val sectorViewModel: SectorViewModel = viewModel()
                
                // Llamamos directamente a tu pantalla de sectores y sensores
                SectorScreen(viewModel = sectorViewModel)
            }
        }
    }
}