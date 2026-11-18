package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarSectorScreen(
    navController: NavController,
    viewModel: SectorViewModel,
    sectorId: Int
) {
    val sectores by viewModel.sectores.collectAsState()
    val cursos by viewModel.cursosDisponibles.collectAsState()
    val sector = sectores.firstOrNull { it.id == sectorId }
    val cursoActualId = viewModel.cursoIdDeSector(sectorId)
    var nombre by remember(sectorId) { mutableStateOf(sector?.nombre.orEmpty()) }
    var ubicacion by remember(sectorId) { mutableStateOf(sector?.ubicacion.orEmpty()) }
    var descripcion by remember(sectorId) { mutableStateOf(sector?.descripcion.orEmpty()) }
    var estado by remember(sectorId) { mutableStateOf(sector?.estado.orEmpty()) }
    var cursoSeleccionadoId by remember(sectorId) { mutableStateOf<Long?>(null) }
    var cursoInicializado by remember(sectorId) { mutableStateOf(false) }
    var desplegado by remember(sectorId) { mutableStateOf(false) }
    var confirmarTraslado by remember(sectorId) { mutableStateOf(false) }

    LaunchedEffect(sectorId) { viewModel.cargarCursos() }

    // Cuando los sectores llegan desde MariaDB, inicializar una sola vez.
    var inicializado by remember(sectorId) { mutableStateOf(false) }
    LaunchedEffect(sector?.id, cursoActualId) {
        if (sector != null && !inicializado) {
            nombre = sector.nombre
            ubicacion = sector.ubicacion
            descripcion = sector.descripcion
            estado = sector.estado
            inicializado = true
        }
    }
    LaunchedEffect(cursoActualId) {
        if (cursoActualId != null && !cursoInicializado) {
            cursoSeleccionadoId = cursoActualId
            cursoInicializado = true
        }
    }
    DisposableEffect(sectorId) {
        viewModel.limpiarErrorSector()
        onDispose { viewModel.limpiarErrorSector() }
    }

    if (confirmarTraslado && sector != null) {
        AlertDialog(
            onDismissRequest = { confirmarTraslado = false },
            title = { Text("Cambiar curso del sector") },
            text = { Text("El sector y sus cultivos pasarán a ${cursos.firstOrNull { it.id == cursoSeleccionadoId }?.codigo ?: "otro curso"}. Si existen tareas o alertas, el servidor rechazará el cambio para proteger sus datos.") },
            confirmButton = { TextButton(onClick = {
                confirmarTraslado = false
                viewModel.editarSector(sector, nombre, ubicacion, descripcion, estado,
                    cursoSeleccionadoId) { navController.popBackStack() }
            }) { Text("Confirmar traslado") } },
            dismissButton = { TextButton(onClick = { confirmarTraslado = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Editar sector") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sector == null) {
                Text("No se encontró el sector. Regresa a la lista y actualiza los datos.")
            } else {
                Text("Código: ${sector.codigo}", style = MaterialTheme.typography.labelLarge)
                Text("El código y el responsable se conservan al editar.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Curso asignado *", style = MaterialTheme.typography.labelLarge)
                Box {
                    OutlinedButton(onClick = { desplegado = true },
                        modifier = Modifier.fillMaxWidth(), enabled = cursos.isNotEmpty()) {
                        Text(cursos.firstOrNull { it.id == cursoSeleccionadoId }?.let {
                            "${it.codigo} · ${it.nombre}"
                        } ?: "Seleccionar curso", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = desplegado, onDismissRequest = { desplegado = false }) {
                        cursos.forEach { curso ->
                            DropdownMenuItem(text = { Text("${curso.codigo} · ${curso.nombre}") },
                                onClick = {
                                    cursoSeleccionadoId = curso.id
                                    cursoInicializado = true
                                    desplegado = false
                                })
                        }
                    }
                }
                if (cursos.isEmpty()) Text("No se pudieron cargar los cursos. Revisa usuarios-service.",
                    color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ubicacion, onValueChange = { ubicacion = it },
                    label = { Text("Ubicación *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion, onValueChange = { descripcion = it },
                    label = { Text("Descripción") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Estado", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = estado == "ACTIVO", onClick = { estado = "ACTIVO" },
                        label = { Text("Activo") })
                    FilterChip(selected = estado == "INACTIVO", onClick = { estado = "INACTIVO" },
                        label = { Text("Archivado") })
                }
                viewModel.errorGeneral?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        if (cursoSeleccionadoId != cursoActualId) confirmarTraslado = true
                        else viewModel.editarSector(sector, nombre, ubicacion, descripcion, estado,
                            cursoSeleccionadoId) { navController.popBackStack() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.cargando && nombre.isNotBlank() &&
                        ubicacion.isNotBlank() && estado in setOf("ACTIVO", "INACTIVO") &&
                        cursos.any { it.id == cursoSeleccionadoId }
                ) {
                    Text(if (viewModel.cargando) "Guardando…" else "Guardar cambios")
                }
            }
        }
    }
}
