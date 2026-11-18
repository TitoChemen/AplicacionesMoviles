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
import cl.duoc.huertaescolar.viewmodel.ClimaViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

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
    val climaViewModel: ClimaViewModel = viewModel()
    val clima by climaViewModel.estado.collectAsState()
    LaunchedEffect(Unit) { climaViewModel.actualizar() }
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
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Clima exterior · Santiago", style = MaterialTheme.typography.titleMedium)
                        Text("Open-Meteo · Referencia para planificar el trabajo en la huerta",
                            style = MaterialTheme.typography.bodySmall)
                        when {
                            clima.dato != null -> {
                                Text("${clima.dato!!.temperaturaCelsius} °C · " +
                                    "Humedad del aire ${clima.dato!!.humedadAirePorcentaje} % · " +
                                    "Precipitación ${clima.dato!!.precipitacionMm} mm")
                                if (clima.error != null) Text(clima.error.orEmpty(),
                                    color = MaterialTheme.colorScheme.error)
                            }
                            clima.cargando -> CircularProgressIndicator()
                            else -> Text(clima.error ?: "Sin datos meteorológicos")
                        }
                        TextButton(onClick = climaViewModel::actualizar,
                            enabled = !clima.cargando) { Text("Actualizar clima") }
                    }
                }
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
