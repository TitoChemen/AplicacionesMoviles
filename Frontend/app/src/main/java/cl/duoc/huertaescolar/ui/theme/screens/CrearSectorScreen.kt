package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearSectorScreen(
    navController: NavController,
    viewModel: SectorViewModel,
    idAdministradorActivo: String,
    esAdminGlobal: Boolean
) {
    val cursos by viewModel.cursosDisponibles.collectAsState()
    var desplegado by remember { mutableStateOf(false) }
    LaunchedEffect(esAdminGlobal) { if (esAdminGlobal) viewModel.cargarCursos() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Nuevo sector") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer))
    }) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets).imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos del sector", style = MaterialTheme.typography.titleLarge)
            if (esAdminGlobal) {
                Text("Curso donde se creará", style = MaterialTheme.typography.labelLarge)
                Box {
                    OutlinedButton(onClick = { desplegado = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(cursos.firstOrNull { it.id == viewModel.cursoSeleccionadoId }?.let {
                            "${it.codigo} · ${it.nombre}"
                        } ?: "Seleccionar curso", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = desplegado, onDismissRequest = { desplegado = false }) {
                        cursos.forEach { curso ->
                            DropdownMenuItem(text = { Text("${curso.codigo} · ${curso.nombre}") }, onClick = {
                                viewModel.cursoSeleccionadoId = curso.id
                                desplegado = false
                            })
                        }
                    }
                }
                if (cursos.isEmpty()) Text("No hay cursos disponibles. Revisa usuarios-service.",
                    color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(value = viewModel.nombreSector,
                onValueChange = viewModel::onNombreSectorChange,
                label = { Text("Nombre del sector *") }, singleLine = true,
                isError = viewModel.errorNombreSector != null,
                supportingText = { viewModel.errorNombreSector?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = viewModel.ubicacionSector,
                onValueChange = viewModel::onUbicacionSectorChange,
                label = { Text("Ubicación *") }, singleLine = true,
                isError = viewModel.errorUbicacionSector != null,
                supportingText = { viewModel.errorUbicacionSector?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = viewModel.descripcionSector,
                onValueChange = { viewModel.descripcionSector = it },
                label = { Text("Descripción") }, minLines = 2,
                modifier = Modifier.fillMaxWidth())
            viewModel.errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                viewModel.registrarSector(idAdministradorActivo, esAdminGlobal) {
                    navController.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !viewModel.cargando &&
                (!esAdminGlobal || viewModel.cursoSeleccionadoId != null)) {
                Text(if (viewModel.cargando) "Guardando…" else "Guardar sector")
            }
        }
    }
}
