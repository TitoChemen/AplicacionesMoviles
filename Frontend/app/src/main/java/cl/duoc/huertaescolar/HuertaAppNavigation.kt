
package cl.duoc.huertaescolar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    // =====================================
    // VIEWMODELS COMPARTIDOS
    // =====================================

    val authViewModel: AuthViewModel = viewModel()
    val sectorViewModel: SectorViewModel = viewModel()
    val tareaViewModel: TareaViewModel = viewModel()

    val usuario by authViewModel.usuarioActivo.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()
    val accessToken by authViewModel.accessToken.collectAsState()

    // =====================================
    // CARGAR TAREAS SEGÚN EL USUARIO
    // =====================================

    LaunchedEffect(usuario, accessToken) {
        tareaViewModel.cargarTareasSegunRol(
            usuarioActivo = usuario,
            accessToken = accessToken
        )
    }

    // =====================================
    // CONFIGURAR SESIÓN DE SECTORES
    // =====================================

    LaunchedEffect(accessToken) {
        sectorViewModel.configurarSesion(accessToken)
    }

    // =====================================
    // NAVEGACIÓN PRINCIPAL
    // =====================================

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        // =====================================
        // LOGIN
        // =====================================

        composable("login") {

            LoginScreen(
                error = loginError,

                onLogin = {
                        identificadorIngresado,
                        contrasenaIngresada ->

                    authViewModel.login(
                        identificadorIngresado,
                        contrasenaIngresada
                    ) {

                        val usuarioActual =
                            authViewModel.usuarioActivo.value

                        if (
                            usuarioActual?.perfil ==
                            "ADMINISTRADOR"
                        ) {
                            navController.navigate("menu_admin") {
                                popUpTo("login") {
                                    inclusive = true
                                }
                            }
                        } else {
                            navController.navigate("menu_estudiante") {
                                popUpTo("login") {
                                    inclusive = true
                                }
                            }
                        }
                    }
                },

                onEditar = {
                    authViewModel.clearError()
                }
            )
        }

        // =====================================
        // MENÚ ADMINISTRADOR
        // =====================================

        composable("menu_admin") {

            DashboardScreen(
                usuario = usuario,
                navController = navController,

                onLogout = {
                    authViewModel.logout()

                    navController.navigate("login") {
                        popUpTo("menu_admin") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // =====================================
        // MENÚ ESTUDIANTE
        // =====================================

        composable("menu_estudiante") {

            DashboardScreen(
                usuario = usuario,
                navController = navController,

                onLogout = {
                    authViewModel.logout()

                    navController.navigate("login") {
                        popUpTo("menu_estudiante") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // =====================================
        // DASHBOARD GENÉRICO
        // =====================================

        composable("dashboard") {

            DashboardScreen(
                usuario = usuario,
                navController = navController,

                onLogout = {
                    authViewModel.logout()

                    navController.navigate("login") {
                        popUpTo("dashboard") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // =====================================
        // LISTA DE SECTORES
        // =====================================

        composable("lista_sectores") {

            LaunchedEffect(accessToken) {
                if (!accessToken.isNullOrBlank()) {
                    sectorViewModel.cargarDatos()
                }
            }

            SectorScreen(
                viewModel = sectorViewModel,

                onNavigateToCamera = {
                    navController.navigate("evidencia/1")
                },

                onBackClick = {
                    navController.popBackStack()
                },

                idResponsable =
                    usuario?.identificador ?: "admin1",

                onSectorClick = { id ->
                    navController.navigate(
                        "detalle_sector/$id"
                    )
                }
            )
        }

        // =====================================
        // CREAR SECTOR
        // =====================================

        composable("crear_sector") {

            CrearSectorScreen(
                navController = navController,
                viewModel = sectorViewModel,

                idAdministradorActivo =
                    usuario?.identificador ?: "admin1"
            )
        }

        // =====================================
        // DETALLE DE SECTOR
        // =====================================

        composable(
            route = "detalle_sector/{sectorId}",

            arguments = listOf(
                navArgument("sectorId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->

            val sectorId =
                entry.arguments?.getInt("sectorId") ?: 0

            DetalleSectorScreen(
                navController = navController,
                viewModel = sectorViewModel,
                sectorId = sectorId
            )
        }

        // =====================================
        // CREAR CULTIVO
        // =====================================

        composable(
            route = "crear_cultivo/{sectorId}",

            arguments = listOf(
                navArgument("sectorId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->

            val sectorId =
                entry.arguments?.getInt("sectorId") ?: 0

            CrearCultivoScreen(
                navController = navController,
                viewModel = sectorViewModel,
                sectorId = sectorId
            )
        }

        // =====================================
        // LISTAS DE TAREAS
        // =====================================

        listOf(
            "lista_tareas",
            "mis_tareas",
            "historial_actividades",
            "revision_tareas"
        ).forEach { ruta ->

            composable(ruta) {

                ListaTareasScreen(
                    navController = navController,
                    viewModel = tareaViewModel,
                    usuarioActivo = usuario
                )
            }
        }

        // =====================================
        // CREAR TAREA
        // =====================================

        composable("crear_tarea") {

            CrearTareaScreen(
                navController = navController,
                viewModel = tareaViewModel
            )
        }

        // =====================================
        // DETALLE TAREA ESTUDIANTE
        // =====================================

        composable(
            route = "detalle_tarea/{tareaId}",

            arguments = listOf(
                navArgument("tareaId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->

            val tareaId =
                entry.arguments?.getInt("tareaId") ?: 0

            DetalleTareaEstudianteScreen(
                navController = navController,
                viewModel = tareaViewModel,
                tareaId = tareaId
            )
        }

        // =====================================
        // VALIDACIÓN DE TAREA
        // =====================================

        composable(
            route = "validacion_tarea/{tareaId}",

            arguments = listOf(
                navArgument("tareaId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->

            val tareaId =
                entry.arguments?.getInt("tareaId") ?: 0

            ValidacionTareaScreen(
                navController = navController,
                viewModel = tareaViewModel,
                tareaId = tareaId,
                accessToken = accessToken
            )
        }

        // =====================================
        // EVIDENCIAS
        // =====================================

        composable(
            route = "evidencia/{tareaId}",

            arguments = listOf(
                navArgument("tareaId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->

            val tareaId =
                entry.arguments?.getInt("tareaId") ?: 0

            EvidenciaScreen(
                tareaId = tareaId,
                navController = navController,
                viewModel = tareaViewModel,
                accessToken = accessToken
            )
        }

        // =====================================
        // REPORTES PENDIENTES
        // =====================================

        composable("crear_reporte") {

            PantallaEnConstruccion(
                "Reportar Problema"
            ) {
                navController.popBackStack()
            }
        }

        composable("lista_reportes") {

            PantallaEnConstruccion(
                "Reportes Huerto"
            ) {
                navController.popBackStack()
            }
        }
    }
}

// =====================================
// PANTALLA TEMPORAL
// =====================================

@Composable
fun PantallaEnConstruccion(
    titulo: String,
    onVolver: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("$titulo: en construcción")

            Button(onClick = onVolver) {
                Text("Volver")
            }
        }
    }
}
