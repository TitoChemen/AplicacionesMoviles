package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.huertaescolar.viewmodel.SectorViewModel
import cl.duoc.huertaescolar.data.local.entity.Cultivo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleSectorScreen(
    navController: NavController,
    viewModel: SectorViewModel,
    sectorId: Int,
    puedeGestionar: Boolean,
    puedeCrearCultivo: Boolean
) {
    val sectores by viewModel.sectores.collectAsState()
    val cultivos by viewModel.cultivos.collectAsState()
    val cursoPorSector by viewModel.cursoPorSector.collectAsState()

    // Buscamos el sector actual y filtramos solo sus cultivos asociados
    val sectorActual = sectores.find { it.id == sectorId }
    val cultivosDelSector = cultivos.filter { it.sectorId == sectorId }
    var confirmarEliminacion by remember(sectorId) { mutableStateOf(false) }
    var cultivoEditando by remember(sectorId) { mutableStateOf<Cultivo?>(null) }
    var cultivoEliminando by remember(sectorId) { mutableStateOf<Cultivo?>(null) }
    var nombreCultivoEditado by remember(sectorId) { mutableStateOf("") }
    var descripcionCultivoEditada by remember(sectorId) { mutableStateOf("") }
    DisposableEffect(sectorId) {
        viewModel.limpiarErrorSector()
        onDispose { viewModel.limpiarErrorSector() }
    }

    if (confirmarEliminacion && sectorActual != null) {
        AlertDialog(
            onDismissRequest = { confirmarEliminacion = false },
            title = { Text("¿Eliminar sector?") },
            text = { Text("Esta acción elimina definitivamente el sector vacío \"${sectorActual.nombre}\". No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminacion = false
                    viewModel.eliminarSector(sectorActual) { navController.popBackStack() }
                }) { Text("Eliminar definitivamente") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminacion = false }) { Text("Cancelar") }
            }
        )
    }

    cultivoEditando?.let { cultivo ->
        AlertDialog(onDismissRequest = { cultivoEditando = null },
            title = { Text("Editar cultivo") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombreCultivoEditado,
                    onValueChange = { nombreCultivoEditado = it }, label = { Text("Nombre *") })
                OutlinedTextField(value = descripcionCultivoEditada,
                    onValueChange = { descripcionCultivoEditada = it },
                    label = { Text("Descripción") })
                viewModel.errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(onClick = {
                viewModel.editarCultivo(cultivo, nombreCultivoEditado, descripcionCultivoEditada) {
                    cultivoEditando = null
                }
            }, enabled = !viewModel.cargando && nombreCultivoEditado.isNotBlank()) {
                Text("Guardar")
            } },
            dismissButton = { TextButton(onClick = { cultivoEditando = null }) {
                Text("Cancelar")
            } })
    }

    cultivoEliminando?.let { cultivo ->
        AlertDialog(onDismissRequest = { cultivoEliminando = null },
            title = { Text("Eliminar cultivo") },
            text = { Column {
                Text("¿Eliminar definitivamente ${cultivo.nombreComun}?")
                viewModel.errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton(onClick = {
                viewModel.eliminarCultivo(cultivo) { cultivoEliminando = null }
            }, enabled = !viewModel.cargando) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { cultivoEliminando = null }) {
                Text("Cancelar")
            } })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sectorActual?.nombre ?: "Detalle del Sector") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (puedeCrearCultivo && sectorActual != null && sectorActual.estado == "ACTIVO") FloatingActionButton(
                onClick = { navController.navigate("crear_cultivo/$sectorId") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Cultivo")
            }
        }
    ) { paddingValues ->
        if (sectorActual == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECCIÓN 1: INFO DEL SECTOR + BOTÓN ELIMINAR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ubicación: ${sectorActual.ubicacion}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (sectorActual.descripcion.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sectorActual.descripcion,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Estado: ${if (sectorActual.estado == "INACTIVO") "Archivado" else sectorActual.estado}",
                            style = MaterialTheme.typography.labelMedium)
                        Text("Curso: ${cursoPorSector[sectorId] ?: "Sin curso"}",
                            style = MaterialTheme.typography.labelMedium)
                        if (puedeGestionar) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { navController.navigate("editar_sector/$sectorId") },
                                enabled = !viewModel.cargando,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Editar sector")
                            }
                            if (cultivosDelSector.isNotEmpty()) {
                                Text(
                                    "Este sector tiene ${cultivosDelSector.size} cultivo(s). " +
                                        "No es posible eliminarlo sin perderlos. Puedes archivarlo sin borrar datos.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (sectorActual.estado != "INACTIVO") {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.archivarSector(sectorActual) { /* Estado actualizado localmente */ }
                                    },
                                    enabled = !viewModel.cargando,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Archivar sector (conservar cultivos)") }
                            }
                            Button(
                                onClick = { confirmarEliminacion = true },
                                enabled = !viewModel.cargando && cultivosDelSector.isEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Eliminar sector vacío")
                            }
                            if (viewModel.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
                            viewModel.errorGeneral?.let { error ->
                                Text(error, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // SECCIÓN 2: PANEL DE SENSORES
            item {
                Text(
                    text = "Monitoreo en Tiempo Real",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tarjeta de Temperatura
                    ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = "Temperatura",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Temperatura", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${viewModel.temperaturaSimulada} °C",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Tarjeta de Humedad del Suelo
                    ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Humedad",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Humedad Suelo", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${viewModel.humedadSimulada} %",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // SECCIÓN 3: CULTIVOS DEL SECTOR
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cultivos Asociados",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (puedeCrearCultivo) viewModel.errorGeneral?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            if (cultivosDelSector.isEmpty()) {
                item {
                    Text(
                        text = "Aún no hay cultivos registrados en este sector.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                items(cultivosDelSector) { cultivo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = cultivo.nombreComun,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Estado: ${cultivo.estadoDesarrollo}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (puedeCrearCultivo) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = {
                                            viewModel.limpiarErrorSector()
                                            nombreCultivoEditado = cultivo.nombreComun
                                            descripcionCultivoEditada = cultivo.descripcion
                                            cultivoEditando = cultivo
                                        }) { Text("Editar") }
                                        TextButton(onClick = {
                                            viewModel.limpiarErrorSector()
                                            cultivoEliminando = cultivo
                                        }) { Text("Eliminar") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
