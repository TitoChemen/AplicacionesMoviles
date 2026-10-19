package cl.duoc.huertaescolar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.huertaescolar.ui.theme.screens.*
import cl.duoc.huertaescolar.viewmodel.AuthViewModel
import cl.duoc.huertaescolar.viewmodel.SectorViewModel
import cl.duoc.huertaescolar.viewmodel.TareaViewModel

@Composable
fun HuertaAppNavigation() {
    val navController = rememberNavController()

    // ViewModels compartidos entre pantallas (sobreviven a los cambios de pantalla)
    val authViewModel: AuthViewModel = viewModel()
    val sectorViewModel: SectorViewModel = viewModel()
    val tareaViewModel: TareaViewModel = viewModel()

    val usuario by authViewModel.usuarioActivo.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()

    NavHost(navController = navController, startDestination = "login") {

        // --- Login ---
        composable("login") {
            LoginScreen(
                error = loginError,
                onLogin = { id ->
                    authViewModel.login(id) {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onEditar = { authViewModel.clearError() }
            )
        }

        // --- Dashboard según rol ---
        composable("dashboard") {
            DashboardScreen(
                usuario = usuario,
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

        // --- Sectores y cultivos ---
        composable("lista_sectores") {
            SectorScreen(
                viewModel = sectorViewModel,
                onNavigateToCamera = { navController.navigate("camara_evidencia") },
                idResponsable = usuario?.identificador ?: "admin1",
                onSectorClick = { id -> navController.navigate("detalle_sector/$id") }
            )
        }
        composable("crear_sector") {
            CrearSectorScreen(
                navController = navController,
                viewModel = sectorViewModel,
                idAdministradorActivo = usuario?.identificador ?: "admin1"
            )
        }
        composable(
            route = "detalle_sector/{sectorId}",
            arguments = listOf(navArgument("sectorId") { type = NavType.IntType })
        ) { entry ->
            DetalleSectorScreen(
                navController = navController,
                viewModel = sectorViewModel,
                sectorId = entry.arguments?.getInt("sectorId") ?: 0
            )
        }
        composable(
            route = "crear_cultivo/{sectorId}",
            arguments = listOf(navArgument("sectorId") { type = NavType.IntType })
        ) { entry ->
            CrearCultivoScreen(
                navController = navController,
                viewModel = sectorViewModel,
                sectorId = entry.arguments?.getInt("sectorId") ?: 0
            )
        }

        // --- Tareas (las mismas pantallas sirven para varias entradas del dashboard) ---
        listOf("lista_tareas", "mis_tareas", "historial_actividades", "revision_tareas").forEach { ruta ->
            composable(ruta) {
                ListaTareasScreen(
                    navController = navController,
                    viewModel = tareaViewModel,
                    usuarioActivo = usuario
                )
            }
        }
        composable("crear_tarea") {
            CrearTareaScreen(navController = navController, viewModel = tareaViewModel)
        }
        composable(
            route = "detalle_tarea/{tareaId}",
            arguments = listOf(navArgument("tareaId") { type = NavType.IntType })
        ) { entry ->
            DetalleTareaEstudianteScreen(
                navController = navController,
                viewModel = tareaViewModel,
                tareaId = entry.arguments?.getInt("tareaId") ?: 0
            )
        }
        composable(
            route = "validacion_tarea/{tareaId}",
            arguments = listOf(navArgument("tareaId") { type = NavType.IntType })
        ) { entry ->
            ValidacionTareaScreen(
                navController = navController,
                viewModel = tareaViewModel,
                tareaId = entry.arguments?.getInt("tareaId") ?: 0
            )
        }

        // --- Evidencia (cámara / galería) ---
        composable("camara_evidencia") {
            EvidenciaScreen(navController = navController)
        }

        // --- Pantallas aún no implementadas ---
        composable("crear_reporte") {
            PantallaEnConstruccion("Reportar Problema") { navController.popBackStack() }
        }
        composable("lista_reportes") {
            PantallaEnConstruccion("Reportes Huerto") { navController.popBackStack() }
        }
    }
}

// Pantalla temporal para funciones pendientes (evita que la app se caiga al navegar)
@Composable
fun PantallaEnConstruccion(titulo: String, onVolver: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$titulo: en construcción")
            Button(onClick = onVolver) { Text("Volver") }
        }
    }
}
