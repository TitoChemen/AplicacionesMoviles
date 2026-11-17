package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    val sector = sectores.firstOrNull { it.id == sectorId }
    var nombre by remember(sectorId) { mutableStateOf(sector?.nombre.orEmpty()) }
    var ubicacion by remember(sectorId) { mutableStateOf(sector?.ubicacion.orEmpty()) }
    var descripcion by remember(sectorId) { mutableStateOf(sector?.descripcion.orEmpty()) }
    var estado by remember(sectorId) { mutableStateOf(sector?.estado.orEmpty()) }

    // Cuando los sectores llegan desde MariaDB, inicializar una sola vez.
    var inicializado by remember(sectorId) { mutableStateOf(false) }
    LaunchedEffect(sector?.id) {
        if (sector != null && !inicializado) {
            nombre = sector.nombre
            ubicacion = sector.ubicacion
            descripcion = sector.descripcion
            estado = sector.estado
            inicializado = true
        }
    }
    DisposableEffect(sectorId) {
        viewModel.limpiarErrorSector()
        onDispose { viewModel.limpiarErrorSector() }
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
                Text("El código, el curso y el responsable se conservan al editar.",
                    style = MaterialTheme.typography.bodySmall)
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
                        viewModel.editarSector(sector, nombre, ubicacion, descripcion, estado) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.cargando && nombre.isNotBlank() &&
                        ubicacion.isNotBlank() && estado in setOf("ACTIVO", "INACTIVO")
                ) {
                    Text(if (viewModel.cargando) "Guardando…" else "Guardar cambios")
                }
            }
        }
    }
}
