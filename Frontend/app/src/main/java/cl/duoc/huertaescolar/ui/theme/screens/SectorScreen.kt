package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectorScreen(
    viewModel: SectorViewModel,
    onBackClick: () -> Unit,
    onCrearSector: () -> Unit,
    puedeCrear: Boolean,
    esAdminGlobal: Boolean,
    onSectorClick: (Int) -> Unit
) {
    val sectores by viewModel.sectores.collectAsState()
    val cursoPorSector by viewModel.cursoPorSector.collectAsState()
    LaunchedEffect(esAdminGlobal) { if (esAdminGlobal) viewModel.cargarCursos() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Sectores y sensores") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer))
    }) { insets ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (puedeCrear) item {
                Button(onClick = onCrearSector, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear sector")
                }
            }
            if (viewModel.errorGeneral != null) item {
                Text(viewModel.errorGeneral.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            item {
                Text("Sensores · Datos simulados", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Thermostat, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error)
                            Text("Temperatura", style = MaterialTheme.typography.bodySmall)
                            Text("${viewModel.temperaturaSimulada} °C", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Text("Humedad suelo", style = MaterialTheme.typography.bodySmall)
                            Text("${viewModel.humedadSimulada} %", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            item { Text("Sectores registrados", style = MaterialTheme.typography.titleMedium) }
            if (sectores.isEmpty()) item { Text("No hay sectores registrados.") }
            items(sectores, key = { it.id }) { sector ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()
                    .clickable { onSectorClick(sector.id) }) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sector.nombre, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                        if (sector.estado == "INACTIVO") {
                            Text("Archivado · solo consulta", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(sector.descripcion.ifBlank { "Sin descripción" },
                            style = MaterialTheme.typography.bodyMedium)
                        cursoPorSector[sector.id]?.let { curso ->
                            Text(curso, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
