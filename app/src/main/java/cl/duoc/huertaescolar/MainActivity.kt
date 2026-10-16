package cl.duoc.huertaescolar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.huertaescolar.ui.theme.HuertaEscolarTheme
import cl.duoc.huertaescolar.ui.theme.screens.SectorScreen
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuertaEscolarTheme {
                val sectorViewModel: SectorViewModel = viewModel()
                SectorScreen(viewModel = sectorViewModel)
            }
        }
    }
}