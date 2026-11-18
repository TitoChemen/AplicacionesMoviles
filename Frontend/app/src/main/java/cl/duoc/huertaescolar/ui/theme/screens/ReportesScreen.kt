package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cl.duoc.huertaescolar.viewmodel.AlertaViewModel
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

private val tiposReporte = listOf("PLAGA", "FALTA_RIEGO", "INFRAESTRUCTURA", "OTRO")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarProblemaScreen(navController: NavController,
                           alertas: AlertaViewModel, sectoresViewModel: SectorViewModel) {
    val sectores by sectoresViewModel.sectores.collectAsState()
    val disponibles = sectores.filter { it.estado == "ACTIVO" }
    var sectorId by remember { mutableStateOf<Int?>(null) }
    var tipo by remember { mutableStateOf("PLAGA") }
    var descripcion by remember { mutableStateOf("") }
    var menuSectores by remember { mutableStateOf(false) }
    var menuTipos by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { sectoresViewModel.cargarDatos() }

    Scaffold(topBar = { TopAppBar(title = { Text("Reportar problema") },
        navigationIcon = { IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
        } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("El reporte se enviará al docente de tu curso.")
            Text("Sector de tu curso *", style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton(onClick = { menuSectores = true },
                    enabled = disponibles.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text(disponibles.firstOrNull { it.id == sectorId }?.nombre ?: "Seleccionar sector",
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = menuSectores, onDismissRequest = { menuSectores = false }) {
                    disponibles.forEach { sector ->
                        DropdownMenuItem(text = { Text(sector.nombre) }, onClick = {
                            sectorId = sector.id; menuSectores = false
                        })
                    }
                }
            }
            if (disponibles.isEmpty()) Text("No hay sectores activos disponibles en tu curso.")
            Text("Tipo de problema *", style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton(onClick = { menuTipos = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(tipo.replace('_', ' '), modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = menuTipos, onDismissRequest = { menuTipos = false }) {
                    tiposReporte.forEach { opcion ->
                        DropdownMenuItem(text = { Text(opcion.replace('_', ' ')) }, onClick = {
                            tipo = opcion; menuTipos = false
                        })
                    }
                }
            }
            OutlinedTextField(value = descripcion, onValueChange = { descripcion = it },
                label = { Text("Descripción *") }, minLines = 3,
                modifier = Modifier.fillMaxWidth())
            alertas.error.value?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                alertas.reportar(sectorId?.toLong(), tipo, descripcion) {
                    navController.popBackStack()
                }
            }, enabled = !alertas.cargando.value && sectorId != null && descripcion.isNotBlank(),
                modifier = Modifier.fillMaxWidth()) {
                Text(if (alertas.cargando.value) "Enviando…" else "Enviar reporte")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaReportesScreen(navController: NavController,
                        alertas: AlertaViewModel, sectoresViewModel: SectorViewModel) {
    val reportes by alertas.alertas.collectAsState()
    val sectores by sectoresViewModel.sectores.collectAsState()
    LaunchedEffect(Unit) { alertas.cargar(); sectoresViewModel.cargarDatos() }
    Scaffold(topBar = { TopAppBar(title = { Text("Reportes del huerto") },
        navigationIcon = { IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
        } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Solo se muestran reportes autorizados para esta cuenta.")
                alertas.error.value?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = alertas::cargar, enabled = !alertas.cargando.value) {
                    Text("Actualizar reportes")
                }
            }
            if (reportes.isEmpty()) item { Text(if (alertas.cargando.value) "Cargando…" else "No hay reportes.") }
            items(reportes, key = { it.id }) { reporte ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${reporte.tipo.replace('_', ' ')} · ${reporte.estado}",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Sector: ${sectores.firstOrNull { it.id.toLong() == reporte.sectorId }?.nombre ?: "#${reporte.sectorId}"}")
                        Text(reporte.descripcion)
                        Text("Reportó usuario #${reporte.usuarioId ?: "—"} · ${reporte.fechaReporte.orEmpty()}",
                            style = MaterialTheme.typography.bodySmall)
                        if (reporte.estado == "PENDIENTE") TextButton(onClick = {
                            alertas.cambiarEstado(reporte.id, "EN_REVISION")
                        }, enabled = !alertas.cargando.value) { Text("Marcar en revisión") }
                        if (reporte.estado != "RESUELTA") TextButton(onClick = {
                            alertas.cambiarEstado(reporte.id, "RESUELTA")
                        }, enabled = !alertas.cargando.value) { Text("Marcar resuelta") }
                    }
                }
            }
        }
    }
}
