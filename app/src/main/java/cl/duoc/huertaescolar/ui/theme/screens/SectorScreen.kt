package cl.duoc.huertaescolar.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.default.Thermostat
import androidx.compose.material.icons.default.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.huertaescolar.viewmodel.SectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectorScreen(viewModel: SectorViewModel) {
    val sectores by viewModel.sectores.collectAsState()
    
    // Estado para controlar errores visuales en el formulario
    var hasError by remember { mutableStateOf(false) }
    var mensajeExito by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Sectores y Sensores - Huerto") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECCIÓN 1: FORMULARIO DE REGISTRO
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Registrar Nuevo Sector",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = viewModel.nombreSector,
                            onValueChange = { 
                                viewModel.nombreSector = it
                                hasError = false 
                            },
                            label = { Text("Nombre del Sector") },
                            isError = hasError && viewModel.nombreSector.isBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = viewModel.descripcionSector,
                            onValueChange = { viewModel.descripcionSector = it },
                            label = { Text("Descripción / Ubicación") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (hasError && viewModel.nombreSector.isBlank()) {
                            Text(
                                text = "El nombre del sector no puede estar vacío.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (mensajeExito.isNotBlank()) {
                            Text(
                                text = mensajeExito,
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                if (viewModel.nombreSector.isBlank()) {
                                    hasError = true
                                } else {
                                    viewModel.registrarSector {
                                        mensajeExito = "¡Sector registrado con éxito!"
                                        hasError = false
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Guardar Sector")
                        }
                    }
                }
            }

            // SECCIÓN 2: PANEL DE SENSORES SIMULADOS
            item {
                Text(
                    text = "Monitoreo de Sensores (Simulación)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tarjeta de Temperatura
                    ElevatedCard(
                        modifier = Modifier.weight(1f)
                    ) {
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
                    ElevatedCard(
                        modifier = Modifier.weight(1f)
                    ) {
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

            // SECCIÓN 3: LISTA DE SECTORES REGISTRADOS
            item {
                Text(
                    text = "Sectores Registrados en la Base de Datos",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (sectores.isEmpty()) {
                item {
                    Text(
                        text = "No hay sectores registrados todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(sectores) { sector ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = sector.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sector.descripcion.ifBlank { "Sin descripción" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}