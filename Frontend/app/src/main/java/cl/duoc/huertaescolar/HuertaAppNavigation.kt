
package cl.duoc.huertaescolar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.duoc.huertaescolar.data.Perfil
import cl.duoc.huertaescolar.data.local.entity.Usuario
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

    Column(modifier = Modifier.fillMaxSize()
        .statusBarsPadding().navigationBarsPadding()) {
        if (usuario != null && !accessToken.isNullOrBlank()) {
            EncabezadoSesion(usuario)
        }

        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.weight(1f)
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
                            "ADMINISTRADOR" || usuarioActual?.perfil == "DOCENTE"
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
                onBackClick = { navController.popBackStack() },
                onCrearSector = { navController.navigate("crear_sector") },
                puedeCrear = Perfil.esPersonal(usuario?.perfil),
                esAdminGlobal = usuario?.perfil == Perfil.ADMINISTRADOR,
                onSectorClick = { id -> navController.navigate("detalle_sector/$id") }
            )
        }

        // =====================================
        // CREAR SECTOR
        // =====================================

        composable("crear_sector") {

            CrearSectorScreen(
                navController = navController,
                viewModel = sectorViewModel,

                idAdministradorActivo = usuario?.identificador ?: "admin1",
                esAdminGlobal = usuario?.perfil == Perfil.ADMINISTRADOR
            )
        }

        // =====================================
        // EDICIÓN DE SECTOR (DOCENTE / ADMIN)
        // =====================================
        composable(
            route = "editar_sector/{sectorId}",
            arguments = listOf(navArgument("sectorId") { type = NavType.IntType })
        ) { entry ->
            if (Perfil.esPersonal(usuario?.perfil)) {
                EditarSectorScreen(
                    navController = navController,
                    viewModel = sectorViewModel,
                    sectorId = entry.arguments?.getInt("sectorId") ?: 0
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
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
                sectorId = sectorId,
                puedeGestionar = Perfil.esPersonal(usuario?.perfil)
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
            "revision_tareas"
        ).forEach { ruta ->

            composable(ruta) {

                ListaTareasScreen(
                    navController = navController,
                    viewModel = tareaViewModel,
                    usuarioActivo = usuario,
                    accessToken = accessToken
                )
            }
        }

        composable("historial_actividades") {
            HistorialActividadesScreen(navController, tareaViewModel)
        }

        composable(
            route = "historial_tarea/{tareaId}",
            arguments = listOf(navArgument("tareaId") { type = NavType.IntType })
        ) { entry ->
            val tareaId = entry.arguments?.getInt("tareaId") ?: 0
            HistorialTareaScreen(navController, tareaViewModel, tareaId, accessToken)
        }

        // =====================================
        // CREAR TAREA
        // =====================================

        composable("crear_tarea") {

            CrearTareaScreen(
                navController = navController,
                viewModel = tareaViewModel,
                accessToken = accessToken
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
}

@Composable
private fun EncabezadoSesion(usuario: Usuario?) {
    if (usuario == null) return
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hola, ${if (Perfil.esPersonal(usuario.perfil)) usuario.nombreFicticio else usuario.nombreFicticio.substringBefore(' ')}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val cargo = when (usuario.perfil) {
                    Perfil.ADMINISTRADOR -> "Administrador global · Todos los cursos"
                    Perfil.DOCENTE -> "Docente · ${usuario.tallerAsignado}"
                    else -> "Estudiante · ${usuario.tallerAsignado}"
                }
                Text(
                    text = cargo,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
