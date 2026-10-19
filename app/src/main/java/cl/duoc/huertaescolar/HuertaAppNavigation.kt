package cl.duoc.huertaescolar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.duoc.huertaescolar.ui.theme.screens.SectorScreen
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

@Composable
fun HuertaAppNavigation() {
    val navController = rememberNavController()
    // Instanciamos el ViewModel aquí para que sobreviva a los cambios de pantalla
    val sectorViewModel: SectorViewModel = viewModel()

    NavHost(navController = navController, startDestination = "gestion_sectores") {

        // Tu pantalla principal
        composable("gestion_sectores") {
            SectorScreen(
                viewModel = sectorViewModel,
                onNavigateToCamera = { navController.navigate("camara_evidencia") }
            )
        }

        // La nueva pantalla de la cámara
        composable("camara_evidencia") {
            CamaraScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Pantalla de relleno temporal para que la app reaccione y no se caiga
@Composable
fun CamaraScreen(onNavigateBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Pantalla de la Cámara en construcción :0")
            Button(onClick = { onNavigateBack() }) {
                Text("Volver a Sectores")
            }
        }
    }
}